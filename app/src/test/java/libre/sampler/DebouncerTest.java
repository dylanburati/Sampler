package libre.sampler;

import org.junit.Test;

import libre.sampler.utils.Debouncer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class DebouncerTest {
    @Test
    public void debounce_correct() {
        Debouncer debouncer = new Debouncer();
        int i1 = debouncer.getCurrentId("test");
        int i2 = debouncer.getNextId("test");
        assertNotEquals("Debouncer did not advance the id when nextId was called", i1, i2);
        int i2a = debouncer.getCurrentId("test");
        assertEquals("Debouncer advanced the id for no reason", i2, i2a);
        int i3 = debouncer.getNextId("test");
        int i4 = debouncer.getNextId("test");
        int i4a = debouncer.getCurrentId("test");
        assertNotEquals("Debouncer did not advance the id when nextId was called", i3, i4a);
        assertEquals("Debouncer advanced the id for no reason", i4, i4a);
    }
}
