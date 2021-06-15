package fr.univtours.info.dataset;

import fr.univtours.info.dataset.metadata.DatasetDimension;
import fr.univtours.info.dataset.metadata.DatasetMeasure;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@Data
@AllArgsConstructor
public class DBConfig {
    public static String CONF_FILE_PATH = "src/main/resources/enedis.properties";
    // 1 = Postgres
    public static int DIALECT = 1;

    List<DatasetDimension> dimensions;
    List<DatasetMeasure> measures;
    String table;
    String sampleURL, samplePassword, sampleUser, sampleDriver;
    Connection connection;
    String baseURL, baseUser, basePassword;

    public static DBConfig newFromFile() throws IOException, SQLException {
        final FileReader fr = new FileReader(CONF_FILE_PATH);
        final Properties props = new Properties();
        props.load(fr);

        String table = props.getProperty("datasource.table");
        String dimensions=props.getProperty("datasource.dimensions");
        String measures=props.getProperty("datasource.measures");
        List<DatasetDimension> theDimensions = new ArrayList<>();
        List<DatasetMeasure> theMeasures = new ArrayList<>();
        final String passwd = props.getProperty("datasource.password");
        final String user = props.getProperty("datasource.user");
        String url = props.getProperty("datasource.url") + "?user=" + user + "&password=" + passwd;;
        Connection conn = DriverManager.getConnection(url);

        String[] dim = dimensions.split(",");
        for (String dName : dim) {
            String formatted = dName;
            // encapsulate in quotes to avoid errors
            if (DIALECT == 1)
                formatted = "\"" + dName + "\"";
            DatasetDimension dd = new DatasetDimension(formatted, conn, table);
            theDimensions.add(dd);
            dd.computeActiveDomain();
            if (DIALECT == 1) {
                dd.setPrettyName(dName);
            }
        }

        for (String mea : measures.split(",")) {
            String formatted = mea;
            // encapsulate in quotes to avoid errors
            if (DIALECT == 1)
                formatted = "\"" + mea + "\"";
            DatasetMeasure dm = new DatasetMeasure(formatted, conn, table);
            if (DIALECT == 1){
                dm.setPrettyName(mea);
            }
            theMeasures.add(dm);
        }


        return new DBConfig(theDimensions, theMeasures, table,
                props.getProperty("sample.url"), props.getProperty("sample.password"), props.getProperty("sample.user"),
                props.getProperty("sample.driver"), conn, url, user, passwd);
    }
}
