package libre.sampler.models;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;
import libre.sampler.databases.ProjectSettingsConverter;
import libre.sampler.utils.AppConstants;
import libre.sampler.utils.IdStatus;

@Entity(tableName = "project")
public class Project {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String name;
    public long mtime;

    @TypeConverters(ProjectSettingsConverter.class)
    public Map<String, Object> settings;

    @Ignore
    private List<Instrument> instruments;
    @Ignore
    private int nextInstrumentId;
    @Ignore
    private IdStatus instrumentIdStatus = new IdStatus("Project,Instrument");

    @Ignore
    private List<Pattern> patterns;
    @Ignore
    private int nextPatternId;
    @Ignore
    private IdStatus patternIdStatus = new IdStatus("Project,Pattern");

    @Ignore
    public Project(String name, long mtime) {
        this.id = 0;
        this.name = name;
        this.mtime = mtime;

        this.settings = new HashMap<>();
        this.instruments = new ArrayList<>();
        this.patterns = new ArrayList<>();
    }

    // should be called with the `id` obtained from the database
    public Project(int id, String name, long mtime) {
        this(name, mtime);
        this.id = id;
        this.nextInstrumentId = id * AppConstants.MAX_INSTRUMENTS_PER_PROJECT;
        this.nextPatternId = id * AppConstants.MAX_PATTERNS_PER_PROJECT;

        if(id >= 0) {
            instrumentIdStatus.set(IdStatus.SELF);
            patternIdStatus.set(IdStatus.SELF);
        }
    }

    public void setProjectId(int id) {
        this.id = id;
        this.nextInstrumentId = id * AppConstants.MAX_INSTRUMENTS_PER_PROJECT;
        this.nextPatternId = id * AppConstants.MAX_PATTERNS_PER_PROJECT;

        instrumentIdStatus.set(IdStatus.SELF);
        patternIdStatus.set(IdStatus.SELF);
    }

    // should be called with a list obtained from the database
    public void setInstruments(List<Instrument> instruments) {
        this.instruments = instruments;
        for(Instrument t : instruments) {
            if(t.id >= nextInstrumentId) {
                nextInstrumentId = t.id + 1;
            }
        }

        instrumentIdStatus.set(IdStatus.CHILDREN_DB);
    }

    public void registerInstrument(Instrument e) {
        e.setInstrumentId(nextInstrumentId);
        nextInstrumentId++;

        instrumentIdStatus.require(IdStatus.SELF);
        instrumentIdStatus.set(IdStatus.CHILDREN_ADDED);
    }

    public void addInstrument(Instrument e) {
        e.projectId = this.id;

        if(e.name == null || e.name.isEmpty()) {
            e.name = "New instrument";
        }
        int suffix = 1;
        String origName = e.name;
        while(!checkInstrumentName(e)) {
            e.name = String.format("%s (%d)", origName, suffix);
            suffix++;
        }

        instruments.add(e);
    }

    public void removeInstrument(Instrument e) {
        instruments.remove(e);
    }

    public List<Instrument> getInstruments() {
        return instruments;
    }

    // should be called with a list obtained from the database
    public void setPatterns(List<Pattern> patterns) {
        this.patterns = patterns;
        for(Pattern p : patterns) {
            if(p.id >= nextPatternId) {
                nextPatternId = p.id + 1;
            }
        }

        patternIdStatus.set(IdStatus.CHILDREN_DB);
    }

    public void registerPattern(Pattern e) {
        e.setPatternId(nextPatternId);
        nextPatternId++;

        patternIdStatus.require(IdStatus.SELF);
        patternIdStatus.set(IdStatus.CHILDREN_ADDED);
    }

    public void addPattern(Pattern e) {
        e.projectId = this.id;

        int suffix = patterns.size() + 1;
        if(e.name == null || e.name.isEmpty()) {
            e.name = String.format("Pattern %d", suffix);
        }
        while(!checkPatternName(e)) {
            e.name = String.format("Pattern %d", suffix);
            suffix++;
        }

        patterns.add(e);
    }

    public void removePattern(Pattern e) {
        patterns.remove(e);
    }

    public List<Pattern> getPatterns() {
        return patterns;
    }

    public String getDefaultSamplePath() {
        String s = (String) settings.get(AppConstants.PREF_DEFAULT_SAMPLE_PATH);
        if(s == null) {
            return "";
        }
        return s;
    }

    public void setDefaultSamplePath(String s) {
        settings.put(AppConstants.PREF_DEFAULT_SAMPLE_PATH, s);
    }

    public String getRelativeTime() {
        Date then = new Date(mtime);
        Date now = new Date();
        if(then.getYear() == now.getYear()) {
            return (new SimpleDateFormat("MMM d h:mm a", Locale.US)).format(then);
        } else {
            return (new SimpleDateFormat("MMM d, yyyy", Locale.US)).format(then);
        }
    }

    private boolean checkInstrumentName(Instrument e) {
        // Run before adding to instruments
        for(Instrument t : instruments) {
            if(e.name.equals(t.name)) {
                return false;
            }
        }
        return true;
    }

    private boolean checkPatternName(Pattern e) {
        // Run before adding to patterns
        for(Pattern p : patterns) {
            if(e.name.equals(p.name)) {
                return false;
            }
        }
        return true;
    }

}
