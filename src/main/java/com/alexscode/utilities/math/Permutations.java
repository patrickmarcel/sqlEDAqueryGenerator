package com.alexscode.utilities.math;

import com.alexscode.utilities.collection.Pair;
import com.google.common.math.BigIntegerMath;


import java.math.BigInteger;
import java.util.BitSet;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;

public class Permutations {
    /**
     *
     * @param a initial first sample
     * @param b initial second sample
     * @param permutations number of permutations to perform
     * @return r[0] stat for mean smaller / r[1] stat for mean equals
     */
    @Deprecated
    public static double[] mean(double[] a, double[] b, int permutations){
        double[] meanDiffs = new double[permutations];

        double[] ab = new double[a.length + b.length];
        System.arraycopy(a, 0, ab, 0, a.length);
        System.arraycopy(b, 0, ab, a.length, b.length);

        PowerSet ps = new PowerSet(ab);
        if (BigIntegerMath.binomial(ab.length, a.length).compareTo(BigInteger.valueOf(permutations)) < 0){
            permutations = BigIntegerMath.binomial(ab.length, a.length).intValue();
            //On vérifie si le nbre de combinaison est sup nbre de permutation, si true le nbre de permutiation prend la valeur du nbre de permutation
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


    public static double[][] meanAndvariance(double[] a, double[] b, int permutations){

        double[] meanDiffs = new double[permutations];
        double[] varDiffs = new double[permutations];

        double[] ab = new double[a.length + b.length];
        System.arraycopy(a, 0, ab, 0, a.length);
        System.arraycopy(b, 0, ab, a.length, b.length);

        final int fullSize = ab.length;
        if (BigIntegerMath.binomial(fullSize, a.length).compareTo(BigInteger.valueOf(permutations)) < 0){
            permutations = BigIntegerMath.binomial(fullSize, a.length).intValue();
        }

        // Very low probability of drawing the same permutation twice ....
        ThreadLocalRandom rd = null;
        PowerSet ps = null;
        boolean safe = fullSize <= 20;
        if (safe)
            ps = new PowerSet(ab);
        else {
            rd = ThreadLocalRandom.current();
        }

        for (int i = 0; i < permutations; ++i) {
            double mua = 0, mub = 0; // mean
            double muasq = 0, mubsq = 0; // mean of squares
            int countA = 0, countB = 0; // count

            BitSet pa;
            if (safe) pa = ps.getNewRandomElementOFSize_new(a.length);
            else {
                pa = new BitSet(fullSize);
                for (int iter = 0; iter < a.length; iter++){
                    int pos = rd.nextInt(fullSize);
                    while (pa.get(pos)){
                        pos = rd.nextInt(fullSize);
                    }
                    pa.set(pos);
                }
            }
            for (int j = 0; j < fullSize; j++) {
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
        return new double[][]{meanDiffs, varDiffs};
    }

    public static Pair<double[][], double[][]> meanAndvariance(double[][] a, double[][] b, int permutations){

        double[][] meanDiffs = new double[a.length][permutations];
        double[][] varDiffs = new double[a.length][permutations];
        final int fullSize = a[0].length + b[0].length;

        // Can't do more permutations than possible
        if (BigIntegerMath.binomial(fullSize, a.length).compareTo(BigInteger.valueOf(permutations)) < 0){
            permutations = BigIntegerMath.binomial(fullSize, a.length).intValue();
        }

        // Very low probability of drawing the same permutation twice ....
        ThreadLocalRandom rd = null;
        PowerSet ps = null;
        BitSet[] perms = new BitSet[permutations];
        if (fullSize <= 20){
            ps = new PowerSet(fullSize);
            for (int i = 0; i < permutations; i++)
                perms[i] = ps.getNewRandomElementOFSize_new(a.length);
            }
        else {
            rd = ThreadLocalRandom.current();
            for (int i = 0; i < permutations; i++)
                perms[i] = randomPermNoCheck(fullSize, a.length, rd);
        }

        //For each measure
        for (int m = 0; m < a.length; m++) {
            double[] ab = new double[a[m].length + b[m].length];
            System.arraycopy(a[m], 0, ab, 0, a[m].length);
            System.arraycopy(b[m], 0, ab, a[m].length, b[m].length);

            for (int i = 0; i < permutations; ++i) {
                double mua = 0, mub = 0; // mean
                double muasq = 0, mubsq = 0; // mean of squares
                int countA = 0, countB = 0; // count

                for (int j = 0; j < fullSize; j++) {
                    if (perms[i].get(j)){
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
                varDiffs[m][i] =  (mubsq - (mub*mub)) - (muasq - (mua*mua));
                //Compute stat: E[b] - E[a]
                meanDiffs[m][i] =  (mub/countB) - (mua/countA);
            }
        }

        return new Pair<>(meanDiffs, varDiffs);
    }

    public static BitSet randomPermNoCheck(int elements, int k, Random rd){
        BitSet set = new BitSet(elements);
        for (int i = 0; i < k; i++){
            int pos = rd.nextInt(elements);
            while (set.get(pos)){
                pos = rd.nextInt(elements);
            }
            set.set(pos);
        }
        return set;
    }


    public static void main(String[] args) {

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
        meanAndvariance(biga, bigb, biga.length*bigb.length);
        long endTime = System.nanoTime();

        long duration = (endTime - startTime)/1000000;

        System.out.println("total " + duration + " ms");

    }


}
