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

    public MyDialogBuilder setPositiveButton(@StringRes int textId, DialogInterface.OnClickListener listener) {
        this.positiveButtonText = context.getString(textId);
        this.positiveButtonListener = listener;
        return this;
    }

    public MyDialogBuilder setNegativeButton(@StringRes int textId, DialogInterface.OnClickListener listener) {
        this.negativeButtonText = context.getString(textId);
        this.negativeButtonListener = listener;
        return this;
    }

    public Dialog create() {
        return dialog;
    }

    public void installContent(View parentPanel) {
        dialog.findViewById(android.R.id.button1);
    }

    private static @StyleRes int resolveDialogTheme(Context context) {
        final TypedValue outValue = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.alertDialogTheme, outValue, true);
        return outValue.resourceId;
    }

    private static class ButtonHandler implements View.OnClickListener {
        private View positiveButton;
        private View negativeButton;

        public ButtonHandler(View positiveButton, View negativeButton) {
            this.positiveButton = positiveButton;
            this.negativeButton = negativeButton;
        }

        @Override
        public void onClick(View v) {
            if(v == positiveButton) {

            }
        }
    }
}
