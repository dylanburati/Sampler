package libre.sampler.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.core.util.Consumer;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.RecyclerView;
import libre.sampler.MainActivity;
import libre.sampler.R;
import libre.sampler.adapters.AddInstrumentListAdapter;
import libre.sampler.models.Instrument;
import libre.sampler.models.MainViewModel;
import libre.sampler.models.Project;
import libre.sampler.models.ProjectEvent;
import libre.sampler.utils.AppConstants;

public class MainDetailFragment extends Fragment {
    public static final String TAG = "MainDetailFragment";

    private View rootView;
    private EditText nameInputView;
    private RecyclerView addInstrumentsView;
    private AddInstrumentListAdapter addInstrumentsAdapter;

    private boolean isAdapterLoaded;
    private MainViewModel viewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        viewModel = ViewModelProviders.of(getActivity()).get(MainViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_main_detail, container, false);

        ActionBar actionBar = ((MainActivity) getActivity()).getSupportActionBar();
        if(actionBar != null) {
            actionBar.setTitle((viewModel.getDialogActionType() == ProjectEvent.PROJECT_EDIT ?
                    R.string.dialog_project_edit_title : R.string.dialog_project_create_title));
        }

        initUI();
        viewModel.loadEventSource.add(TAG, new Consumer<String>() {
            @Override
            public void accept(String eventName) {
                if(eventName.equals(AppConstants.ALL_INSTRUMENTS_LOADED) ||
                        eventName.equals(AppConstants.PROJECTS_LOADED)) {
                    updateUI();
                }
            }
        });
        viewModel.projectEventSource.add(TAG, new Consumer<ProjectEvent>() {
            @Override
            public void accept(ProjectEvent event) {
                if(event.action == viewModel.getDialogActionType()) {
                    FragmentManager fm = getFragmentManager();
                    if(fm != null && fm.getBackStackEntryCount() > 0) fm.popBackStack();
                }
            }
        });
        isAdapterLoaded = false;
        updateUI();

        return rootView;
    }

    private void initUI() {
        nameInputView = rootView.findViewById(R.id.input_name);
        addInstrumentsView = rootView.findViewById(R.id.add_instruments_select);
        addInstrumentsAdapter = new AddInstrumentListAdapter();
        addInstrumentsView.setAdapter(addInstrumentsAdapter);
    }

    private void updateUI() {
        List<Instrument> instruments = viewModel.getInstruments();
        List<Project> projects = viewModel.getProjects();
        Project dialogProject = viewModel.getDialogProject();
        if(projects == null || instruments == null || isAdapterLoaded) {
            return;
        }

        List<Project> otherProjects = new ArrayList<>(projects);
        otherProjects.remove(dialogProject);
        addInstrumentsAdapter.setProjectsAndInstruments(otherProjects, instruments);
        isAdapterLoaded = true;
        if(dialogProject != null) {
            // update fields
            nameInputView.setText(dialogProject.name);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        rootView = null;
        viewModel.loadEventSource.remove(TAG);
        viewModel.projectEventSource.remove(TAG);
    }

    private Project createProject() {
        return new Project(nameInputView.getText().toString(), System.currentTimeMillis());
    }

    private boolean editProject() {
        Project toEdit = viewModel.getDialogProject();
        if(toEdit == null) {
            return false;
        }

        toEdit.name = nameInputView.getText().toString();
        return true;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_project_detail, menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId() == R.id.action_cancel) {
            FragmentManager fm = getFragmentManager();
            if(fm != null && fm.getBackStackEntryCount() > 0) fm.popBackStack();
            return true;
        } else if(item.getItemId() == R.id.action_done) {
            if(viewModel.getDialogActionType() == ProjectEvent.PROJECT_CREATE) {
                final Project toAdd = createProject();
                if(toAdd != null) {
                    viewModel.addNewProject(toAdd, addInstrumentsAdapter.getSelectedInstruments());
                } else {
                    Toast.makeText(getContext(), R.string.project_could_not_create, Toast.LENGTH_SHORT).show();
                }
            } else if(viewModel.getDialogActionType() == ProjectEvent.PROJECT_EDIT) {
                if(editProject()) {
                    viewModel.updateProject(viewModel.getDialogProject(), addInstrumentsAdapter.getSelectedInstruments());
                } else {
                    Toast.makeText(getContext(), R.string.project_could_not_edit, Toast.LENGTH_SHORT).show();
                }
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
