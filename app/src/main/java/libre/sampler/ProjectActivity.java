package libre.sampler;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.util.Consumer;
import androidx.viewpager.widget.ViewPager;
import libre.sampler.adapters.ProjectFragmentAdapter;
import libre.sampler.models.NoteEvent;
import libre.sampler.publishers.NoteEventSource;

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
import org.puredata.core.PdBase;
import org.puredata.core.PdReceiver;
import org.puredata.core.utils.IoUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ProjectActivity extends AppCompatActivity {
    private ViewPager pager;
    private ProjectFragmentAdapter adapter;
    public NoteEventSource noteEventSource;

    private PdService pdService = null;

    private PdReceiver pdReceiver = new PdReceiver() {
        @Override
        public void print(String s) {
            // makeToast(s);
            Log.d("pdReceiver", s);
        }

        @Override
        public void receiveBang(String source) {
            // makeToast(source + ": bang");
            Log.d("pdReceiver", source + ": bang");
        }

        @Override
        public void receiveFloat(String source, float x) {
            // makeToast(source + String.format(": float %f", x));
            // Log.d("pdReceiver", source + String.format(": float %f", x));
            if("voice_free".equals(source)) {
                int indexToFree = (int) x;
                if(indexToFree >= 0 && indexToFree < pdVoiceBindingLen) {
                    pdVoiceBindings.set(indexToFree, null);
                    Log.d("pdReceiver", String.format("voice_free %d", indexToFree));
                }
            }
        }

        @Override
        public void receiveSymbol(String source, String symbol) {
            // makeToast(source + String.format(": symbol %s", symbol));
            Log.d("pdReceiver", source + String.format(": symbol %s", symbol));
        }

        @Override
        public void receiveList(String source, Object... args) {
            // makeToast(source + ": list");
            Log.d("pdReceiver", source + ": list");

            if("sample_info".equals(source)) {
                if(args.length >= 3) {
                    try {
                        int sampleIndex = (int)((float) args[0]);
                        if(sampleIndex >= 0 && sampleIndex <= pdSampleInfoLen) {
                            pdSampleInfo.set(sampleIndex, (int)((float) args[2]));
                        }
                    } catch(ClassCastException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        @Override
        public void receiveMessage(String source, String symbol, Object... args) {
            // makeToast(source + String.format(": message: symbol %s", symbol));
            Log.d("pdReceiver", source + String.format(": message: symbol %s", symbol));
        }
    };

    private final ServiceConnection pdConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            pdService = ((PdService.PdBinder)service).getService();
            initPd();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            if(pdService != null) {
                pdService.stopAudio();
                pdService.release();
            }
        }
    };

    private class VoiceBindingData {
        NoteEvent event;
        int eventCount;

        public VoiceBindingData(NoteEvent evt) {
            this.event = evt;
            this.eventCount = 1;
        }

        public boolean tryAddEvent(NoteEvent closeEvt) {
            if(this.eventCount >= 2 || !this.event.eventId.equals(closeEvt.eventId)) {
                return false;
            }
            this.eventCount = 2;
            return true;
        }
    }

    private final int pdVoiceBindingLen = 96;
    private List<VoiceBindingData> pdVoiceBindings;
    private final int pdSampleInfoLen = 48;
    private List<Integer> pdSampleInfo;
    private File sampleFile;

    private void makeToast(final String s) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast t = Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT);
                t.show();
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project);
        ActionBar toolbar = getSupportActionBar();
        if(toolbar != null) {
            toolbar.setTitle(getIntent().getStringExtra(Intent.EXTRA_TITLE));
        }

        final String name = getResources().getString(R.string.app_name);
        noteEventSource = new NoteEventSource();
        pdVoiceBindings = new ArrayList<>(pdVoiceBindingLen);
        for(int i = 0; i < pdVoiceBindingLen; i++) {
            pdVoiceBindings.add(null);
        }
        pdSampleInfo = new ArrayList<>(pdSampleInfoLen);
        for(int i = 0; i < pdSampleInfoLen; i++) {
            pdSampleInfo.add(null);
        }

        pager = findViewById(R.id.pager);
        adapter = new ProjectFragmentAdapter(getSupportFragmentManager());
        pager.setAdapter(adapter);

        noteEventSource.add(new Consumer<NoteEvent>() {
            @Override
            public void accept(NoteEvent noteEvent) {
                if(pdService != null) {
                    try {
                        int sampleIndex = (noteEvent.keyNum < 36 ? 1 : 0);
                        int basePitch = (noteEvent.keyNum < 36 ? 24 : 60);
                        float startTime = (noteEvent.keyNum < 36 ? 7.897f : 0.769f);
                        float resumeTime = (noteEvent.keyNum < 36 ? -1f : 5.900f);
                        float endTime = (noteEvent.keyNum < 36 ? 8.500f : 7.500f);
                        Integer sampleRate = pdSampleInfo.get(sampleIndex);
                        if(sampleRate == null) {
                            return;
                        }

                        if(noteEvent.action == NoteEvent.ACTION_BEGIN) {
                            if(!pdService.isRunning()) {
                                pdService.initAudio(AudioParameters.suggestSampleRate(), 0, 2, 8);
                                pdService.startAudio();
                            }
                            int voiceIndex = 0;
                            while(voiceIndex < pdVoiceBindingLen && pdVoiceBindings.get(voiceIndex) != null) {
                                voiceIndex++;
                            }
                            if(voiceIndex >= pdVoiceBindingLen) {
                                return;
//                                voiceIndex = 0;
//                                while(voiceIndex < pdVoiceBindingLen && pdVoiceBindings.get(voiceIndex).keyNum != noteEvent.keyNum) {
//                                    voiceIndex++;
//                                }
//                                if(voiceIndex >= pdVoiceBindingLen) {
//                                    return;
//                                }
//                                PdBase.sendList("note", voiceIndex, pdVoiceBindings.get(voiceIndex).keyNum, 0, 0, 0, 0, 0);
                            }
                            pdVoiceBindings.set(voiceIndex, new VoiceBindingData(noteEvent));
                            PdBase.sendList("note", voiceIndex, noteEvent.keyNum,
                                    /*velocity*/   100,
                                    /*ADSR*/       8, 80, 0.75, 160,
                                    /*sampleInfo*/ sampleIndex, startTime, resumeTime, endTime, sampleRate, basePitch);
                        } else if(noteEvent.action == NoteEvent.ACTION_END) {
                            int voiceIndex = 0;
                            while(voiceIndex < pdVoiceBindingLen && (pdVoiceBindings.get(voiceIndex) == null ||
                                    !pdVoiceBindings.get(voiceIndex).tryAddEvent(noteEvent))) {
                                voiceIndex++;
                            }
                            if(voiceIndex >= pdVoiceBindingLen) {
                                return;
                            }
                            if(pdService.isRunning()) {
                                PdBase.sendList("note", voiceIndex, noteEvent.keyNum,
                                        /*velocity*/   0,
                                        /*ADSR*/       8, 80, 0.75, 160,
                                        /*sampleInfo*/ sampleIndex, startTime, resumeTime, endTime, sampleRate, basePitch);
                            }
                        }
                    } catch(IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    Log.d(name,"Audio service not running");
                }
            }
        });

        AudioParameters.init(this);
        PdPreferences.initPreferences(getApplicationContext());
        Intent serviceIntent = new Intent(this, PdService.class);
        bindService(serviceIntent, pdConnection, BIND_AUTO_CREATE);
        startService(serviceIntent);
    }

    private void initPd() {
        Resources res = getResources();
        File patchFile = null;
        try {
            PdBase.setReceiver(pdReceiver);
            PdBase.subscribe("voice_free");
            PdBase.subscribe("sample_info");
            InputStream in = res.openRawResource(R.raw.pd_test);
//            BufferedReader inReader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
//            patchFile = new File(getCacheDir(), "test.pd");
//            PrintWriter p = new PrintWriter(patchFile, "UTF-8");
//            String line = null;
//            while((line = inReader.readLine()) != null) {
//                p.write(line.replace("{{cacheDir}}", getCacheDir().getAbsolutePath()));
//            }
            patchFile = IoUtils.extractResource(in, "test.pd", getCacheDir());
            PdBase.openPatch(patchFile);
            InputStream sampleIn0 = res.openRawResource(R.raw.sample);
            InputStream sampleIn1 = res.openRawResource(R.raw.sample1);
            IoUtils.extractResource(sampleIn0, "sample.wav", getCacheDir());
            IoUtils.extractResource(sampleIn1, "sample1.wav", getCacheDir());
            pdService.initAudio(AudioParameters.suggestSampleRate(), 0, 2, 8);
            pdService.startAudio();
            PdBase.sendList("sample_file", 0, "sample.wav");
            PdBase.sendList("sample_file", 1, "sample1.wav");
        } catch (IOException e) {
            finish();
        } finally {
            if (patchFile != null) patchFile.delete();
        }
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
}

