package com.techtweaking.libextra;

import android.util.Log;

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;

public class IOUtils {
    private static final String TAG = "PLUGIN . UNITY";
    public static void flushQuietly(Flushable input) {
        try {
            if (input != null) {
                input.flush();
            }
        } catch (IOException e) {
            Log.w(TAG, "Failure to flush stream: ", e);
        }
    }
    public static void closeQuietly(Closeable input) {
        try {
            if (input != null) {
                input.close();
            }
        } catch (IOException e) {
            Log.w(TAG, "Failure to close stream: ", e);
        }
    }
}