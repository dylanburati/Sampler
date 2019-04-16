package libre.sampler.fragments;

import android.content.Context;
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
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;
import libre.sampler.ProjectActivity;
import libre.sampler.R;
import libre.sampler.adapters.InstrumentListAdapter;
import libre.sampler.adapters.ProjectListAdapter;
import libre.sampler.dialogs.InstrumentCreateDialog;
import libre.sampler.models.Instrument;
import libre.sampler.models.Project;
import libre.sampler.utils.AdapterLoader;

public class ProjectInstrumentsFragment extends Fragment {
    private RecyclerView data;
    private Project project;
    private InstrumentListAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_project_instruments, container, false);

        this.data = (RecyclerView) rootView.findViewById(R.id.instruments_data);
        project = ((ProjectActivity) getActivity()).project;
        adapter = new InstrumentListAdapter(new ArrayList<Instrument>(),
                new InstrumentEditConsumer(), new InstrumentSelectConsumer(), new InstrumentCreateRunnable(this));
        data.setAdapter(adapter);
        ((ProjectActivity) getActivity()).instrumentListAdapter = adapter;

        return rootView;
    }

    private static class InstrumentEditConsumer implements Consumer<Instrument> {
        @Override
        public void accept(Instrument instrument) {
            Log.d("InstrumentListAdapter", "" + instrument.name + " clicked");
        }
    }

    private static class InstrumentCreateRunnable implements Runnable {
        private final Fragment ctx;

        public InstrumentCreateRunnable(Fragment ctx) {
            this.ctx = ctx;
        }

        @Override
        public void run() {
            FragmentManager fm = ctx.getFragmentManager();
            if(fm != null) {
                new InstrumentCreateDialog().show(fm, "dialog_instrument_create");
            }
        }
    }

    private class InstrumentSelectConsumer implements Consumer<Instrument> {
        @Override
        public void accept(Instrument instrument) {
            int activeIdx = project.setActiveInstrument(instrument);
            adapter.activateItem(activeIdx + 1);
        }
    }
}
