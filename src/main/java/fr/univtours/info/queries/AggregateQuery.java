package fr.univtours.info.queries;

import fr.univtours.info.metadata.DatasetDimension;
import fr.univtours.info.metadata.DatasetMeasure;
import org.apache.commons.dbutils.ResultSetIterator;
import org.apache.commons.math3.stat.StatUtils;

import java.sql.Connection;
import java.sql.ResultSetMetaData;
import java.util.Set;

public class AggregateQuery extends AbstractEDAsqlQuery{




    public AggregateQuery(Connection conn, String table, Set<DatasetDimension> dimensions, DatasetMeasure m, String agg){
        this.conn=conn;
        this.table=table;

        this.dimensions =dimensions;
        this.measure=m;
        this.function=agg;



        // System.out.println(sql);
        //this.explain = "explain  " + sql;
        //this.explainAnalyze = "explain analyze " + sql;

    }


    @Override
    public String getSqlInt() {
        String sql= "select ";
        String groupby="";
        for (DatasetDimension d :dimensions){
            groupby = groupby + d.getName() + ", ";
        }
        if(groupby.length()==0){ // empty set<dim>
            sql=sql + " " + this.function + "(" + this.measure.getName() + ") as " + this.measure.getName() +" from " + table + ";";
        }
        else{
            //groupby=groupby.substring(0,groupby.length()-1);
            //System.out.println(groupby);
            sql=sql + groupby + " " + this.function+ "(" + this.measure.getName() + ") as " + this.measure.getName() + " from " + table +" group by  ";
            groupby=groupby.substring(0,groupby.length()-2);
            //System.out.println(groupby);
            sql=sql + groupby + ";";
            //+ "avg(" + m + ") from " + table +" group by" + ";";
        }
        return sql;
    }

    public void interestFromResult() throws Exception {

        this.execute();

        ResultSetMetaData rmsd = resultset.getMetaData();
        //resultset.beforeFirst();
        ResultSetIterator rsit=new ResultSetIterator(resultset);
        Object[] tab=null;

        int colNumber=0;
        int nbColumn=rmsd.getColumnCount();
        for(int i=1;i<=nbColumn;i++){
            //System.out.println(i + " " + rmsd.getColumnName(i) + " " + this.measure.getName());
            if(rmsd.getColumnName(i).compareTo(this.measure.getName())==0){

                colNumber=i;
            }
        }


        resultset.last();    // moves cursor to the last row
        int size = resultset.getRow(); // get row id
        resultset.beforeFirst();


        double sf=0;
        double[] d=new double[size]; //resultset.size

        int i=0;
        resultset.next();
        while(!resultset.isAfterLast()){
            //System.out.println(sql);
            //System.out.println(colNumber);
            d[i]=resultset.getDouble(colNumber);
            i++;
            resultset.next();
        }

        double variance = StatUtils.variance(d);
        double sd = Math.sqrt(variance);
        double mean = StatUtils.mean(d);
        //NormalDistributionImpl nd = new NormalDistributionImpl();

        double stdscore=0, acc=0, nb=0;

        for (double value : d ) {

                stdscore = (value-mean)/sd;
                //sf = 1.0 - nd.cumulativeProbability(Math.abs(stdscore));
                acc = acc + stdscore;

                nb++;
           }


        this.interest= acc / nb;



    }
}
