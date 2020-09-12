package libre.sampler.models;

import java.util.List;

public class GlobalSample extends Sample {
    private Instrument instrument;
    public GlobalSample(Instrument instrument) {
        super("", "");
        this.instrument = instrument;
        List<Sample> samples = instrument.getSamples();
        if(samples.size() > 0) {
            float VARIES = -4;  // not valid for any
            Sample s1 = samples.get(0);
            float aStartTime = s1.getStartTime();
            float aEndTime = s1.getEndTime();
            float aResumeTime = s1.getLoopResume();
            float aVolume = s1.getVolume();
            float aAttack = s1.getAttack();
            float aDecay = s1.getDecay();
            float aSustain = s1.getSustain();
            float aRelease = s1.getRelease();

            for(Sample s : samples.subList(1, samples.size())) {
                if(aStartTime != s.getStartTime()) aStartTime = VARIES;
                if(aEndTime != s.getEndTime()) aEndTime = VARIES;
                if(aResumeTime != s.getResumeTime()) aResumeTime = VARIES;
                if(aVolume != s.getVolume()) aVolume = VARIES;
                if(aAttack != s.getAttack()) aAttack = VARIES;
                if(aDecay != s.getDecay()) aDecay = VARIES;
                if(aSustain != s.getSustain()) aSustain = VARIES;
                if(aRelease != s.getRelease()) aRelease = VARIES;
            }

            if(aStartTime != VARIES) super.setStartTime(aStartTime);
            if(aEndTime != VARIES) super.setEndTime(aEndTime);
            if(aResumeTime != VARIES) super.setResumeTime(aResumeTime);
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
    public void setStartTime(float startTime) {
        super.setStartTime(startTime);
        for(Sample s : instrument.getSamples()) {
            s.setStartTime(startTime);
        }
    }

    @Override
    public void setResumeTime(float resumeTime) {
        super.setResumeTime(resumeTime);
        for(Sample s : instrument.getSamples()) {
            s.setResumeTime(resumeTime);
        }
    }

    @Override
    public void setEndTime(float endTime) {
        super.setEndTime(endTime);
        for(Sample s : instrument.getSamples()) {
            s.setEndTime(endTime);
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

    @Override
    public String getDisplayName() {
        return "Global";
    }
}
