package libre.sampler.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputLayout;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProviders;
import libre.sampler.R;
import libre.sampler.listeners.DefaultTextWatcher;
import libre.sampler.models.FileSelectResult;
import libre.sampler.models.Instrument;
import libre.sampler.models.InstrumentEvent;
import libre.sampler.models.Project;
import libre.sampler.models.ProjectViewModel;
import libre.sampler.tasks.ImportInstrumentTask;
import libre.sampler.utils.AppConstants;
import libre.sampler.utils.DatabaseConnectionManager;
import libre.sampler.views.MyDialogBuilder;

import static android.app.Activity.RESULT_OK;

public class InstrumentCreateDialog extends DialogFragment {
    public final String TAG = "InstrumentCreateDialog";

    private EditText nameInputView;
    private EditText pathInputView;
    private boolean importIsChecked;
    private final FileSelectResult importSampleZip = new FileSelectResult();

    private ProjectViewModel viewModel;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        MyDialogBuilder builder = new MyDialogBuilder(getActivity());
        LayoutInflater inflater = LayoutInflater.from(requireActivity());
        View rootView = inflater.inflate(R.layout.dialog_instrument_create, null);
        nameInputView = (EditText) rootView.findViewById(R.id.input_name);
        CheckBox importCheckBox = rootView.findViewById(R.id.import_checkbox);
        final ImageButton browseButton = rootView.findViewById(R.id.browse_button);
        Button submitButton = rootView.findViewById(R.id.submit_button);
        Button cancelButton = rootView.findViewById(R.id.cancel_button);

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
                    importPathInput.setVisibility(View.VISIBLE);
                    browseButton.setVisibility(View.VISIBLE);
                } else {
                    importPathInput.setVisibility(View.GONE);
                    browseButton.setVisibility(View.GONE);
                }
            }
        });

        pathInputView = rootView.findViewById(R.id.input_import_path);
        pathInputView.addTextChangedListener(new DefaultTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                importSampleZip.setStringValue(s.toString());
            }
        });

        browseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browse = new Intent(Intent.ACTION_GET_CONTENT);
                browse.setType("*/*");
                // browse.setType("application/zip");
                startActivityForResult(browse, AppConstants.FILE_REQUEST_INSTRUMENT_CREATE);
            }
        });

        builder.setContentView(rootView);
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = nameInputView.getText().toString();
                Instrument toCreate = viewModel.getDialogInstrument();
                toCreate.name = name;

                if(importIsChecked) {
                    if(!importSampleZip.canRead()) {
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

                    try {
                        DatabaseConnectionManager.runTask(new ImportInstrumentTask(
                                toCreate,
                                importSampleZip.openInputStream(getContext()),
                                extractDir,
                                new ImportTaskCallback(getDialog(), viewModel, viewModel.getProject(), toCreate)));
                    } catch(IOException e) {
                        Toast.makeText(getContext(), R.string.file_not_found, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    viewModel.getProject().addInstrument(toCreate);
                    viewModel.instrumentEventSource.dispatch(new InstrumentEvent(InstrumentEvent.INSTRUMENT_CREATE, toCreate));
                    viewModel.setDialogInstrument(null);
                    getDialog().dismiss();
                }
            }

        });

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getDialog().cancel();
            }
        });

        return builder.create();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(data != null && requestCode == AppConstants.FILE_REQUEST_INSTRUMENT_CREATE && resultCode == RESULT_OK) {
            Uri uri = data.getData();
            if(uri != null) {
                pathInputView.setText(uri.getPath());
                importSampleZip.setUriValue(uri);
            }
        }
    }

    @Nullable
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putString(AppConstants.TAG_SAVED_STATE_INSTRUMENT_CREATE_NAME, nameInputView.getText().toString());
    }

    private static class ImportTaskCallback implements Consumer<String> {
        private WeakReference<Dialog> dialogRef;
        private ProjectViewModel viewModel;
        private Project project;
        private Instrument instrument;

        public ImportTaskCallback(Dialog dialog, ProjectViewModel viewModel, Project project,
                                  Instrument instrument) {
            Log.d("ImportInstrumentTask", "start");
            this.dialogRef = new WeakReference<>(dialog);
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
