package com.dimen.butterknife;

import android.app.Activity;

import java.lang.reflect.Constructor;

/**
 * 文件名：com.dimen.butterknife
 * 描    述：
 * 作    者：Dimen
 * 时    间：2020/7/24
 */
public class ButterKnife {
    public static Unbinder bind(Activity activity) {
          // xxxActivity_ViewBinding viewBinding = new xxxActivity_ViewBinding(this);
        try {
            Class<? extends Unbinder> bindClassName = (Class<? extends Unbinder>)
                    Class.forName(activity.getClass().getName() + "_ViewBinding");
            // 构造函数
            Constructor<? extends Unbinder> bindConstructor = bindClassName.getDeclaredConstructor(activity.getClass());

            Unbinder unbinder = bindConstructor.newInstance(activity);
            // 返回 Unbinder
            return unbinder;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return Unbinder.EMPTY;

    }
}