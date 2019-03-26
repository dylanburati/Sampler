package libre.sampler.models;

import java.util.Date;

public class Project {
    public String name;
    private Date mtime;

    public Project(String name, Long mtime) {
        this.name = name;
        if(mtime != null) this.mtime = new Date(mtime);
    }

    public String getRelativeTime() {
        return mtime.toString();
    }
}
