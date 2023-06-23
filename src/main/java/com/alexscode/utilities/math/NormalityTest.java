package com.alexscode.utilities.math;

import org.apache.commons.math3.stat.descriptive.moment.*;


/**
 * A class that performs a normality test on a given sample.
 * @author Thierno Amadou DIALLO
 */
public class NormalityTest {

    private double [] sample;
    private double mean ;
    private double std;
    private double skewnessValue;
    private double kurtosisValue;
    private double variance;
    private double dStat;
    private double pValue;

    /**
     * Constructs a NormalityTest object with the given sample.
     *
     * @param sample The sample to test for normality.
     */
    public NormalityTest(double [] sample){

        this.sample = sample;

        Mean m = new Mean();
        mean = m.evaluate(sample);

        StandardDeviation standardDeviation = new StandardDeviation();
        std = standardDeviation.evaluate(sample);

        Skewness skewness = new Skewness();
        skewnessValue = skewness.evaluate(sample);

        Kurtosis kurtosis = new Kurtosis();
        kurtosisValue = kurtosis.evaluate(sample);

        Variance var = new Variance();
        variance = var.evaluate(sample);
    }

    public double[] getSample() {
        return sample;
    }
    public double getMean() {
        return mean;
    }

    public double getSkewnessValue() {
        return skewnessValue;
    }

    public double getVariance() {
        return variance;
    }

    /**
     * Checks if samples A and B are considered normal.
     *
     * @param a The sample A to check.
     * @param b The sample B to check.
     * @return true if the samples are considered normal, false otherwise.
     */
    public static boolean isNormal(NormalityTest a, NormalityTest b) {
        // Calculate the normalized skewness difference
        double skewnessDiff = Math.abs((a.skewnessValue / a.sample.length) - (b.skewnessValue / b.sample.length));

        // Compare with the threshold of 0.049
        return skewnessDiff < 0.049;
    }

    public void afficher() {
        System.out.println("Kurtosis = " + kurtosisValue);
        System.out.println("Skewness = " + skewnessValue);
        System.out.println("moyenne = " + mean);
        System.out.println("Variance = " + variance);
        System.out.println("écart type = " + std);
    }

    public static void main(String [] args){
        long startTime = System.nanoTime();

        double[] d = {7, 1, 3, 8, 6, 7, 8, 10};
        double[] d1 = {5.39, 4.96, 5.76, 4.60, 5.33, 4.93, 6.19,	4.88, 5.23,	5.78, 5.15,	5.07, 5.09, 5.39, 5.32, 4.90, 4.38,	5.14, 5.43, 5.86, 4.46, 5.03, 4.54, 5.02, 5.12};
        NormalityTest n = new NormalityTest(d);
        NormalityTest m = new NormalityTest(d1);
        if( isNormal(n, m)){
            WelchTTest test = new WelchTTest(n, m);
        }
        n.afficher();
    }
}
