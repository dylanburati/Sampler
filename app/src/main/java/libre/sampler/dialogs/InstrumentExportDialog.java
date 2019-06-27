package libre.sampler.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.File;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProviders;
import libre.sampler.R;
import libre.sampler.models.ProjectViewModel;
import libre.sampler.tasks.ExportInstrumentTask;
import libre.sampler.utils.AppConstants;
import libre.sampler.utils.DatabaseConnectionManager;

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

                        final Toast toast0 = Toast.makeText(getActivity(), R.string.instrument_exported, Toast.LENGTH_SHORT);
                        final Toast toast1 = Toast.makeText(getActivity(), R.string.export_file_exists, Toast.LENGTH_SHORT);
                        final Toast toast2 = Toast.makeText(getActivity(), R.string.export_could_not_create, Toast.LENGTH_SHORT);
                        DatabaseConnectionManager.runTask(new ExportInstrumentTask(viewModel.getDialogInstrument(), outFile,
                                new Consumer<String>() {
                                    @Override
                                    public void accept(String message) {
                                        if(AppConstants.SUCCESS_EXPORT_INSTRUMENT.equals(message)) {
                                            toast0.show();
                                        } else if(AppConstants.ERROR_EXPORT_ZIP_EXISTS.equals(message)) {
                                            toast1.show();
                                        } else {
                                            toast2.show();
                                        }
                                    }
                                }));

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