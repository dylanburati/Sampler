package libre.sampler.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import java.lang.ref.WeakReference;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProviders;
import libre.sampler.R;
import libre.sampler.models.ProjectViewModel;
import libre.sampler.tasks.UpdateProjectTask;
import libre.sampler.utils.AppConstants;
import libre.sampler.utils.DatabaseConnectionManager;

public class ProjectLeaveDialog extends DialogFragment {
    private ProjectViewModel viewModel;
    private Intent upIntent;

    public void setUpIntent(Intent upIntent) {
        this.upIntent = upIntent;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = LayoutInflater.from(requireActivity());
        viewModel = ViewModelProviders.of(getActivity()).get(ProjectViewModel.class);

        View msgView = inflater.inflate(R.layout.dialog_project_leave, null);

        builder.setView(msgView);
        builder.setNeutralButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });

        builder.setNegativeButton(R.string.discard, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(upIntent != null) {
                    upIntent.putExtra(AppConstants.TAG_EXTRA_PROJECT_DISCARD, true);
                    ((AppCompatActivity) getActivity()).supportNavigateUpTo(upIntent);
                }
            }
        });

        builder.setPositiveButton(R.string.action_save, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                viewModel.updateProjectHash();
                DatabaseConnectionManager.runTask(new UpdateProjectTask(viewModel.getProject(),
                        new UpdateProjectTaskCallback((AppCompatActivity) getActivity(), upIntent)));
            }
        });

        return builder.create();
    }

    public static class UpdateProjectTaskCallback implements Runnable {
        private final WeakReference<AppCompatActivity> contextRef;
        private final Intent upIntent;

        public UpdateProjectTaskCallback(AppCompatActivity context, Intent upIntent) {
            this.contextRef = new WeakReference<>(context);
            this.upIntent = upIntent;
        }

        @Override
        public void run() {
            if(this.contextRef.get() != null) {
                Toast.makeText(this.contextRef.get(), R.string.project_saved, Toast.LENGTH_SHORT).show();
                if(upIntent != null) {
                    upIntent.putExtra(AppConstants.TAG_EXTRA_PROJECT_DISCARD, true);
                    this.contextRef.get().supportNavigateUpTo(upIntent);
                }
            }
        }
    }
}
