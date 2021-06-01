package com.alexscode.utilities.math;

import lombok.Getter;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author Alexandre Chanson
 * A power set implementation allowing to draw random elements
 * uses bitset as representation for elements for easier use.
 *
 * An element of the power set P(S) is represented as a binary number of size |S|
 * this requires that the elements of S be stored in consistant order (but can be arbitrary)
 * but allows to directly generate random members of P(S) without using nested loops
 */
public class PowerSet {

    private final TreeSet<BigInteger> alreadyDrawn = new TreeSet<>();
    private Random rd = new Random();
    @Getter
    private int elements;
    @Getter
    private BigInteger size;

    public PowerSet(double[] baseSet) {
        elements = baseSet.length;
        size = BigInteger.valueOf(2).pow(elements);
    }

    public PowerSet(char[] baseSet) {
        elements = baseSet.length;
        size = BigInteger.valueOf(2).pow(elements);
    }

    public PowerSet(int[] baseSet) {
        elements = baseSet.length;
        size = BigInteger.valueOf(2).pow(elements);
    }

    public PowerSet(Object[] baseSet) {
        elements = baseSet.length;
        size = BigInteger.valueOf(2).pow(elements);
    }

    public BitSet getRandomElement(){
        BigInteger randomNumber;
        do {
            randomNumber = new BigInteger(size.bitLength(), rd);
        } while (randomNumber.compareTo(size) >= 0);

        return convertTo(randomNumber);
    }

    public BitSet getNewRandomElement(){
        BigInteger randomNumber;

        do {
            randomNumber = new BigInteger(size.bitLength(), rd);
        } while ( randomNumber.compareTo(size) >= 0 || alreadyDrawn.contains(randomNumber));

        alreadyDrawn.add(randomNumber);
        return convertTo(randomNumber);

    }

    // Shuffle labels (0 left ou / 1 in the set) and check if already drawn using
    // the numeric representation of the set

    private int pks;
    List<Integer> indexes;
    public BitSet getNewRandomElementOFSize(int k){
        if (pks != k) {
            indexes = IntStream.range(0, elements).boxed().collect(Collectors.toList());
            pks = k;
        }
        BitSet set;

        do {
            Collections.shuffle(indexes, rd);
            set = new BitSet(elements);
            for (int i = 0; i < k; i++)
                set.set(indexes.get(i));

        } while (alreadyDrawn.contains(new BigInteger(set.toByteArray())));

        alreadyDrawn.add(new BigInteger(set.toByteArray()));
        return set;
    }

    private static BitSet convertTo (BigInteger bi) {
        /*BitSet bs = new BitSet(elements);
        for (int i = 0; i < elements; i++) {
            if (bi.testBit(i))
                bs.set(i);
        }
        return bs;*/

        byte[] bia = bi.toByteArray();
        byte[] bsa = new byte[bia.length + 1];
        System.arraycopy(bia,0,bsa,0, bia.length);
        bsa[bia.length] = 0x01;
        return BitSet.valueOf(bsa);
    }

    public static void main (String[] args)
    {
        char []set = {'a', 'b', 'c', 'd'};

        PowerSet testSet = new PowerSet(set);
        BitSet bs = testSet.getNewRandomElement();
        for (int i = 0; i < testSet.elements; i++) {
            if( bs.get(i))
                System.out.print(1);
            else
                System.out.print(0);
        }
        System.out.println();

    }





}
