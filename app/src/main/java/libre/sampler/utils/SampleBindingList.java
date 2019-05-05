package libre.sampler.utils;

import java.util.ArrayList;
import java.util.List;

import libre.sampler.models.Sample;

public class SampleBindingList {
    private List<String> bindings;
    public int remaining;

    public SampleBindingList(int size) {
        this.remaining = size;
        this.bindings = new ArrayList<>(size);
        for(int i = 0; i < size; i++) {
            bindings.add(null);
        }
    }

    // Returns false if the binding existed already or is invalid,
    // true if the binding was created and must be propagated to PdBase
    public boolean getBinding(Sample s) {
        if(bindings.indexOf(s.filename) >= 0) {
            s.sampleIndex = bindings.indexOf(s.filename);
            return false;
        }

        int insertIdx = 0;
        while(insertIdx < bindings.size() && bindings.get(insertIdx) != null) {
            insertIdx++;
        }
        if(insertIdx < bindings.size()) {
            bindings.set(insertIdx, s.filename);
            s.sampleIndex = insertIdx;
            remaining--;
            return true;
        }
        return false;
    }

    public void clearBindings() {
        remaining = bindings.size();
        for(int i = 0; i < remaining; i++) {
            bindings.set(i, null);
        }
    }
}
