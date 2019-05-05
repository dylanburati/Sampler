package libre.sampler;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.util.Consumer;
import androidx.viewpager.widget.ViewPager;
import libre.sampler.adapters.InstrumentListAdapter;
import libre.sampler.adapters.ProjectFragmentAdapter;
import libre.sampler.dialogs.InstrumentCreateDialog;
import libre.sampler.dialogs.InstrumentEditDialog;
import libre.sampler.models.Instrument;
import libre.sampler.models.NoteEvent;
import libre.sampler.models.PatternEvent;
import libre.sampler.models.Project;
import libre.sampler.models.Sample;
import libre.sampler.models.ScheduledNoteEvent;
import libre.sampler.publishers.MidiEventDispatcher;
import libre.sampler.publishers.NoteEventSource;
import libre.sampler.publishers.PatternEventSource;
import libre.sampler.tasks.GetInstrumentsTask;
import libre.sampler.tasks.UpdateProjectTask;
import libre.sampler.utils.AdapterLoader;
import libre.sampler.utils.AppConstants;
import libre.sampler.utils.DatabaseConnectionManager;
import libre.sampler.utils.PatternThread;
import libre.sampler.utils.VoiceBindingList;

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
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;

import org.puredata.android.io.AudioParameters;
import org.puredata.android.service.PdPreferences;
import org.puredata.android.service.PdService;
import org.puredata.core.PdBase;
import org.puredata.core.PdReceiver;
import org.puredata.core.utils.IoUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static libre.sampler.utils.AppConstants.NANOS_PER_MILLI;

public class ProjectActivity extends AppCompatActivity implements
        InstrumentCreateDialog.InstrumentCreateDialogListener, InstrumentEditDialog.InstrumentEditDialogListener {
    private ViewPager pager;
    private ProjectFragmentAdapter adapter;

    public NoteEventSource noteEventSource;
    public PatternEventSource patternEventSource;
    private PatternThread patternThread;

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

    private final int pdVoiceBindingLen = 96;
    private VoiceBindingList pdVoiceBindings;
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
            if(savedInstanceState.getByte(AppConstants.TAG_SAVED_STATE_MIDI_CONNECTED) != 0) {
                refreshMidiConnection();
            }
        }
        if(!projectLoaded) {
            Log.d("ProjectActivity","Project load");
            project = getIntent().getParcelableExtra(AppConstants.TAG_EXTRA_PROJECT);
            DatabaseConnectionManager.initialize(this);
            DatabaseConnectionManager.runTask(new GetInstrumentsTask(project.id, new Consumer<List<Instrument>>() {
                @Override
                public void accept(List<Instrument> instruments) {
                    project.setInstruments(instruments);
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
            int activeIdx = project.getActiveIdx();
            if(activeIdx >= 0) {
                instrumentListAdapter.activateItem(activeIdx + 1);
            }
        }
    }

    private void initEventSources() {
        noteEventSource = new NoteEventSource();
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

        // tmp
        patternEventSource.add("test", new Consumer<PatternEvent>() {
            @Override
            public void accept(PatternEvent patternEvent) {
                if(patternEvent.action == PatternEvent.PATTERN_ON) {
                    List<ScheduledNoteEvent> events = new ArrayList<>();
                    long[] offsetsOn = new long[]{1, 1, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10,11,12,13,13,13,14,15,16,17,18,19,20,21,22,23,24};
                    int[] keyNumsOn = new int[]  {49,37,56,61,64,56,61,64,56,61,64,56,61,64,47,35,56,61,64,56,61,64,56,61,64,56,61,64};

                    long[] offsetsOff = new long[]{2, 3, 4, 5, 6, 7, 8, 9, 10,11,12,13,13,13,14,15,16,17,18,19,20,21,22,23,24,25,25,25};
                    int[] keyNumsOff = new int[]  {56,61,64,56,61,64,56,61,64,56,61,64,49,37,56,61,64,56,61,64,56,61,64,56,61,64,47,35};
                    for(int i = 0; i < offsetsOn.length; i++) {
                        events.add(new ScheduledNoteEvent((offsetsOn[i] - 1) * 400 * NANOS_PER_MILLI,
                                new NoteEvent(NoteEvent.NOTE_ON, keyNumsOn[i], 100, new Pair<>(-2L, keyNumsOn[i]))));
                    }
                    for(int i = 0; i < offsetsOff.length; i++) {
                        events.add(new ScheduledNoteEvent((offsetsOff[i] - 1) * 400 * NANOS_PER_MILLI,
                                new NoteEvent(NoteEvent.NOTE_OFF, keyNumsOff[i], 100, new Pair<>(-2L, keyNumsOff[i]))));
                    }
                    Collections.sort(events, new Comparator<ScheduledNoteEvent>() {
                        @Override
                        public int compare(ScheduledNoteEvent o1, ScheduledNoteEvent o2) {
                            return o1.offsetNanos.compareTo(o2.offsetNanos);
                        }
                    });
                    patternThread.addPattern(events, 9600 * NANOS_PER_MILLI);
                } else {
                    patternThread.clearPatterns();
                }
            }
        });
        // tmp>

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
        }
        if(adapter == null) {
            initUI();
        }
        if(pdService == null) {
            initPdService();
        }
    }

    private void initPd() {
        pdVoiceBindings = new VoiceBindingList(pdVoiceBindingLen);

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
            project.instrumentEventSource.add("keyboard", new Consumer<Instrument>() {
                @Override
                public void accept(Instrument instrument) {
                    for(Sample s : instrument.getSamples()) {
                        PdBase.sendList("sample_file", s.sampleIndex, s.filename);
                    }
                }
            });
            int activeIdx = project.getActiveIdx();
            if(activeIdx >= 0) {
                project.updateActiveInstrument();
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
        closeNotes();
        pdService.stopAudio();
        patternThread.finish();
    }

    @SuppressLint("NewApi")
    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(pdConnection);
        if(midiEventDispatcher != null) {
            midiEventDispatcher.closeMidi();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(AppConstants.TAG_SAVED_STATE_PROJECT, (Parcelable) project);
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
            DatabaseConnectionManager.initialize(this);
            DatabaseConnectionManager.runTask(new UpdateProjectTask(project));
            DatabaseConnectionManager.runTask(new GetInstrumentsTask(project.id, new Consumer<List<Instrument>>() {
                @Override
                public void accept(List<Instrument> instruments) {
                    Log.d("GetInstrumentsTask", "");
                }
            }));
            return true;
        } else if(item.getItemId() == R.id.appbar_refresh_midi) {
            refreshMidiConnection();
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

    @Override
    public void onInstrumentCreate(Instrument instrument) {
        project.addInstrument(instrument);
        if(instrumentListAdapter != null) {
            AdapterLoader.insertItem(instrumentListAdapter, instrument);
        }
    }

    @Override
    public void onInstrumentEdit(Instrument instrument) {
        int changeIdx = instrumentListAdapter.items.indexOf(instrument);
        if(changeIdx != -1) {
            instrumentListAdapter.notifyItemChanged(changeIdx);
        }
        if(instrument == project.getActiveInstrument()) {
            project.updateActiveInstrument();
        }
    }

    @Override
    public void onInstrumentDelete(Instrument instrument) {
        int removeIdx = project.removeInstrument(instrument);
        AdapterLoader.removeItem(instrumentListAdapter, removeIdx + 1);
    }

    private void closeNotes() {
        if(!pdService.isRunning() || noteEventSource == null) {
            return;
        }
        List<NoteEvent> events = pdVoiceBindings.getCloseEvents();
        for(NoteEvent e : events) {
            noteEventSource.dispatch(e);
        }
    }

    private class KeyboardNoteEventConsumer implements Consumer<NoteEvent> {
        private final String name;

        public KeyboardNoteEventConsumer(String name) {
            this.name = name;
        }

        @Override
        public void accept(NoteEvent noteEvent) {
            if(pdService != null) {
                List<Sample> samples = project.getSamples(noteEvent);
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
                        for(Sample s : project.getActiveInstrument().getSamples()) {
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