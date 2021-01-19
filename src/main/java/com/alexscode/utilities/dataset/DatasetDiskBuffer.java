package com.alexscode.utilities.dataset;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

public class DatasetDiskBuffer<E> implements Dataset<E> {

    private File cache;
    private Function<String, E> from_disk_repr;

    public static <E> Optional<DatasetDiskBuffer<E>> createDatasetDiskBuffer(Dataset<E> dataset, Function<E, String> to_disk_repr, Function<String, E> from_disk_repr){
        try {
            return Optional.of(new DatasetDiskBuffer<>(dataset, to_disk_repr, from_disk_repr));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    private DatasetDiskBuffer(Dataset<E> dataset, Function<E, String> to_disk_repr, Function<String, E> from_disk_repr) throws IOException {
        cache = File.createTempFile("dataset_buffer_", ".tmp");
        cache.deleteOnExit();
        Files.write(cache.toPath(), (Iterable<String>) dataset.stream().map(to_disk_repr)::iterator);
        this.from_disk_repr = from_disk_repr;
    }

    @Override
    public long size(){
        try {
            return Files.lines(cache.toPath()).count();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return -1;
    }

    @Override
    public Stream<E> stream(){
        try {
            return Files.lines(cache.toPath()).map(from_disk_repr);
        } catch (IOException e) {
            return null;
        }

    }

    @Override
    public void close() {
        cache.delete(); // TODO: better handling?
    }
}
