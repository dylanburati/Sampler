package libre.sampler.utils;

public class AppConstants {
    public static final String TAG_EXTRA_PROJECT_ID = "libre.sampler.tags.EXTRA_PROJECT_ID";
    public static final String TAG_SHARED_PREFS = "libre.sampler.tags.SHARED_PREFS";
    public static final String PREF_DEFAULT_SAMPLE_PATH = "default_sample_path";
    public static final String PREF_DEFAULT_INSTRUMENT_EXPORT_PATH = "default_instrument_export_path";

    public static final String TAG_SAVED_STATE_INSTRUMENT_CREATE_NAME = "libre.sampler.tags.SAVED_STATE_INSTRUMENT_CREATE_NAME";
    public static final String TAG_SAVED_STATE_INSTRUMENT_EDIT_NAME = "libre.sampler.tags.SAVED_STATE_INSTRUMENT_EDIT_NAME";
    public static final String TAG_SAVED_STATE_INSTRUMENT_EXPORT_NAME = "libre.sampler.tags.SAVED_STATE_INSTRUMENT_EXPORT_NAME";
    public static final String TAG_SAVED_STATE_INSTRUMENT_EXPORT_PATH = "libre.sampler.tags.SAVED_STATE_INSTRUMENT_EXPORT_PATH";

    public static final int MAX_INSTRUMENTS_PER_PROJECT = 65536;
    public static final int MAX_PATTERNS_PER_PROJECT = 65536;

    public static final int PERM_REQUEST_READ_EXTERNAL_STORAGE = 0;

    public static final long NANOS_PER_MILLI = 1000000;

    public static final int PD_NUM_VOICES = 64;
    public static final int PD_NUM_SAMPLES = 64;

    public static final String PROJECTS_LOADED = "projectsLoaded";
    public static final String SELECTED_NOTES = "selectedNotes";
    public static final String PIANO_ROLL_NOTES = "pianoRollNotes";
    public static final String INSTRUMENTS_PATTERNS_LOADED = "instrumentsPatternsLoaded";

    public static final int PATTERN_EDITOR_BASE = 0;
    public static final int PATTERN_EDITOR_NOTE_PROPERTIES = 1;
    public static final int PATTERN_EDITOR_COPY_MULTIPLE = 2;
    public static final int PATTERN_EDITOR_SELECT_SPECIAL = 3;
    public static final int PATTERN_EDITOR_SNAP_LENGTH = 10;
    public static final int PATTERN_EDITOR_PATTERN_LENGTH = 11;
    public static final int PATTERN_EDITOR_BACK = 20;

    public static final int PIANO_ROLL_BOTTOM_KEYNUM = 24;
    public static final int PIANO_ROLL_TOP_KEYNUM = 119;

    public static final String ERROR_EXPORT_ZIP_EXISTS = "File exists";
    public static final String SUCCESS_EXPORT_INSTRUMENT = "Instrument exported";
    public static final String SUCCESS_IMPORT_INSTRUMENT = "Instrument imported";
}
