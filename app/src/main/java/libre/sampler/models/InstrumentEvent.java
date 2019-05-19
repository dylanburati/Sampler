package libre.sampler.models;

public class InstrumentEvent {
    public static final int INSTRUMENT_CREATE = 0;
    public static final int INSTRUMENT_EDIT = 1;
    public static final int INSTRUMENT_DELETE = 2;
    public static final int INSTRUMENT_SELECT = 3;
    public static final int INSTRUMENT_LOAD = 4;

    public int action;
    public Instrument instrument;

    public InstrumentEvent(int action, Instrument instrument) {
        this.action = action;
        this.instrument = instrument;
    }
}

