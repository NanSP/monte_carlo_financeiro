package br.uneb.montecarlo.simulation;

/** A standard normal draw determined only by the seed and trajectory index. */
public final class DeterministicGaussian {
    private static final long GAMMA = 0x9E3779B97F4A7C15L;

    private DeterministicGaussian() {
    }

    public static double sample(long seed, long index) {
        if (index < 0) throw new IllegalArgumentException("index must be nonnegative");
        long key = seed + GAMMA * (index + 1);
        double u1 = unitOpen(mix64(key));
        double u2 = unitOpen(mix64(key + GAMMA));
        return StrictMath.sqrt(-2.0 * StrictMath.log(u1))
                * StrictMath.cos(2.0 * StrictMath.PI * u2);
    }

    private static double unitOpen(long bits) {
        return ((bits >>> 11) + 0.5) * 0x1.0p-53;
    }

    private static long mix64(long value) {
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }
}
