package libre.sampler.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import libre.sampler.views.VisualNote;

public class PatternDerivedData {
    private Map<Instrument, List<VisualNote>> noteMap;
    private List<Instrument> instrumentList;

    public PatternDerivedData(Map<Instrument, List<VisualNote>> noteMap) {
        this.noteMap = noteMap;
        this.instrumentList = new ArrayList<>(noteMap.keySet());
        Collections.sort(this.instrumentList, new Comparator<Instrument>() {
            @Override
            public int compare(Instrument o1, Instrument o2) {
                return Integer.compare(o1.id, o2.id);
            }
        });
    }

    public List<VisualNote> getNotesForInstrument(@NonNull Instrument t) {
        List<VisualNote> list = noteMap.get(t);
        if(list == null) {
            list = new ArrayList<>();
            noteMap.put(t, list);
        }
        return list;
    }

    public List<Instrument> getInstrumentList() {
        return instrumentList;
    }
}
