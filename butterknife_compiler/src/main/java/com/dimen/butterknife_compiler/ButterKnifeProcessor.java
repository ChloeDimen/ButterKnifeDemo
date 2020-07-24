package com.dimen.butterknife_compiler;

import com.dimen.butterknife_annotations.BindView;
import com.dimen.butterknife_annotations.OnClick;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;


import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;

import static java.util.Objects.requireNonNull;

//一定要添加，否则不会自动生成代码
@AutoService(Processor.class)
public class ButterKnifeProcessor extends AbstractProcessor {

    private Filer mFiler;
    private Elements mElementUtils;


    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        mFiler = processingEnvironment.getFiler();
        mElementUtils = processingEnvironment.getElementUtils();
    }

    //指定处理的版本
    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }


    //给到需要处理的注解
    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> types = new LinkedHashSet<>();
        for (Class<? extends Annotation> annotation : getSupportedAnnotations()) {
            types.add(annotation.getCanonicalName());
        }
        return types;
    }

    private Set<Class<? extends Annotation>> getSupportedAnnotations() {
        Set<Class<? extends Annotation>> annotations = new LinkedHashSet<>();
        // 需要解析的自定义注解 BindView  OnClick
        annotations.add(BindView.class);
        annotations.add(OnClick.class);
        return annotations;
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        System.out.println("------->");
        System.out.println("------->");
        System.out.println("------->");
        System.out.println("------->");
        //process方法代表的是有注解就会都进来，到那时这里面是一团乱麻

        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(BindView.class);
       /* for (Element element : elements) {

            Element enclosingElement = element.getEnclosingElement();
            System.out.println("------->"+element.getSimpleName().toString()+"----"+enclosingElement.getSimpleName().toString());
        }*/
        //解析属性activity->list<element>
        Map<Element, List<Element>> elementListMap = new LinkedHashMap<>();
        for (Element element : elements) {
            Element enclosingElement = element.getEnclosingElement();
            List<Element> viewBindElement = elementListMap.get(enclosingElement);
            if (viewBindElement == null) {
                viewBindElement = new ArrayList<>();
                elementListMap.put(enclosingElement, viewBindElement);

            }
            //private提示错误
            Set<Modifier> modifiers = element.getModifiers();
            if (modifiers.contains(Modifier.PRIVATE)) {
                String message = String.format("must not be private", element.getSimpleName().toString());
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, message, element);
            } else {
                viewBindElement.add(element);
            }
        }
        //生成代码
        for (Map.Entry<Element, List<Element>> entry : elementListMap.entrySet()) {
            Element enclosingElement = entry.getKey();
            List<Element> viewBindElements = entry.getValue();

            //拼接 public final class xxxActivity_ViewBinding implements Unbinder
            String activityClassNameStr = enclosingElement.getSimpleName().toString();//activity的名称
            ClassName activityClassName = ClassName.bestGuess(activityClassNameStr);
            ClassName unbinderClassName = ClassName.get("com.dimen.butterknife", "Unbinder");
            TypeSpec.Builder classBuilder = TypeSpec.classBuilder(activityClassNameStr + "_ViewBinding")
                    .addModifiers(Modifier.FINAL, Modifier.PUBLIC).addSuperinterface(unbinderClassName)
                    .addField(activityClassName, "target", Modifier.PRIVATE);


            // 实现 unbind 方法
            // android.support.annotation.CallSuper
            ClassName callSuperClassName = ClassName.get("android.support.annotation", "CallSuper");
            MethodSpec.Builder unbindMethodBuilder = MethodSpec.methodBuilder("unbind")
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                    .addAnnotation(callSuperClassName);

            unbindMethodBuilder.addStatement("$T target = this.target", activityClassName);
            unbindMethodBuilder.addStatement("if (target == null) throw new IllegalStateException(\"Bindings already cleared.\");");

            // 构造函数
            MethodSpec.Builder constructorMethodBuilder = MethodSpec.constructorBuilder()
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(activityClassName, "target");
            // this.target = target;
            constructorMethodBuilder.addStatement("this.target = target");
            // findViewById 属性
            for (Element viewBindElement : viewBindElements) {
                // target.textView1 = Utils.findRequiredViewAsType(source, R.id.tv1, "field 'textView1'", TextView.class);
                // target.textView1 = Utils.findViewById(source, R.id.tv1);
                String filedName = viewBindElement.getSimpleName().toString();
                ClassName utilsClassName = ClassName.get("com.dimen.butterknife", "Utils");
                //int resId = viewBindElement.getAnnotation(BindView.class).value();
                //constructorMethodBuilder.addStatement("target.$L = $T.findViewById(target, $L)", filedName, utilsClassName, resId);
                //显示R.id.xxx的id
                ClassName idName = ClassName.get("R.id", viewBindElement.getSimpleName().toString());
                constructorMethodBuilder.addStatement("target.$L = $T.findViewById(target, $L)", filedName, utilsClassName, idName);

                // target.textView1 = null;
                unbindMethodBuilder.addStatement("target.$L = null", filedName);
            }


            classBuilder.addMethod(unbindMethodBuilder.build());
            classBuilder.addMethod(constructorMethodBuilder.build());

            // 生成类，看下效果
            try {
                //获取包名
                String packageName = mElementUtils.getPackageOf(enclosingElement).getQualifiedName().toString();

                JavaFile.builder(packageName, classBuilder.build())
                        .addFileComment("butterknife 自动生成")
                        .build().writeTo(mFiler);
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("翻车了！");
            }
        }

        return false;
    }


}