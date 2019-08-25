package libre.sampler.fragments;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.core.content.ContextCompat;
import androidx.core.util.Consumer;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.RecyclerView;
import libre.sampler.MainActivity;
import libre.sampler.ProjectActivity;
import libre.sampler.R;
import libre.sampler.adapters.ProjectListAdapter;
import libre.sampler.models.MainViewModel;
import libre.sampler.models.Project;
import libre.sampler.models.ProjectEvent;
import libre.sampler.tasks.CleanFilesTask;
import libre.sampler.tasks.DeleteProjectsTask;
import libre.sampler.utils.AdapterLoader;
import libre.sampler.utils.AppConstants;
import libre.sampler.utils.DatabaseConnectionManager;

import static android.content.Context.MODE_PRIVATE;

public class MainProjectsFragment extends Fragment {
    public static final String TAG = "ProjectInstrumentsFragment";
    private RecyclerView projectListView;
    private ProjectListAdapter projectListAdapter;
    private MainViewModel viewModel;

    private boolean willShowDialogCreateProject;
    private boolean isAdapterLoaded;
    private View rootView;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_main_projects, container, false);

        viewModel = ViewModelProviders.of(getActivity()).get(MainViewModel.class);
        ActionBar actionBar = ((MainActivity) getActivity()).getSupportActionBar();
        if(actionBar != null) {
            actionBar.setTitle(R.string.app_name);
        }

        initUI();

        viewModel.loadEventSource.add(TAG, new Consumer<String>() {
            @Override
            public void accept(String eventName) {
                if(eventName.equals(AppConstants.PROJECTS_LOADED)) {
                    updateAdapter();
                }
            }
        });
        viewModel.projectEventSource.add(TAG, new Consumer<ProjectEvent>() {
            @Override
            public void accept(final ProjectEvent event) {
                if(event.action == ProjectEvent.PROJECT_CREATE) {
                    AdapterLoader.insertItem(projectListAdapter, 0, event.project);
                } else if(event.action == ProjectEvent.PROJECT_EDIT) {
                    int changeIdx = projectListAdapter.items.indexOf(event.project);
                    if(changeIdx != -1) {
                        projectListAdapter.notifyItemChanged(changeIdx);
                    }
                } else if(event.action == ProjectEvent.PROJECT_DELETE) {
                    AdapterLoader.removeItem(projectListAdapter, event.project);
                }
            }
        });
        isAdapterLoaded = false;
        updateAdapter();

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if(willShowDialogCreateProject) {
            ((MainActivity) getActivity()).openDetailFragment(ProjectEvent.PROJECT_CREATE, null);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        this.rootView = null;
        this.projectListView = null;
        viewModel.loadEventSource.remove(TAG);
        viewModel.projectEventSource.remove(TAG);
    }

    private void initUI() {
        this.projectListView = rootView.findViewById(R.id.projects_select);
        this.projectListAdapter = new ProjectListAdapter(new ArrayList<Project>(),
                new MyProjectActionConsumer());
        projectListView.setAdapter(this.projectListAdapter);
    }

    private void updateAdapter() {
        List<Project> projects = viewModel.getProjects();
        if(!isAdapterLoaded && projects != null) {
            AdapterLoader.insertAll(projectListAdapter, viewModel.getProjects());
            isAdapterLoaded = true;
        }
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_main, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.appbar_create) {
            if(requireReadWritePermissions()) {
                ((MainActivity) getActivity()).openDetailFragment(ProjectEvent.PROJECT_CREATE, null);
            }
            return true;
        } else if(item.getItemId() == R.id.appbar_delete) {
            DatabaseConnectionManager.runTask(new DeleteProjectsTask(projectListAdapter.items, new Runnable() {
                @Override
                public void run() {
                    AdapterLoader.clear(projectListAdapter);
                }
            }));
            return true;
        } else if(item.getItemId() == R.id.appbar_clean) {
            File dataDir = getContext().getDir("data", MODE_PRIVATE);
            DatabaseConnectionManager.runTask(new CleanFilesTask(dataDir, getContext()));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean requireReadWritePermissions() {
        boolean granted = true;

        if(ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            granted = false;
        } else if(ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            granted = false;
        }

        if(!granted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE}, AppConstants.PERM_REQUEST_READ_EXTERNAL_STORAGE);
        }
        return granted;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch(requestCode) {
            case AppConstants.PERM_REQUEST_READ_EXTERNAL_STORAGE:
                if(grantResults.length >= 2 && grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                        grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    willShowDialogCreateProject = true;
                }
                break;
            default:
                break;
        }
    }

    private class MyProjectActionConsumer implements ProjectListAdapter.ProjectActionConsumer {
        @Override
        public void startRename(Project project) {
            ((MainActivity) getActivity()).openDetailFragment(ProjectEvent.PROJECT_EDIT, project);
        }

        @Override
        public void open(Project project) {
            Activity activity = getActivity();
            if(activity != null) {
                Intent intent = new Intent(getActivity(), ProjectActivity.class);
                intent.setAction(Intent.ACTION_VIEW);
                intent.putExtra(Intent.EXTRA_TITLE, project.name);
                intent.putExtra(AppConstants.TAG_EXTRA_PROJECT_ID, project.id);
                if(intent.resolveActivity(activity.getPackageManager()) != null) {
                    startActivity(intent);
                }
            }
        }

        @Override
        public void delete(final Project project) {
            viewModel.removeProject(project);
        }
    }
}
