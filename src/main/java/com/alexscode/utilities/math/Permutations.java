package com.alexscode.utilities.math;

import com.alexscode.utilities.collection.Pair;
import com.google.common.math.BigIntegerMath;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Random;
import java.util.Scanner;

public class Permutations {
    /**
     *
     * @param a initial first sample
     * @param b initial second sample
     * @param permutations number of permutations to perform
     * @return r[0] stat for mean smaller / r[1] stat for mean equals
     */
    public static double[] mean(double[] a, double[] b, int permutations){
        double[] meanDiffs = new double[permutations];

        double[] ab = new double[a.length + b.length];
        System.arraycopy(a, 0, ab, 0, a.length);
        System.arraycopy(b, 0, ab, a.length, b.length);

        PowerSet ps = new PowerSet(ab);
        if (BigIntegerMath.binomial(ab.length, a.length).compareTo(BigInteger.valueOf(permutations)) < 0){
            permutations = BigIntegerMath.binomial(ab.length, a.length).intValue();
        }

        for (int i = 0; i < permutations; ++i) {
            double mua = 0, mub = 0; // mean
            int countA = 0, countB = 0; // counts
            BitSet pa = ps.getNewRandomElementOFSize_new(a.length);
            for (int j = 0; j < ab.length; j++) {
                if (pa.get(j)){
                    countA++;
                    mua += ab[j];
                } else {
                    countB ++;
                    mub += ab[j];
                }
            }
            //Compute stat: E[b] - E[a]
            meanDiffs[i] =  (mub/countB) - (mua/countA);
        }
        return meanDiffs;
    }


    public static Pair<double[], double[]> meanAndvariance(double[] a, double[] b, int permutations){

        double[] meanDiffs = new double[permutations];
        double[] varDiffs = new double[permutations];

        double[] ab = new double[a.length + b.length];
        System.arraycopy(a, 0, ab, 0, a.length);
        System.arraycopy(b, 0, ab, a.length, b.length);

        PowerSet ps = new PowerSet(ab);
        if (BigIntegerMath.binomial(ab.length, a.length).compareTo(BigInteger.valueOf(permutations)) < 0){
            permutations = BigIntegerMath.binomial(ab.length, a.length).intValue();
        }


        for (int i = 0; i < permutations; ++i) {
            double mua = 0, mub = 0; // mean
            double muasq = 0, mubsq = 0; // mean of squares
            int countA = 0, countB = 0; // count

            BitSet pa = ps.getNewRandomElementOFSize_new(a.length);
            for (int j = 0; j < ab.length; j++) {
                if (pa.get(j)){
                    countA++;
                    mua += ab[j];
                    muasq += ab[j]*ab[j];
                } else {
                    countB ++;
                    mub += ab[j];
                    mubsq += ab[j]*ab[j];
                }
            }
            //Compute means
            mub = mub/countB;
            mua = mua/countA;
            mubsq = mubsq/countB;
            muasq = muasq/countA;
            // Compute stat: var(b) - var(a)
            varDiffs[i] =  (mubsq - (mub*mub)) - (muasq - (mua*mua));
            //Compute stat: E[b] - E[a]
            meanDiffs[i] =  (mub/countB) - (mua/countA);
        }
        return new Pair<>(meanDiffs, varDiffs);
    }

    /*
    public static void main(String[] args) {
        double[] b = {35, 36, 37};
        double[] a = {2, 3};

        System.out.println(Arrays.toString(mean_smaller(a, b, 12)));


        Random rand = new Random();
        Scanner scanner = new Scanner(System.in);


        System.out.println("Enter a size");
        double[] biga = new double[scanner.nextInt()];
        System.out.println("Enter b size");
        double[] bigb = new double[scanner.nextInt()];
        //populate the array with doubles
        for(int i =0; i < biga.length; i++) {
            biga[i] = rand.nextDouble();
            bigb[i] = rand.nextDouble();
        }

        long startTime = System.nanoTime();
        mean_smaller(biga, bigb, biga.length*bigb.length);
        long endTime = System.nanoTime();

        long duration = (endTime - startTime)/1000000;

        System.out.println("total " + duration + " ms");

    }
    */

}
