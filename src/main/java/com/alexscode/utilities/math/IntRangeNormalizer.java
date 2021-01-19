package com.alexscode.utilities.math;


public class IntRangeNormalizer {
    int targetLow, targetHigh;
    double[] data;
    double dataLow, dataHigh;

    public IntRangeNormalizer(int targetLow, int targetHigh, double[] data) {
        this.targetLow = targetLow;
        this.targetHigh = targetHigh;
        this.data = data;
    }

    public int[] normalized(){
        dataLow = min(data, data.length);
        dataHigh = max(data, data.length);
        int out[] = new int[data.length];
        for (int i = 0; i < data.length; i++) {
            double raw = Math.round( (data[i] - dataLow) / (dataHigh - dataLow)) * (targetHigh - targetLow) + targetLow;
            out[i] = Math.toIntExact(Math.round(raw));
        }
        return out;
    }

    private static double min(double[] a, int bound){

        double best = a[0];
        for (int i = 0; i < bound; i++) {
            if (a[i] < best){
                best = a[i];
            }
        }
        return best;
    }

    private static double max(double[] a, int bound){

        double best = a[0];
        for (int i = 0; i < bound; i++) {
            if (a[i] > best){
                best = a[i];
            }
        }
        return best;
    }
}
