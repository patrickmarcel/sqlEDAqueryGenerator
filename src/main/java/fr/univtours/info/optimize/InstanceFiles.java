package fr.univtours.info.optimize;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class InstanceFiles {
    @Data
    @AllArgsConstructor
    public static class RawInstance {
        int size;
        double[][] distances;
        double[] costs;
        double[] interest;
    }

    public static RawInstance readFile(String path){
        Scanner scanner = null;
        try {
            scanner = new Scanner(new File(path));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return new RawInstance(0, null, null, null);
        }

        String line = scanner.nextLine();
        int nbActions = Integer.parseInt(line);


        double[] relevancesOrig = new double[nbActions];
        line = scanner.nextLine();
        String[] val;
        val = line.split(" ");
        for (int i = 0; i < nbActions; i++) {
            relevancesOrig[i] =Double.parseDouble(val[i]);
        }


        double[] costsOrig = new double[nbActions];
        line = scanner.nextLine();
        val = line.split(" ");
        for (int i = 0; i < nbActions; i++) {
            costsOrig[i] = Double.parseDouble(val[i]);
        }

        int i = 0;
        double[][] distances = new double[nbActions][nbActions];
        while (scanner.hasNext()) {
            line = scanner.nextLine();

            val = line.split(" ");
            for (int j = 0; j < nbActions; j++) {
                //System.out.println("val "+ val[j]);
                distances[i][j] = Double.parseDouble(val[j]);
            }
            i++;
        }

        return new RawInstance(nbActions, distances, costsOrig, relevancesOrig);
    }
}
