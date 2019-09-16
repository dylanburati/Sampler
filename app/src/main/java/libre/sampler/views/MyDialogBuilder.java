package libre.sampler.views;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.annotation.StyleRes;

public class MyDialogBuilder {
    private Context context;
    private Dialog dialog;

    private CharSequence positiveButtonText;
    private DialogInterface.OnClickListener positiveButtonListener;

    private CharSequence negativeButtonText;
    private DialogInterface.OnClickListener negativeButtonListener;

    public MyDialogBuilder(Context context) {
        this.context = context;
        this.dialog = new Dialog(context, resolveDialogTheme(context));
        dialog.setCanceledOnTouchOutside(true);
    }

    public MyDialogBuilder setContentView(@NonNull View view) {
        dialog.setContentView(view);
        return this;
    }

    public MyDialogBuilder setTitle(@StringRes int titleId) {
        dialog.setTitle(titleId);
        return this;
    }

    public Dialog create() {
        return dialog;
    }

    private static @StyleRes int resolveDialogTheme(Context context) {
        final TypedValue outValue = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.alertDialogTheme, outValue, true);
        return outValue.resourceId;
    }
}
