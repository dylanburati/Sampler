package libre.sampler;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.util.Consumer;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import libre.sampler.adapters.ProjectListAdapter;
import libre.sampler.databases.ProjectDao;
import libre.sampler.dialogs.ProjectCreateDialog;
import libre.sampler.listeners.MySwipeRefreshListener;
import libre.sampler.models.Project;
import libre.sampler.tasks.GetProjectsTask;
import libre.sampler.utils.AdapterLoader;
import libre.sampler.utils.DatabaseConnectionManager;

import android.os.Parcelable;
import android.view.Menu;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity implements ProjectCreateDialog.ProjectCreateDialogListener {
    private SwipeRefreshLayout refreshLayout;
    private RecyclerView data;
    private ProjectListAdapter dataAdapter;

    @RequiresApi(api = Build.VERSION_CODES.N)
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

        // tmp
//        List<String> inputData = new ArrayList<>();
//        List<Project> structuredData = new ArrayList<>();
//        Collections.addAll(inputData,"Anaconda3", "Android", "Autodesk", "Blender Foundation", "Bonjour", "Common Files", "Dell", "Docker", "GIMP 2", "Goodix", "Intel", "Java", "JetBrains", "Killer Networking", "Linux Containers", "MATLAB", "Microsoft Office 15", "Microsoft SQL Server", "Microsoft Visual Studio 10.0", "Microsoft.NET", "MiKTeX 2.9", "Mozilla Firefox", "MSBuild", "NVIDIA Corporation", "Oracle", "Reference Assemblies", "Shotcut", "SOLIDWORKS Corp", "VideoLAN", "VMware", "WindowsPowerShell");
//        for(String s : inputData) {
//            structuredData.add(new Project(0, s, (long) s.hashCode()));
//        }
        // tmp>

        this.data = (RecyclerView) findViewById(R.id.main_data);
        this.dataAdapter = new ProjectListAdapter(new ArrayList<Project>(), new Consumer<Project>() {
            @Override
            public void accept(Project project) {
                Intent intent = new Intent(MainActivity.this, ProjectActivity.class);
                intent.setAction(Intent.ACTION_VIEW);
                intent.putExtra(Intent.EXTRA_TITLE, project.name);
                intent.putExtra(ProjectActivity.TAG_EXTRA_PROJECT, (Parcelable) project);
                if(intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(intent);
                }
            }
        });
        data.setAdapter(this.dataAdapter);

        DatabaseConnectionManager.getInstance(this);  // initialize database
        DatabaseConnectionManager.runTask(new GetProjectsTask(new Consumer<List<Project>>() {
            @Override
            public void accept(List<Project> projects) {
                AdapterLoader.insertAll(dataAdapter, projects);
                dataAdapter.autoScrollOnInsert = true;
            }
        }));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.appbar_create) {
            new ProjectCreateDialog().show(getSupportFragmentManager(), "dialog_create");
            return true;
        } else if(item.getItemId() == R.id.appbar_delete) {
            final ProjectDao dao = DatabaseConnectionManager.getInstance(this).projectDao();
            DatabaseConnectionManager.execute(new Runnable() {
                @Override
                public void run() {
                    dao.deleteAll();
                }
            });
            AdapterLoader.clear(dataAdapter);
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onSubmitProjectCreate(String projectName) {
        final Project toAdd = new Project(0, projectName, System.currentTimeMillis());
        final ProjectDao dao = DatabaseConnectionManager.getInstance(this).projectDao();
        DatabaseConnectionManager.execute(new Runnable() {
            @Override
            public void run() {
                dao.insertAll(toAdd);
            }
        });
        AdapterLoader.insertItem(this.dataAdapter, 0, toAdd);
    }
}
