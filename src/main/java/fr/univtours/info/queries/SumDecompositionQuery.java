package fr.univtours.info.queries;

/*
SELECT dim1, year, sum_cases from (
(select continentexp as dim1, year, sum(cases) as sum_cases, 1 as pos
from covid
where continentexp = 'Europe'
group by continentexp, year)
union
(select countriesandterritories as dim1, year, sum(cases) as sum_cases, 2 as pos
from covid
where continentexp = 'Europe'
group by countriesandterritories, year
order by sum(cases) desc)) X
order by pos, sum_cases desc;
*/

import fr.univtours.info.dataset.metadata.DatasetDimension;
import fr.univtours.info.dataset.metadata.DatasetMeasure;
import lombok.Getter;

import java.sql.Connection;
import java.util.HashSet;

public class SumDecompositionQuery extends AbstractEDAsqlQuery{

    @Getter
    String val1;

    SumDecompositionQuery(Connection conn, String table, DatasetDimension assessed, String val1, DatasetDimension reference, DatasetMeasure m){
        this.conn=conn;
        this.table=table;

        this.assessed=assessed;
        this.reference=reference;

        this.dimensions =new HashSet<>();
        this.dimensions.add(assessed);
        this.dimensions.add(reference);

        this.measure=m;
        this.function="sum";

        this.val1 = val1;
    }

    @Override
    protected String getSqlInt() {
        return "SELECT dim1, year, sum_cases from (\n" +
                "(select continentexp as dim1, year, sum(cases) as sum_cases, 1 as pos\n" +
                "from covid\n" +
                "where continentexp = '"+val1+"'\n" +
                "group by continentexp, year)\n" +
                "union\n" +
                "(select countriesandterritories as dim1, year, sum(cases) as sum_cases, 2 as pos\n" +
                "from covid\n" +
                "where continentexp = '\"+val1+\"'\n" +
                "group by countriesandterritories, year\n" +
                "order by sum(cases) desc)) X\n" +
                "order by pos, sum_cases desc;";
    }

    @Override
    public float getDistance(AbstractEDAsqlQuery other) {
        return 0;
    }

    @Override
    public void interestFromResult() throws Exception {

    }
}
