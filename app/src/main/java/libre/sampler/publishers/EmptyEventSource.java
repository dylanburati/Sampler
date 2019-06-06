package libre.sampler.publishers;

public class EmptyEventSource extends MapEventSource<String> {
    @Override
    public void dispatch(String eventName) {
        super.dispatch(eventName);
    }
}
