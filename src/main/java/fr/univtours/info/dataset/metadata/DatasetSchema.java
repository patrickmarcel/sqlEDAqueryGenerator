package fr.univtours.info.dataset.metadata;

import com.alexscode.utilities.collection.Pair;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import fr.univtours.info.DBUtils;
import fr.univtours.info.dataset.Dataset;
import org.jgrapht.Graph;
import org.jgrapht.alg.connectivity.KosarajuStrongConnectivityInspector;
import org.jgrapht.alg.interfaces.StrongConnectivityAlgorithm;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import java.util.*;

public class DatasetSchema {
    List<DatasetDimension> dimensions;
    List<DatasetMeasure> measures;
    DefaultDirectedGraph<DatasetDimension, DefaultEdge> hierarchy;

    public DatasetSchema(Dataset ds){
        dimensions = ds.getTheDimensions();
        measures = ds.getTheMeasures();
        hierarchy = new DefaultDirectedGraph<>(DefaultEdge.class);


        ImmutableSet<DatasetDimension> set = ImmutableSet.copyOf(ds.getTheDimensions());
        Set<Set<DatasetDimension>> combinations = Sets.combinations(set, 2);

        //For each pair of dimension attributes
        for(Set<DatasetDimension> s : combinations){
            Iterator<DatasetDimension> it = s.iterator();
            Pair<DatasetDimension, DatasetDimension> dims = new Pair<>(it.next(), it.next());
            if (! dims.getA().equals(dims.getB()) ) {
                if (DBUtils.checkAimpliesB(dims.getA(), dims.getB(), ds.getConn(), ds.getTable())) {
                    hierarchy.addVertex(dims.getA());
                    hierarchy.addVertex(dims.getB());
                    hierarchy.addEdge(dims.getA(), dims.getB());
                    //System.out.println(dims.getA() + " -> " + dims.getB());
                }
                if (DBUtils.checkAimpliesB(dims.getB(), dims.getA(), ds.getConn(), ds.getTable())) {
                    hierarchy.addVertex(dims.getA());
                    hierarchy.addVertex(dims.getB());
                    hierarchy.addEdge(dims.getB(), dims.getA());
                    //System.out.println(dims.getB() + " -> " + dims.getA());
                }
            }
        }
    }

    //TODO infer weak connectivity instead, then add all
    public void getIndividualHierarchies(){
        // computes all the strongly connected components of the directed graph
        StrongConnectivityAlgorithm<DatasetDimension, DefaultEdge> scAlg = new KosarajuStrongConnectivityInspector<>(hierarchy);
        List<Graph<DatasetDimension, DefaultEdge>> stronglyConnectedSubgraphs = scAlg.getStronglyConnectedComponents();

        // prints the strongly connected components
        System.out.println("Strongly connected components:");
        for (int i = 0; i < stronglyConnectedSubgraphs.size(); i++) {
            System.out.println(stronglyConnectedSubgraphs.get(i));
        }
        System.out.println();
    }

}
