package libre.sampler.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import androidx.annotation.NonNull;
import libre.sampler.views.VisualNote;

public class PatternDerivedData {
    private Map<Instrument, List<VisualNote>> noteMap;

    public PatternDerivedData(Map<Instrument, List<VisualNote>> noteMap) {
        this.noteMap = noteMap;
    }

    public List<VisualNote> getNotesForInstrument(@NonNull Instrument t) {
        List<VisualNote> notes = noteMap.get(t);
        if(notes == null) {
            notes = new ArrayList<>();
            noteMap.put(t, notes);
        }
        return notes;
    }

    public Set<Instrument> getInstrumentList() {
        return noteMap.keySet();
    }

    public void removeInstrument(@NonNull Instrument t) {
        noteMap.remove(t);
    }
}
