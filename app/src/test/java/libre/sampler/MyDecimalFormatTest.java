package libre.sampler;

import org.junit.Test;

import libre.sampler.utils.MyDecimalFormat;

import static org.junit.Assert.*;

public class MyDecimalFormatTest {
    @Test
    public void format_correct() {
        MyDecimalFormat fmt = new MyDecimalFormat(3, 5);
        assertEquals("Did not truncate to 3 decimal places", "0.123", fmt.format(0.1234));
        assertEquals("Did not truncate to 5 significant digits", "9876.5", fmt.format(9876.5432));
        assertEquals("Did not truncate to 5 significant digits", "98765", fmt.format(98765.432));
    }
}
