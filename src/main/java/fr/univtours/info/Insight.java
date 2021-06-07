package fr.univtours.info;

import fr.univtours.info.dataset.metadata.DatasetDimension;
import fr.univtours.info.dataset.metadata.DatasetMeasure;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;


public class Insight {
    public static final int MEAN_SMALLER = 1, MEAN_EQUALS = 2, MEAN_GREATER = 3;
    public static final String[] pprint = new String[]{"", "Mean Smaller", "Mean Equals", "Mean Greater"};

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
                ", type=" + pprint[type] +
                ", p=" + p +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Insight)) return false;
        Insight insight = (Insight) o;
        return getType() == insight.getType() && getDim().equals(insight.getDim()) && getSelA().equals(insight.getSelA()) && getSelB().equals(insight.getSelB()) && getMeasure().equals(insight.getMeasure());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getDim(), getSelA(), getSelB(), getMeasure(), getType());
    }
}
