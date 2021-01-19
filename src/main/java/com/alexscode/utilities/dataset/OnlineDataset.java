package com.alexscode.utilities.dataset;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class OnlineDataset implements Dataset<String> {

    URL url;

    InputStream stre;

    public OnlineDataset(URL url) {
        this.url = url;
    }


    @Override
    public Iterator<String> iterator(){
        try {
            stre = url.openStream();
            return new Iterator<String>() {
                String buf;
                byte[] b = new byte[1];

                @Override
                public boolean hasNext() {
                    StringBuilder sb = new StringBuilder();
                    try {
                        boolean keepgoing = false;
                        while (stre.read(b) > 0){
                            char c = (char) b[0];
                            if (c == '\n'){
                                buf = sb.toString();
                                keepgoing = true;
                                break;
                            }
                            else {
                                sb.append(c);
                            }
                        }
                        return keepgoing;
                    }
                    catch (IOException e){

                    }


                        return false;
                }

                @Override
                public String next() {
                    return buf;
                }
            };
        } catch (IOException e) {
            return new Iterator<String>() {
                @Override
                public boolean hasNext() {
                    return false;
                }

                @Override
                public String next() {
                    return null;
                }
            };
        }
    }

    @Override
    public Stream<String> stream(){
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(this.iterator(), Spliterator.ORDERED), false);
    }


    @Override
    public void close() throws Exception {
        if (stre != null){
            stre.close();
        }
    }
}
