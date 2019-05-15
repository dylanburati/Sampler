package libre.sampler;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.Menu;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.util.Consumer;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import libre.sampler.adapters.ProjectListAdapter;
import libre.sampler.dialogs.ProjectCreateDialog;
import libre.sampler.listeners.MySwipeRefreshListener;
import libre.sampler.models.Project;
import libre.sampler.tasks.CreateProjectTask;
import libre.sampler.tasks.DeleteProjectsTask;
import libre.sampler.tasks.GetProjectsTask;
import libre.sampler.utils.AdapterLoader;
import libre.sampler.utils.AppConstants;
import libre.sampler.utils.DatabaseConnectionManager;

public class MainActivity extends AppCompatActivity implements ProjectCreateDialog.ProjectCreateDialogListener {
    private SwipeRefreshLayout refreshLayout;
    private RecyclerView data;
    private ProjectListAdapter dataAdapter;
    private ProjectCreateDialog projectCreateDialog;
    private boolean projectsLoaded;
    private ArrayList<Project> projects;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        refreshLayout = (SwipeRefreshLayout) findViewById(R.id.main_refresh);
        refreshLayout.setOnRefreshListener(new MySwipeRefreshListener() {
            @Override
            public void onRefresh() {
                refreshLayout.setRefreshing(false);
            }
        });

        projectsLoaded = false;
        if(savedInstanceState != null) {
            projects = savedInstanceState.getParcelableArrayList(AppConstants.TAG_SAVED_STATE_PROJECT_LIST);
            projectsLoaded = (projects != null);
        }
        if(!projectsLoaded) {
            projects = new ArrayList<>();
        }

        this.data = (RecyclerView) findViewById(R.id.main_data);
        this.dataAdapter = new ProjectListAdapter(projects, new Consumer<Project>() {
            @Override
            public void accept(Project project) {
                Intent intent = new Intent(MainActivity.this, ProjectActivity.class);
                intent.setAction(Intent.ACTION_VIEW);
                intent.putExtra(Intent.EXTRA_TITLE, project.name);
                intent.putExtra(AppConstants.TAG_EXTRA_PROJECT, (Parcelable) project);
                if(intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(intent);
                }
            }
        });
        data.setAdapter(this.dataAdapter);

        if(!projectsLoaded) {
            DatabaseConnectionManager.initialize(this);
            DatabaseConnectionManager.runTask(new GetProjectsTask(new Consumer<List<Project>>() {
                @Override
                public void accept(List<Project> prjs) {
                    dataAdapter.autoScrollOnInsert = true;
                    AdapterLoader.insertAll(dataAdapter, prjs);
                }
            }));
        } else {
            dataAdapter.autoScrollOnInsert = true;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        projectsLoaded = (projects != null);
        if(!projectsLoaded) {
            projects = new ArrayList<>();
        }
        if(data == null) {
            this.data = (RecyclerView) findViewById(R.id.main_data);
            this.dataAdapter = new ProjectListAdapter(projects, new Consumer<Project>() {
                @Override
                public void accept(Project project) {
                    Intent intent = new Intent(MainActivity.this, ProjectActivity.class);
                    intent.setAction(Intent.ACTION_VIEW);
                    intent.putExtra(Intent.EXTRA_TITLE, project.name);
                    intent.putExtra(AppConstants.TAG_EXTRA_PROJECT, (Parcelable) project);
                    if(intent.resolveActivity(getPackageManager()) != null) {
                        startActivity(intent);
                    }
                }
            });
            data.setAdapter(this.dataAdapter);
        }
        if(!projectsLoaded) {
            DatabaseConnectionManager.initialize(this);
            DatabaseConnectionManager.runTask(new GetProjectsTask(new Consumer<List<Project>>() {
                @Override
                public void accept(List<Project> prjs) {
                    dataAdapter.autoScrollOnInsert = true;
                    AdapterLoader.insertAll(dataAdapter, prjs);
                }
            }));
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(AppConstants.TAG_SAVED_STATE_PROJECT_LIST, projects);
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
            DatabaseConnectionManager.runTask(new DeleteProjectsTask(dataAdapter.items, new Runnable() {
                @Override
                public void run() {
                    AdapterLoader.clear(dataAdapter);
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
        AdapterLoader.insertItem(this.dataAdapter, 0, toAdd);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch(requestCode) {
            case AppConstants.PERM_REQUEST_READ_EXTERNAL_STORAGE:
                if(grantResults.length >= 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    projectCreateDialog = new ProjectCreateDialog();
                    projectCreateDialog.show(getSupportFragmentManager(), "dialog_project_create");
                }
                break;
            default:
                break;
        }
    }
}
