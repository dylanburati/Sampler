package libre.sampler;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.util.Consumer;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager.widget.ViewPager;
import libre.sampler.adapters.ProjectFragmentAdapter;
import libre.sampler.fragments.ProjectKeyboardFragment;
import libre.sampler.models.NoteEvent;
import libre.sampler.publishers.NoteEventSource;

import android.content.ComponentName;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import org.puredata.android.io.AudioParameters;
import org.puredata.android.service.PdPreferences;
import org.puredata.android.service.PdService;
import org.puredata.core.PdBase;
import org.puredata.core.PdReceiver;
import org.puredata.core.utils.IoUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

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
                if(indexToFree >= 0 && indexToFree < pdSampleBindingLen) {
                    pdSampleBindings.set(indexToFree, null);
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

    private class SampleBindingData {
        NoteEvent event;
        int eventCount;

        public SampleBindingData(NoteEvent evt) {
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

    private final int pdSampleBindingLen = 24;
    private List<SampleBindingData> pdSampleBindings;
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
        pdSampleBindings = new ArrayList<>(pdSampleBindingLen);
        for(int i = 0; i < pdSampleBindingLen; i++) {
            pdSampleBindings.add(null);
        }

        pager = findViewById(R.id.pager);
        adapter = new ProjectFragmentAdapter(getSupportFragmentManager());
        pager.setAdapter(adapter);

        noteEventSource.add(new Consumer<NoteEvent>() {
            @Override
            public void accept(NoteEvent noteEvent) {
                if(pdService != null) {
                    try {
                        if(noteEvent.action == NoteEvent.ACTION_BEGIN) {
                            if(!pdService.isRunning()) {
                                pdService.initAudio(AudioParameters.suggestSampleRate(), 0, 2, 8);
                                pdService.startAudio();
                            }
                            int sampleVoice = 0;
                            while(sampleVoice < pdSampleBindingLen && pdSampleBindings.get(sampleVoice) != null) {
                                sampleVoice++;
                            }
                            if(sampleVoice >= pdSampleBindingLen) {
                                return;
//                                sampleVoice = 0;
//                                while(sampleVoice < pdSampleBindingLen && pdSampleBindings.get(sampleVoice).keyNum != noteEvent.keyNum) {
//                                    sampleVoice++;
//                                }
//                                if(sampleVoice >= pdSampleBindingLen) {
//                                    return;
//                                }
//                                PdBase.sendList("note", sampleVoice, pdSampleBindings.get(sampleVoice).keyNum, 0, 0, 0, 0, 0);
                            }
                            pdSampleBindings.set(sampleVoice, new SampleBindingData(noteEvent));
                            PdBase.sendList("note", sampleVoice, noteEvent.keyNum, 100, 8, 80, 0.75, 160, 0);
                        } else if(noteEvent.action == NoteEvent.ACTION_END) {
                            int sampleVoice = 0;
                            while(sampleVoice < pdSampleBindingLen && (pdSampleBindings.get(sampleVoice) == null ||
                                    !pdSampleBindings.get(sampleVoice).tryAddEvent(noteEvent))) {
                                sampleVoice++;
                            }
                            if(sampleVoice >= pdSampleBindingLen) {
                                return;
                            }
                            if(pdService.isRunning()) {
                                PdBase.sendList("note",sampleVoice, noteEvent.keyNum, 0, 8, 80, 0.75, 160, 0);
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
            InputStream sampleIn = res.openRawResource(R.raw.sample);
            sampleFile = IoUtils.extractResource(sampleIn, "sample.wav", getCacheDir());
            pdService.initAudio(AudioParameters.suggestSampleRate(), 0, 2, 8);
            pdService.startAudio();
            PdBase.sendBang("dsp");
            PdBase.sendSymbol("sample_file", "sample.wav");
            PdBase.sendFloat("sample_base_pitch", 60);
            PdBase.sendFloat("sample_start", 0.769f);
            PdBase.sendFloat("sample_resume", 5.900f);
            PdBase.sendFloat("sample_end", 7.500f);
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
