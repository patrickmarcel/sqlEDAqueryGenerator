package fr.univtours.info;

import fr.univtours.info.metadata.DatasetDimension;
import fr.univtours.info.metadata.DatasetMeasure;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@Data
@AllArgsConstructor
public class Config {
    public static String CONF_FILE_PATH = "src/main/resources/application.properties";

    List<DatasetDimension> dimensions;
    List<DatasetMeasure> measures;
    String table;
    String sampleURL, samplePassword, sampleUser, sampleDriver;

    public static Config readProperties() throws IOException, SQLException {
        final FileReader fr = new FileReader(new File(CONF_FILE_PATH));
        final Properties props = new Properties();
        props.load(fr);
        String table = props.getProperty("datasource.table");
        String dimensions=props.getProperty("datasource.dimensions");
        String measures=props.getProperty("datasource.measures");
        List<DatasetDimension> theDimensions = new ArrayList<>();
        List<DatasetMeasure> theMeasures = new ArrayList<>();
        String[] dim=dimensions.split(",");
        DBservices dBservices = new DBservices();
        Connection conn = dBservices.connectToPostgresql();
        for (int x=0; x<dim.length; x++) {
            theDimensions.add(new DatasetDimension(dim[x], conn, table));
            theDimensions.get(x).computeActiveDomain();
        }
        String[] meas=measures.split(",");
        for (int x=0; x<meas.length; x++) {
            theMeasures.add(new DatasetMeasure(meas[x], conn, table));
        }
        conn.close();

        return new Config(theDimensions, theMeasures, table, props.getProperty("sample.url"), props.getProperty("sample.password"), props.getProperty("sample.user"), props.getProperty("sample.driver"));
    }
}
