package libre.sampler.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import libre.sampler.R;

public class ProjectCreateDialog extends DialogFragment {
    public interface ProjectCreateDialogListener {
        public void onSubmitProjectCreate(String projectName);
    }

    private ProjectCreateDialogListener listener;
    private EditText nameInputView;


    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        listener = (ProjectCreateDialogListener) context;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = LayoutInflater.from(requireActivity());
        LinearLayout rootView = (LinearLayout) inflater.inflate(R.layout.dialog_main_create, null);
        nameInputView = (EditText) rootView.getChildAt(1);
        builder.setView(rootView)
                .setPositiveButton(R.string.dialog_create_submit, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(listener != null) listener.onSubmitProjectCreate(nameInputView.getText().toString());
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(R.string.dialog_create_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

        return builder.create();
    }
}
