package libre.sampler.fragments;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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
import libre.sampler.listeners.StatefulTextWatcher;
import libre.sampler.listeners.StatefulVerticalSliderChangeListener;
import libre.sampler.models.Instrument;
import libre.sampler.models.InstrumentEvent;
import libre.sampler.models.Project;
import libre.sampler.models.ProjectViewModel;
import libre.sampler.models.Sample;
import libre.sampler.publishers.InstrumentEventSource;
import libre.sampler.utils.AdapterLoader;
import libre.sampler.utils.MyDecimalFormat;
import libre.sampler.utils.SliderConverter;
import libre.sampler.views.VerticalSlider;

public class ProjectInstrumentsFragment extends Fragment {
    private View rootView;
    private RecyclerView data;
    private ProjectViewModel viewModel;
    private InstrumentEventSource instrumentEventSource;
    private InstrumentListAdapter adapter;

    private boolean isAdapterLoaded;

    private Spinner sampleSpinner;
    private ArrayAdapter<String> sampleSpinnerAdapter;

    private boolean isInstrumentEditorReady;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        this.rootView = inflater.inflate(R.layout.fragment_project_instruments, container, false);

        this.data = (RecyclerView) rootView.findViewById(R.id.instruments_select);

        // if landscape and not tablet, put instrument editor on right instead of bottom
        if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE &&
                getResources().getDisplayMetrics().widthPixels < getResources().getDimensionPixelOffset(R.dimen.split_screen_direction_threshold)) {
            LinearLayout fragmentBody = (LinearLayout) rootView;
            fragmentBody.setOrientation(LinearLayout.HORIZONTAL);
            int nChildren = fragmentBody.getChildCount();
            for(int i = 0; i < nChildren; i++) {
                ViewGroup.LayoutParams params = fragmentBody.getChildAt(i).getLayoutParams();
                params.height = ViewGroup.LayoutParams.MATCH_PARENT;
                params.width = 0;
                fragmentBody.getChildAt(i).setLayoutParams(params);
                if(i > 0) {
                    fragmentBody.getChildAt(i).setBackground(getResources().getDrawable(R.drawable.border_left));
                }
            }
        }

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
                    // accounts for offset due to 'New' instrument tile
                    int removeIdx = adapter.items.indexOf(event.instrument) - 1;
                    AdapterLoader.removeItem(adapter, event.instrument);
                    if(event.instrument == viewModel.getKeyboardInstrument()) {
                        if(viewModel.getProject().getInstruments().size() > removeIdx) {
                            viewModel.setKeyboardInstrument(viewModel.getProject().getInstruments().get(removeIdx));
                        } else if(removeIdx > 0) {
                            // removeIdx is one past the end of the list
                            viewModel.setKeyboardInstrument(viewModel.getProject().getInstruments().get(removeIdx - 1));
                        } else {
                            viewModel.setKeyboardInstrument(null);
                        }
                        data.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                adapter.activateInstrument(viewModel.getKeyboardInstrument());
                            }
                        }, 100);
                    }
                } else if(event.action == InstrumentEvent.INSTRUMENT_KEYBOARD_SELECT) {
                    updateInstrumentEditor();
                }
            }
        });
        isAdapterLoaded = false;
        loadAdapter();

        isInstrumentEditorReady = false;
        initInstrumentEditor();

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
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

    private void initInstrumentEditor() {
        sampleSpinner = rootView.findViewById(R.id.sample_edit_select);
        sampleSpinnerAdapter = new ArrayAdapter<>(sampleSpinner.getContext(), android.R.layout.simple_spinner_item);
        sampleSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sampleSpinner.setAdapter(sampleSpinnerAdapter);

        updateInstrumentEditor();
    }

    private void attachInstrumentEditorListeners() {
        sampleSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                viewModel.setEditorSample(viewModel.getKeyboardInstrument().getSamples().get(position));
                updateInstrumentEditor();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        ((Button) rootView.findViewById(R.id.sample_add)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String path = ((EditText) rootView.findViewById(R.id.input_sample_paths)).getText().toString();
                File sampleFile = new File(path);
                // todo support wildcard search: File.listFiles
                if(sampleFile.isFile() && sampleFile.canRead()) {
                    Sample s = viewModel.getKeyboardInstrument().addSample(sampleFile.getAbsolutePath());
                    viewModel.setEditorSample(s);
                    viewModel.instrumentEventSource.dispatch(new InstrumentEvent(
                            InstrumentEvent.INSTRUMENT_PD_LOAD, viewModel.getKeyboardInstrument()));
                    updateInstrumentEditor();
                }
            }
        });

        ((Button) rootView.findViewById(R.id.sample_replace)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String path = ((EditText) rootView.findViewById(R.id.input_sample_paths)).getText().toString();
                File sampleFile = new File(path);
                // todo support wildcard search: File.listFiles
                Sample editorSample = viewModel.getEditorSample();
                if(sampleFile.isFile() && sampleFile.canRead() && editorSample != null) {
                    editorSample.setFilename(sampleFile.getAbsolutePath());
                    viewModel.instrumentEventSource.dispatch(new InstrumentEvent(
                            InstrumentEvent.INSTRUMENT_PD_LOAD, viewModel.getKeyboardInstrument()));
                    updateSampleSpinner();
                }
            }
        });

        ((Button) rootView.findViewById(R.id.sample_delete)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Sample toRemove = viewModel.getEditorSample();
                if(toRemove != null) {
                    Instrument keyboardInstrument = viewModel.getKeyboardInstrument();
                    int currentSelection = sampleSpinner.getSelectedItemPosition();
                    if(currentSelection == keyboardInstrument.getSamples().size() - 1) {
                        if(keyboardInstrument.getSamples().size() > 1) {
                            viewModel.setEditorSample(keyboardInstrument.getSamples().get(currentSelection - 1));
                        } else {
                            viewModel.setEditorSample(null);
                        }
                    } else {
                        viewModel.setEditorSample(keyboardInstrument.getSamples().get(currentSelection + 1));
                    }
                    viewModel.getKeyboardInstrument().removeSample(toRemove);
                    updateInstrumentEditor();
                }
            }
        });

        final MyDecimalFormat fmt1 = new MyDecimalFormat(1, 4);
        int[] textOnlyInputs = new int[]{R.id.pitch_min, R.id.pitch_max, R.id.pitch_base,
                R.id.velocity_min, R.id.velocity_max,
                R.id.position_start, R.id.position_end, R.id.position_resume};

        for(int id : textOnlyInputs) {
            EditText ed = (EditText) rootView.findViewById(id);
            ed.addTextChangedListener(new StatefulTextWatcher<EditText>(ed) {
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    float val;
                    if(s.length() == 0) {
                        return;
                    }
                    try {
                        val = Float.parseFloat(s.toString());
                    } catch(NumberFormatException e) {
                        return;
                    }
                    Sample editorSample = viewModel.getEditorSample();
                    if(editorSample == null) {
                        return;
                    }
                    switch(this.data.getId()) {
                        case R.id.pitch_min:
                            editorSample.setMinPitch((int) val);
                            break;
                        case R.id.pitch_max:
                            editorSample.setMaxPitch((int) val);
                            break;
                        case R.id.pitch_base:
                            editorSample.setBasePitch((int) val);
                            break;
                        case R.id.velocity_min:
                            editorSample.setMinVelocity((int) val);
                            break;
                        case R.id.velocity_max:
                            editorSample.setMaxVelocity((int) val);
                            break;
                        case R.id.position_start:
                            editorSample.setLoopStart(val);
                            break;
                        case R.id.position_end:
                            editorSample.setLoopEnd(val);
                            break;
                        case R.id.position_resume:
                            editorSample.setLoopResume(val);
                            break;
                        default:
                            break;
                    }
                }
            });
        }

        int[] sliderTextPairInputs = new int[]{R.id.instrument_volume_slider, R.id.instrument_volume,
                R.id.sample_volume_slider, R.id.sample_volume,
                R.id.sample_attack_slider, R.id.sample_attack,
                R.id.sample_decay_slider, R.id.sample_decay,
                R.id.sample_sustain_slider, R.id.sample_sustain,
                R.id.sample_release_slider, R.id.sample_release};

        for(int i = 0; i < sliderTextPairInputs.length; i += 2) {
            VerticalSlider slider = (VerticalSlider) rootView.findViewById(sliderTextPairInputs[i]);
            EditText ed = (EditText) rootView.findViewById(sliderTextPairInputs[i + 1]);
            ed.addTextChangedListener(new StatefulTextWatcher<VerticalSlider>(slider) {
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    float val;
                    if(s.length() == 0) {
                        return;
                    }
                    try {
                        val = Float.parseFloat(s.toString());
                    } catch(NumberFormatException e) {
                        return;
                    }
                    switch(this.data.getId()) {
                        case R.id.instrument_volume_slider:
                        case R.id.sample_volume_slider:
                        case R.id.sample_sustain_slider:
                            this.data.setProgress(SliderConverter.DECIBELS.toSlider(val));
                            break;
                        case R.id.sample_attack_slider:
                        case R.id.sample_decay_slider:
                        case R.id.sample_release_slider:
                            this.data.setProgress(SliderConverter.MILLISECONDS.toSlider(val));
                            break;
                        default:
                            break;
                    }
                    Sample editorSample = viewModel.getEditorSample();
                    if(editorSample != null) {
                        switch(this.data.getId()) {
                            case R.id.instrument_volume_slider:
                                // todo add volume property
                                break;
                            case R.id.sample_volume_slider:
                                // todo add volume property
                                break;
                            case R.id.sample_attack_slider:
                                editorSample.setAttack(val);
                                break;
                            case R.id.sample_decay_slider:
                                editorSample.setDecay(val);
                                break;
                            case R.id.sample_sustain_slider:
                                editorSample.setSustainDecibels(val);
                                break;
                            case R.id.sample_release_slider:
                                editorSample.setRelease(val);
                                break;
                            default:
                                break;
                        }
                    }
                }
            });

            slider.addListener(new StatefulVerticalSliderChangeListener<EditText>(ed) {
                @Override
                public void onProgressChanged(VerticalSlider v, float progress, boolean fromUser) {
                    if(fromUser) {
                        switch(v.getId()) {
                            case R.id.instrument_volume_slider:
                            case R.id.sample_volume_slider:
                            case R.id.sample_sustain_slider:
                                this.data.setText(fmt1.format(SliderConverter.DECIBELS.fromSlider(progress)));
                                break;
                            case R.id.sample_attack_slider:
                            case R.id.sample_decay_slider:
                            case R.id.sample_release_slider:
                                this.data.setText(fmt1.format(SliderConverter.MILLISECONDS.fromSlider(progress)));
                                break;
                            default:
                                break;
                        }
                    }
                }
            });
        }
    }

    private boolean updateSampleSpinner() {
        Instrument keyboardInstrument = viewModel.getKeyboardInstrument();
        if(keyboardInstrument == null) {
            return false;
        }

        List<Sample> sampleSpinnerItems = keyboardInstrument.getSamples();
        sampleSpinnerAdapter.clear();
        for(int i = 0; i < sampleSpinnerItems.size(); i++) {
            sampleSpinnerAdapter.add(String.format("%03d %s", i + 1, sampleSpinnerItems.get(i).filename));
        }

        Sample editorSample = viewModel.getEditorSample();
        if(editorSample != null) {
            sampleSpinner.setSelection(sampleSpinnerItems.indexOf(editorSample));
            ((EditText) rootView.findViewById(R.id.input_sample_paths)).setText(editorSample.filename);
        }
        return true;
    }

    private void updateInstrumentEditor() {
        boolean projectReady = updateSampleSpinner();
        if(!projectReady) {
            return;
        }

        int[] textOnlyInputs = new int[]{R.id.pitch_min, R.id.pitch_max, R.id.pitch_base,
                R.id.velocity_min, R.id.velocity_max,
                R.id.position_start, R.id.position_end, R.id.position_resume};

        int[] sliderTextPairInputs = new int[]{R.id.instrument_volume_slider, R.id.instrument_volume,
                R.id.sample_volume_slider, R.id.sample_volume,
                R.id.sample_attack_slider, R.id.sample_attack,
                R.id.sample_decay_slider, R.id.sample_decay,
                R.id.sample_sustain_slider, R.id.sample_sustain,
                R.id.sample_release_slider, R.id.sample_release};

        Sample editorSample = viewModel.getEditorSample();
        if(editorSample != null) {
            final MyDecimalFormat fmt3 = new MyDecimalFormat(3, 6);
            for(int id : textOnlyInputs) {
                EditText ed = ((EditText) rootView.findViewById(id));
                switch(id) {
                    case R.id.pitch_min:
                        if(editorSample.shouldDisplay(Sample.FIELD_MIN_PITCH)) {
                            ed.setText(String.format("%d", editorSample.minPitch));
                        } else {
                            ed.setText("");
                        }
                        break;
                    case R.id.pitch_max:
                        if(editorSample.shouldDisplay(Sample.FIELD_MAX_PITCH)) {
                            ed.setText(String.format("%d", editorSample.maxPitch));
                        } else {
                            ed.setText("");
                        }
                        break;
                    case R.id.pitch_base:
                        if(editorSample.shouldDisplay(Sample.FIELD_BASE_PITCH)) {
                            ed.setText(String.format("%d", editorSample.basePitch));
                        } else {
                            ed.setText("");
                        }
                        break;
                    case R.id.velocity_min:
                        if(editorSample.shouldDisplay(Sample.FIELD_MIN_VELOCITY)) {
                            ed.setText(String.format("%d", editorSample.minVelocity));
                        } else {
                            ed.setText("");
                        }
                        break;
                    case R.id.velocity_max:
                        if(editorSample.shouldDisplay(Sample.FIELD_MAX_VELOCITY)) {
                            ed.setText(String.format("%d", editorSample.maxVelocity));
                        } else {
                            ed.setText("");
                        }
                        break;
                    case R.id.position_start:
                        if(!editorSample.shouldUseDefaultLoopStart) {
                            ed.setText(fmt3.format(editorSample.startTime));
                        } else {
                            ed.setText("");
                        }
                        break;
                    case R.id.position_end:
                        if(!editorSample.shouldUseDefaultLoopResume) {
                            ed.setText(fmt3.format(editorSample.endTime));
                        } else {
                            ed.setText("");
                        }
                        break;
                    case R.id.position_resume:
                        if(!editorSample.shouldUseDefaultLoopEnd) {
                            ed.setText(fmt3.format(editorSample.resumeTime));
                        } else {
                            ed.setText("");
                        }
                        break;
                    default:
                        break;
                }
            }

            for(int i = 0; i < sliderTextPairInputs.length; i += 2) {
                VerticalSlider slider = (VerticalSlider) rootView.findViewById(sliderTextPairInputs[i]);
                EditText ed = (EditText) rootView.findViewById(sliderTextPairInputs[i + 1]);
                switch(sliderTextPairInputs[i + 1]) {
                    case R.id.instrument_volume:
                        // todo add volume property
                        break;
                    case R.id.sample_volume:
                        // todo add volume property
                        break;
                    case R.id.sample_attack:
                        ed.setText(fmt3.format(editorSample.attack));
                        slider.setProgress(SliderConverter.MILLISECONDS.toSlider(editorSample.attack));
                        break;
                    case R.id.sample_decay:
                        ed.setText(fmt3.format(editorSample.decay));
                        slider.setProgress(SliderConverter.MILLISECONDS.toSlider(editorSample.decay));
                        break;
                    case R.id.sample_sustain:
                        float db = editorSample.getSustainDecibels();
                        db = Math.max(-100, Math.min(0, db));
                        ed.setText(fmt3.format(db));
                        slider.setProgress(SliderConverter.DECIBELS.toSlider(db));
                        break;
                    case R.id.sample_release:
                        ed.setText(fmt3.format(editorSample.release));
                        slider.setProgress(SliderConverter.MILLISECONDS.toSlider(editorSample.release));
                        break;
                    default:
                        break;
                }
            }
        } else {
            // no editor sample
            String defaultSamplePath = viewModel.getProject().getDefaultSamplePath();
            if(defaultSamplePath != null) {
                ((EditText) rootView.findViewById(R.id.input_sample_paths)).setText(defaultSamplePath);
            }
            for(int id : textOnlyInputs) {
                ((EditText) rootView.findViewById(id)).setText("");
            }
            for(int i = 0; i < sliderTextPairInputs.length; i += 2) {
                VerticalSlider slider = (VerticalSlider) rootView.findViewById(sliderTextPairInputs[i]);
                EditText ed = (EditText) rootView.findViewById(sliderTextPairInputs[i + 1]);
                slider.setProgress(0);
                ed.setText("");
            }
        }

        if(!isInstrumentEditorReady) {
            attachInstrumentEditorListeners();
            isInstrumentEditorReady = true;
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
            viewModel.setKeyboardInstrument(instrument);
            adapter.activateInstrument(instrument);
        }
    }
}
