package com.alexscode.utilities.math;

import com.google.common.math.BigIntegerMath;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Random;
import java.util.Scanner;

public class Permutations {
    public static double[] mean(double[] a, double[] b, int permutations){

        double[] meanDiff = new double[permutations];

        double[] ab = new double[a.length + b.length];
        System.arraycopy(a, 0, ab, 0, a.length);
        System.arraycopy(b, 0, ab, a.length, b.length);

        PowerSet ps = new PowerSet(ab);
        if (BigIntegerMath.binomial(ab.length, a.length).compareTo(BigInteger.valueOf(permutations)) < 0){
            permutations = BigIntegerMath.binomial(ab.length, a.length).intValue();
        }

        System.out.printf("Will do %s permutations%n", permutations);

        for (int i = 0; i < permutations; ++i) {
            double mua = 0, mub = 0;
            int sa = 0, sb = 0;
            BitSet pa = ps.getNewRandomElementOFSize(a.length);
            for (int j = 0; j < ab.length; j++) {
                if (pa.get(j)){
                    sa++;
                    mua += ab[j];
                } else {
                    sb ++;
                    mub += ab[j];
                }
            }
            meanDiff[i] = Math.abs((mua/sa) - (mub/sb));
        }
        return meanDiff;
    }

    public static void main(String[] args) {
        double[] b = {35, 36, 37};
        double[] a = {2, 3};

        System.out.println(Arrays.toString(mean(a, b, 12)));


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
        mean(biga, bigb, biga.length*bigb.length);
        long endTime = System.nanoTime();

        long duration = (endTime - startTime)/1000000;

        System.out.println("total " + duration + " ms");

    }
}
