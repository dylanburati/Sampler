package libre.sampler.utils;

public class ViewUtil {
    public static int dpToPxSize(float density, int dps) {
        return (int) (dps * density + 0.5f);
    }
}
