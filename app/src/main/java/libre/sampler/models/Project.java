package libre.sampler.models;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "project")
public class Project {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "name")
    public String name;

    @ColumnInfo(name = "mtime")
    public long mtime;

    public Project(int id, String name, Long mtime) {
        this.id = id;
        this.name = name;
        if(mtime != null) this.mtime = mtime;
    }

    public String getRelativeTime() {
        StringBuilder relativeTime = new StringBuilder();
        Date then = new Date(mtime);
        Date now = new Date();
        if(then.getYear() == now.getYear()) {
            return (new SimpleDateFormat("MMM d h:mm a", Locale.US)).format(then);
        } else {
            return (new SimpleDateFormat("MMM d, yyyy", Locale.US)).format(then);
        }
    }
}
