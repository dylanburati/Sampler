package libre.sampler.publishers;

import java.util.HashMap;
import java.util.Map;

import androidx.core.util.Consumer;
import libre.sampler.models.Project;
import libre.sampler.utils.EventSource;

public class ProjectEventSource implements EventSource<Project> {
    private Map<String, Consumer<Project>> listeners = new HashMap<>();

    @Override
    public void add(String tag, Consumer<Project> listener) {
        listeners.put(tag, listener);
    }

    @Override
    public void dispatch(Project project) {
        for(Consumer<Project> fn : listeners.values()) {
            fn.accept(project);
        }
    }

    @Override
    public void dispatchTo(String tag, Project project) {
        Consumer<Project> fn = listeners.get(tag);
        if(fn != null) {
            fn.accept(project);
        }
    }

    @Override
    public void remove(String tag) {
        listeners.remove(tag);
    }
}