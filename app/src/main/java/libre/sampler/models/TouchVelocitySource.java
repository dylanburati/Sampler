package libre.sampler.models;

public enum TouchVelocitySource {
    NONE, LOCATION, PRESSURE;

    public int getVelocity(float yFraction, float pressure) {
        if(this == TouchVelocitySource.NONE) {
            return 100;
        } else if(this == TouchVelocitySource.LOCATION) {
            return (int) Math.floor(24.0 + Math.min(112.0 * yFraction, 103.99));
        } else {
            return (int) Math.floor(24.0 + Math.min(112.0 * pressure, 103.99));
        }
    }
}
