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
import libre.sampler.models.MainViewModel;
import libre.sampler.models.Project;
import libre.sampler.tasks.ImportProjectTask;
import libre.sampler.utils.AppConstants;
import libre.sampler.utils.DatabaseConnectionManager;
import libre.sampler.utils.ModelState;
import libre.sampler.views.MyDialogBuilder;

import static android.content.Context.MODE_PRIVATE;

public class ProjectImportDialog extends DialogFragment {
    private EditText downloadUrlInputView;
    private EditText nameInputView;

    private MainViewModel viewModel;
    private ProgressBar progressBar;
    private ModelState importingState = ModelState.INVALID;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        MyDialogBuilder builder = new MyDialogBuilder(getActivity());
        LayoutInflater inflater = LayoutInflater.from(requireActivity());
        ConstraintLayout rootView = (ConstraintLayout) inflater.inflate(R.layout.dialog_project_import, null);
        downloadUrlInputView = rootView.findViewById(R.id.input_download_url);
        nameInputView = rootView.findViewById(R.id.input_name);
        Button submitButton = rootView.findViewById(R.id.submit_button);
        Button cancelButton = rootView.findViewById(R.id.cancel_button);
        progressBar = rootView.findViewById(R.id.progress_bar);

        viewModel = ViewModelProviders.of(getActivity()).get(MainViewModel.class);

        if(savedInstanceState != null) {
            downloadUrlInputView.setText(savedInstanceState.getString(AppConstants.TAG_SAVED_STATE_PROJECT_IMPORT_URL));
            nameInputView.setText(savedInstanceState.getString(AppConstants.TAG_SAVED_STATE_PROJECT_IMPORT_NAME));
        } else {
            downloadUrlInputView.setText(AppConstants.SERIALIZED_PROJECT_URL);
        }

        builder.setContentView(rootView);

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(importingState == ModelState.INVALID) {
                    String downloadUrl = downloadUrlInputView.getText().toString();
                    String name = nameInputView.getText().toString();

                    final Project toCreate = new Project(name, System.currentTimeMillis());
                    File dataDir = getContext().getDir("data", MODE_PRIVATE);
                    progressBar.setVisibility(View.VISIBLE);
                    importingState = ModelState.LOADING;
                    DatabaseConnectionManager.runTask(new ImportProjectTask(
                            toCreate,
                            downloadUrl,
                            dataDir,
                            new ImportTaskCallback(viewModel, toCreate, ProjectImportDialog.this)));
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
        outState.putString(AppConstants.TAG_SAVED_STATE_INSTRUMENT_EXPORT_PATH, downloadUrlInputView.getText().toString());
        outState.putString(AppConstants.TAG_SAVED_STATE_INSTRUMENT_EXPORT_NAME, nameInputView.getText().toString());
    }

    private static class ImportTaskCallback implements ImportProjectTask.Callbacks {
        private final MainViewModel viewModel;
        private final Project project;
        private final WeakReference<ProjectImportDialog> dialogRef;

        public ImportTaskCallback(MainViewModel viewModel, Project project, ProjectImportDialog aThis) {
            this.viewModel = viewModel;
            this.project = project;
            this.dialogRef = new WeakReference<>(aThis);
        }

        @Override
        public void onProgressUpdate(float progress) {
            ProjectImportDialog dialog = dialogRef.get();
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
            ProjectImportDialog dialog = dialogRef.get();
            if(dialog != null && dialog.getDialog().isShowing()) {
                // Progress bar shows completion
                if(AppConstants.SUCCESS_IMPORT_PROJECT.equals(message)) {
                    viewModel.addImportedProject(project);
                    dialog.dismiss();
                    return;
                } else {
                    dialog.progressBar.setVisibility(View.GONE);
                    dialog.importingState = ModelState.INVALID;
                }

                Context ctx = dialog.getContext();
                if(AppConstants.SUCCESS_IMPORT_PROJECT.equals(message)) {
                    Toast.makeText(ctx, R.string.project_imported, Toast.LENGTH_SHORT).show();
                } else if(AppConstants.ERROR_COULD_NOT_CONNECT.equals(message)) {
                    Toast.makeText(ctx, R.string.import_could_not_connect, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(ctx, R.string.import_failed, Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}