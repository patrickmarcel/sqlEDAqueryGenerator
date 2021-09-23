package fr.univtours.info.dataset;

import com.alexscode.utilities.collection.Pair;
import com.google.common.collect.Sets;
import fr.univtours.info.dataset.metadata.DatasetAttribute;
import fr.univtours.info.dataset.metadata.DatasetDimension;
import fr.univtours.info.dataset.metadata.DatasetMeasure;
import lombok.Getter;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

// Only for sums !!!
public class PartialAggregate {
    @Getter
    final List<DatasetDimension> groupBySet;
    final List<DatasetMeasure> measures;
    final Object[][] dims;
    final double[][] data;
    final int len;

    public PartialAggregate(List<DatasetDimension> groupBySet, List<DatasetMeasure> measures, Dataset origin) {
        this.groupBySet = groupBySet;
        this.measures = measures;

        List<List<Object>> tmpDim = groupBySet.stream().map(ignored -> (List<Object>) new ArrayList<>()).collect(Collectors.toList());
        List<List<Double>> tmpMeas = measures.stream().map(ignored -> (List<Double>) new ArrayList<Double>()).collect(Collectors.toList());
        int counter = 0;

        try {
            ResultSet rs = origin.getConn().createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)
                    .executeQuery("Select " + groupBySet.stream().map(DatasetAttribute::getName).collect(Collectors.joining(","))
                            + "," + measures.stream().map(da -> "sum(" + da.getName() + ")").collect(Collectors.joining(",")) +
                            " from " + origin.getTable() + " group by " +  groupBySet.stream().map(DatasetAttribute::getName).collect(Collectors.joining(",")) + ";");
            while (rs.next()) {
                for (int i = 0; i < groupBySet.size(); i++)
                    tmpDim.get(i).add(rs.getObject(1 + i));
                for (int i = groupBySet.size(); i < measures.size() + groupBySet.size(); i++) {
                    tmpMeas.get(i - groupBySet.size()).add(rs.getDouble(1 + i));
                }
                counter++;
            }
        } catch (SQLException e){
            System.err.println("[ERROR] Impossible to construct aggregate !\n" + e.getMessage());
        }

        dims = new Object[groupBySet.size()][counter];
        data = new double[measures.size()][counter];
        for (int i = 0; i < groupBySet.size(); i++)
            dims[i] = tmpDim.get(i).toArray();
        tmpDim.clear();
        for (int i = 0; i < measures.size(); i++)
            data[i] = tmpMeas.get(i).stream().mapToDouble(Double::doubleValue).toArray();
        tmpMeas.clear();

        this.len = counter;

    }

    public double[][] assessSum(DatasetMeasure m, DatasetDimension group, DatasetDimension selection, Object val1, Object val2){
        HashMap<Object, Double> resultA = new HashMap<>();
        HashMap<Object, Double> resultB = new HashMap<>();
        int selDimIdx = groupBySet.indexOf(selection);
        int grpDimIdx = groupBySet.indexOf(group);
        int mIdx = measures.indexOf(m);
        for (int i = 0; i < len; i++) {
            if (dims[selDimIdx][i].equals(val1)){
                Object g = dims[grpDimIdx][i];
                resultA.putIfAbsent(g, 0d);
                resultA.put(g, resultA.get(g) + data[mIdx][i]);
            }else if (dims[selDimIdx][i].equals(val2)){
                Object g = dims[grpDimIdx][i];
                resultB.putIfAbsent(g, 0d);
                resultB.put(g, resultB.get(g) + data[mIdx][i]);
            }
        }
        double[][] result = new double[2][Math.max(resultA.size(), resultB.size())];
        int pos = 0;
        for (Object key : resultA.size() > resultB.size() ? resultA.keySet() : resultB.keySet()){
            result[0][pos] = resultA.getOrDefault(key, 0d);
            result[1][pos] = resultB.getOrDefault(key, 0d);
            pos++;
        }
        return result;
    }
}
