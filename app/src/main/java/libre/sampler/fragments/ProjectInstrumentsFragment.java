package libre.sampler.fragments;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
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
import libre.sampler.dialogs.InstrumentExportDialog;
import libre.sampler.listeners.StatefulTextWatcher;
import libre.sampler.listeners.StatefulVerticalSliderChangeListener;
import libre.sampler.models.GlobalSample;
import libre.sampler.models.Instrument;
import libre.sampler.models.InstrumentEvent;
import libre.sampler.models.Project;
import libre.sampler.models.ProjectViewModel;
import libre.sampler.models.Sample;
import libre.sampler.utils.AdapterLoader;
import libre.sampler.utils.AppConstants;
import libre.sampler.utils.ModelState;
import libre.sampler.utils.MyDecimalFormat;
import libre.sampler.utils.SliderConverter;
import libre.sampler.views.VerticalSlider;

public class ProjectInstrumentsFragment extends Fragment {
    public static final String TAG = "ProjectInstrumentsFragment";
    private View rootView;
    private RecyclerView instrumentListView;
    private ProjectViewModel viewModel;
    private InstrumentListAdapter instrumentListAdapter;

    private boolean isAdapterLoaded;

    private Spinner sampleSpinner;
    private ArrayAdapter<String> sampleSpinnerAdapter;

    private ModelState instrumentEditorState = ModelState.INVALID;
    private boolean isInstrumentEditorReady;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        this.rootView = inflater.inflate(R.layout.fragment_project_instruments, container, false);

        this.instrumentListView = rootView.findViewById(R.id.instruments_select);

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
        instrumentListAdapter = new InstrumentListAdapter(new ArrayList<Instrument>(),
                new MyInstrumentActionConsumer());
        instrumentListView.setAdapter(instrumentListAdapter);
        viewModel.loadEventSource.add(TAG, new Consumer<String>() {
            @Override
            public void accept(String eventName) {
                if(eventName.equals(AppConstants.INSTRUMENTS_PATTERNS_LOADED)) {
                    loadAdapter();
                }
            }
        });
        viewModel.instrumentEventSource.add(TAG, new Consumer<InstrumentEvent>() {
            @Override
            public void accept(InstrumentEvent event) {
                if(event.action == InstrumentEvent.INSTRUMENT_CREATE) {
                    AdapterLoader.insertItem(instrumentListAdapter, event.instrument);
                } else if(event.action == InstrumentEvent.INSTRUMENT_EDIT) {
                    int changeIdx = instrumentListAdapter.items.indexOf(event.instrument);
                    if(changeIdx != -1) {
                        instrumentListAdapter.notifyItemChanged(changeIdx);
                    }
                    if(event.instrument == viewModel.getKeyboardInstrument()) {
                        instrumentListAdapter.activateInstrument(event.instrument);
                    }
                } else if(event.action == InstrumentEvent.INSTRUMENT_DELETE) {
                    // accounts for offset due to 'New' instrument tile
                    int removeIdx = instrumentListAdapter.items.indexOf(event.instrument) - 1;
                    AdapterLoader.removeItem(instrumentListAdapter, event.instrument);
                    if(event.instrument == viewModel.getKeyboardInstrument()) {
                        if(viewModel.getProject().getInstruments().size() > removeIdx) {
                            viewModel.setKeyboardInstrument(viewModel.getProject().getInstruments().get(removeIdx));
                        } else if(removeIdx > 0) {
                            // removeIdx is one past the end of the list
                            viewModel.setKeyboardInstrument(viewModel.getProject().getInstruments().get(removeIdx - 1));
                        } else {
                            viewModel.setKeyboardInstrument(null);
                        }
                        instrumentListView.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                instrumentListAdapter.activateInstrument(viewModel.getKeyboardInstrument());
                            }
                        }, 100);
                    }
                } else if(event.action == InstrumentEvent.INSTRUMENT_KEYBOARD_SELECT) {
                    updateSampleSpinner();
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

    @Override
    public void onDestroyView() {
        sampleSpinner.setAdapter(null);
        instrumentListView.setAdapter(null);
        super.onDestroyView();
        this.rootView = null;
        this.instrumentListView = null;
        this.sampleSpinner = null;
        viewModel.loadEventSource.remove(TAG);
        viewModel.instrumentEventSource.remove(TAG);
    }

    private void loadAdapter() {
        if(!isAdapterLoaded) {
            Project project = viewModel.tryGetProject();
            if(project != null && !project.getInstruments().isEmpty()) {
                AdapterLoader.insertAll(instrumentListAdapter, viewModel.getProject().getInstruments());
                instrumentListAdapter.activateInstrument(viewModel.getKeyboardInstrument());
                isAdapterLoaded = true;
            }
        }
    }

    private void initInstrumentEditor() {
        sampleSpinner = rootView.findViewById(R.id.sample_edit_select);
        sampleSpinnerAdapter = new ArrayAdapter<>(sampleSpinner.getContext(), android.R.layout.simple_spinner_item);
        sampleSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sampleSpinner.setAdapter(sampleSpinnerAdapter);

        updateSampleSpinner();
        updateInstrumentEditor();
    }

    private void attachInstrumentEditorListeners() {
        sampleSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if(position == 0) {
                    viewModel.setEditorSample(new GlobalSample(viewModel.getKeyboardInstrument()));
                } else {
                    viewModel.setEditorSample(viewModel.getKeyboardInstrument().getSamples().get(position - 1));
                }
                updateInstrumentEditor();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        rootView.findViewById(R.id.sample_add).setOnClickListener(new View.OnClickListener() {
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
                    updateSampleSpinner();
                    updateInstrumentEditor();
                }
            }
        });

        rootView.findViewById(R.id.sample_replace).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String path = ((EditText) rootView.findViewById(R.id.input_sample_paths)).getText().toString();
                File sampleFile = new File(path);
                // todo support wildcard search: File.listFiles
                Sample editorSample = viewModel.getEditorSample();
                if(sampleFile.isFile() && sampleFile.canRead() && editorSample != null && !(editorSample instanceof GlobalSample)) {
                    editorSample.setFilename(sampleFile.getAbsolutePath());
                    viewModel.instrumentEventSource.dispatch(new InstrumentEvent(
                            InstrumentEvent.INSTRUMENT_PD_LOAD, viewModel.getKeyboardInstrument()));
                    updateSampleSpinner();
                }
            }
        });

        rootView.findViewById(R.id.sample_delete).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Sample toRemove = viewModel.getEditorSample();
                if(toRemove != null && !(toRemove instanceof GlobalSample)) {
                    Instrument keyboardInstrument = viewModel.getKeyboardInstrument();
                    int currentSelection = sampleSpinner.getSelectedItemPosition() - 1;
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
                    updateSampleSpinner();
                    updateInstrumentEditor();
                }
            }
        });

        final MyDecimalFormat fmt1 = new MyDecimalFormat(1, 4);
        int[] textOnlyInputs = new int[]{R.id.pitch_min, R.id.pitch_max, R.id.pitch_base,
                R.id.velocity_min, R.id.velocity_max,
                R.id.position_start, R.id.position_end, R.id.position_resume};

        for(int id : textOnlyInputs) {
            EditText ed = rootView.findViewById(id);
            ed.addTextChangedListener(new StatefulTextWatcher<EditText>(ed) {
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if(instrumentEditorState != ModelState.LOADED || s.length() == 0) {
                        return;
                    }
                    float val;
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
                            editorSample.setBasePitch(val);
                            break;
                        case R.id.velocity_min:
                            editorSample.setMinVelocity((int) val);
                            break;
                        case R.id.velocity_max:
                            editorSample.setMaxVelocity((int) val);
                            break;
                        case R.id.position_start:
                            editorSample.setStartTime(val);
                            break;
                        case R.id.position_end:
                            editorSample.setEndTime(val);
                            break;
                        case R.id.position_resume:
                            editorSample.setResumeTime(val);
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
            VerticalSlider slider = rootView.findViewById(sliderTextPairInputs[i]);
            EditText ed = rootView.findViewById(sliderTextPairInputs[i + 1]);
            ed.addTextChangedListener(new StatefulTextWatcher<VerticalSlider>(slider) {
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if(instrumentEditorState != ModelState.LOADED || s.length() == 0) {
                        return;
                    }
                    float val;
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
                                viewModel.getKeyboardInstrument().setVolumeDecibels(val);
                                break;
                            case R.id.sample_volume_slider:
                                editorSample.setVolumeDecibels(val);
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

    private void updateSampleSpinner() {
        Instrument keyboardInstrument = viewModel.getKeyboardInstrument();
        if(keyboardInstrument == null) {
            return;
        }

        List<Sample> sampleSpinnerItems = keyboardInstrument.getSamples();
        sampleSpinnerAdapter.clear();
        if(sampleSpinnerItems.size() > 0) {
            sampleSpinnerAdapter.add("Global");
        }
        for(int i = 0; i < sampleSpinnerItems.size(); i++) {
            sampleSpinnerAdapter.add(String.format("%03d %s", i + 1, sampleSpinnerItems.get(i).getDisplayName()));
        }

        Sample editorSample = viewModel.getEditorSample();
        if(editorSample != null) {
            sampleSpinner.setSelection(sampleSpinnerItems.indexOf(editorSample) + 1);
        }
    }

    private void updateInstrumentEditor() {
        Instrument keyboardInstrument = viewModel.getKeyboardInstrument();
        if(keyboardInstrument == null) {
            return;
        }

        instrumentEditorState = ModelState.LOADING;
        int[] textOnlyInputs = new int[]{R.id.pitch_min, R.id.pitch_max, R.id.pitch_base,
                R.id.velocity_min, R.id.velocity_max,
                R.id.position_start, R.id.position_end, R.id.position_resume};

        int[] sliderTextPairInputs = new int[]{R.id.sample_volume_slider, R.id.sample_volume,
                R.id.sample_attack_slider, R.id.sample_attack,
                R.id.sample_decay_slider, R.id.sample_decay,
                R.id.sample_sustain_slider, R.id.sample_sustain,
                R.id.sample_release_slider, R.id.sample_release};

        final MyDecimalFormat fmt3 = new MyDecimalFormat(3, 6);
        final MyDecimalFormat fmt7 = new MyDecimalFormat(7, 10);

        float iVol = keyboardInstrument.getVolumeDecibels();
        iVol = Math.max(-100, Math.min(0, iVol));
        ((EditText) rootView.findViewById(R.id.instrument_volume)).setText(fmt3.format(iVol));
        ((VerticalSlider) rootView.findViewById(R.id.instrument_volume_slider)).setProgress(SliderConverter.DECIBELS.toSlider(iVol));

        Sample editorSample = viewModel.getEditorSample();
        if(editorSample != null) {
            ((EditText) rootView.findViewById(R.id.input_sample_paths)).setText(editorSample.filename);
            for(int id : textOnlyInputs) {
                EditText ed = rootView.findViewById(id);
                switch(id) {
                    case R.id.pitch_min:
                        if(editorSample.shouldDisplay(Sample.FIELD_MIN_PITCH)) {
                            ed.setText(String.format("%d", editorSample.getMinPitch()));
                        } else {
                            ed.setText("");
                        }
                        break;
                    case R.id.pitch_max:
                        if(editorSample.shouldDisplay(Sample.FIELD_MAX_PITCH)) {
                            ed.setText(String.format("%d", editorSample.getMaxPitch()));
                        } else {
                            ed.setText("");
                        }
                        break;
                    case R.id.pitch_base:
                        if(editorSample.shouldDisplay(Sample.FIELD_BASE_PITCH)) {
                            ed.setText(fmt3.format(editorSample.getBasePitch()));
                        } else {
                            ed.setText("");
                        }
                        break;
                    case R.id.velocity_min:
                        if(editorSample.shouldDisplay(Sample.FIELD_MIN_VELOCITY)) {
                            ed.setText(String.format("%d", editorSample.getMinVelocity()));
                        } else {
                            ed.setText("");
                        }
                        break;
                    case R.id.velocity_max:
                        if(editorSample.shouldDisplay(Sample.FIELD_MAX_VELOCITY)) {
                            ed.setText(String.format("%d", editorSample.getMaxVelocity()));
                        } else {
                            ed.setText("");
                        }
                        break;
                    case R.id.position_start:
                        if(editorSample.shouldDisplay(Sample.FIELD_LOOP_START)) {
                            ed.setText(fmt7.format(editorSample.getStartTime()));
                        } else {
                            ed.setText("");
                        }
                        break;
                    case R.id.position_end:
                        if(editorSample.shouldDisplay(Sample.FIELD_LOOP_END)) {
                            ed.setText(fmt7.format(editorSample.getEndTime()));
                        } else {
                            ed.setText("");
                        }
                        break;
                    case R.id.position_resume:
                        if(editorSample.shouldDisplay(Sample.FIELD_LOOP_RESUME)) {
                            ed.setText(fmt7.format(editorSample.getResumeTime()));
                        } else {
                            ed.setText("");
                        }
                        break;
                    default:
                        break;
                }
            }

            for(int i = 0; i < sliderTextPairInputs.length; i += 2) {
                VerticalSlider slider = rootView.findViewById(sliderTextPairInputs[i]);
                EditText ed = rootView.findViewById(sliderTextPairInputs[i + 1]);
                switch(sliderTextPairInputs[i + 1]) {
                    case R.id.sample_volume:
                        float sVol = editorSample.getVolumeDecibels();
                        sVol = Math.max(-100, Math.min(0, sVol));
                        ed.setText(fmt3.format(sVol));
                        slider.setProgress(SliderConverter.DECIBELS.toSlider(sVol));
                        break;
                    case R.id.sample_attack:
                        ed.setText(fmt3.format(editorSample.getAttack()));
                        slider.setProgress(SliderConverter.MILLISECONDS.toSlider(editorSample.getAttack()));
                        break;
                    case R.id.sample_decay:
                        ed.setText(fmt3.format(editorSample.getDecay()));
                        slider.setProgress(SliderConverter.MILLISECONDS.toSlider(editorSample.getDecay()));
                        break;
                    case R.id.sample_sustain:
                        float sus = editorSample.getSustainDecibels();
                        sus = Math.max(-100, Math.min(0, sus));
                        ed.setText(fmt3.format(sus));
                        slider.setProgress(SliderConverter.DECIBELS.toSlider(sus));
                        break;
                    case R.id.sample_release:
                        ed.setText(fmt3.format(editorSample.getRelease()));
                        slider.setProgress(SliderConverter.MILLISECONDS.toSlider(editorSample.getRelease()));
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
                VerticalSlider slider = rootView.findViewById(sliderTextPairInputs[i]);
                EditText ed = rootView.findViewById(sliderTextPairInputs[i + 1]);
                slider.setProgress(0);
                ed.setText("");
            }
        }

        instrumentEditorState = ModelState.LOADED;
        if(!isInstrumentEditorReady) {
            attachInstrumentEditorListeners();
            isInstrumentEditorReady = true;
        }
    }

    private class MyInstrumentActionConsumer implements InstrumentListAdapter.InstrumentActionConsumer {
        @Override
        public void startCreate() {
            FragmentManager fm = ProjectInstrumentsFragment.this.getFragmentManager();
            if(fm != null) {
                InstrumentCreateDialog dialog = new InstrumentCreateDialog();
                Instrument toCreate = new Instrument(null);
                viewModel.setDialogInstrument(toCreate);
                viewModel.getProject().registerInstrument(toCreate);
                dialog.show(fm, "InstrumentCreateDialog");
            }
        }

        @Override
        public void startRename(Instrument instrument) {
            FragmentManager fm = ProjectInstrumentsFragment.this.getFragmentManager();
            if(fm != null) {
                InstrumentEditDialog dialog = new InstrumentEditDialog();
                viewModel.setDialogInstrument(instrument);
                dialog.show(fm, "InstrumentEditDialog");
            }
        }

        @Override
        public void startExport(Instrument instrument) {
            FragmentManager fm = ProjectInstrumentsFragment.this.getFragmentManager();
            if(fm != null) {
                InstrumentExportDialog dialog = new InstrumentExportDialog();
                viewModel.setDialogInstrument(instrument);
                dialog.show(fm, "InstrumentExportDialog");
            }
        }

        @Override
        public void select(Instrument instrument) {
            instrumentListAdapter.activateInstrument(instrument);
            viewModel.setKeyboardInstrument(instrument);
        }
    }
}
