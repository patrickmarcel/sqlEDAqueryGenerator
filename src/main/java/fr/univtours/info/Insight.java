package fr.univtours.info;

import fr.univtours.info.dataset.metadata.DatasetDimension;
import fr.univtours.info.dataset.metadata.DatasetMeasure;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;


public class Insight {
    public static final int MEAN_SMALLER = 1;

    public Insight(DatasetDimension dim, String selA, String selB, DatasetMeasure measure, int type) {
        this.dim = dim;
        this.selA = selA;
        this.selB = selB;
        this.measure = measure;
        this.type = type;
    }

    @Getter
    DatasetDimension dim;
    @Getter
    String selA, selB;
    @Getter
    DatasetMeasure measure;
    @Getter
    int type;

    @Getter @Setter
    double p = Double.NaN;

    @Override
    public String toString() {
        return "Insight {" +
                "dim=" + dim.getName() +
                ", selA='" + selA + '\'' +
                ", selB='" + selB + '\'' +
                ", measure=" + measure.getName() +
                ", type=" + type +
                ", p=" + p +
                '}';
    }
}
