package libre.sampler.models;

import java.util.ArrayList;
import java.util.List;

public class Instrument {
    public String name;
    public int id;
    public List<String> filenames = new ArrayList<>();
    public List<Sample> samples = new ArrayList<>();

    public Instrument(String name) {
        this.name = name;
    }

    public Sample addSample(String filename) {
        int insertIdx = this.samples.size();
        int sampleIdx = this.filenames.indexOf(filename);
        if(sampleIdx == -1) {
            sampleIdx = this.filenames.size();
            this.filenames.add(filename);
        }

        Sample s = new Sample(filename, sampleIdx, insertIdx);
        samples.add(s);
        return s;
    }

    public void addSample(Sample s) {
        int insertIdx = this.samples.size();
        int sampleIdx = this.filenames.indexOf(s.filename);
        if(sampleIdx == -1) {
            sampleIdx = this.filenames.size();
            this.filenames.add(s.filename);
        }
        s.id = insertIdx;
        s.sampleIndex = sampleIdx;

        samples.add(s);
    }
}
