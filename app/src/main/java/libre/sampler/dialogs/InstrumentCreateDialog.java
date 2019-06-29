package libre.sampler.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputLayout;

import java.io.File;
import java.lang.ref.WeakReference;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.util.Consumer;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProviders;
import libre.sampler.R;
import libre.sampler.listeners.DefaultTextWatcher;
import libre.sampler.models.Instrument;
import libre.sampler.models.InstrumentEvent;
import libre.sampler.models.Project;
import libre.sampler.models.ProjectViewModel;
import libre.sampler.tasks.ImportInstrumentTask;
import libre.sampler.utils.AppConstants;
import libre.sampler.utils.DatabaseConnectionManager;

public class InstrumentCreateDialog extends DialogFragment {
    private EditText nameInputView;
    private boolean importIsChecked;
    private String importPath = "";

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
        ConstraintLayout rootView = (ConstraintLayout) inflater.inflate(R.layout.dialog_instrument_create, null);
        nameInputView = (EditText) rootView.findViewById(R.id.input_name);
        CheckBox importCheckBox = rootView.findViewById(R.id.import_checkbox);

        viewModel = ViewModelProviders.of(getActivity()).get(ProjectViewModel.class);

        if(savedInstanceState != null) {
            nameInputView.setText(savedInstanceState.getString(AppConstants.TAG_SAVED_STATE_INSTRUMENT_CREATE_NAME));
        }

        final TextInputLayout importPathInput = rootView.findViewById(R.id.input_import_path_container);
        importCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                importIsChecked = isChecked;
                if(isChecked) {
                    // ((AlertDialog) InstrumentCreateDialog.this.getDialog()).getButton(Dialog.BUTTON_POSITIVE);
                    importPathInput.setVisibility(View.VISIBLE);
                } else {
                    importPathInput.setVisibility(View.GONE);
                }
            }
        });

        ((EditText) rootView.findViewById(R.id.input_import_path)).addTextChangedListener(new DefaultTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                importPath = s.toString();
            }
        });

        builder.setView(rootView)
                .setPositiveButton(R.string.dialog_project_create_submit, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String name = nameInputView.getText().toString();
                        Instrument toCreate = viewModel.getDialogInstrument();
                        toCreate.name = name;

                        if(importIsChecked) {
                            if(importPath.isEmpty()) {
                                Toast.makeText(getContext(), R.string.file_not_found, Toast.LENGTH_SHORT).show();
                                return;
                            }
                            File inFile = new File(importPath);
                            if(!inFile.isFile() || !inFile.canRead()) {
                                Toast.makeText(getContext(), R.string.file_not_found, Toast.LENGTH_SHORT).show();
                                return;
                            }
                            String extractPath = viewModel.getProject().getDefaultSamplePath();
                            File extractDir = getContext().getDir("data", Context.MODE_PRIVATE);
                            if(!extractPath.isEmpty()) {
                                File sampleDir = new File(extractPath);
                                if(sampleDir.isDirectory() && sampleDir.canWrite()) {
                                    extractDir = sampleDir;
                                }
                            }

                            DatabaseConnectionManager.runTask(new ImportInstrumentTask(toCreate, inFile, extractDir,
                                    new ImportTaskCallback(new WeakReference<>(getDialog()), viewModel, viewModel.getProject(), toCreate)));
                        } else {
                            viewModel.getProject().addInstrument(toCreate);
                            viewModel.instrumentEventSource.dispatch(new InstrumentEvent(InstrumentEvent.INSTRUMENT_CREATE, toCreate));
                            viewModel.setDialogInstrument(null);
                            dialog.dismiss();
                        }
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

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putString(AppConstants.TAG_SAVED_STATE_INSTRUMENT_CREATE_NAME, nameInputView.getText().toString());
    }

    private static class ImportTaskCallback implements Consumer<String> {
        private WeakReference<Dialog> dialogRef;
        private ProjectViewModel viewModel;
        private Project project;
        private Instrument instrument;

        public ImportTaskCallback(WeakReference<Dialog> dialogRef, ProjectViewModel viewModel,
                                  Project project, Instrument instrument) {
            Log.d("ImportInstrumentTask", "start");
            this.dialogRef = dialogRef;
            this.viewModel = viewModel;
            this.project = project;
            this.instrument = instrument;
        }

        @Override
        public void accept(String message) {
            Log.d("ImportInstrumentTask", "finish");
            Dialog dialog = dialogRef.get();
            if(AppConstants.SUCCESS_IMPORT_INSTRUMENT.equals(message)) {
                project.addInstrument(instrument);
                viewModel.instrumentEventSource.dispatch(new InstrumentEvent(InstrumentEvent.INSTRUMENT_CREATE, instrument));
                viewModel.setDialogInstrument(null);
                if(dialog != null) {
                    dialog.dismiss();
                }
            }
        }
    }
}
