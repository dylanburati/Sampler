package libre.sampler.tasks;

import android.os.AsyncTask;

import com.leff.midi.MidiFile;
import com.leff.midi.MidiTrack;
import com.leff.midi.event.ProgramChange;
import com.leff.midi.event.meta.EndOfTrack;
import com.leff.midi.event.meta.Tempo;
import com.leff.midi.event.meta.TimeSignature;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import libre.sampler.models.Instrument;
import libre.sampler.models.Pattern;
import libre.sampler.models.PatternDerivedData;
import libre.sampler.utils.AppConstants;
import libre.sampler.utils.MidiConstants;
import libre.sampler.utils.MusicTime;
import libre.sampler.views.VisualNote;

public class ExportPatternTask extends AsyncTask<Void, Float, String> {
    private Pattern pattern;
    private PatternDerivedData derivedData;
    private File outFile;
    private final Callbacks callbackObj;

    public ExportPatternTask(Pattern pattern, PatternDerivedData derivedData, File outFile,
                             Callbacks callbackObj) {
        this.pattern = pattern;
        this.derivedData = derivedData;
        this.outFile = outFile;
        this.callbackObj = callbackObj;
    }

    @Override
    protected String doInBackground(Void... voids) {
        try {
            MidiTrack tempoTrack = new MidiTrack();
            MidiTrack noteTrack = new MidiTrack();

            List<MidiTrack> tracks = new ArrayList<>();
            tracks.add(tempoTrack);
            tracks.add(noteTrack);

            // Track 0 is the tempo map
            TimeSignature ts = new TimeSignature();
            ts.setTimeSignature(4, 4, TimeSignature.DEFAULT_METER, TimeSignature.DEFAULT_DIVISION);

            Tempo tempo = new Tempo();
            tempo.setBpm((float) pattern.getTempo());

            tempoTrack.insertEvent(ts);
            tempoTrack.insertEvent(tempo);

            // Track 1 will have some notes in it
            int channel = 0;
            for(Instrument t : derivedData.getInstrumentList()) {
                noteTrack.insertEvent(new ProgramChange(0L, channel, channel));
                for(VisualNote vn : derivedData.getNotesForInstrument(t)) {
                    noteTrack.insertNote(channel, vn.getKeyNum(), vn.getVelocity(),
                            vn.getStartTicks() / MusicTime.TICKS_PER_MIDI_TICK,
                            vn.getLengthTicks() / MusicTime.TICKS_PER_MIDI_TICK);
                }
                channel++;
                if(channel >= MidiConstants.CHANNELS_PER_TRACK) {
                    noteTrack = new MidiTrack();
                    tracks.add(noteTrack);
                    channel = 0;
                }
            }

            for(MidiTrack tr : tracks) {
                tr.insertEvent(new EndOfTrack(pattern.getLoopLengthTicks() / MusicTime.TICKS_PER_MIDI_TICK, 0L));
            }

            MidiFile midi = new MidiFile((int) MusicTime.MIDI_TICKS_PER_BEAT, tracks);

            // 4. Write the MIDI data to a file
            midi.writeToFile(outFile);
        } catch(IOException e) {
            e.printStackTrace();
            return e.getMessage();
        }

        return AppConstants.SUCCESS_EXPORT_PATTERN;
    }

    @Override
    protected void onPostExecute(String s) {
        if(this.callbackObj != null) callbackObj.onPostExecute(s);
    }

    public interface Callbacks {
        void onPostExecute(String message);
    }
}
