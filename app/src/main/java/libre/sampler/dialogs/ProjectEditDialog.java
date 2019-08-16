package libre.sampler.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProviders;
import libre.sampler.R;
import libre.sampler.models.MainViewModel;
import libre.sampler.models.ProjectEvent;

public class ProjectEditDialog extends DialogFragment {
    private MainViewModel viewModel;

    private EditText nameInputView;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        viewModel = ViewModelProviders.of(getActivity()).get(MainViewModel.class);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = LayoutInflater.from(requireActivity());
        LinearLayout rootView = (LinearLayout) inflater.inflate(R.layout.dialog_project_create, null);
        ((TextView) rootView.findViewById(R.id.title)).setText(R.string.dialog_project_edit_title);
        nameInputView = (EditText) rootView.findViewById(R.id.input_name);

        nameInputView.setText(viewModel.getDialogProject().name);

        builder.setView(rootView)
                .setPositiveButton(R.string.done, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        viewModel.getDialogProject().name = nameInputView.getText().toString();
                        viewModel.projectEventSource.dispatch(new ProjectEvent(ProjectEvent.PROJECT_EDIT, viewModel.getDialogProject()));
                        viewModel.setDialogProject(null);
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

        return builder.create();
    }
}
