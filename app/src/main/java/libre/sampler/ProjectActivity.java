package libre.sampler;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.util.Consumer;
import androidx.lifecycle.ViewModelProviders;
import androidx.viewpager.widget.ViewPager;
import libre.sampler.adapters.ProjectFragmentAdapter;
import libre.sampler.dialogs.ProjectLeaveDialog;
import libre.sampler.fragments.ProjectPatternsFragment;
import libre.sampler.listeners.SampleLoadListener;
import libre.sampler.listeners.VoiceFreeListener;
import libre.sampler.models.InstrumentEvent;
import libre.sampler.models.NoteEvent;
import libre.sampler.models.Pattern;
import libre.sampler.models.PatternDerivedData;
import libre.sampler.models.PatternEvent;
import libre.sampler.models.ProjectViewModel;
import libre.sampler.models.Sample;
import libre.sampler.publishers.MidiEventDispatcher;
import libre.sampler.tasks.UpdateProjectTask;
import libre.sampler.utils.AppConstants;
import libre.sampler.utils.DatabaseConnectionManager;
import libre.sampler.utils.PatternThread;
import libre.sampler.utils.VoiceBindingList;
import libre.sampler.views.VisualNote;

public class ProjectActivity extends AppCompatActivity {
    public static final String TAG = "ProjectActivity";
    private ViewPager pager;
    private ProjectFragmentAdapter fragmentAdapter;
    private OnBackPressedCallback backPressedCallback;

    private PatternThread patternThread;

    public ProjectViewModel viewModel;

    private VoiceBindingList pdVoiceBindings;
    private Map<String, Sample> pdSampleBindings;

    static {
        System.loadLibrary("native-lib");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project);
        final ActionBar toolbar = getSupportActionBar();
        if(toolbar != null) {
            toolbar.setTitle(getIntent().getStringExtra(Intent.EXTRA_TITLE));
        }

        this.viewModel = ViewModelProviders.of(this).get(ProjectViewModel.class);
        viewModel.setProjectId(getIntent().getStringExtra(AppConstants.TAG_EXTRA_PROJECT_ID));

        attachEventListeners();
        initPatternThread();
        initUI();

        viewModel.tryGetProject();

        refreshMidiConnection();

        backPressedCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                this.setEnabled(false);
                supportNavigateUpTo(getSupportParentActivityIntent());
                this.setEnabled(true);
            }
        };
        getOnBackPressedDispatcher().addCallback(this, backPressedCallback);
    }

    private void attachEventListeners() {
        viewModel.instrumentEventSource.add(TAG, new Consumer<InstrumentEvent>() {
            @Override
            public void accept(InstrumentEvent event) {
                if(event.action == InstrumentEvent.INSTRUMENT_KEYBOARD_SELECT ||
                        event.action == InstrumentEvent.INSTRUMENT_PIANO_ROLL_SELECT ||
                        event.action == InstrumentEvent.INSTRUMENT_PD_LOAD) {

                    if(pdSampleBindings != null) {
                        viewModel.instrumentEventSource.runReplayQueue(InstrumentEvent.QUEUE_FOR_INIT_PD);
                        Log.d("InstrumentLoader", "sync  " + event.instrument.name);
                        for(Sample s : event.instrument.getSamples()) {
                            boolean exists = event.action != InstrumentEvent.INSTRUMENT_PD_LOAD
                                    && pdSampleBindings.containsKey(s.id)
                                    && pdSampleBindings.get(s.id).isInfoLoaded();
                            if(!exists) {
                                pdSampleBindings.put(s.id, s);
                                loadSoundFile(s.id, s.filename);
                            }
                        }
                    } else {
                        Log.d("InstrumentLoader", "async " + event.instrument.name);
                        viewModel.instrumentEventSource.addToReplayQueue(InstrumentEvent.QUEUE_FOR_INIT_PD, event);
                    }
                } else if(event.action == InstrumentEvent.INSTRUMENT_PREDELETE) {
                    for(Pattern p : viewModel.getProject().getPatterns()) {
                        PatternDerivedData derivedData = viewModel.getPatternDerivedData(p);
                        List<VisualNote> toDelete = derivedData.getNotesForInstrument(event.instrument);
                        if(toDelete != null) {
                            try(PatternThread.Editor editor = patternThread.getEditor(viewModel.getPianoRollPattern())) {
                                ProjectPatternsFragment.removeBatchFromPattern(editor, toDelete, viewModel.noteEventSource);
                            }
                            derivedData.removeInstrument(event.instrument);
                        }
                    }
                }
            }
        });

        viewModel.noteEventSource.add(TAG, new NoteEventConsumer());

        viewModel.patternEventSource.add(TAG, new Consumer<PatternEvent>() {
            @Override
            public void accept(PatternEvent event) {
                if(patternThread == null) {
                    return;
                }
                if(event.action == PatternEvent.PATTERN_ON) {
                    // Derived data must be complete before pattern starts playing
                    viewModel.getPatternDerivedData(event.pattern);
                    patternThread.addPattern("test", event.pattern);
                } else if(event.action == PatternEvent.PATTERN_OFF) {
                    closeNotes();
                    patternThread.clearPatterns();
                    patternThread.resumeLoop();
                }
            }
        });
    }

    private void initPatternThread() {
        patternThread = new PatternThread(viewModel.noteEventSource);
        patternThread.start();
    }

    private void initUI() {
        pager = findViewById(R.id.pager);
        fragmentAdapter = new ProjectFragmentAdapter(getSupportFragmentManager());
        pager.setAdapter(fragmentAdapter);
    }

    private void initPdReceiverListeners() {
        startAudioService();
        pdSampleBindings = new HashMap<>(AppConstants.PD_NUM_SAMPLES);
        pdVoiceBindings = new VoiceBindingList(AppConstants.PD_NUM_VOICES);

        setVoiceFreeListener(new VoiceFreeListener() {
            @Override
            public void voiceFree(int indexToFree) {
                pdVoiceBindings.voiceFree(indexToFree);
                Log.d("MyPdDispatcher", "voice_free " + indexToFree);
            }
        });

        setSampleLoadListener(new SampleLoadListener() {
            @Override
            public void setSampleInfo(String sampleId, int sampleLength, int sampleRate) {
                Sample s = pdSampleBindings.get(sampleId);
                if (s != null) {
                    s.setSampleInfo(sampleLength, sampleRate);
                    Log.d("MyPdDispatcher", String.format("sample_info filename=%s length=%d rate=%d",
                            s.filename.replaceAll(".*/", ""), sampleLength, sampleRate));
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(viewModel.getKeyboardInstrument() != null) {
            viewModel.instrumentEventSource.dispatch(new InstrumentEvent(InstrumentEvent.INSTRUMENT_PD_LOAD,
                    viewModel.getKeyboardInstrument()));
        }
        initPatternThread();
        if(fragmentAdapter == null) {
            initUI();
        }
        initPdReceiverListeners();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(patternThread != null) {
            patternThread.finish();
        }
        viewModel.instrumentEventSource.clearReplayQueue(InstrumentEvent.QUEUE_FOR_INIT_PD);
        closeNotes();
        stopAudioService();
    }

    @SuppressLint("NewApi")
    @Override
    protected void onDestroy() {
        super.onDestroy();
        backPressedCallback = null;
        viewModel.instrumentEventSource.remove(TAG);
        viewModel.noteEventSource.remove(TAG);
        viewModel.patternEventSource.remove(TAG);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_project, menu);
        return true;
    }

    @Override
    public void supportNavigateUpTo(@NonNull Intent upIntent) {
        if(viewModel.projectHasUnsavedChanges() && !upIntent.getBooleanExtra(AppConstants.TAG_EXTRA_PROJECT_DISCARD, false)) {
            ProjectLeaveDialog dialog = new ProjectLeaveDialog();
            dialog.setUpIntent(upIntent);
            dialog.show(getSupportFragmentManager(), "ProjectLeaveDialog");
        } else {
            super.supportNavigateUpTo(upIntent);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId() == R.id.appbar_save) {
            viewModel.prepareSave();
            DatabaseConnectionManager.runTask(new UpdateProjectTask(viewModel.getProject(),
                    new UpdateProjectTaskCallback(new WeakReference<Context>(this))));
            return true;
        } else if(item.getItemId() == R.id.appbar_refresh_midi) {
            refreshMidiConnection();
        }
        return super.onOptionsItemSelected(item);
    }

    private void refreshMidiConnection() {
        MidiEventDispatcher midiEventDispatcher = viewModel.getMidiEventDispatcher();
        if(midiEventDispatcher != null) {
            Log.d("MidiEventDispatcher", "obtained");
        }
    }

    private void closeNotes() {
        List<NoteEvent> events = pdVoiceBindings.getCloseEvents();
        for(NoteEvent e : events) {
            viewModel.noteEventSource.dispatch(e);
        }
    }

    public PatternThread getPatternThread() {
        return patternThread;
    }

    public boolean isPatternThreadRunning() {
        return (patternThread.runningPatterns.size() > 0);
    }

    public boolean isPatternThreadPaused() {
        return patternThread.isSuspended;
    }

    public static class UpdateProjectTaskCallback implements Runnable {
        private final WeakReference<Context> contextRef;

        public UpdateProjectTaskCallback(WeakReference<Context> context) {
            this.contextRef = context;
        }

        @Override
        public void run() {
            if(this.contextRef.get() != null) {
                Toast.makeText(this.contextRef.get(), R.string.project_saved, Toast.LENGTH_SHORT).show();
            }
        }
    }

    public native void startAudioService();
    public native void stopAudioService();
    public native void loadSoundFile(String sampleId, String filename);
    public native void sendNoteMsg(int voiceIndex, int keynum, float velocity, float attack,
                                   float decay, float sustain, float release, String sampleId,
                                   float start, float resume, float end, float baseKey);
    public native void setVoiceFreeListener(VoiceFreeListener l);
    public native void setSampleLoadListener(SampleLoadListener l);

    private class NoteEventConsumer implements Consumer<NoteEvent> {
        @Override
        public void accept(NoteEvent noteEvent) {
            if(noteEvent.instrument != null) {
                List<Sample> samples = null;
                if(noteEvent.action == NoteEvent.NOTE_ON) {
                    samples = noteEvent.instrument.getSamplesForEvent(noteEvent, true);
                } else if(noteEvent.action == NoteEvent.NOTE_OFF) {
                    NoteEvent openEvent = pdVoiceBindings.getOpenEvent(noteEvent);
                    if(openEvent == null) {
                        samples = noteEvent.instrument.getSamplesForEvent(noteEvent, false);
                    } else {
                        samples = noteEvent.instrument.getSamplesForEvent(openEvent, true);
                    }
                } else if(noteEvent.action == NoteEvent.CLOSE) {
                    samples = noteEvent.instrument.getSamplesForEvent(noteEvent, false);
                }
                if(samples == null || samples.size() == 0) {
                    return;
                }
                for(Sample s : samples) {
                    if(!s.isInfoLoaded() || !s.checkLoop()) {
                        continue;
                    }

                    if(noteEvent.action == NoteEvent.NOTE_ON) {
                        float adjVelocity = (float) (Math.pow(noteEvent.velocity / 128.0, 2) *
                                noteEvent.instrument.getVolume() * s.getVolume());
                        int voiceIndex = pdVoiceBindings.newBinding(noteEvent, s.id);
                        if(voiceIndex == -1) {
                            continue;
                        }
                        Log.d("NoteEventConsumer", String.format("NOTE_ON : voice=%d sample=%s vel=%d",
                                voiceIndex, s.filename.replaceAll(".*/", ""), noteEvent.velocity));
                        sendNoteMsg(voiceIndex, noteEvent.keyNum,
                                /*velocity*/   adjVelocity,
                                /*ADSR*/       s.getAttack(), s.getDecay(), s.getSustain(), s.getRelease(),
                                /*sampleInfo*/ s.id, s.getLoopStart(), s.getLoopResume(), s.getLoopEnd(), s.getBasePitch());
                    } else if(noteEvent.action == NoteEvent.NOTE_OFF) {
                        int voiceIndex = pdVoiceBindings.releaseBinding(noteEvent, s.id);
                        if(voiceIndex == -1) {
                            continue;
                        }
                        Log.d("NoteEventConsumer", String.format("NOTE_OFF: voice=%d sample=%s",
                                voiceIndex, s.filename.replaceAll(".*/", "")));
                        sendNoteMsg(voiceIndex, noteEvent.keyNum,
                                /*velocity*/   0,
                                /*ADSR*/       s.getAttack(), s.getDecay(), s.getSustain(), s.getRelease(),
                                /*sampleInfo*/ s.id, s.getLoopStart(), s.getLoopResume(), s.getLoopEnd(), s.getBasePitch());
                    } else if(noteEvent.action == NoteEvent.CLOSE) {
                        int voiceIndex = pdVoiceBindings.releaseBinding(noteEvent, s.id);
                        if(voiceIndex == -1) {
                            continue;
                        }
                        sendNoteMsg(voiceIndex, noteEvent.keyNum,
                                /*velocity*/   0,
                                /*ADSR*/       s.getAttack(), s.getDecay(), s.getSustain(), 0,
                                /*sampleInfo*/ s.id, s.getLoopStart(), s.getLoopResume(), s.getLoopEnd(), s.getBasePitch());
                    }
                }
            } else if(noteEvent.action != NoteEvent.NOTHING) {
                Log.d("NoteEventConsumer", "Null instrument");
            }
        }
    }
}