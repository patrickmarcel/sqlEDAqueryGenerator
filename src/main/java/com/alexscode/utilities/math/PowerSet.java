package com.alexscode.utilities.math;

import lombok.Getter;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
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
    private ThreadLocalRandom rd = ThreadLocalRandom.current();
    @Getter
    private int elements;
    @Getter
    private BigInteger size;

    public PowerSet(int baseSetSize) {
        elements = baseSetSize;
        size = BigInteger.valueOf(2).pow(elements);
    }

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

        } while (!alreadyDrawn.add(new BigInteger(set.toByteArray())));

        return set;
    }

    public BitSet getNewRandomElementOFSize_new(int k){
        BitSet set;

        do {
            set = new BitSet(elements);
            for (int i = 0; i < k; i++){
                int pos = rd.nextInt(elements);
                while (set.get(pos)){
                    pos = rd.nextInt(elements);
                }
                set.set(pos);
            }
        } while (!alreadyDrawn.add(new BigInteger(set.toByteArray())));

        return set;
    }

    public BitSet getNewRandomElementOFSize_unsafe(int k){
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
        char []set = {'a', 'b', 'c', 'd', 'e'};

        PowerSet testSet = new PowerSet(set);
        for (int j = 0; j < 4; j++) {
            BitSet bs = testSet.getNewRandomElementOFSize_new(3);
            for (int i = 0; i < testSet.elements; i++) {
                if( bs.get(i))
                    System.out.print(1);
                else
                    System.out.print(0);
            }
            System.out.println();
        }


    }





}
