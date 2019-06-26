package libre.sampler.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.LinearLayout;

import java.io.File;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProviders;
import libre.sampler.R;
import libre.sampler.models.ProjectViewModel;
import libre.sampler.utils.AppConstants;

public class InstrumentExportDialog extends DialogFragment {
    private EditText exportPathInputView;
    private EditText nameInputView;

    private ProjectViewModel viewModel;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = LayoutInflater.from(requireActivity());
        LinearLayout rootView = (LinearLayout) inflater.inflate(R.layout.dialog_instrument_export, null);
        exportPathInputView = (EditText) rootView.findViewById(R.id.input_export_path);
        nameInputView = (EditText) rootView.findViewById(R.id.input_name);

        viewModel = ViewModelProviders.of(getActivity()).get(ProjectViewModel.class);

        if(savedInstanceState != null) {
            exportPathInputView.setText(savedInstanceState.getString(AppConstants.TAG_SAVED_STATE_INSTRUMENT_EXPORT_PATH));
            nameInputView.setText(savedInstanceState.getString(AppConstants.TAG_SAVED_STATE_INSTRUMENT_EXPORT_NAME));
        } else {
            exportPathInputView.setText(viewModel.getProject().getDefaultExportPath());
            nameInputView.setText(viewModel.getDialogInstrument().name);
        }

        builder.setView(rootView)
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                })
                .setPositiveButton(R.string.dialog_project_export_submit, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String path = exportPathInputView.getText().toString();
                        String filename = nameInputView.getText().toString();
                        if(!filename.endsWith(".zip")) {
                            filename += ".zip";
                        }
                        File outFile = new File(path, filename);
                        Log.d("InstrumentExportDialog", outFile.getAbsolutePath());

                        dialog.dismiss();
                    }
                });

        return builder.create();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putString(AppConstants.TAG_SAVED_STATE_INSTRUMENT_EXPORT_PATH, exportPathInputView.getText().toString());
        outState.putString(AppConstants.TAG_SAVED_STATE_INSTRUMENT_EXPORT_NAME, nameInputView.getText().toString());
    }
}