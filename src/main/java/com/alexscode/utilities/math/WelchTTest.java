package com.alexscode.utilities.math;

import fr.univtours.info.Insight;
import org.apache.commons.math3.stat.inference.TTest;
import org.apache.commons.math3.util.FastMath;

/**
 * A class that performs a mean test using the Welch's t-test
 * @author Thierno Amadou DIALLO
 */
public class WelchTTest {

    /**
     * Constructs a WelchTTest object and performs the Welch's t-test on the given NormalityTest samples.
     *
     * @param sample1 The first NormalityTest sample.
     * @param sample2 The second NormalityTest sample.
     */
    public WelchTTest(NormalityTest sample1, NormalityTest sample2) {

        int df = (int) Math.round( degreeOfFreedom(sample1.getVariance(), sample2.getVariance(), (double) sample1.getSample().length, (double) sample2.getSample().length) );

        System.out.println("Degree of freedom = " + df);

        double tStat = Math.abs( tStatistic(sample1.getMean(), sample2.getMean(), sample1.getVariance(), sample2.getVariance(), (double) sample1.getSample().length, (double) sample2.getSample().length) );
        System.out.println("La stat t = " + tStat);

        System.out.println("On rejete H0 ? " + rejectH0(sample1, sample2));

        System.out.println("pValue = " + computePvalue(sample1, sample2));

        System.out.println("Covariance lié au moment d'ordre 3 : " + computeCovarianceSkewness(sample1, sample2));
    }

    /**
     * Performs a mean test using the Welch's t-test on two NormalityTest objects.
     *
     * @param i The Insight object.
     * @param a The first NormalityTest object.
     * @param b The second NormalityTest object.
     * @return An array of Insight objects containing the test results.
     */
    public static Insight[] MeanTest (Insight i, NormalityTest a, NormalityTest b) {

        Insight[] added = new Insight[1];
        int addPos = 0;

        double pValueTestWelch = computePvalue(a, b);
        boolean rejectH0 = rejectH0(a, b);

        Insight tmp ;
        if( rejectH0 && ( pValueTestWelch < 0.05 ) && ( Math.abs(computeCovarianceSkewness(a, b)) < 1 ) ) {
            if(a.getMean() > b.getMean()) {
                tmp = new Insight(i.getDim(), i.getSelA(), i.getSelB(), i.getMeasure(), Insight.MEAN_GREATER);
                added[addPos] = tmp;
                tmp.setP(pValueTestWelch);
            }
            else if(a.getMean() < b.getMean()){
                tmp = new Insight(i.getDim(), i.getSelA(), i.getSelB(), i.getMeasure(), Insight.MEAN_SMALLER);
                added[addPos] = tmp;
                tmp.setP(pValueTestWelch);
            }
        }else{
            tmp = new Insight(i.getDim(), i.getSelA(), i.getSelB(), i.getMeasure(), Insight.MEAN_EQUALS);
            added[addPos] = tmp;
            tmp.setP(pValueTestWelch);
        }

        return added;
    }

    /**
     * Computes the p-value using the T-test between two NormalityTest samples.
     *
     * @param a The first NormalityTest sample.
     * @param b The second NormalityTest sample.
     * @return The computed p-value.
     */
    public static double computePvalue(NormalityTest a, NormalityTest b){
        TTest tTest = new TTest();
        double pValue = tTest.tTest(a.getSample(), b.getSample());
        return  pValue;
    }

    /**
     * Determines whether to reject the null hypothesis based on the T-test between two normal samples.
     *
     * @param a The first sample of the normality test.
     * @param b The second sample of the normality test.
     * @return True if the null hypothesis is rejected, false otherwise.
     */
    public static boolean rejectH0(NormalityTest a, NormalityTest b){
        TTest tTest = new TTest();										//H0 : les moyennes sont égal
        boolean rejectH0 = tTest.tTest(a.getSample(), b.getSample(), 0.05); // retourne true si on doit rejeter l'hypothèse null avec un niveau de confiance de 1 - 0.05
        return rejectH0;
    }

    /**
     * Computes the covariance related to skewness between two Normal samples.
     *
     * @param a The first NormalityTest sample.
     * @param b The second NormalityTest sample.
     * @return The computed covariance.
     */
    public static double computeCovarianceSkewness(NormalityTest a, NormalityTest b){
        double covariance = (a.getSkewnessValue() / a.getSample().length) - (b.getSkewnessValue() / b.getSample().length) ;
        return covariance;
    }

    /**
     * Computes t test statistic for 2-sample t-test.
     * <p>
     * Does not assume that subpopulation variances are equal.</p>
     *
     * @param m1 first sample mean
     * @param m2 second sample mean
     * @param v1 first sample variance
     * @param v2 second sample variance
     * @param n1 first sample n
     * @param n2 second sample n
     * @return t test statistic
     */
    public static double tStatistic(final double m1, final double m2, final double v1, final double v2, final double n1, final double n2)  {
        return (m1 - m2) / FastMath.sqrt((v1 / n1) + (v2 / n2));
    }

    /**
     * Computes approximate degrees of freedom for 2-sample t-test.
     *
     * @param v1 first sample variance
     * @param v2 second sample variance
     * @param n1 first sample n
     * @param n2 second sample n
     * @return approximate degrees of freedom
     */
    public static double degreeOfFreedom(double v1, double v2, double n1, double n2) {
        return (((v1 / n1) + (v2 / n2)) * ((v1 / n1) + (v2 / n2))) /
                ((v1 * v1) / (n1 * n1 * (n1 - 1d)) + (v2 * v2) /
                        (n2 * n2 * (n2 - 1d)));
    }

}
