package fr.univtours.info.queries;

import fr.univtours.info.dataset.DBConfig;
import fr.univtours.info.dataset.Dataset;
import fr.univtours.info.optimize.tsp.Measurable;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.dbutils.ResultSetIterator;

import java.sql.*;

public abstract class Query implements Measurable<Query> {

    Dataset source;

    private String sqlText;

    /**
     * Real execution cost (milliseconds)
     */
    protected long actualCost = -1;

    /**
     * Estimated execution cost (DBMS dependant)
     */
    protected long explainCost = -1;

    @Getter
    @Setter
    double interest = 0;

    public String getSql(){
        if (sqlText == null)
            sqlText = this.getSqlInt();
        return sqlText;
    }

    public void printSql(){
        System.out.println(this.getSql());
    }

    public long estimatedTime() {
        if (explainCost == -1)
            explain();
        return explainCost;
    }

    public long actualTime() {
        if (actualCost == -1){
            System.err.println("[WARN] Run query before getting real run time !");
        }
        return actualCost;
    }

    /**
     * This must return the SQL text for your query
     * @return a sql string ready to be executed
     */
    abstract protected String getSqlInt();

    /**
     * Needs to update explainCost as a side effect
     */
    public void explain(){
        explainInt(source.getConn());
    }

    protected void explainInt(Connection conn) {
        try (Statement pstmt = conn.createStatement()) {
            // For classic dbms
            if (DBConfig.DIALECT != 2) {
                ResultSet rs = pstmt.executeQuery("explain  " + this.getSql());

                rs.next();

                String s1 = rs.getString("QUERY PLAN");
                String[] s2 = s1.split("=");
                String[] s3 = s2[1].split("\\.\\.");
                this.explainCost = (long) Float.parseFloat(s3[0]);
                rs.close();
            }
            // For MonetDB
            else {
                String line = "";
                ResultSet rs = pstmt.executeQuery("explain  " + this.getSql());
                while (rs.next()){
                    line = rs.getString(1);
                    if (line.contains("#total") && line.contains("time=")){
                        this.explainCost = 1 + Long.parseLong(line.split("time=")[1].replace("usec", "").stripTrailing())/1000;
                    }
                }
            }
        } catch (SQLException e){
            System.err.println("[ERROR] Failed to fetch query plan for " + this);
        }

    }


    public void explainAnalyze() throws Exception{
        final Statement pstmt = source.getConn().createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
                ResultSet.CONCUR_UPDATABLE);
        ResultSet rs = pstmt.executeQuery("explain analyze " + this.getSql()) ;

        ResultSetMetaData rmsd = rs.getMetaData();
        rs.beforeFirst();
        ResultSetIterator rsit=new ResultSetIterator(rs);
        Object[] tab=null;
        while(rsit.hasNext()) { // move to last for getting execution time
            tab=rsit.next();
        }
        String last= tab[0].toString();
        String tmp1=last.split("Execution Time: ")[1];
        String[] tmp2=tmp1.split(" ms");
        this.actualCost = (long) Float.parseFloat(tmp2[0]);

        pstmt.close();
        rs.close();
    }


    //TODO add caching ?
    public ResultSet execute() {
        final Statement pstmt;
        try {
            pstmt = source.getConn().createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            ResultSet rs = pstmt.executeQuery(this.getSql()) ;
            rs.next();
            return rs;
        } catch (SQLException throwables) {
            throwables.printStackTrace();
            return null;
        }
    }


    public void printResult() throws SQLException {
        ResultSet resultset = execute();

        System.out.println("--- Result Set ---");
        resultset.beforeFirst();
        ResultSetMetaData rsmd = resultset.getMetaData();
        int columnsNumber = rsmd.getColumnCount();
        boolean first = true;
        while (resultset.next()) {
            if (first){
                for (int i = 1; i <= columnsNumber; i++) {
                    if (i > 1) System.out.print(",  ");
                    System.out.print(rsmd.getColumnName(i));
                }
                System.out.println();
                first = false;
            }
            for (int i = 1; i <= columnsNumber; i++) {
                if (i > 1) System.out.print(",  ");
                String columnValue = resultset.getString(i);
                System.out.print(columnValue);
            }
            System.out.println();
        }
        System.out.println("--- Result Set END ---");
        resultset.close();

    }
}
