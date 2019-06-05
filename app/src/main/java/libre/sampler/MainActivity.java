package libre.sampler;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.util.Consumer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.RecyclerView;
import libre.sampler.adapters.ProjectListAdapter;
import libre.sampler.dialogs.ProjectCreateDialog;
import libre.sampler.models.MainViewModel;
import libre.sampler.models.Project;
import libre.sampler.tasks.CreateProjectTask;
import libre.sampler.tasks.DeleteProjectsTask;
import libre.sampler.utils.AdapterLoader;
import libre.sampler.utils.AppConstants;
import libre.sampler.utils.DatabaseConnectionManager;

public class MainActivity extends AppCompatActivity implements ProjectCreateDialog.ProjectCreateDialogListener {
    private RecyclerView projectListView;
    private ProjectListAdapter projectListAdapter;
    private ProjectCreateDialog projectCreateDialog;
    private boolean isAdapterLoaded;
    private MainViewModel viewModel;
    private boolean willShowDialogCreateProject;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        viewModel = ViewModelProviders.of(this).get(MainViewModel.class);

        initUI();

        viewModel.loadEventSource.add("MainActivity", new Consumer<String>() {
            @Override
            public void accept(String eventName) {
                if(eventName.equals(AppConstants.PROJECTS_LOADED)) {
                    updateAdapter(viewModel.getProjects());
                }
            }
        });
        isAdapterLoaded = false;
        updateAdapter(viewModel.getProjects());
    }

    private void initUI() {
        this.projectListView = (RecyclerView) findViewById(R.id.main_data);
        this.projectListAdapter = new ProjectListAdapter(new ArrayList<Project>(), new Consumer<Project>() {
            @Override
            public void accept(Project project) {
                Intent intent = new Intent(MainActivity.this, ProjectActivity.class);
                intent.setAction(Intent.ACTION_VIEW);
                intent.putExtra(Intent.EXTRA_TITLE, project.name);
                intent.putExtra(AppConstants.TAG_EXTRA_PROJECT_ID, project.id);
                if(intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(intent);
                }
            }
        });
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
            projectCreateDialog = new ProjectCreateDialog();
            projectCreateDialog.show(getSupportFragmentManager(), "dialog_project_create");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.appbar_create) {
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, AppConstants.PERM_REQUEST_READ_EXTERNAL_STORAGE);
                }
            } else {
                projectCreateDialog = new ProjectCreateDialog();
                projectCreateDialog.show(getSupportFragmentManager(), "dialog_project_create");
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
        } else if(item.getItemId() == R.id.appbar_about) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onProjectCreate(String projectName) {
        final Project toAdd = new Project(projectName, System.currentTimeMillis());
        DatabaseConnectionManager.initialize(this);
        DatabaseConnectionManager.runTask(new CreateProjectTask(toAdd, new Consumer<Integer>() {
            @Override
            public void accept(Integer id) {
                toAdd.setProjectId(id);
            }
        }));
        AdapterLoader.insertItem(this.projectListAdapter, 0, toAdd);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch(requestCode) {
            case AppConstants.PERM_REQUEST_READ_EXTERNAL_STORAGE:
                if(grantResults.length >= 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    willShowDialogCreateProject = true;
                }
                break;
            default:
                break;
        }
    }
}
