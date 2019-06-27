package libre.sampler.utils;

import android.util.Log;

public class IdStatus {
    public static final int INVALID = 0;
    public static final int SELF = 1;
    public static final int CHILDREN_DB = 2;
    public static final int CHILDREN_ADDED = 3;
    private static final boolean[] repeatsAllowed = new boolean[]{
            true,   // INVALID can be set multiple times
            false,  // SELF can be set once
            false,  // CHILDREN_DB can be set once
            true    // CHILDREN_ADDED can be set multiple times
    };

    private static boolean outputEnabled = false;

    private String tag;
    private int status = INVALID;

    public IdStatus(String tag) {
        this.tag = tag;
    }

    public void set(int newStatus) {
        if(this.status > newStatus || (this.status == newStatus && !repeatsAllowed[this.status])) {
            throw new AssertionError(String.format("Invalid ID status change: %d -> %d", this.status, newStatus));
        }
        if(outputEnabled && this.tag != null) {
            Log.d("IdStatus", String.format("ID status change: %s, %d -> %d", this.tag, this.status, newStatus));
        }
        this.status = newStatus;
    }

    public void require(int minStatus) {
        if(this.status < minStatus) {
            throw new AssertionError(String.format("Invalid ID status: %d, requires %d", this.status, minStatus));
        }
    }

    public static void setOutputEnabled(boolean enabled) {
        outputEnabled = enabled;
    }
}
