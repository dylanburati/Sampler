package libre.sampler;

import org.junit.Test;

import java.util.Random;

import libre.sampler.utils.SliderConverter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SliderConverterTest {
    @Test
    public void milliseconds_correct() {
        float sliderVal = new Random().nextFloat();
        float millis = SliderConverter.MILLISECONDS.fromSlider(sliderVal);
        assertTrue("Converter gave a negative value in milliseconds", millis >= 0);
        assertEquals("Converter is not reversible", sliderVal, SliderConverter.MILLISECONDS.toSlider(millis), 1e-9);
    }

    @Test
    public void decibels_correct() {
        float sliderVal = new Random().nextFloat();
        float decibels = SliderConverter.DECIBELS.fromSlider(sliderVal);
        assertTrue("Converter gave a positive value in decibels", decibels <= 0);
        assertEquals("Converter is not reversible", sliderVal, SliderConverter.DECIBELS.toSlider(decibels), 1e-9);
    }
}
