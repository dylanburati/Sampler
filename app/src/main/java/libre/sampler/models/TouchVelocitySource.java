package libre.sampler.models;

public enum TouchVelocitySource {
    NONE, LOCATION, PRESSURE;

    public int getVelocity(float yFraction, float pressure) {
        if(this == TouchVelocitySource.NONE) {
            return 100;
        } else if(this == TouchVelocitySource.LOCATION) {
            return (int) Math.floor(32.0 + 95.999 * yFraction);
        } else {
            return (int) Math.floor(32.0 + 95.999 * Math.min(1f, pressure));
        }
    }
}
