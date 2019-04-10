package libre.sampler.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import libre.sampler.ProjectActivity;
import libre.sampler.R;
import libre.sampler.adapters.InstrumentListAdapter;
import libre.sampler.adapters.ProjectListAdapter;
import libre.sampler.models.Instrument;
import libre.sampler.models.Project;

public class ProjectInstrumentsFragment extends Fragment {
    private RecyclerView data;
    private InstrumentListAdapter dataAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_project_instruments, container, false);

        // tmp
        List<String> inputData = new ArrayList<>();
        List<Instrument> structuredData = new ArrayList<>();
        Collections.addAll(inputData,"Anaconda3", "Android", "Autodesk", "Blender Foundation", "Bonjour", "Common Files", "Dell", "Docker", "GIMP 2", "Goodix", "Intel", "Java", "JetBrains", "Killer Networking", "Linux Containers", "MATLAB", "Microsoft Office 15", "Microsoft SQL Server", "Microsoft Visual Studio 10.0", "Microsoft.NET", "MiKTeX 2.9", "Mozilla Firefox", "MSBuild", "NVIDIA Corporation", "Oracle", "Reference Assemblies", "Shotcut", "SOLIDWORKS Corp", "VideoLAN", "VMware", "WindowsPowerShell");
        for(String s : inputData) {
            structuredData.add(new Instrument(s));
        }
        // tmp>

        this.data = (RecyclerView) rootView.findViewById(R.id.instruments_data);
        this.dataAdapter = new InstrumentListAdapter(structuredData, new Consumer<Instrument>() {
            @Override
            public void accept(Instrument instrument) {
                Log.d("InstrumentListAdapter", "" + instrument.name + " clicked");
            }
        });
        data.setAdapter(this.dataAdapter);

        return rootView;
    }
}
