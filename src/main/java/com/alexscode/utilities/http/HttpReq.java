package com.alexscode.utilities.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class HttpReq {
    private Map<String, String> header;
    private Map<String, String> urlParams;
    private String head = "ERROR";
    private String body;

    public HttpReq() {
        header = new HashMap<>();
        urlParams = new HashMap<>();
        body = "";
    }

    public void doParse(InputStream input){
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(input));
            String line = br.readLine();
            if (line == null)
                return;
            head = line;

            // Decoding parameters from url
            int n = getPath().indexOf('?');
            if (n != -1){
                String params = getPath().substring(n+1);
                head = head.replace("?" + params, "");
                String[] temp = params.split("&");
                for (String s : temp) {
                    String[] p = s.split("=");
                    urlParams.put(p[0], p[1]);
                }
            }


            // Reading the header
            while (!line.equals("")) { // The empty line "" is the delimiter between header and body
                line = br.readLine();
                if (line.length() > 0 && line.indexOf(':') != -1){ // Is line format ok
                    String[] splited = split(line);                // Custom split to remove : & space ignore all : but the first
                    this.header.put(splited[0].toLowerCase(), splited[1]);       // Puts the result in the map
                }

            }
            // Reading the body if any
            boolean hasBody = false;
            int len = -1;
            if (header.get("content-length") != null){
                len = Integer.parseInt(header.get("content-length").replace(" ", ""));
                hasBody = true;
            }
            if (hasBody){
                char[] buffer = new char[len];
                br.read(buffer);
                body = new String(buffer);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String[] split(String line) {
        int sep = line.indexOf(':');
        return new String[] {line.substring(0, sep), line.substring(sep+1)};
    }

    boolean isPost(){
        return head != null && head.toLowerCase().contains("post");
    }

    boolean isGet(){
        return head != null && head.toLowerCase().contains("get");
    }

    boolean isPut(){
        return head != null && head.toLowerCase().contains("put");
    }

    boolean isOptions(){
        return head != null && head.toLowerCase().contains("options");
    }

    boolean isDelete(){
        return head != null && head.toLowerCase().contains("delete");
    }

    boolean isFirefox(){
        return header.containsKey("user-agent") && header.get("user-agent").toLowerCase().contains("firefox");
    }

    String getPath(){
        try {
            return head.split(" ")[1];
        }catch (NullPointerException | ArrayIndexOutOfBoundsException e){
            return "/";
        }

    }

    String getHeader(String key){
        return header.get(key);
    }
    String getVersion(){
        return head.split(" ")[2];
    }

    Map<String,String> getParameters(){
        return urlParams;
    }

    String getParameter(String key){
        return urlParams.get(key);
    }

    @Override
    public String toString() {
        String ret = "Requette HTTP :";
        ret += head + '\n';
        ret += header.toString() + '\n';
        ret += body;
        return ret;
    }

    public boolean supportsGzip() {
        String s = header.get("accept-encoding");
        return s != null && s.toLowerCase().contains("gzip");
    }

    public void print(){
        System.out.println(" ### ### Debug ### ###");
        System.out.println(this.toString());
        System.out.println(" ### ### ### ### ###");
    }

    public String getBody() {
        return body;
    }

    public String getHead() {
        return head;
    }
}
