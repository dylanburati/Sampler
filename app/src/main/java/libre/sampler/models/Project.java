package libre.sampler.models;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;
import libre.sampler.utils.MD5OutputStream;

@Entity(tableName = "project")
public class Project {
    @PrimaryKey
    @NonNull
    public String id;

    public String name;
    public long mtime;

    @TypeConverters(ProjectSettings.JsonConverter.class)
    public ProjectSettings settings;

    @Ignore
    private List<Instrument> instruments;

    @Ignore
    private List<Pattern> patterns;

    @Ignore
    public Project(String name, long mtime) {
        this(UUID.randomUUID().toString(), name, mtime);
    }

    // should be called with the `id` obtained from the database
    public Project(@NonNull String id, String name, long mtime) {
        this.id = id;
        this.name = name;
        this.mtime = mtime;

        this.settings = new ProjectSettings();
        this.instruments = new ArrayList<>();
        this.patterns = new ArrayList<>();
    }

    public void setProjectId(String id) {
        this.id = id;
    }

    // should be called with a list obtained from the database
    public void setInstruments(List<Instrument> instruments) {
        this.instruments = instruments;
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

    public TouchVelocitySource getTouchVelocitySource() {
        return settings.touchVelocitySource;
    }

    public void setTouchVelocitySource(TouchVelocitySource vs) {
        settings.touchVelocitySource = vs;
    }

    public String getDefaultSamplePath() {
        String s = settings.defaultSamplePath;
        if(s == null) {
            return "";
        }
        return s;
    }

    public void setDefaultSamplePath(String s) {
        settings.defaultSamplePath = s;
    }


    public String getDefaultExportPath() {
        String s = settings.defaultInstrumentExportPath;
        if(s == null) {
            return "";
        }
        return s;
    }

    public void setDefaultExportPath(String s) {
        settings.defaultInstrumentExportPath = s;
    }

    public void prepareSave() {
        for (int i = 0; i < this.instruments.size(); i++) {
            Instrument e = this.instruments.get(i);
            e.sort = i;
            e.prepareSave();
        }
        for (int i = 0; i < this.patterns.size(); i++) {
            Pattern e = this.patterns.get(i);
            e.sort = i;
        }
    }

    public String getRelativeTime() {
        Date then = new Date(mtime);
        Calendar cal = Calendar.getInstance();
        int yearNow = cal.get(Calendar.YEAR);
        cal.setTime(then);
        int yearThen = cal.get(Calendar.YEAR);
        if(yearNow == yearThen) {
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

    public byte[] valueHash() {
        try(MD5OutputStream outputStream = new MD5OutputStream()) {
            outputStream.writeInt(id.hashCode());
            outputStream.writeInt(name.hashCode());
            outputStream.writeInt(settings.hashCode());
            for(Instrument t : getInstruments()) {
                t.writeHashCodes(outputStream);
            }
            for(Pattern p : getPatterns()) {
                p.writeHashCodes(outputStream);
            }
            outputStream.flush();
            return outputStream.getMessageDigest().digest();
        } catch(IOException | NoSuchAlgorithmException e) {
            return null;
        }
    }
}
