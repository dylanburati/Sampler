package libre.sampler.utils;

import android.util.SparseArray;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import libre.sampler.models.Sample;

public class SampleBindingList {
    private Map<String, Binding> bindings;
    private SparseArray<String> filenameBindings;
    private List<Integer> remainingIndices;

    private static class Binding {
        public int sampleIndex;
        public final Sample firstSample;

        private Set<Sample> samples;
        private boolean isInfoLoaded = false;

        public Binding(int sampleIndex, Sample sample) {
            this.sampleIndex = sampleIndex;
            this.firstSample = sample;
            sample.sampleIndex = sampleIndex;
            this.samples = new HashSet<>(4);
            samples.add(sample);
        }

        public void addSample(Sample s) {
            if(samples.add(s)) {
                s.sampleIndex = this.sampleIndex;
                if(isInfoLoaded) {
                    s.setSampleInfo(firstSample.getSampleLength(), firstSample.getSampleRate());
                }
            }
        }

        public void setSampleInfo(int sampleLength, int sampleRate) {
            isInfoLoaded = true;
            for(Sample s : samples) {
                s.setSampleInfo(sampleLength, sampleRate);
            }
        }
    }

    public SampleBindingList(int size) {
        this.remainingIndices = new ArrayList<>(size);
        for(int i = 0; i < size; i++) {
            remainingIndices.add(i);
        }
        this.filenameBindings = new SparseArray<>(size);
        this.bindings = new HashMap<>(size);
    }

    // Returns false if the binding existed already or is invalid,
    // true if the binding was created and must be propagated to PdBase
    public boolean getBinding(Sample s) {
        Binding b = bindings.get(s.filename);
        if(b != null) {
            b.addSample(s);
            return false;  // already created
        }

        if(remainingIndices.size() == 0) {
            return false;  // can't create, full
        }
        int insertIdx = remainingIndices.remove(0);
        b = new Binding(insertIdx, s);
        filenameBindings.put(insertIdx, s.filename);
        bindings.put(s.filename, b);

        return true;
    }

    public void clearBindings() {
        filenameBindings.clear();
        bindings.clear();
    }

    public void setSampleInfo(int sampleIndex, int sampleLength, int sampleRate) {
        Binding b = bindings.get(filenameBindings.get(sampleIndex));
        if(b != null) {
            b.setSampleInfo(sampleLength, sampleRate);
        }
    }
}
