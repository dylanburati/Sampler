package android.util;

public class Log {
    public synchronized static int d(String tag, String msg) {
        System.out.format("D/%s: %s\n", tag, msg);
        return 0;
    }
}
