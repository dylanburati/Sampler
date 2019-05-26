package libre.sampler.utils;

public abstract class SliderConverter {
    public static SliderConverter MILLISECONDS = new SliderConverter() {
        @Override
        public float fromSlider(float sliderVal) {
            return (float) (1e4 * Math.pow(sliderVal, 4));
        }

        @Override
        public float toSlider(float millis) {
            float sliderVal = (float) (Math.pow(millis / 1e4, 0.25));
            return Math.min(sliderVal, 1);
        }
    };

    public static SliderConverter DECIBELS = new SliderConverter() {
        @Override
        public float fromSlider(float sliderVal) {
            float db = (float) (12 * Math.log(sliderVal) / Math.log(2));
            return Math.max(-100, db);
        }

        @Override
        public float toSlider(float val) {
            if(val <= -100) {
                return 0;
            }
            float sliderVal = (float) Math.pow(2.0, val / 12.0);
            return Math.min(sliderVal, 1);
        }
    };

    public abstract float fromSlider(float sliderVal);
    public abstract float toSlider(float val);
}
