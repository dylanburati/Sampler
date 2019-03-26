package libre.sampler.models;

import android.icu.text.RelativeDateTimeFormatter;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Project {
    public String name;
    private Date mtime;

    public Project(String name, Long mtime) {
        this.name = name;
        if(mtime != null) this.mtime = new Date(mtime);
    }

    public String getRelativeTime() {
        StringBuilder relativeTime = new StringBuilder();
        Date now = new Date();
        if(mtime.getYear() == now.getYear()) {
            return (new SimpleDateFormat("MMM d h:mm a", Locale.US)).format(mtime);
        } else {
            return (new SimpleDateFormat("MMM d, yyyy", Locale.US)).format(mtime);
        }
    }
}
