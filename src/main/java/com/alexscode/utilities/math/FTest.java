package com.alexscode.utilities.math;


import org.apache.commons.math3.distribution.FDistribution;
import org.apache.commons.math3.stat.descriptive.moment.Variance;

public class FTest {
    private double[] x, y;
    Variance variance = new Variance();

    public FTest(double[] x, double[] y) {
        super();
        this.x = x;
        this.y = y;
    }

    public double getPValue() {
        double q = Math.pow(variance.evaluate(x), 2);
        double p = Math.pow(variance.evaluate(y), 2);
        double f = q / p;
        FDistribution fd=new FDistribution(x.length - 1, y.length - 1);

        return (1 - fd.cumulativeProbability(f)) *2;
    }
}

