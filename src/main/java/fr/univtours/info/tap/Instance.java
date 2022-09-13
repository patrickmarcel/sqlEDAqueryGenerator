package fr.univtours.info.tap;

import fr.univtours.info.queries.AssessQuery;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Instance {
    @Accessors
    private double epTime, epDist;
    @Getter
    private int size;
    public double[][] distances;
    public double[] costs;
    public double[] interest;

    private Instance() {
    }

    public Instance(List<AssessQuery> queries, double epTime, double epDist) {
        this.size = queries.size();
        this.epTime = epTime;
        this.epDist = epDist;
        this.distances = new double[size][size];
        this.costs = new double[size];
        this.interest = new double[size];
        for (int i = 0; i < size; i++) {
            AssessQuery qi = queries.get(i);
            costs[i] = qi.estimatedTime();
            interest[i] = qi.getInterest();
            for (int j = i; j < size; j++) {
                AssessQuery qj = queries.get(j);
                distances[i][j] = qi.dist(qj);
                distances[j][i] = distances[i][j];
            }
        }
        
    }


    public Instance(List<AssessQuery> queries, double epTime, double epDist, boolean nodist) {
        this.size = queries.size();
        this.epTime = epTime;
        this.epDist = epDist;
        this.costs = new double[size];
        this.interest = new double[size];
        for (int i = 0; i < size; i++) {
            AssessQuery qi = queries.get(i);
            costs[i] = qi.estimatedTime();
            interest[i] = qi.getInterest();
        }

    }

    public static Instance newFromFileBinary(String path){
        try {
            InstanceOuterClass.Instance in = InstanceOuterClass.Instance.parseFrom(Files.readAllBytes(Paths.get(path)));
            Instance out = new Instance();
            
            out.size = in.getSize();
            out.epDist = in.getEpDist();
            out.epTime = in.getEpTime();
            out.costs = in.getCostsList().stream().mapToDouble(Double::doubleValue).toArray();
            out.interest = in.getInterestsList().stream().mapToDouble(Double::doubleValue).toArray();
            
            out.distances = new double[out.size][out.size];
            List<Double> distances = in.getDistancesList();
            int pos = 0;
            for (int i = 0; i < out.size; i++) {
                for (int j = i; j < out.size; j++) {
                    out.distances[i][j] = distances.get(pos++);
                    out.distances[j][i] = out.distances[i][j];
                }
            }
            return out;
        } catch (IOException e) {
            System.err.println("[ERROR] Failed to read instance from " + path);
            return null;
        }

    }
    
    public void toFileBinary(String path){
        List<Double> d = new ArrayList<>(size + ((size*size)/2));
        for (int i = 0; i < size; i++) {
            for (int j = i; j < size; j++) {
                d.add(distances[i][j]);
            }
        }
        InstanceOuterClass.Instance toWrite = InstanceOuterClass.Instance.newBuilder()
                .setSize(size)
                .setEpTime(epTime)
                .setEpDist(epDist)
                .addAllCosts(Arrays.stream(costs).boxed().collect(Collectors.toList()))
                .addAllInterests(Arrays.stream(interest).boxed().collect(Collectors.toList()))
                .addAllDistances(d).build();
        try {
            FileOutputStream fos = new FileOutputStream(path);
            toWrite.writeTo(fos);
            fos.close();
        } catch (IOException e){
            System.err.println("[ERROR] Failed to write instance to " + path);
        }
        
    }

    public void toFileBinaryNoDist(String path){
        InstanceOuterClass.Instance toWrite = InstanceOuterClass.Instance.newBuilder()
                .setSize(size)
                .setEpTime(epTime)
                .setEpDist(epDist)
                .addAllCosts(Arrays.stream(costs).boxed().collect(Collectors.toList()))
                .addAllInterests(Arrays.stream(interest).boxed().collect(Collectors.toList()))
                .build();
        try {
            FileOutputStream fos = new FileOutputStream(path);
            toWrite.writeTo(fos);
            fos.close();
        } catch (IOException e){
            System.err.println("[ERROR] Failed to write instance to " + path);
        }

    }

    public void toFileLegacy(String path){
        try {
            FileOutputStream fos = new FileOutputStream(path);
            PrintWriter io = new PrintWriter(fos);
            io.println(size);
            for (int i = 0; i < size; i++) {
                io.print(interest[i]);
                if (i < size - 1)
                    io.print(" ");
            }
            io.print('\n');
            for (int i = 0; i < size; i++) {
                io.print((int) costs[i]);
                if (i < size - 1)
                    io.print(" ");
            }
            io.print('\n');
            for (int i = 0; i < size; i++){
                for (int j = 0; j < size; j++) {
                    io.print((int) distances[i][j]);
                    if (j < size - 1)
                        io.print(" ");
                }
                io.print('\n');
            }
            io.flush();
            fos.close();
        } catch (IOException e){
            System.err.println("Could not save legacy file !");
        }
    }

    public static void main(String[] args) {
        String dir = "data/serial_tests/";
        Instance demo = new Instance();
        demo.size=4;
        demo.costs = new double[]{1,3,5,7};
        demo.interest = new double[]{3,3,5,9};
        demo.distances = new double[][]{
                {-1, 2, 3, 5},
                {0, -2, 5, 6},
                {0, 0, -1, 6},
                {0, 0, 0, -1}};

        demo.toFileLegacy(dir + "legacy_out.dat");
        demo.toFileBinary(dir + "bin_out.dat");
        Instance.newFromFileBinary(dir + "bin_out.dat").toFileLegacy(dir + "bin_readable_out.dat");

    }
    
}
