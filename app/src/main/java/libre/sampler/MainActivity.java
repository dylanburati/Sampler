package libre.sampler;

import android.os.Build;
import android.os.Bundle;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import libre.sampler.adapters.ProjectListAdapter;
import libre.sampler.listeners.MySwipeRefreshListener;
import libre.sampler.listeners.RefreshPostHook;
import libre.sampler.models.Project;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {
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
        refreshLayout.setOnRefreshListener(new MySwipeRefreshListener(new RefreshPostHook(
        ) {
            @Override
            public void afterRefresh() {
                refreshLayout.setRefreshing(false);
            }
        }));

        // tmp
        List<String> inputData = new ArrayList<>();
        List<Project> structuredData = new ArrayList<>();
        Collections.addAll(inputData,"Anaconda3", "Android", "Autodesk", "Blender Foundation", "Bonjour", "Common Files", "Dell", "Docker", "GIMP 2", "Goodix", "Intel", "Java", "JetBrains", "Killer Networking", "Linux Containers", "MATLAB", "Microsoft Office 15", "Microsoft SQL Server", "Microsoft Visual Studio 10.0", "Microsoft.NET", "MiKTeX 2.9", "Mozilla Firefox", "MSBuild", "NVIDIA Corporation", "Oracle", "Reference Assemblies", "Shotcut", "SOLIDWORKS Corp", "VideoLAN", "VMware", "WindowsPowerShell");
        for(String s : inputData) {
            structuredData.add(new Project(s, (long) s.hashCode()));
        }
        // tmp>

        this.data = (RecyclerView) findViewById(R.id.main_data);
        this.dataAdapter = new ProjectListAdapter(structuredData);
        data.setAdapter(this.dataAdapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.appbar_create) {
            // tmp
            this.dataAdapter.insertAll(this.dataAdapter.items);
            // tmp>
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
