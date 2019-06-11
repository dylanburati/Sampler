package libre.sampler.utils;

public class MidiConstants {
    // Source: https://github.com/googlesamples/android-MidiSynth -> com.example.android.common.midi.MidiConstants

    public static final byte STATUS_COMMAND_MASK = (byte) 0xF0;
    public static final byte STATUS_CHANNEL_MASK = (byte) 0x0F;

    // Channel Voice Messages
    public static final byte STATUS_NOTE_OFF = (byte) 0x80;
    public static final byte STATUS_NOTE_ON = (byte) 0x90;
    public static final byte STATUS_POLYPHONIC_AFTERTOUCH = (byte) 0xA0;
    public static final byte STATUS_CONTROL_CHANGE = (byte) 0xB0;
    public static final byte STATUS_PROGRAM_CHANGE = (byte) 0xC0;
    public static final byte STATUS_CHANNEL_PRESSURE = (byte) 0xD0;
    public static final byte STATUS_PITCH_BEND = (byte) 0xE0;

    // System Common Messages
    public static final byte STATUS_SYSTEM_EXCLUSIVE = (byte) 0xF0;
    public static final byte STATUS_MIDI_TIME_CODE = (byte) 0xF1;
    public static final byte STATUS_SONG_POSITION = (byte) 0xF2;
    public static final byte STATUS_SONG_SELECT = (byte) 0xF3;
    public static final byte STATUS_TUNE_REQUEST = (byte) 0xF6;
    public static final byte STATUS_END_SYSEX = (byte) 0xF7;

    // System Real-Time Messages
    public static final byte STATUS_TIMING_CLOCK = (byte) 0xF8;
    public static final byte STATUS_START = (byte) 0xFA;
    public static final byte STATUS_CONTINUE = (byte) 0xFB;
    public static final byte STATUS_STOP = (byte) 0xFC;
    public static final byte STATUS_ACTIVE_SENSING = (byte) 0xFE;
    public static final byte STATUS_RESET = (byte) 0xFF;

    public static final int[] CHANNEL_BYTE_LENGTHS = { 3, 3, 3, 3, 2, 2, 3 };

    public static final int[] SYSTEM_BYTE_LENGTHS = { 1, 2, 3, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 };

    public static int getBytesPerMessage(byte statusByte) {
        // Java bytes are signed so we need to mask off the high bits
        // to get a value between 0 and 255.
        int statusInt = statusByte & 0xFF;
        if (statusInt >= 0xF0) {
            // System messages use low nibble for size.
            return SYSTEM_BYTE_LENGTHS[statusInt & 0x0F];
        } else if(statusInt >= 0x80) {
            // Channel voice messages use high nibble for size.
            return CHANNEL_BYTE_LENGTHS[(statusInt >> 4) - 8];
        } else {
            return 0;  // data byte
        }
    }

    private static final String[] keyLetters = new String[]{
            "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};

    public static String getNoteName(int keyNum) {
        int arrIndex = keyNum % 12;
        if(keyNum < 0) {
            return "";
        }
        int octave = keyNum / 12 - 1;
        return keyLetters[arrIndex] + octave;
    }
}
