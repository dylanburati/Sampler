package libre.sampler.utils;

import java.util.HashMap;
import java.util.Map;

public class Debouncer {
    private final Map<String, Integer> taskCounters = new HashMap<>();

    public synchronized int getNextId(String taskName) {
        Integer id = taskCounters.get(taskName);
        if(id == null) {
            id = 0;
        } else {
            id++;
        }
        taskCounters.put(taskName, id);
        return id;
    }

    public synchronized int getCurrentId(String taskName) {
        Integer id = taskCounters.get(taskName);
        if(id == null) {
            id = 0;
            taskCounters.put(taskName, 0);
        }
        return id;
    }
}
