package libre.sampler;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.util.Consumer;
import androidx.viewpager.widget.ViewPager;
import libre.sampler.adapters.ProjectFragmentAdapter;
import libre.sampler.models.Instrument;
import libre.sampler.models.NoteEvent;
import libre.sampler.models.Project;
import libre.sampler.models.Sample;
import libre.sampler.models.SampleZone;
import libre.sampler.publishers.NoteEventSource;
import libre.sampler.utils.VoiceBindingList;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
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
import java.util.List;

public class ProjectActivity extends AppCompatActivity {
    public static final String TAG_EXTRA_PROJECT = "libre.sampler.tags.EXTRA_PROJECT";

    private ViewPager pager;
    private ProjectFragmentAdapter adapter;
    public NoteEventSource noteEventSource;

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

    private Project project;

    private final int pdVoiceBindingLen = 96;
    private VoiceBindingList pdVoiceBindings;
    private int pdPatchHandle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project);
        ActionBar toolbar = getSupportActionBar();
        if(toolbar != null) {
            toolbar.setTitle(getIntent().getStringExtra(Intent.EXTRA_TITLE));
        }

        project = getIntent().getParcelableExtra(TAG_EXTRA_PROJECT);
        initNoteEventSource();
        initUI();
        initPdService();
    }

    private void initNoteEventSource() {
        noteEventSource = new NoteEventSource();
    }

    private void initUI() {
        pager = findViewById(R.id.pager);
        adapter = new ProjectFragmentAdapter(getSupportFragmentManager());
        pager.setAdapter(adapter);
    }

    private void initPdService() {
        final String name = getResources().getString(R.string.app_name);
        noteEventSource.add("KeyboardNoteEventConsumer", new KeyboardNoteEventConsumer(name));

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
            initNoteEventSource();
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
                InputStream sampleIn0 = res.openRawResource(R.raw.sample);
                InputStream sampleIn1 = res.openRawResource(R.raw.sample1);
                IoUtils.extractResource(sampleIn0, "sample.wav", getCacheDir());
                IoUtils.extractResource(sampleIn1, "sample1.wav", getCacheDir());
            }
            pdService.initAudio(AudioParameters.suggestSampleRate(), 0, 2, 8);
            pdService.startAudio();
            initInstrument();
        } catch (IOException e) {
            finish();
        } finally {
            if(patchFile != null) patchFile.delete();
        }
    }

    private void initInstrument() {
        Instrument t = new Instrument();
        project.addInstrument(t);
        Sample s0 = t.addSample(new SampleZone(36, 128, 0, 128));
        s0.setFilename("sample.wav");
        s0.setBasePitch(60);
        s0.setLoop(0.769f, 5.900f, 7.500f);
        s0.setEnvelope(8, 80, 0.75f, 160);

        Sample s1 = t.addSample(new SampleZone(0, 35, 0, 128));
        s1.setFilename("sample1.wav");
        s1.setBasePitch(24);
        s1.setLoop(7.894f, -1f, 8.500f);
        s1.setEnvelope(1, 240, 0.0f, 0);

        PdBase.sendList("sample_file", s0.id, s0.filename);
        PdBase.sendList("sample_file", s1.id, s1.filename);
    }

    @Override
    protected void onPause() {
        super.onPause();
        pdService.stopAudio();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
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
        return super.onOptionsItemSelected(item);
    }

    private class KeyboardNoteEventConsumer implements Consumer<NoteEvent> {
        private final String name;

        public KeyboardNoteEventConsumer(String name) {
            this.name = name;
        }

        @Override
        public void accept(NoteEvent noteEvent) {
            if(pdService != null) {
                try {
                    List<Sample> samples = project.getSamples(0, noteEvent);
                    if(samples.size() == 0) {
                        return;
                    }
                    Sample s = samples.get(0);
                    if(!s.loaded) {
                        return;
                    }

                    if(noteEvent.action == NoteEvent.ACTION_BEGIN) {
                        if(!pdService.isRunning()) {
                            pdService.initAudio(AudioParameters.suggestSampleRate(), 0, 2, 8);
                            pdService.startAudio();
                        }
                        int voiceIndex = pdVoiceBindings.openEvent(noteEvent);
                        if(voiceIndex == -1) {
                            return;
                        }
                        PdBase.sendList("note", voiceIndex, noteEvent.keyNum,
                                /*velocity*/   noteEvent.velocity,
                                /*ADSR*/       s.attack, s.decay, s.sustain, s.release,
                                /*sampleInfo*/ s.id, s.startTime, s.resumeTime, s.endTime, s.sampleRate, s.basePitch);
                    } else if(noteEvent.action == NoteEvent.ACTION_END) {
                        int voiceIndex = pdVoiceBindings.closeEvent(noteEvent);
                        if(voiceIndex == -1) {
                            return;
                        }
                        if(pdService.isRunning()) {
                            PdBase.sendList("note", voiceIndex, noteEvent.keyNum,
                                    /*velocity*/   0,
                                    /*ADSR*/       s.attack, s.decay, s.sustain, s.release,
                                    /*sampleInfo*/ s.id, s.startTime, s.resumeTime, s.endTime, s.sampleRate, s.basePitch);
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
                        Sample s = project.instruments.get(0).samples.get(sampleIndex);
                        s.sampleRate = (int)((float) args[2]);
                        s.loaded = true;
                        Log.d("pdReciever", String.format("sample_info index=%d rate=%d", sampleIndex, s.sampleRate));
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

