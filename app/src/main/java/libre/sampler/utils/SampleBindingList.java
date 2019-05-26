package libre.sampler.utils;

import java.util.ArrayList;
import java.util.List;

import libre.sampler.models.Sample;

public class SampleBindingList {
    private List<SampleBindingData> bindings;
    public int remaining;

    private static class SampleBindingData {
        public Sample sample;

        public SampleBindingData(Sample sample) {
            this.sample = sample;
        }

        public boolean hasSameFilename(Sample other) {
            return other.filename.equals(sample.filename);
        }
    }

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
        int insertIdx = -1;
        for(int index = 0; index < bindings.size(); index++) {
            SampleBindingData b = bindings.get(index);
            if(b == null) {
                if(insertIdx == -1) {
                    insertIdx = index;
                }
            } else if(b.hasSameFilename(s)) {
                s.sampleIndex = index;
                s.setSampleInfo(b.sample.sampleLength, b.sample.sampleRate);
                return false;  // binding was reused
            }
        }

        if(insertIdx != -1) {
            bindings.set(insertIdx, new SampleBindingData(s));
            s.sampleIndex = insertIdx;
            remaining--;
            return true;  // binding was created
        }

        return false;  // could not create binding, nowhere to insert
    }

    public void clearBindings() {
        remaining = bindings.size();
        for(int i = 0; i < remaining; i++) {
            bindings.set(i, null);
        }
    }
}
