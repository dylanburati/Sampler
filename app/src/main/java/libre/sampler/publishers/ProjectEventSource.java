package libre.sampler.publishers;

import libre.sampler.models.Project;

public class ProjectEventSource extends MapEventSource<Project> {
    @Override
    public void dispatch(Project project) {
        super.dispatch(project);
    }
}