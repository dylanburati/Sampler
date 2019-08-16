package libre.sampler;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.util.Consumer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.RecyclerView;
import libre.sampler.adapters.ProjectListAdapter;
import libre.sampler.dialogs.ProjectCreateDialog;
import libre.sampler.dialogs.ProjectEditDialog;
import libre.sampler.models.MainViewModel;
import libre.sampler.models.Project;
import libre.sampler.models.ProjectEvent;
import libre.sampler.tasks.CleanFilesTask;
import libre.sampler.tasks.CreateProjectTask;
import libre.sampler.tasks.DeleteProjectsTask;
import libre.sampler.utils.AdapterLoader;
import libre.sampler.utils.AppConstants;
import libre.sampler.utils.DatabaseConnectionManager;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = "MainActivity";
    private RecyclerView projectListView;
    private ProjectListAdapter projectListAdapter;
    private boolean isAdapterLoaded;
    private MainViewModel viewModel;
    private boolean willShowDialogCreateProject;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewModel = ViewModelProviders.of(this).get(MainViewModel.class);

        initUI();

        viewModel.loadEventSource.add(TAG, new Consumer<String>() {
            @Override
            public void accept(String eventName) {
                if(eventName.equals(AppConstants.PROJECTS_LOADED)) {
                    updateAdapter(viewModel.getProjects());
                }
            }
        });
        viewModel.projectEventSource.add(TAG, new Consumer<ProjectEvent>() {
            @Override
            public void accept(final ProjectEvent event) {
                if(event.action == ProjectEvent.PROJECT_CREATE) {
                    DatabaseConnectionManager.initialize(MainActivity.this);
                    DatabaseConnectionManager.runTask(new CreateProjectTask(event.project));
                    AdapterLoader.insertItem(projectListAdapter, 0, event.project);
                } else if(event.action == ProjectEvent.PROJECT_EDIT) {
                    int changeIdx = projectListAdapter.items.indexOf(event.project);
                    if(changeIdx != -1) {
                        projectListAdapter.notifyItemChanged(changeIdx);
                    }
                    DatabaseConnectionManager.initialize(MainActivity.this);
                    DatabaseConnectionManager.execute(new Runnable() {
                        @Override
                        public void run() {
                            DatabaseConnectionManager.getInstance().projectDao().updateAll(event.project);
                        }
                    });
                }
            }
        });
        isAdapterLoaded = false;
        updateAdapter(viewModel.getProjects());
    }

    private void initUI() {
        this.projectListView = (RecyclerView) findViewById(R.id.main_data);
        this.projectListAdapter = new ProjectListAdapter(new ArrayList<Project>(),
                new MyProjectActionConsumer());
        projectListView.setAdapter(this.projectListAdapter);
    }

    private void updateAdapter(List<Project> projects) {
        if(!isAdapterLoaded && projects != null) {
            AdapterLoader.insertAll(projectListAdapter, viewModel.getProjects());
            isAdapterLoaded = true;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(projectListView == null) {
            initUI();
        }

        if(willShowDialogCreateProject) {
            willShowDialogCreateProject = false;
            ProjectCreateDialog dialog = new ProjectCreateDialog();
            dialog.show(getSupportFragmentManager(), "ProjectCreateDialog");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        viewModel.loadEventSource.remove(TAG);
        viewModel.projectEventSource.remove(TAG);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.appbar_create) {
            if(requireReadWritePermissions()) {
                ProjectCreateDialog dialog = new ProjectCreateDialog();
                dialog.show(getSupportFragmentManager(), "ProjectCreateDialog");
            }
            return true;
        } else if(item.getItemId() == R.id.appbar_delete) {
            DatabaseConnectionManager.initialize(this);
            DatabaseConnectionManager.runTask(new DeleteProjectsTask(projectListAdapter.items, new Runnable() {
                @Override
                public void run() {
                    AdapterLoader.clear(projectListAdapter);
                }
            }));
            return true;
        } else if(item.getItemId() == R.id.appbar_clean) {
            DatabaseConnectionManager.initialize(this);
            DatabaseConnectionManager.runTask(new CleanFilesTask(getDir("data", MODE_PRIVATE), this));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean requireReadWritePermissions() {
        boolean granted = true;

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            granted = false;
        } else if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
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
            viewModel.setDialogProject(project);
            ProjectEditDialog dialog = new ProjectEditDialog();
            dialog.show(getSupportFragmentManager(), "ProjectEditDialog");
        }

        @Override
        public void open(Project project) {
            Intent intent = new Intent(MainActivity.this, ProjectActivity.class);
            intent.setAction(Intent.ACTION_VIEW);
            intent.putExtra(Intent.EXTRA_TITLE, project.name);
            intent.putExtra(AppConstants.TAG_EXTRA_PROJECT_ID, project.id);
            if(intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            }
        }

        @Override
        public void delete(final Project project) {
            DatabaseConnectionManager.runTask(new DeleteProjectsTask(Collections.singletonList(project), new Runnable() {
                @Override
                public void run() {
                    AdapterLoader.removeItem(projectListAdapter, project);
                }
            }));
        }
    }
}
