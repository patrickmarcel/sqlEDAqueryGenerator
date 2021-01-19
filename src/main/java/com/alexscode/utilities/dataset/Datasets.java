package com.alexscode.utilities.dataset;


import java.io.IOException;
import java.io.Writer;
import java.util.function.Function;

public final class Datasets {

    private Datasets(){} // private constructor so one doesn't try to instantiate a Datatasets object



    /**
     * Convert a dataset of strings following the CSV format to a Dataset of E using the given function.
     * Filter out the commented lines (the line starting with the caracter '#') as well as the empty lines.
     * Allow to skip a certain amount of headers line at the beginning of the file, counted AFTER filtering.
     * @param dataset A Dataset of string following the CSV convention or similar
     * @param converter Deserialisation function of a CSV line to an object of type E
     * @param nbOfHeaderLines Lines of headers to skip at the beginning of the dataset, skipped after filtering of comments
     *                        Assumption: nbOfHeaderLines >= 0
     * @param <E> Type of a row in the dataset
     * @return A dataset of E
     */
    public static <E> Dataset<E> csvDataset(Dataset<String> dataset, Function<String, E> converter, int nbOfHeaderLines){
        return new DatasetFromStream<>(dataset.stream().filter(s -> !(s.startsWith("#") || s.equals(""))).skip(nbOfHeaderLines).map(converter));
    }



    /**
     * Write a Dataset of Strings to a Writer object (ex: to a file)
     * If you have a Dataset of E, use a Function<E,String> in the .map() function of the dataset to convert it beforehand.
     * @param out
     * @param dataset
     * @throws IOException
     */
    public static void writeDataset(Writer out, Dataset<String> dataset) throws IOException {
        for (String line : dataset){
            out.write(line);
        }
    }



}
