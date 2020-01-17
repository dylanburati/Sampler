package libre.sampler;

import android.util.Pair;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import androidx.core.util.Consumer;
import libre.sampler.models.NoteEvent;
import libre.sampler.models.Pattern;
import libre.sampler.models.ScheduledNoteEvent;
import libre.sampler.publishers.NoteEventSource;
import libre.sampler.utils.PatternThread;

import static libre.sampler.utils.AppConstants.NANOS_PER_MILLI;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
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

    private NoteEventSource noteEventSource;
    private PatternThread patternThread;

    @Before
    public void setUp() {
        // run before each test
        this.noteEventSource = new NoteEventSource();
        this.patternThread = new PatternThread(noteEventSource);
    }

    @Test
    public void patternThread_correctOrderingAndTiming() {
        final int TEST_RESULTS_SIZE = 20;
        final int LOOP_SIZE = 10;

        TimingNoteEventConsumer noteEventConsumer = new TimingNoteEventConsumer(TEST_RESULTS_SIZE);
        noteEventSource.add("test", noteEventConsumer);
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
            assertTrue("Events were not fired at the correct times", averageDevianceNs < 15 * NANOS_PER_MILLI);
        } finally {
            System.out.println("\nTimings");
            System.out.print("Expected: ");
            printFormattedArray("% 11d", expectedTimings);

            System.out.print("Actual:   ");
            printFormattedArray("% 11d", noteEventConsumer.testTimings);
        }
    }

    private static class TimingNoteEventConsumer implements Consumer<NoteEvent> {
        private int noteIdx = 0;
        private final int[] testResults;
        private final long[] testTimings;

        public TimingNoteEventConsumer(int TEST_RESULTS_SIZE) {
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

    @Test
    public void patternThread_correctAdditionAndRemoval() {
        final int NUM_NOTES = 10;
        final int NUM_CHANGES = 100;
        final TestVisualNote[] notes = new TestVisualNote[NUM_NOTES];

        noteEventSource.add("correctAdditionAndRemoval", new Consumer<NoteEvent>() {
            @Override
            public void accept(NoteEvent event) {
                if(event.action == NoteEvent.NOTE_ON) {
                    assertEquals("Note was not closed", 0, notes[event.keyNum].numVoices);
                    notes[event.keyNum].numVoices++;
                    notes[event.keyNum].lastEventIdOn = event.eventId;
                } else if(event.action == NoteEvent.NOTE_OFF) {
                    if(notes[event.keyNum].numVoices > 0) {
                        assertEquals("Note was not closed", notes[event.keyNum].lastEventIdOn.first, event.eventId.first);
                        assertEquals("Note was not closed", notes[event.keyNum].lastEventIdOn.second, event.eventId.second);
                        notes[event.keyNum].numVoices = 0;
                        notes[event.keyNum].lastEventIdOn = null;
                    }
                }
            }
        });

        patternThread.start();
        Pattern pattern = Pattern.getEmptyPattern();
        pattern.setPatternId(0);

        Random rand = new Random();
        final long POSITIVE_MASK = (-1L >>> 1);
        for(int i = 0; i < NUM_NOTES; i++) {
            notes[i] = new TestVisualNote();
            long startTicks = (rand.nextLong() & POSITIVE_MASK) % Pattern.DEFAULT_LOOP_LENGTH.getTicks();
            notes[i].eventOn = new ScheduledNoteEvent(startTicks, NoteEvent.NOTE_ON, null, i, 0, i);
            long endTicks = startTicks + ((rand.nextLong() & POSITIVE_MASK) % (Pattern.DEFAULT_LOOP_LENGTH.getTicks() - startTicks));
            notes[i].eventOff = new ScheduledNoteEvent(endTicks, NoteEvent.NOTE_OFF, null, i, 0, i);
            addToPattern(pattern, notes[i].eventOn, notes[i].eventOff);
        }

        patternThread.addPattern("test", pattern);

        long MIN_WAIT_MILLIS = 30;
        long MAX_WAIT_MILLIS = 300;
        for(int changeIdx = 0; changeIdx < NUM_CHANGES; changeIdx++) {
            long waitMillis = rand.nextInt(Math.toIntExact(MAX_WAIT_MILLIS - MIN_WAIT_MILLIS + 1)) + MIN_WAIT_MILLIS;
            try {
                Thread.sleep(waitMillis);
            } catch(InterruptedException ignored) {
            }

            int j = rand.nextInt(NUM_NOTES);
            removeFromPattern(pattern, notes[j].eventOn, notes[j].eventOff);
            waitMillis = rand.nextInt(Math.toIntExact(MAX_WAIT_MILLIS - MIN_WAIT_MILLIS + 1)) + MIN_WAIT_MILLIS;
            try {
                Thread.sleep(waitMillis);
            } catch(InterruptedException ignored) {
            }

            long startTicks = (rand.nextLong() & POSITIVE_MASK) % Pattern.DEFAULT_LOOP_LENGTH.getTicks();
            notes[j].eventOn.offsetTicks = startTicks;
            long endTicks = startTicks + ((rand.nextLong() & POSITIVE_MASK) % (Pattern.DEFAULT_LOOP_LENGTH.getTicks() - startTicks));
            notes[j].eventOff.offsetTicks = endTicks;
            addToPattern(pattern, notes[j].eventOn, notes[j].eventOff);
        }

        try {
            patternThread.finish();
            patternThread.join();
        } catch(InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static class TestVisualNote {
        public ScheduledNoteEvent eventOn;
        public ScheduledNoteEvent eventOff;
        public Pair<Long, Integer> lastEventIdOn;
        public int numVoices;
    }

    private void addToPattern(Pattern pattern, ScheduledNoteEvent eventOn, ScheduledNoteEvent eventOff) {
        try(PatternThread.Editor editor = patternThread.getEditor(pattern)) {
            editor.pattern.addEvent(eventOn);
            editor.pattern.addEvent(eventOff);
        }
    }

    private void removeFromPattern(Pattern pattern, ScheduledNoteEvent eventOn, ScheduledNoteEvent eventOff) {
        try(PatternThread.Editor editor = patternThread.getEditor(pattern)) {
            editor.pattern.removeEvent(eventOn);
            NoteEvent sendOff = editor.pattern.removeAndGetEvent(eventOff);
            if(sendOff != null) {
                noteEventSource.dispatch(sendOff);
            }
        }
    }
}