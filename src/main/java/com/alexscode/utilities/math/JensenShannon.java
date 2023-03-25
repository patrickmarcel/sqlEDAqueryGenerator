package com.alexscode.utilities.math;

//import smile.math.MathEx;

public class JensenShannon {
    public static double unsafeCompute(double[] p, double[] q){
        int samples_size = p.length;
        double kl = 0.0;

        for (int i = 0; i < samples_size; i++) {
            double m_i = (p[i] + q[i]) / 2;
            if (m_i != 0) {
                if (p[i] != 0.0) {
                    kl += p[i] * Math.log(p[i] / m_i);
                }
                if (q[i] != 0.0) {
                    kl += q[i] * Math.log(q[i] / m_i);
                }
            }
        }

        return kl / 2.0d;

    }

    public static void main(String[] args) {
        //double[] a = new double[]{0.1,0.2,0.3,0.1,0.1,0.2};
        //double[] b = new double[]{0.0,0.25,0.3,0.05,0.1,0.3};

        double[] a = new double[]{0.1,0.9,0.0,0.0,0.0,0.0};
        double[] b = new double[]{0.0,0.0,0.3,0.7,0.0,0.0};

        System.out.println(unsafeCompute(a, b));
        System.out.println(unsafeCompute(b, a));
        //System.out.println(MathEx.JensenShannonDivergence(a, b));
        //System.out.println(MathEx.JensenShannonDivergence(b, a));
    }
}
