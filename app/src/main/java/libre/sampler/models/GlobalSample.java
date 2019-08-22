package libre.sampler.models;

import java.util.List;

public class GlobalSample extends Sample {
    private Instrument instrument;
    public GlobalSample(Instrument instrument) {
        super("", 0);
        this.instrument = instrument;
        List<Sample> samples = instrument.getSamples();
        if(samples.size() > 0) {
            float VARIES = -4;  // not valid for any
            Sample s1 = samples.get(0);
            float aLoopStart = s1.shouldUseDefaultLoopStart ? VARIES : s1.getLoopStart();
            float aLoopEnd = s1.shouldUseDefaultLoopEnd ? VARIES : s1.getLoopEnd();
            float aLoopResume = s1.shouldUseDefaultLoopResume ? VARIES : s1.getLoopResume();
            float aVolume = s1.getVolume();
            float aAttack = s1.attack;
            float aDecay = s1.decay;
            float aSustain = s1.sustain;
            float aRelease = s1.release;

            for(Sample s : samples.subList(1, samples.size())) {
                if(aLoopStart != s.getLoopStart()) aLoopStart = VARIES;
                if(aLoopEnd != s.getLoopEnd()) aLoopEnd = VARIES;
                if(aLoopResume != s.getLoopResume()) aLoopResume = VARIES;
                if(aVolume != s.getVolume()) aVolume = VARIES;
                if(aAttack != s.attack) aAttack = VARIES;
                if(aDecay != s.decay) aDecay = VARIES;
                if(aSustain != s.sustain) aSustain = VARIES;
                if(aRelease != s.release) aRelease = VARIES;
            }

            if(aLoopStart != VARIES) super.setLoopStart(aLoopStart);
            if(aLoopEnd != VARIES) super.setLoopEnd(aLoopEnd);
            if(aLoopResume != VARIES) super.setLoopResume(aLoopResume);
            if(aVolume != VARIES) super.setVolume(aVolume);
            if(aAttack != VARIES) super.setAttack(aAttack);
            if(aDecay != VARIES) super.setDecay(aDecay);
            if(aSustain != VARIES) super.setSustain(aSustain);
            if(aRelease != VARIES) super.setRelease(aRelease);
        }
    }

    @Override
    public boolean shouldDisplay(int field) {
        if(field == Sample.FIELD_MIN_PITCH || field == Sample.FIELD_MAX_PITCH || field == Sample.FIELD_BASE_PITCH ||
                field == Sample.FIELD_MIN_VELOCITY || field == Sample.FIELD_MAX_VELOCITY) {
            return false;
        }
        return super.shouldDisplay(field);
    }

    @Override
    public void setLoopStart(float startTime) {
        super.setLoopStart(startTime);
        for(Sample s : instrument.getSamples()) {
            s.setLoopStart(startTime);
        }
    }

    @Override
    public void setLoopResume(float resumeTime) {
        super.setLoopResume(resumeTime);
        for(Sample s : instrument.getSamples()) {
            s.setLoopResume(resumeTime);
        }
    }

    @Override
    public void setLoopEnd(float endTime) {
        super.setLoopEnd(endTime);
        for(Sample s : instrument.getSamples()) {
            s.setLoopEnd(endTime);
        }
    }

    @Override
    public void setVolume(float volume) {
        super.setVolume(volume);
        for(Sample s : instrument.getSamples()) {
            s.setVolume(volume);
        }
    }

    @Override
    public void setVolumeDecibels(float volumeDecibels) {
        super.setVolumeDecibels(volumeDecibels);
        for(Sample s : instrument.getSamples()) {
            s.setVolumeDecibels(volumeDecibels);
        }
    }

    @Override
    public void setAttack(float attack) {
        super.setAttack(attack);
        for(Sample s : instrument.getSamples()) {
            s.setAttack(attack);
        }
    }

    @Override
    public void setDecay(float decay) {
        super.setDecay(decay);
        for(Sample s : instrument.getSamples()) {
            s.setDecay(decay);
        }
    }

    @Override
    public void setSustainDecibels(float sustainDecibels) {
        super.setSustainDecibels(sustainDecibels);
        for(Sample s : instrument.getSamples()) {
            s.setSustainDecibels(sustainDecibels);
        }
    }

    @Override
    public void setSustain(float sustain) {
        super.setSustain(sustain);
        for(Sample s : instrument.getSamples()) {
            s.setSustain(sustain);
        }
    }

    @Override
    public void setRelease(float release) {
        super.setRelease(release);
        for(Sample s : instrument.getSamples()) {
            s.setRelease(release);
        }
    }
}
