package com.iot.simulator.utils;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Utility for statistical distributions used in IoT simulation.
 * Optimized for performance using ThreadLocalRandom.
 */
public class StatisticsUtils {

    /**
     * Generates a value based on Normal (Gaussian) distribution using Box-Muller transform.
     */
    public static double generateNormal(double mean, double stdDev) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double u1 = random.nextDouble();
        double u2 = random.nextDouble();
        double z0 = Math.sqrt(-2.0 * Math.log(u1)) * Math.cos(2.0 * Math.PI * u2);
        return mean + stdDev * z0;
    }

    /**
     * Generates a value based on Beta distribution (simplified version).
     */
    public static double generateBeta(double alpha, double beta, double min, double max) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double u1 = Math.pow(random.nextDouble(), 1.0 / alpha);
        double u2 = Math.pow(random.nextDouble(), 1.0 / beta);
        double betaValue = u1 / (u1 + u2);
        return min + betaValue * (max - min);
    }

    /**
     * Generates a value based on Exponential distribution.
     */
    public static double generateExponential(double lambda) {
        return -Math.log(1.0 - ThreadLocalRandom.current().nextDouble()) / lambda;
    }

    /**
     * Generates a value based on Poisson distribution.
     */
    public static int generatePoisson(double lambda) {
        double L = Math.exp(-lambda);
        double p = 1.0;
        int k = 0;

        do {
            k++;
            p *= ThreadLocalRandom.current().nextDouble();
        } while (p > L);

        return k - 1;
    }

    /**
     * Generates a value based on Triangular distribution.
     */
    public static double generateTriangular(double min, double max, double mode) {
        double u = ThreadLocalRandom.current().nextDouble();
        double fc = (mode - min) / (max - min);

        if (u < fc) {
            return min + Math.sqrt(u * (max - min) * (mode - min));
        } else {
            return max - Math.sqrt((1.0 - u) * (max - min) * (max - mode));
        }
    }
}
