package libre.sampler.models;

import java.util.ArrayList;
import java.util.List;

public class Instrument {
    public int id;
    public List<SampleZone> sampleZones = new ArrayList<>();
    public List<Sample> samples = new ArrayList<>();

    public Instrument() {
    }

    public Sample addSample(SampleZone sz) {
        int insertIdx = this.samples.size();
        sampleZones.add(sz);
        Sample s = new Sample(insertIdx);
        samples.add(s);
        return s;
    }
}
