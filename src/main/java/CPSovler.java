import com.google.errorprone.annotations.Var;
import fr.univtours.info.optimize.InstanceLegacy;
import fr.univtours.info.optimize.TAPEngine;
import fr.univtours.info.queries.Query;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.IIntConstraintFactory;
import org.chocosolver.solver.constraints.extension.Tuples;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.search.strategy.selectors.values.IntDomainMax;
import org.chocosolver.solver.search.strategy.selectors.values.IntDomainMin;
import org.chocosolver.solver.search.strategy.selectors.variables.MaxRegret;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.RealVar;

import java.util.Arrays;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.stream.Collectors;

public class CPSovler implements TAPEngine {
    @Override
    public List<Query> solve(List<Query> theQ, int timeBudget, int maxDistance) {
        
        return null;
    }

    public static List<Integer> solve(InstanceLegacy ist, int timeBudget, int maxDistance) {

        int maxDist = 20;

        int precision = 10000;
        int[] I = new int[ist.getInterest().length];
        for (int i = 0; i < I.length; i++) {
            I[i] = (int) Math.floor(ist.getInterest()[i] * precision);
        }

        int maxTime = (int) Math.floor(Arrays.stream(ist.getCosts()).max().getAsDouble()) + 1;
        int[] T = new int[ist.getInterest().length];
        for (int i = 0; i < T.length; i++) {
            T[i] = (int) ist.getCosts()[i];
        }

        /*
        Variables
         */
        Model model = new Model("TAP");
        // For each city, the next one visited in the route
        IntVar[] succ = model.intVarArray("succ", ist.getSize(), 0, ist.getSize());
        // For each city, the distance to the succ visited one
        IntVar[] dist = model.intVarArray("dist", ist.getSize(), 0, maxDist);
        IntVar[] interest = model.intVarArray("interest", ist.getSize(), 0, precision);
        IntVar[] time = model.intVarArray("time", ist.getSize(), 0, maxTime);

        IntVar last = model.intVar("last", 0, ist.getSize() - 1);
        IntVar first = model.intVar("first", 0, ist.getSize() - 1);

        // interest and size of solution
        IntVar solSize = model.intVar("sol_size", 0, ist.getSize());
        IntVar solTime = model.intVar("sol_time", 0, ist.getSize());
        IntVar solInterest = model.intVar("sol_value", 0, Arrays.stream(I).sum());

        /*
        Constraints
         */
        // if selected  interest value else 0
        //for (int i = 0; i < ist.getSize(); i++) {
        //    succ[i].eq(i).imp(interest[i].eq(0)).post();
        //    succ[i].ne(i).imp(interest[i].eq(I[i])).post();
        //}
        for (int i = 0; i < ist.getSize(); i++) {
            Tuples int_tup = new Tuples(true);
            for (int j = 0; j < ist.getSize(); j++) {
                if (i == j)
                    int_tup.add(i, 0);
                else
                    int_tup.add(j, I[i]);
            }
            int_tup.add(ist.getSize(), I[i]);
            model.table(succ[i], interest[i], int_tup).post();
        }
        // sum interest of selected queries
        model.sum(interest, "=", solInterest).post();

        // ep_t
        for (int i = 0; i < ist.getSize(); i++) {
            Tuples time_tup = new Tuples(true);
            for (int j = 0; j < ist.getSize(); j++) {
                if (i == j)
                    time_tup.add(i, 0);
                else
                    time_tup.add(j, T[i]);
            }
            time_tup.add(ist.getSize(), T[i]);
            model.table(succ[i], time[i], time_tup).post();
        }
        model.sum(time, "=", solTime).post();
        solTime.le(maxTime).post();


        // ep_d
        model.sum(dist, "<=", maxDistance).post();
        for (int i = 0; i < ist.getSize(); i++) {
            // For each city, the distance to the next one should be maintained
            // this is achieved, here, with a TABLE constraint
            // Such table is inputed with a Tuples object
            // that stores all possible combinations
            Tuples tuples = new Tuples(true);
            for (int j = 0; j < ist.getSize(); j++) {
                // For a given city i
                // a couple is made of a city j and the distance i and j
                if (j != i)
                    tuples.add(j, (int) ist.getDistances()[i][j] );
                else
                    tuples.add(i, 0);
            }
            tuples.add(ist.getSize(), 0);
            // The Table constraint ensures that one combination holds
            // in a solution
            model.table(succ[i], dist[i], tuples).post();
        }


        //First is not a successor
        for (int i = 0; i < ist.getSize(); i++) {
            for (int j = 0; j < ist.getSize(); j++) {
                first.eq(i).imp(succ[j].ne(i)).post();
            }
        }
        //last is one with successor = size
        for (int i = 0; i < ist.getSize(); i++) {
            succ[i].eq(ist.getSize()).imp(last.eq(i)).post();
        }


        model.subPath(succ, first, last, 0, solSize).post();

        model.setObjective(Model.MAXIMIZE, solInterest);

        //force solution
        /*
        succ[2].eq(1).post();
        succ[1].eq(ist.getSize()).post();
        for (int i = 0; i < ist.getSize(); i++) {
            if (i != 1 && i != 2)
                succ[i].eq(i).post();
        }*/

        Solver solver = model.getSolver();
        //solver.showContradiction();
        solver.showShortStatistics();
        while (solver.solve()) {

            int d = 0;
            for (int i = 0; i < ist.getSize(); i++) {
                d += dist[i].getValue();
                if (succ[i].getValue() != i)
                    System.out.print("X");
                else
                    System.out.print("_");
            }
            System.out.println();

            int current = first.getValue();
            while (current != ist.getSize()){
                System.out.print(current);
                if (succ[current].getValue() != ist.getSize())
                    System.out.print(" -> ");
                else
                    System.out.println();
                current = succ[current].getValue();
            }

            System.out.println("Total Distance " + d);
            System.out.println("Interest " + solInterest.getValue()/((double) precision));

            System.out.println();

        }

        return null;
    }


    public static void main(String[] args) {

        String testPath = "/home/alex/instances/tap_0_60.dat";
        InstanceLegacy ist = InstanceLegacy.readFile(testPath);
        solve(ist, 200, 25);

    }
}
