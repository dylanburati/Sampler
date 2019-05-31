package android.util;

public class Log {
    private static boolean enabled = false;

    public static void setEnabled(boolean val) {
        enabled = val;
    }

    public static int d(String tag, String msg) {
        if(enabled) {
            System.out.format("D/%s: %s\n", tag, msg);
        }
        return 0;
    }
}
