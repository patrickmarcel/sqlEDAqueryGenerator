package com.alexscode.utilities.dataset;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;

public class DatasetFromFile implements Dataset<String> {

    Path path;

    Stream<String> stream;

    private DatasetFromFile(Path path) {
        this.path = path;
    }

    public static Optional<DatasetFromFile> getDatasetFromFile(Path path){
        if (Files.exists(path) && Files.isReadable(path)){
            return Optional.of(new DatasetFromFile(path));
        }
        else {
            return Optional.empty();
        }
    }


    @Override
    public Stream<String> stream(){

        try {
            stream = Files.lines(path);
            return stream;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void close() {
        stream.close();
    }
}
