package fr.univtours.info.optimize;

import fr.univtours.info.queries.AssessQuery;
import fr.univtours.info.queries.Query;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.*;
import java.util.List;
import java.util.Scanner;

@Data
@AllArgsConstructor
public class InstanceLegacy {
    int size;
    double[][] distances;
    double[] costs;
    double[] interest;

    public static InstanceLegacy fromFilter(InstanceLegacy original, boolean[] keep){
        int size = 0;
        for (boolean b : keep) {
            if (b) size++;
        }
        double[][] distances = new double[size][size];
        double[] costs = new double[size];
        double[] interest = new double[size];
        int ni = 0;
        for (int i = 0; i < original.size; i++) {
            if (keep[i]){
                costs[ni] = original.costs[i];
                interest[ni] = original.interest[i];
                int nj = 0;
                for (int j = 0; j < original.size; j++) {
                    if (keep[j]){
                        distances[ni][nj++] = original.distances[i][j];
                    }
                }
                ni++;
            }
        }

        return new InstanceLegacy(size, distances, costs, interest);
    }

    public static InstanceLegacy readFile(String path){
        Scanner scanner = null;
        try {
            scanner = new Scanner(new File(path));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return new InstanceLegacy(0, null, null, null);
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

        return new InstanceLegacy(nbActions, distances, costsOrig, relevancesOrig);
    }

    public static void writeFile(String path, List<Query> theQ){
        try {
            FileOutputStream fos = new FileOutputStream(path);
            PrintWriter io = new PrintWriter(fos);
            io.println(theQ.size());
            for (int i = 0; i < theQ.size(); i++) {
                Query q = theQ.get(i);
                io.print(Double.toString(q.getInterest()));
                if (i < theQ.size() - 1)
                    io.print(" ");
            }
            io.print('\n');
            for (int i = 0; i < theQ.size(); i++) {
                Query q = theQ.get(i);
                io.print((int) q.estimatedTime());// Now using time estimate not real run time
                if (i < theQ.size() - 1)
                    io.print(" ");
            }
            io.print('\n');
            for (Query q : theQ){
                for (int i = 0; i < theQ.size(); i++) {
                    Query qp = theQ.get(i);
                    io.print((int) q.dist(qp));
                    if (i < theQ.size() - 1)
                        io.print(" ");
                }
                io.print('\n');
            }
            io.flush();
            fos.close();
        } catch (IOException e){
            System.err.println("Could not save temp file !");
        }
    }
}
