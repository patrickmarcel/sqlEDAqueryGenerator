package fr.univtours.info.columnStore;

public class StringEqPredicate extends Predicate {
    private final String matching;

    public StringEqPredicate(String column, String matching) {
        this.col = column;
        this.matching = matching;
    }

    @Override
    boolean[] getBinaryVector(Object in) {
        String[] input = (String[]) in;
        boolean[] vec = new boolean[input.length];
        for (int i = 0; i < input.length; i++) {
            vec[i] = input[i].equals(matching);
        }
        return vec;
    }
}
