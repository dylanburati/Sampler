package libre.sampler.utils;

public class AppConstants {
    public static final String TAG_EXTRA_PROJECT = "libre.sampler.tags.EXTRA_PROJECT";
    public static final String TAG_SHARED_PREFS = "libre.sampler.tags.SHARED_PREFS";
    public static final String PREF_DEFAULT_SAMPLE_PATH = "default_sample_path";

    public static final String TAG_SAVED_STATE_PROJECT_LIST = "libre.sampler.tags.SAVED_STATE_PROJECT_LIST";
    public static final String TAG_SAVED_STATE_PROJECT = "libre.sampler.tags.SAVED_STATE_PROJECT";
    public static final String TAG_SAVED_STATE_INSTRUMENT = "libre.sampler.tags.SAVED_STATE_INSTRUMENT";
    public static final String TAG_SAVED_STATE_INSTRUMENT_CREATE_NAME = "libre.sampler.tags.SAVED_STATE_INSTRUMENT_CREATE_NAME";
    public static final String TAG_SAVED_STATE_INSTRUMENT_CREATE_PATH = "libre.sampler.tags.SAVED_STATE_INSTRUMENT_CREATE_PATH";
    public static final String TAG_SAVED_STATE_MIDI_CONNECTED = "libre.sampler.tags.SAVED_STATE_MIDI_CONNECTED";

    public static final int MAX_INSTRUMENTS_PER_PROJECT = 65536;

    public static final int PERM_REQUEST_READ_EXTERNAL_STORAGE = 0;

    public static final long NANOS_PER_MILLI = 1000000;

    public static final int PD_NUM_VOICES = 96;
    public static final int PD_NUM_SAMPLES = 48;

    public static final int TICKS_PER_BEAT = 48 * 256;
}