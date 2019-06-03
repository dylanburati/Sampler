package libre.sampler.utils;

import androidx.annotation.NonNull;

public class MusicTime {
    public static final int USER_TICKS_PER_SIXTEENTH = 24;
    public static final int SIXTEENTHS_PER_BAR = 16;
    public static final int BEATS_PER_BAR = 4;
    public static final int SIXTEENTHS_PER_BEAT = 4;

    public static final long TICKS_PER_USER_TICK = 256;
    public static final long TICKS_PER_SIXTEENTH = TICKS_PER_USER_TICK * USER_TICKS_PER_SIXTEENTH;
    public static final long TICKS_PER_BAR = TICKS_PER_SIXTEENTH * SIXTEENTHS_PER_BAR;
    public static final long TICKS_PER_BEAT = TICKS_PER_BAR / 4;

    private static final MusicTime tmpInstance = new MusicTime(0L);

    public int bars;
    public int sixteenths;
    public int userTicks;

    public MusicTime(long ticks) {
        setTicks(ticks);
    }

    public MusicTime(int bars, int sixteenths, int userTicks) {
        this.bars = bars;
        this.sixteenths = sixteenths;
        this.userTicks = userTicks;
    }

    public long getTicks() {
        return (bars * TICKS_PER_BAR + sixteenths * TICKS_PER_SIXTEENTH + userTicks * TICKS_PER_USER_TICK);
    }

    public void setTicks(long ticks) {
        bars = (int) (ticks / TICKS_PER_BAR);
        ticks -= bars * TICKS_PER_BAR;
        sixteenths = (int) (ticks / TICKS_PER_SIXTEENTH);
        ticks -= sixteenths * TICKS_PER_SIXTEENTH;
        userTicks = (int) (ticks / TICKS_PER_USER_TICK);
    }

    @NonNull
    @Override
    public String toString() {
        return String.format("%d:%d:%02d", bars, sixteenths, userTicks);
    }

    public static String ticksToString(long ticks) {
        tmpInstance.setTicks(ticks);
        return tmpInstance.toString();
    }
}
