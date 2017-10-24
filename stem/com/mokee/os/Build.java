package com.mokee.os;

import android.content.Context;

public class Build {

    public static final String RELEASE_TYPE = "NIGHTLY";
    public static final String PRODUCT = "cheeseburger";
    public static final String VERSION = "just-for-fun";

    public static String getUniqueID(Context context) {
        return "unique id";
    }

    public static String getUniqueID(Context context, int i) {
        return "unique id " + i;
    }

}
