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
    public static String CONF_FILE_PATH = "src/main/resources/vaccines.properties";

    List<DatasetDimension> dimensions;
    List<DatasetMeasure> measures;
    String table;
    String sampleURL, samplePassword, sampleUser, sampleDriver;
    Connection connection;
    String baseURL, baseUser, basePassword;

    public static DBConfig readProperties() throws IOException, SQLException {
        final FileReader fr = new FileReader(new File(CONF_FILE_PATH));
        final Properties props = new Properties();
        props.load(fr);
        String table = props.getProperty("datasource.table");
        String dimensions=props.getProperty("datasource.dimensions");
        String measures=props.getProperty("datasource.measures");
        List<DatasetDimension> theDimensions = new ArrayList<>();
        List<DatasetMeasure> theMeasures = new ArrayList<>();
        String[] dim=dimensions.split(",");
        final String passwd = props.getProperty("datasource.password");
        final String user = props.getProperty("datasource.user");
        String url = props.getProperty("datasource.url") + "?user=" + user + "&password=" + passwd;;
        Connection conn = DriverManager.getConnection(url);
        for (int x=0; x<dim.length; x++) {
            theDimensions.add(new DatasetDimension(dim[x], conn, table));
            theDimensions.get(x).computeActiveDomain();
        }
        String[] meas=measures.split(",");
        for (int x=0; x<meas.length; x++) {
            theMeasures.add(new DatasetMeasure(meas[x], conn, table));
        }


        return new DBConfig(theDimensions, theMeasures, table,
                props.getProperty("sample.url"), props.getProperty("sample.password"), props.getProperty("sample.user"),
                props.getProperty("sample.driver"), conn, url, user, passwd);
    }
}
