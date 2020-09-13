package libre.sampler.publishers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import libre.sampler.models.InstrumentEvent;

public class InstrumentEventSource extends MapEventSource<InstrumentEvent> {
    private Map<String, List<InstrumentEvent>> replayQueues = new HashMap<>();

    public void addToReplayQueue(String queueTag, InstrumentEvent event) {
        List<InstrumentEvent> list = replayQueues.get(queueTag);
        if(list == null) {
            list = new ArrayList<>();
            replayQueues.put(queueTag, list);
        }
        list.add(event);
    }

    public void runReplayQueue(String queueTag) {
        List<InstrumentEvent> list = replayQueues.get(queueTag);
        if(list != null) {
            replayQueues.put(queueTag, null);
            for(InstrumentEvent event : list) {
                dispatch(event);
            }
        }
    }

    public void clearReplayQueue(String queueTag) {
        List<InstrumentEvent> list = replayQueues.get(queueTag);
        if(list != null) {
            list.clear();
        }
    }
}