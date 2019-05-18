package libre.sampler;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.media.midi.MidiDeviceInfo;
import android.media.midi.MidiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcelable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import org.puredata.android.io.AudioParameters;
import org.puredata.android.service.PdPreferences;
import org.puredata.android.service.PdService;
import org.puredata.core.PdBase;
import org.puredata.core.PdReceiver;
import org.puredata.core.utils.IoUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.util.Consumer;
import androidx.viewpager.widget.ViewPager;
import libre.sampler.adapters.InstrumentListAdapter;
import libre.sampler.adapters.ProjectFragmentAdapter;
import libre.sampler.models.Instrument;
import libre.sampler.models.InstrumentEvent;
import libre.sampler.models.NoteEvent;
import libre.sampler.models.Pattern;
import libre.sampler.models.PatternEvent;
import libre.sampler.models.Project;
import libre.sampler.models.Sample;
import libre.sampler.publishers.InstrumentEventSource;
import libre.sampler.publishers.MidiEventDispatcher;
import libre.sampler.publishers.NoteEventSource;
import libre.sampler.publishers.PatternEventSource;
import libre.sampler.tasks.GetInstrumentsTask;
import libre.sampler.tasks.UpdateProjectTask;
import libre.sampler.utils.AdapterLoader;
import libre.sampler.utils.AppConstants;
import libre.sampler.utils.DatabaseConnectionManager;
import libre.sampler.utils.PatternThread;
import libre.sampler.utils.SampleBindingList;
import libre.sampler.utils.VoiceBindingList;

public class ProjectActivity extends AppCompatActivity {
    private ViewPager pager;
    private ProjectFragmentAdapter adapter;

    public NoteEventSource noteEventSource;
    public InstrumentEventSource instrumentEventSource;
    public PatternEventSource patternEventSource;
    public PatternThread patternThread;

    private PdService pdService = null;
    private PdReceiver pdReceiver = new MyPdReceiver();
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

    public Project project;
    private boolean projectLoaded;

    public Instrument keyboardInstrument;
    public Pattern pianoRollPattern;

    private VoiceBindingList pdVoiceBindings;
    private SampleBindingList pdSampleBindings;
    private int pdPatchHandle;

    public InstrumentListAdapter instrumentListAdapter;
    private MidiEventDispatcher midiEventDispatcher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project);
        ActionBar toolbar = getSupportActionBar();
        if(toolbar != null) {
            toolbar.setTitle(getIntent().getStringExtra(Intent.EXTRA_TITLE));
        }

        initEventSources();
        initUI();
        initPdService();

        projectLoaded = false;
        if(savedInstanceState != null) {
            project = savedInstanceState.getParcelable(AppConstants.TAG_SAVED_STATE_PROJECT);
            projectLoaded = (project != null);
            if(projectLoaded) {
                int keyboardInstrumentIdx = savedInstanceState.getInt(AppConstants.TAG_SAVED_STATE_KEYBOARD_INSTRUMENT);
                if(keyboardInstrumentIdx != -1) {
                    keyboardInstrument = project.getInstruments().get(keyboardInstrumentIdx);
                }
                int pianoRollPatternIdx = savedInstanceState.getInt(AppConstants.TAG_SAVED_STATE_PIANO_ROLL_PATTERN);
                if(pianoRollPatternIdx != -1) {
                    pianoRollPattern = project.getPatterns().get(pianoRollPatternIdx);
                }
                updateInstrumentListAdapter();
            }
            if(savedInstanceState.getByte(AppConstants.TAG_SAVED_STATE_MIDI_CONNECTED) != 0) {
                refreshMidiConnection();
            }
        }
        if(!projectLoaded) {
            Log.d("ProjectActivity", "Project load");
            project = getIntent().getParcelableExtra(AppConstants.TAG_EXTRA_PROJECT);
            DatabaseConnectionManager.initialize(this);
            DatabaseConnectionManager.runTask(new GetInstrumentsTask(project.id, new Consumer<List<Instrument>>() {
                @Override
                public void accept(List<Instrument> instruments) {
                    project.setInstruments(instruments);
                    if(instruments.size() > 0) {
                        keyboardInstrument = instruments.get(0);
                    }
                    projectLoaded = true;
                    updateInstrumentListAdapter();
                }
            }));
        }
    }

    public void setInstrumentListAdapter(InstrumentListAdapter adapter) {
        this.instrumentListAdapter = adapter;
        if(projectLoaded) {
            // project was loaded before adapter
            updateInstrumentListAdapter();
        }
        // else update from GetInstrumentsTask callback
    }

    public void updateInstrumentListAdapter() {
        if(project != null && instrumentListAdapter != null) {
            AdapterLoader.insertAll(instrumentListAdapter, project.getInstruments());
            instrumentListAdapter.activateInstrument(keyboardInstrument);
        }
    }

    private void initEventSources() {
        noteEventSource = new NoteEventSource();
        instrumentEventSource = new InstrumentEventSource();
        instrumentEventSource.add("keyboard", new InstrumentEventConsumer());
        patternEventSource = new PatternEventSource();
        patternThread = new PatternThread(noteEventSource);
        patternThread.start();
    }

    private void initUI() {
        pager = findViewById(R.id.pager);
        adapter = new ProjectFragmentAdapter(getSupportFragmentManager());
        pager.setAdapter(adapter);
    }

    private void initPdService() {
        final String name = getResources().getString(R.string.app_name);
        noteEventSource.add("KeyboardNoteEventConsumer", new KeyboardNoteEventConsumer(name));

        patternEventSource.add("test", new Consumer<PatternEvent>() {
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

        AudioParameters.init(this);
        PdPreferences.initPreferences(getApplicationContext());
        Intent serviceIntent = new Intent(this, PdService.class);
        bindService(serviceIntent, pdConnection, BIND_AUTO_CREATE);
        startService(serviceIntent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(noteEventSource == null) {
            initEventSources();
        } else {
            patternThread.resumeLoop();
        }

        if(adapter == null) {
            initUI();
        }
        if(pdService == null) {
            initPdService();
        }
    }

    private void initPd() {
        pdVoiceBindings = new VoiceBindingList(AppConstants.PD_NUM_VOICES);
        pdSampleBindings = new SampleBindingList(AppConstants.PD_NUM_SAMPLES);

        Resources res = getResources();
        File patchFile = null;
        try {
            PdBase.setReceiver(pdReceiver);
            PdBase.subscribe("voice_free");
            PdBase.subscribe("sample_info");
            InputStream in = res.openRawResource(R.raw.pd_test);
            if(!PdBase.exists("voices")) {
                patchFile = IoUtils.extractResource(in, "test.pd", getCacheDir());
                pdPatchHandle = PdBase.openPatch(patchFile);
            }
            pdService.initAudio(AudioParameters.suggestSampleRate(), 0, 2, 8);
            pdService.startAudio();
            if(keyboardInstrument != null) {
                instrumentEventSource.dispatch(new InstrumentEvent(InstrumentEvent.INSTRUMENT_SELECT, keyboardInstrument));
            }
        } catch (IOException e) {
            finish();
        } finally {
            if(patchFile != null) patchFile.delete();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        patternThread.suspendLoop();
        closeNotes();
        pdService.stopAudio();
    }

    @SuppressLint("NewApi")
    @Override
    protected void onDestroy() {
        super.onDestroy();
        patternThread.finish();
        unbindService(pdConnection);
        if(midiEventDispatcher != null) {
            midiEventDispatcher.closeMidi();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(AppConstants.TAG_SAVED_STATE_PROJECT, (Parcelable) project);
        outState.putInt(AppConstants.TAG_SAVED_STATE_KEYBOARD_INSTRUMENT, project.getInstruments().indexOf(keyboardInstrument));
        outState.putInt(AppConstants.TAG_SAVED_STATE_PIANO_ROLL_PATTERN, project.getPatterns().indexOf(pianoRollPattern));
        outState.putByte(AppConstants.TAG_SAVED_STATE_MIDI_CONNECTED, (byte) (midiEventDispatcher != null ? 1 : 0));
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
            project.mtime = System.currentTimeMillis();
            DatabaseConnectionManager.initialize(this);
            DatabaseConnectionManager.runTask(new UpdateProjectTask(project, new Runnable() {
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
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            MidiManager midiManager = (MidiManager) getSystemService(MIDI_SERVICE);
            MidiDeviceInfo[] midiDeviceInfos = midiManager.getDevices();
            if(midiDeviceInfos.length > 0) {
                if(midiEventDispatcher == null) {
                    midiEventDispatcher = new MidiEventDispatcher();  // ((View) item).getHandler()
                }
                midiManager.openDevice(midiDeviceInfos[0], midiEventDispatcher, new Handler());
                Log.d("midiManager", "openDevice called");
                midiEventDispatcher.noteEventSource = noteEventSource;
                midiEventDispatcher.patternEventSource = patternEventSource;
            }
        }
    }

    private void closeNotes() {
        if(pdService == null || !pdService.isRunning() || noteEventSource == null) {
            return;
        }
        List<NoteEvent> events = pdVoiceBindings.getCloseEvents();
        for(NoteEvent e : events) {
            noteEventSource.dispatch(e);
        }
    }

    private class InstrumentEventConsumer implements Consumer<InstrumentEvent> {
        @Override
        public void accept(InstrumentEvent event) {
            if(event.action == InstrumentEvent.INSTRUMENT_CREATE) {
                project.addInstrument(event.instrument);
                if(instrumentListAdapter != null) {
                    AdapterLoader.insertItem(instrumentListAdapter, event.instrument);
                }
            } else if(event.action == InstrumentEvent.INSTRUMENT_EDIT) {
                int changeIdx = instrumentListAdapter.items.indexOf(event.instrument);
                if(changeIdx != -1) {
                    instrumentListAdapter.notifyItemChanged(changeIdx);
                }
                if(event.instrument == keyboardInstrument) {
                    instrumentListAdapter.activateInstrument(keyboardInstrument);
                }
            } else if(event.action == InstrumentEvent.INSTRUMENT_DELETE) {
                project.removeInstrument(event.instrument);
                AdapterLoader.removeItem(instrumentListAdapter, event.instrument);
            } else if(event.action == InstrumentEvent.INSTRUMENT_SELECT) {
                keyboardInstrument = event.instrument;
                for(Sample s : keyboardInstrument.getSamples()) {
                    if(pdSampleBindings.getBinding(s)) {
                        PdBase.sendList("sample_file", s.sampleIndex, s.filename);
                    }
                }
            }
        }
    }

    private class KeyboardNoteEventConsumer implements Consumer<NoteEvent> {
        private final String name;

        public KeyboardNoteEventConsumer(String name) {
            this.name = name;
        }

        @Override
        public void accept(NoteEvent noteEvent) {
            if(pdService != null && keyboardInstrument != null) {
                List<Sample> samples = keyboardInstrument.getSamplesForEvent(noteEvent);
                if(samples.size() == 0) {
                    return;
                }
                try {
                    if(!pdService.isRunning()) {
                        pdService.initAudio(AudioParameters.suggestSampleRate(), 0, 2, 8);
                        pdService.startAudio();
                    }
                    for(Sample s : samples) {
                        if(!s.isInfoLoaded || !s.checkLoop()) {
                            continue;
                        }

                        if(noteEvent.action == NoteEvent.NOTE_ON) {
                            int voiceIndex = pdVoiceBindings.getBinding(noteEvent, s.id);
                            if(voiceIndex == -1) {
                                continue;
                            }
                            PdBase.sendList("note", voiceIndex, noteEvent.keyNum,
                                    /*velocity*/   noteEvent.velocity,
                                    /*ADSR*/       s.attack, s.decay, s.sustain, s.release,
                                    /*sampleInfo*/ s.sampleIndex, s.getStartTime(), s.getResumeTime(), s.getEndTime(), s.sampleRate, s.basePitch);
                        } else if(noteEvent.action == NoteEvent.NOTE_OFF) {
                            int voiceIndex = pdVoiceBindings.releaseBinding(noteEvent, s.id);
                            if(voiceIndex == -1) {
                                continue;
                            }
                            PdBase.sendList("note", voiceIndex, noteEvent.keyNum,
                                    /*velocity*/   0,
                                    /*ADSR*/       s.attack, s.decay, s.sustain, s.release,
                                    /*sampleInfo*/ s.sampleIndex, s.getStartTime(), s.getResumeTime(), s.getEndTime(), s.sampleRate, s.basePitch);
                        } else if(noteEvent.action == NoteEvent.CLOSE) {
                            int voiceIndex = pdVoiceBindings.releaseBinding(noteEvent, s.id);
                            if(voiceIndex == -1) {
                                continue;
                            }
                            PdBase.sendList("note", voiceIndex, noteEvent.keyNum,
                                    /*velocity*/   0,
                                    /*ADSR*/       s.attack, s.decay, s.sustain, 0,
                                    /*sampleInfo*/ s.sampleIndex, s.getStartTime(), s.getResumeTime(), s.getEndTime(), s.sampleRate, s.basePitch);
                        }
                    }
                } catch(IOException e) {
                    e.printStackTrace();
                }
            } else {
                Log.d(name,"Audio service not running");
            }
        }
    }

    private class MyPdReceiver implements PdReceiver {
        @Override
        public void print(String s) {
            Log.d("pdReceiver", s);
        }

        @Override
        public void receiveBang(String source) {
            Log.d("pdReceiver", source + ": bang");
        }

        @Override
        public void receiveFloat(String source, float x) {
            if("voice_free".equals(source)) {
                int indexToFree = (int) x;
                pdVoiceBindings.voiceFree(indexToFree);
                Log.d("pdReceiver", String.format("voice_free %d", indexToFree));
            }
        }

        @Override
        public void receiveSymbol(String source, String symbol) {
            // Log.d("pdReceiver", source + String.format(": symbol %s", symbol));
        }

        @Override
        public void receiveList(String source, Object... args) {
            if("sample_info".equals(source)) {
                if(args.length >= 3) {
                    try {
                        int sampleIndex = (int)((float) args[0]);
                        for(Sample s : keyboardInstrument.getSamples()) {
                            if(s.sampleIndex == sampleIndex) {
                                s.setSampleInfo((int)((float) args[args.length - 1]), (int)((float) args[2]));
                                s.isInfoLoaded = true;
                            }
                        }
                        Log.d("pdReciever", String.format("sample_info index=%d length=%d rate=%d",
                                sampleIndex, (int)((float) args[args.length - 1]), (int)((float) args[2])));
                    } catch(ClassCastException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        @Override
        public void receiveMessage(String source, String symbol, Object... args) {
            // Log.d("pdReceiver", source + String.format(": message: symbol %s", symbol));
        }
    }
}