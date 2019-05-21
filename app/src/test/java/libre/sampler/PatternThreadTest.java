package libre.sampler;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import androidx.core.util.Consumer;
import libre.sampler.models.NoteEvent;
import libre.sampler.models.Pattern;
import libre.sampler.models.ScheduledNoteEvent;
import libre.sampler.publishers.NoteEventSource;
import libre.sampler.utils.PatternThread;

import static libre.sampler.utils.AppConstants.NANOS_PER_MILLI;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

public class PatternThreadTest {
    public void printFormattedArray(String format, long[] arr) {
        System.out.print("[");
        for(int i = 0; i < arr.length; i++) {
            System.out.format(format, arr[i]);
            if(i < arr.length - 1) {
                System.out.print(", ");
            }
        }
        System.out.println("]");
    }

    @Test
    public void patternThread() {
        final int TEST_RESULTS_SIZE = 20;
        final int LOOP_SIZE = 10;

        TestNoteEventConsumer noteEventConsumer = new TestNoteEventConsumer(TEST_RESULTS_SIZE);
        final NoteEventSource noteEventSource = new NoteEventSource();
        noteEventSource.add("test", noteEventConsumer);

        PatternThread patternThread = new PatternThread(noteEventSource);
        patternThread.start();

        int[] expectedResults = new int[]{56, 49, 37, 56, 61, 61, 64, 64, 49, 37, 56, 49, 37, 56, 61, 61, 64, 64, 49, 37};
        long[] offsetTicks = new long[]{0, 0, 0, 1, 1, 2, 2, 3, 3, 3, 4, 4, 4, 5, 5, 6, 6, 7, 7, 7};
        long[] expectedTimings = new long[offsetTicks.length];
        long nanosPerTick = 400 * NANOS_PER_MILLI;
        for(int i = 0; i < expectedTimings.length; i++) {
            expectedTimings[i] = offsetTicks[i] * nanosPerTick;
        }

        List<ScheduledNoteEvent> events = new ArrayList<>();
        for(int i = 0; i < LOOP_SIZE; i++) {
            events.add(new ScheduledNoteEvent(offsetTicks[i], NoteEvent.NOTE_OFF, null, expectedResults[i], 0, 0));
        }

        Pattern pattern = Pattern.getEmptyPattern();
        pattern.setPatternId(0);
        pattern.setEvents(events);
        pattern.setLoopLengthTicks(4);
        pattern.setNanosPerTick(400 * NANOS_PER_MILLI);
        patternThread.addPattern("test", pattern);

        try {
            Thread.sleep(3100L);
            patternThread.finish();
            patternThread.join();
        } catch(InterruptedException e) {
            e.printStackTrace();
        }

        try {
            assertArrayEquals("Events were not fired in order", noteEventConsumer.testResults, expectedResults);
        } finally {
            System.out.println("Events");
            System.out.println("Expected: " + Arrays.toString(expectedResults));
            System.out.println("Actual:   " + Arrays.toString(noteEventConsumer.testResults));
        }

        // Calculate deviance from expected timings
        long zeroTimeNs = noteEventConsumer.testTimings[0] - expectedTimings[0];
        long averageDevianceNs = 0;
        for(int i = 0; i < expectedTimings.length; i++) {
            noteEventConsumer.testTimings[i] -= zeroTimeNs;
            averageDevianceNs += Math.abs(noteEventConsumer.testTimings[i] - expectedTimings[i]);
        }
        averageDevianceNs /= (TEST_RESULTS_SIZE - 1);

        try {
            assertTrue("Events were not fired at the correct times", averageDevianceNs < 3 * NANOS_PER_MILLI);
        } finally {
            System.out.println("\nTimings");
            System.out.print("Expected: ");
            printFormattedArray("% 11d", expectedTimings);

            System.out.print("Actual:   ");
            printFormattedArray("% 11d", noteEventConsumer.testTimings);
        }
    }

    private static class TestNoteEventConsumer implements Consumer<NoteEvent> {
        private int noteIdx = 0;
        private final int[] testResults;
        private final long[] testTimings;

        public TestNoteEventConsumer(int TEST_RESULTS_SIZE) {
            this.testResults = new int[TEST_RESULTS_SIZE];
            this.testTimings = new long[TEST_RESULTS_SIZE];
        }

        @Override
        public void accept(NoteEvent noteEvent) {
            if(noteIdx < testResults.length && noteEvent.action != NoteEvent.NOTHING) {
                testResults[noteIdx] = noteEvent.keyNum;
                testTimings[noteIdx] = System.nanoTime();
                noteIdx++;
            }
        }
    }
}