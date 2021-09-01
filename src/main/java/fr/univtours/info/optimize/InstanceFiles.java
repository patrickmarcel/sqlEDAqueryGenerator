package fr.univtours.info.optimize;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class InstanceFiles {

    public static Instance readFile(String path){
        Scanner scanner = null;
        try {
            scanner = new Scanner(new File(path));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return new Instance(0, null, null, null);
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

        return new Instance(nbActions, distances, costsOrig, relevancesOrig);
    }
}
