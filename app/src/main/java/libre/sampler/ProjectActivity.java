package libre.sampler;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import org.puredata.android.io.AudioParameters;
import org.puredata.android.service.PdPreferences;
import org.puredata.android.service.PdService;
import org.puredata.android.utils.PdUiDispatcher;
import org.puredata.core.PdBase;
import org.puredata.core.utils.IoUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.util.Consumer;
import androidx.lifecycle.ViewModelProviders;
import androidx.viewpager.widget.ViewPager;
import libre.sampler.adapters.ProjectFragmentAdapter;
import libre.sampler.listeners.PdFloatListener;
import libre.sampler.listeners.PdListListener;
import libre.sampler.models.InstrumentEvent;
import libre.sampler.models.NoteEvent;
import libre.sampler.models.Pattern;
import libre.sampler.models.PatternEvent;
import libre.sampler.models.ProjectViewModel;
import libre.sampler.models.Sample;
import libre.sampler.publishers.MidiEventDispatcher;
import libre.sampler.tasks.UpdateProjectTask;
import libre.sampler.utils.AppConstants;
import libre.sampler.utils.DatabaseConnectionManager;
import libre.sampler.utils.PatternThread;
import libre.sampler.utils.SampleBindingList;
import libre.sampler.utils.VoiceBindingList;

public class ProjectActivity extends AppCompatActivity {
    private ViewPager pager;
    private ProjectFragmentAdapter fragmentAdapter;

    public PatternThread patternThread;

    private PdService pdService = null;
    private PdUiDispatcher pdReceiver;
    private final ServiceConnection pdConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            pdService = ((PdService.PdBinder)service).getService();
            initPd();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            pdService = null;
        }
    };

    public ProjectViewModel viewModel;

    private VoiceBindingList pdVoiceBindings;
    private SampleBindingList pdSampleBindings;
    private int pdPatchHandle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project);
        ActionBar toolbar = getSupportActionBar();
        if(toolbar != null) {
            toolbar.setTitle(getIntent().getStringExtra(Intent.EXTRA_TITLE));
        }

        this.viewModel = ViewModelProviders.of(this).get(ProjectViewModel.class);
        viewModel.setProjectId(getIntent().getIntExtra(AppConstants.TAG_EXTRA_PROJECT_ID, -1));

        attachEventListeners();
        initPatternThread();
        initUI();
        initPdService();

        viewModel.tryGetProject();

        refreshMidiConnection();
    }

    private void attachEventListeners() {
        viewModel.keyboardNoteSource.add("Evolve", new Consumer<NoteEvent>() {
            @Override
            public void accept(NoteEvent event) {
                event.instrument = viewModel.getKeyboardInstrument();
                viewModel.noteEventSource.dispatch(event);
            }
        });

        viewModel.noteEventSource.add("logger", new Consumer<NoteEvent>() {
            @Override
            public void accept(NoteEvent noteEvent) {
                Log.d("ProjectActivity", String.format("noteEvent: action=%d keynum=%d velocity=%d id=%d", noteEvent.action, noteEvent.keyNum, noteEvent.velocity, noteEvent.eventId.second));
            }
        });

        viewModel.instrumentEventSource.add("InstrumentLoader", new Consumer<InstrumentEvent>() {
            @Override
            public void accept(InstrumentEvent event) {
                if(event.action == InstrumentEvent.INSTRUMENT_KEYBOARD_SELECT ||
                        event.action == InstrumentEvent.INSTRUMENT_PIANO_ROLL_SELECT ||
                        event.action == InstrumentEvent.INSTRUMENT_PD_LOAD) {

                    if(pdService != null && pdService.isRunning()) {
                        Log.d("InstrumentLoader", "sync  " + event.instrument.name);
                        for(Sample s : event.instrument.getSamples()) {
                            if(pdSampleBindings.getBinding(s)) {
                                PdBase.sendList("sample_file", s.sampleIndex, s.filename);
                            }
                        }
                    } else {
                        Log.d("InstrumentLoader", "async " + event.instrument.name);
                        viewModel.instrumentEventSource.addToReplayQueue(InstrumentEvent.QUEUE_FOR_INIT_PD, event);
                    }
                }
            }
        });

        viewModel.noteEventSource.add("NoteEventConsumer", new NoteEventConsumer());

        viewModel.patternEventSource.add("test", new Consumer<PatternEvent>() {
            @Override
            public void accept(PatternEvent event) {
                if(event.action == PatternEvent.PATTERN_ON) {
                    patternThread.addPattern("test", event.pattern);
                } else if(event.action == PatternEvent.PATTERN_OFF) {
                    closeNotes();
                    patternThread.clearPatterns();
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

    private void initPdService() {
        AudioParameters.init(this);
        PdPreferences.initPreferences(getApplicationContext());
        Intent serviceIntent = new Intent(this, PdService.class);
        bindService(serviceIntent, pdConnection, BIND_AUTO_CREATE);
        startService(serviceIntent);
    }

    private void initPd() {
        pdVoiceBindings = new VoiceBindingList(AppConstants.PD_NUM_VOICES);
        pdSampleBindings = new SampleBindingList(AppConstants.PD_NUM_SAMPLES);
        pdReceiver = new PdUiDispatcher();

        Resources res = getResources();
        File patchFile = null;
        try {
            PdBase.setReceiver(pdReceiver);
            initPdReceiverListeners();
            if(!PdBase.exists("voices")) {
                InputStream in = res.openRawResource(R.raw.pd_test);
                patchFile = IoUtils.extractResource(in, "test.pd", getCacheDir());
                pdPatchHandle = PdBase.openPatch(patchFile);
            }
            pdService.initAudio(AudioParameters.suggestSampleRate(), 0, 2, 8);
            pdService.startAudio();
            viewModel.instrumentEventSource.runReplayQueue(InstrumentEvent.QUEUE_FOR_INIT_PD);
        } catch (IOException e) {
            finish();
        } finally {
            if(patchFile != null) patchFile.delete();
        }
    }

    private void initPdReceiverListeners() {
        pdReceiver.addListener("voice_free", new PdFloatListener() {
            @Override
            public void receiveFloat(String source, float x) {
                int indexToFree = (int) x;
                pdVoiceBindings.voiceFree(indexToFree);
                Log.d("PdUiDispatcher", "voice_free " + indexToFree);
            }
        });
        pdReceiver.addListener("sample_info", new PdListListener() {
            @Override
            public void receiveList(String source, Object... args) {
                if(args.length >= 3) {
                    try {
                        int sampleIndex = (int)((float) args[0]);
                        pdSampleBindings.setSampleInfo(sampleIndex,
                                (int)((float) args[args.length - 1]), (int)((float) args[2]));

                        Log.d("PdUiDispatcher", String.format("sample_info index=%d length=%d rate=%d",
                                sampleIndex, (int)((float) args[args.length - 1]), (int)((float) args[2])));
                    } catch(ClassCastException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(fragmentAdapter == null) {
            initUI();
        }
        if(pdService == null) {
            initPdService();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(patternThread != null) {
            patternThread.suspendLoop();
        }
        if(pdService != null) {
            closeNotes();
            pdService.stopAudio();
        }
    }

    @SuppressLint("NewApi")
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(patternThread != null) {
            patternThread.finish();
        }
        unbindService(pdConnection);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_project, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.appbar_save) {
            viewModel.getProject().mtime = System.currentTimeMillis();
            patternThread.lock.lock();
            try {
                for(Pattern p : viewModel.getProject().getPatterns()) {
                    p.prepareEventsDeepCopy();
                }
            } finally {
                patternThread.lock.unlock();
            }
            DatabaseConnectionManager.runTask(new UpdateProjectTask(viewModel.getProject(), new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(ProjectActivity.this, R.string.project_saved, Toast.LENGTH_SHORT).show();
                }
            }));
            return true;
        } else if(item.getItemId() == R.id.appbar_refresh_midi) {
            refreshMidiConnection();
        } else if(item.getItemId() == R.id.appbar_export) {
            Log.d("ProjectActivity", "export");
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
        if(pdService == null || !pdService.isRunning()) {
            return;
        }
        List<NoteEvent> events = pdVoiceBindings.getCloseEvents();
        for(NoteEvent e : events) {
            viewModel.noteEventSource.dispatch(e);
        }
    }

    private class NoteEventConsumer implements Consumer<NoteEvent> {
        @Override
        public void accept(NoteEvent noteEvent) {
            if(pdService != null && noteEvent.instrument != null) {
                List<Sample> samples = noteEvent.instrument.getSamplesForEvent(noteEvent);
                if(samples.size() == 0) {
                    return;
                }
                try {
                    if(!pdService.isRunning()) {
                        pdService.initAudio(AudioParameters.suggestSampleRate(), 0, 2, 8);
                        pdService.startAudio();
                    }
                    for(Sample s : samples) {
                        if(!s.isInfoLoaded() || !s.checkLoop()) {
                            continue;
                        }

                        if(noteEvent.action == NoteEvent.NOTE_ON) {
                            float adjVelocity = (float) (Math.pow(noteEvent.velocity / 128.0, 2) *
                                    noteEvent.instrument.getVolume() * s.getVolume());
                            Log.d("NoteEventConsumer", String.valueOf(adjVelocity));
                            int voiceIndex = pdVoiceBindings.getBinding(noteEvent, s.id);
                            if(voiceIndex == -1) {
                                continue;
                            }
                            PdBase.sendList("note", voiceIndex, noteEvent.keyNum,
                                    /*velocity*/   adjVelocity,
                                    /*ADSR*/       s.attack, s.decay, s.sustain, s.release,
                                    /*sampleInfo*/ s.sampleIndex, s.getLoopStart(), s.getLoopResume(), s.getLoopEnd(), s.sampleRate, s.basePitch);
                        } else if(noteEvent.action == NoteEvent.NOTE_OFF) {
                            int voiceIndex = pdVoiceBindings.releaseBinding(noteEvent, s.id);
                            if(voiceIndex == -1) {
                                continue;
                            }
                            PdBase.sendList("note", voiceIndex, noteEvent.keyNum,
                                    /*velocity*/   0,
                                    /*ADSR*/       s.attack, s.decay, s.sustain, s.release,
                                    /*sampleInfo*/ s.sampleIndex, s.getLoopStart(), s.getLoopResume(), s.getLoopEnd(), s.sampleRate, s.basePitch);
                        } else if(noteEvent.action == NoteEvent.CLOSE) {
                            int voiceIndex = pdVoiceBindings.releaseBinding(noteEvent, s.id);
                            if(voiceIndex == -1) {
                                continue;
                            }
                            PdBase.sendList("note", voiceIndex, noteEvent.keyNum,
                                    /*velocity*/   0,
                                    /*ADSR*/       s.attack, s.decay, s.sustain, 0,
                                    /*sampleInfo*/ s.sampleIndex, s.getLoopStart(), s.getLoopResume(), s.getLoopEnd(), s.sampleRate, s.basePitch);
                        }
                    }
                } catch(IOException e) {
                    e.printStackTrace();
                }
            } else if(noteEvent.action != NoteEvent.NOTHING) {
                Log.d("NoteEventConsumer", "Null instrument");
            }
        }
    }
}