package libre.sampler.utils;

import android.content.Context;
import android.content.Intent;

public class IntentFactory {
    private Context ctx;

    public IntentFactory(Context ctx) {
        this.ctx = ctx;
    }

    public Intent fromClass(Class<?> cls) {
        return new Intent(ctx, cls);
    }
}
