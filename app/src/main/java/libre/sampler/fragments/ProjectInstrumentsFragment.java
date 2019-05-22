package libre.sampler.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.RecyclerView;
import libre.sampler.R;
import libre.sampler.adapters.InstrumentListAdapter;
import libre.sampler.dialogs.InstrumentCreateDialog;
import libre.sampler.dialogs.InstrumentEditDialog;
import libre.sampler.models.Instrument;
import libre.sampler.models.InstrumentEvent;
import libre.sampler.models.Project;
import libre.sampler.models.ProjectViewModel;
import libre.sampler.publishers.InstrumentEventSource;
import libre.sampler.utils.AdapterLoader;

public class ProjectInstrumentsFragment extends Fragment {
    private RecyclerView data;
    private ProjectViewModel viewModel;
    private InstrumentEventSource instrumentEventSource;
    private InstrumentListAdapter adapter;

    private boolean isAdapterLoaded;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_project_instruments, container, false);

        this.data = (RecyclerView) rootView.findViewById(R.id.instruments_data);

        viewModel = ViewModelProviders.of(getActivity()).get(ProjectViewModel.class);
        adapter = new InstrumentListAdapter(new ArrayList<Instrument>(),
                new InstrumentEditConsumer(), new InstrumentSelectConsumer(), new InstrumentCreateRunnable());
        data.setAdapter(adapter);
        viewModel.projectEventSource.add("InstrumentsFragment", new Consumer<Project>() {
            @Override
            public void accept(Project project) {
                loadAdapter();
            }
        });
        viewModel.instrumentEventSource.add("InstrumentsFragment", new Consumer<InstrumentEvent>() {
            @Override
            public void accept(InstrumentEvent event) {
                if(event.action == InstrumentEvent.INSTRUMENT_CREATE) {
                    AdapterLoader.insertItem(adapter, event.instrument);
                } else if(event.action == InstrumentEvent.INSTRUMENT_EDIT) {
                    int changeIdx = adapter.items.indexOf(event.instrument);
                    if(changeIdx != -1) {
                        adapter.notifyItemChanged(changeIdx);
                    }
                    if(event.instrument == viewModel.getKeyboardInstrument()) {
                        adapter.activateInstrument(event.instrument);
                    }
                } else if(event.action == InstrumentEvent.INSTRUMENT_DELETE) {
                    AdapterLoader.removeItem(adapter, event.instrument);
                }
            }
        });
        loadAdapter();

        return rootView;
    }

    private void loadAdapter() {
        if(!isAdapterLoaded) {
            Project project = viewModel.getProject();
            if(project != null && !project.getInstruments().isEmpty()) {
                AdapterLoader.insertAll(adapter, viewModel.getProject().getInstruments());
                adapter.activateInstrument(viewModel.getKeyboardInstrument());
                isAdapterLoaded = true;
            }
        }
    }

    private class InstrumentEditConsumer implements Consumer<Instrument> {
        public InstrumentEditConsumer() {
        }

        @Override
        public void accept(Instrument instrument) {
            FragmentManager fm = ProjectInstrumentsFragment.this.getFragmentManager();
            if(fm != null) {
                InstrumentEditDialog dialog = new InstrumentEditDialog();
                dialog.defaultSamplePath = viewModel.getProject().getDefaultSamplePath();
                viewModel.setEditDialogInstrument(instrument);
                dialog.show(fm, "dialog_instrument_edit");
            }
        }
    }

    private class InstrumentCreateRunnable implements Runnable {
        public InstrumentCreateRunnable() {
        }

        @Override
        public void run() {
            FragmentManager fm = ProjectInstrumentsFragment.this.getFragmentManager();
            if(fm != null) {
                InstrumentCreateDialog dialog = new InstrumentCreateDialog();
                dialog.defaultSamplePath = viewModel.getProject().getDefaultSamplePath();
                Instrument toCreate = new Instrument(null);
                viewModel.setCreateDialogInstrument(toCreate);
                viewModel.getProject().registerInstrument(toCreate);
                dialog.show(fm, "dialog_instrument_create");
            }
        }
    }

    private class InstrumentSelectConsumer implements Consumer<Instrument> {
        @Override
        public void accept(Instrument instrument) {
            viewModel.instrumentEventSource.dispatch(new InstrumentEvent(InstrumentEvent.INSTRUMENT_SELECT, instrument));
            adapter.activateInstrument(instrument);
        }
    }
}
