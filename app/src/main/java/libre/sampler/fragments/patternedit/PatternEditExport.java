package libre.sampler.fragments.patternedit;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;
import java.lang.ref.WeakReference;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import libre.sampler.R;
import libre.sampler.models.Pattern;
import libre.sampler.models.ProjectViewModel;
import libre.sampler.tasks.ExportPatternTask;
import libre.sampler.utils.AppConstants;
import libre.sampler.utils.DatabaseConnectionManager;

public class PatternEditExport extends Fragment {
    private ProjectViewModel viewModel;

    private View rootView;
    private EditText exportPathInputView;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_pattern_edit_export, container, false);
        viewModel = ViewModelProviders.of(getActivity()).get(ProjectViewModel.class);

        exportPathInputView = rootView.findViewById(R.id.input_export_path);

        updateControl();

        rootView.findViewById(R.id.submit_export).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String path = exportPathInputView.getText().toString();
                if(!path.endsWith(".mid") || path.endsWith(".midi")) {
                    path += ".midi";
                }
                File outFile = new File(path);
                if(outFile.isFile()) {
                    Toast.makeText(getContext(), R.string.export_file_exists, Toast.LENGTH_SHORT).show();
                    return;
                }

                Pattern pianoRollPattern = viewModel.getPianoRollPattern();

                DatabaseConnectionManager.runTask(new ExportPatternTask(pianoRollPattern,
                        viewModel.getPatternDerivedData(pianoRollPattern),
                        outFile,
                        new ExportTaskCallback(getActivity())));
            }
        });

        return rootView;
    }

    @Override
    public void onDestroyView() {
        setEnterTransition(null);
        setExitTransition(null);
        setReenterTransition(null);
        super.onDestroyView();
        this.rootView = null;
        this.exportPathInputView = null;
    }

    private void updateControl() {
        exportPathInputView.setText(viewModel.getProject().getDefaultExportPath());
    }

    private static class ExportTaskCallback implements ExportPatternTask.Callbacks {
        private WeakReference<Context> contextRef;

        public ExportTaskCallback(Context context) {
            this.contextRef = new WeakReference<>(context);
        }

        @Override
        public void onPostExecute(String message) {
            if(this.contextRef.get() != null) {
                if(AppConstants.SUCCESS_EXPORT_PATTERN.equals(message)) {
                    Toast.makeText(this.contextRef.get(), R.string.pattern_exported, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this.contextRef.get(), R.string.export_could_not_create, Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}
