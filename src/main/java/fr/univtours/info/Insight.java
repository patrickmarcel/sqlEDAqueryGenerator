package fr.univtours.info;

import fr.univtours.info.dataset.metadata.DatasetDimension;
import fr.univtours.info.dataset.metadata.DatasetMeasure;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;


public class Insight {
    public static final int RAW = 0, MEAN_SMALLER = 1, MEAN_EQUALS = 2, MEAN_GREATER = 3, VARIANCE_SMALLER = 4, VARIANCE_EQUALS = 5, VARIANCE_GREATER = 6;
    public static final String[] pprint = new String[]{"Raw insight (to be expanded)", "Mean Smaller", "Mean Equals", "Mean Greater", "Variance Smaller", "Variance Equals", "Variance Greater"};

    public Insight(DatasetDimension dim, String selA, String selB, DatasetMeasure measure) {
        this.dim = dim;
        this.selA = selA;
        this.selB = selB;
        this.measure = measure;
        this.type = 0;
    }

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

    @Getter @Setter
    double credibility;

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

    /**
     * Internal use only ! (statistical verifier can use this to turn raw insights to a specific type to avoid object copy)
     * @param type the type of the insight (except RAW)
     */
    public void setType(int type) {
        if (type == 0)
            throw new IllegalArgumentException();
        this.type = type;
    }
}
