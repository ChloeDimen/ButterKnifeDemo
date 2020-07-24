package com.dimen.butterknife;

import android.support.annotation.UiThread;

/**
 * 文件名：com.dimen.butterknife
 * 描    述：
 * 作    者：Dimen
 * 时    间：2020/7/24
 */
public interface Unbinder {
    @UiThread
    void unbind();

    Unbinder EMPTY = new Unbinder() {
        @Override
        public void unbind() {
        }
    };
}