package libre.sampler.models;

public class ProjectEvent {
    public static final int PROJECT_CREATE = 0;
    public static final int PROJECT_EDIT = 1;
    public static final int PROJECT_DELETE = 2;

    public int action;
    public Project project;

    public ProjectEvent(int action, Project project) {
        this.action = action;
        this.project = project;
    }
}
