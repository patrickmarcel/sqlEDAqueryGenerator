package com.alexscode.utilities.http;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.function.Function;

/**
 * @author Alexandre Chanson
 * This class is used to build the HTTP Answer Header
 */
public class HttpAns {
    public static final String
            _400 = "HTTP/1.1 400 Bad Request",
            _404 = "HTTP/1.1 404 NOT FOUND",
            _403 = "HTTP/1.1 403 Forbidden",
            _200 = "HTTP/1.1 200 OK",
            _500 = "HTTP/1.1 500 Internal Server Error",
            _html = "text/html",
            _jpeg = "image/jpeg",
            _js = "application/javascript",
            _mp4 = "application/mp4",
            _png = "image/png",
            _json = "application/json",
            _css = "text/css";

    private ArrayList<String> header = new ArrayList<>();
    private int len = -1;
    Function<OutputStream, Boolean> bodyFactory;

    public HttpAns() {
        header.add(_200);
        header.add("Date: " + new Date());
        header.add("Server: Elsa/1.1");
        header.add("Content-type: text/html");
        header.add("Access-Control-Allow-Origin: *");
        header.add("Connection: close");
    }


    String build(){
        StringBuilder r = new StringBuilder();
        header.set(1, "Date: " + new Date());
        for (String s : header) {
            r.append(s).append('\n');
        }
        r.append("Content-length: ").append(len).append('\n');
        return r.toString();
    }

    public HttpAns setCode(String code){
        header.set(0, code);
        return this;
    }

    public HttpAns setLen(int n){
        len = n;
        return this;
    }

    public HttpAns setType(String type){
        header.set(3, "Content-type: " + type);
        return this;
    }

    public HttpAns setCookie(Cookie c){
        header.add("Set-Cookie: "+c.name+"="+c.value);
        return this;
    }

    HttpAns setCompressed(){
        header.add("Content-Encoding: gzip");
        return this;
    }

    public HttpAns setBody(Function<OutputStream, Boolean> bodyMaker){
        bodyFactory = bodyMaker;
        return this;
    }

    /**
     * Builds the Answer and then prints it to an output stream
     * @param out A valid output stream (i.e. thee socket's out)
     * @throws IOException
     */
    void printTo(OutputStream out) throws IOException {
        out.write(this.build().getBytes());
    }
}
