package fr.univtours.info.optimize;

import fr.univtours.info.queries.AssessQuery;

import java.io.*;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CPLEXTAP implements TAPEngine{
    String binary_path;
    String temp_file_path;

    public CPLEXTAP(String binary_path, String temp_file_path) {
        this.binary_path = binary_path;
        this.temp_file_path = temp_file_path;
    }

    @Override
    public List<AssessQuery> solve(List<AssessQuery> theQ, int timeBudget, int maxDistance) {

        try {
            FileOutputStream fos = new FileOutputStream(temp_file_path);
            PrintWriter io = new PrintWriter(fos);
            io.println(theQ.size());
            for (int i = 0; i < theQ.size(); i++) {
                AssessQuery q = theQ.get(i);
                io.print(q.getInterest());
                if (i < theQ.size() - 1)
                    io.print(" ");
            }
            io.print('\n');
            for (int i = 0; i < theQ.size(); i++) {
                AssessQuery q = theQ.get(i);
                io.print((int) q.estimatedTime());// Now using time estimate not real run time
                if (i < theQ.size() - 1)
                    io.print(" ");
            }
            io.print('\n');
            for (AssessQuery q : theQ){
                for (int i = 0; i < theQ.size(); i++) {
                    AssessQuery qp = theQ.get(i);
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

        System.out.println("Running CPLEX");
        String[] cplex_cmd = new String[]{binary_path, Paths.get(temp_file_path).toAbsolutePath().toString(), String.valueOf(timeBudget), String.valueOf(maxDistance)};
        System.out.println(Arrays.toString(cplex_cmd));
        try {
            String solutionRaw = "";
            String line;
            Process p = Runtime.getRuntime().exec(cplex_cmd);
            BufferedReader bri = new BufferedReader(new InputStreamReader(p.getInputStream()));
            BufferedReader bre = new BufferedReader(new InputStreamReader(p.getErrorStream()));

            while ((line = bri.readLine()) != null) {
                System.out.println(line);
                if (line.startsWith("SOLUTION:"))
                    solutionRaw = line;
            }
            bri.close();

            bre.transferTo(new PrintWriter(System.err));
            bre.close();

            p.waitFor();

            System.out.println("CPLEX is Done");
            System.out.println(solutionRaw);
            ArrayList<AssessQuery> solution = new ArrayList<>();
            Arrays.stream(solutionRaw.replace("SOLUTION: ", "").stripTrailing().split(" "))
                    .mapToInt(Integer::parseInt)
                    .forEach(i -> solution.add(theQ.get(i-1)));
            return solution;
        }
        catch (Exception err) {
            err.printStackTrace();
        }

        return new ArrayList<>();
    }
}
