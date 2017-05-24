/**
 * Created by tim-azul on 2/28/17.
 */

package com.m87.sam.ui.util;

import android.util.Log;


public class Logger {
    public static String mTag = "SAM";

    public static void debug(String format, Object... args) {
        Log.d(mTag, String.format(format, args));
    }

    public static void debug(String message) {
        Log.d(mTag, message);
    }

    public static void error(String error) {
        Log.e(mTag, error);
    }
}
