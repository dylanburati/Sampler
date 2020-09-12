package libre.sampler.dialogs;

import android.animation.ObjectAnimator;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.File;
import java.lang.ref.WeakReference;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProviders;
import libre.sampler.R;
import libre.sampler.models.ProjectViewModel;
import libre.sampler.tasks.ExportInstrumentTask;
import libre.sampler.utils.AppConstants;
import libre.sampler.utils.DatabaseConnectionManager;
import libre.sampler.utils.ModelState;
import libre.sampler.views.MyDialogBuilder;

public class InstrumentExportDialog extends DialogFragment {
    private EditText exportPathInputView;
    private EditText nameInputView;

    private ProjectViewModel viewModel;
    private ProgressBar progressBar;
    private ModelState exportingState = ModelState.INVALID;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        MyDialogBuilder builder = new MyDialogBuilder(getActivity());
        LayoutInflater inflater = LayoutInflater.from(requireActivity());
        ConstraintLayout rootView = (ConstraintLayout) inflater.inflate(R.layout.dialog_instrument_export, null);
        exportPathInputView = rootView.findViewById(R.id.input_export_path);
        nameInputView = rootView.findViewById(R.id.input_name);
        Button submitButton = rootView.findViewById(R.id.submit_button);
        Button cancelButton = rootView.findViewById(R.id.cancel_button);
        progressBar = rootView.findViewById(R.id.progress_bar);

        viewModel = ViewModelProviders.of(getActivity()).get(ProjectViewModel.class);

        if(savedInstanceState != null) {
            exportPathInputView.setText(savedInstanceState.getString(AppConstants.TAG_SAVED_STATE_INSTRUMENT_EXPORT_PATH));
            nameInputView.setText(savedInstanceState.getString(AppConstants.TAG_SAVED_STATE_INSTRUMENT_EXPORT_NAME));
        } else {
            exportPathInputView.setText(viewModel.getProject().getDefaultExportPath());
            nameInputView.setText(viewModel.getDialogInstrument().name);
        }

        builder.setContentView(rootView);

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(exportingState == ModelState.INVALID) {
                    String path = exportPathInputView.getText().toString();
                    String filename = nameInputView.getText().toString();
                    if(!filename.endsWith(".zip")) {
                        filename += ".zip";
                    }
                    File outFile = new File(path, filename);

                    progressBar.setVisibility(View.VISIBLE);
                    exportingState = ModelState.LOADING;
                    DatabaseConnectionManager.runTask(new ExportInstrumentTask(viewModel.getDialogInstrument(), outFile,
                            new ExportTaskCallback(InstrumentExportDialog.this)));
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
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putString(AppConstants.TAG_SAVED_STATE_INSTRUMENT_EXPORT_PATH, exportPathInputView.getText().toString());
        outState.putString(AppConstants.TAG_SAVED_STATE_INSTRUMENT_EXPORT_NAME, nameInputView.getText().toString());
    }

    private static class ExportTaskCallback implements ExportInstrumentTask.Callbacks {
        private final WeakReference<InstrumentExportDialog> dialogRef;

        public ExportTaskCallback(InstrumentExportDialog aThis) {
            this.dialogRef = new WeakReference<>(aThis);
        }

        @Override
        public void onProgressUpdate(float progress) {
            InstrumentExportDialog dialog = dialogRef.get();
            if(dialog != null) {
                ProgressBar bar = dialog.progressBar;
                ObjectAnimator anim = ObjectAnimator.ofInt(bar, "progress", Math.round(progress * bar.getMax()));
                anim.setAutoCancel(true);
                anim.setDuration(150);
                anim.setInterpolator(new DecelerateInterpolator());
                anim.start();
            }
        }

        @Override
        public void onPostExecute(String message) {
            InstrumentExportDialog dialog = dialogRef.get();
            if(dialog != null && dialog.getDialog().isShowing()) {
                // Progress bar shows completion
                if(AppConstants.SUCCESS_EXPORT_INSTRUMENT.equals(message)) {
                    dialog.dismiss();
                    return;
                } else {
                    dialog.progressBar.setVisibility(View.GONE);
                    dialog.exportingState = ModelState.INVALID;
                }

                Context ctx = dialog.getContext();
                if(AppConstants.SUCCESS_EXPORT_INSTRUMENT.equals(message)) {
                    Toast.makeText(ctx, R.string.instrument_exported, Toast.LENGTH_SHORT).show();
                } else if(AppConstants.ERROR_EXPORT_ZIP_EXISTS.equals(message)) {
                    Toast.makeText(ctx, R.string.export_file_exists, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(ctx, R.string.export_could_not_create, Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}