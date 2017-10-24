package android.app;

import com.mokee.center.BuildConfig;

public class ActivityThread {

    public static String currentProcessName() {
        return BuildConfig.APPLICATION_ID;
    }

}
