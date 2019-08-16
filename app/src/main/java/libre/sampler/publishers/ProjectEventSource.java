package libre.sampler.publishers;

import libre.sampler.models.ProjectEvent;

public class ProjectEventSource extends MapEventSource<ProjectEvent> {
    @Override
    public void dispatch(ProjectEvent event) {
        super.dispatch(event);
    }
}