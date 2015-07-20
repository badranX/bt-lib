package com.badran.library;

import android.util.Log;

/**
 * Created by a on 7/11/15.
 */
public class NativeBuffer {


    public static native void add();
    public  void contact() {

        Log.v("unity", "JAVA contacted C++");

    }
    static {
        // as defined by LOCAL_MODULE in Android.mk
        System.loadLibrary("libjavabridge");
    }
}
