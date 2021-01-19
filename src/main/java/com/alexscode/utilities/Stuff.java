package com.alexscode.utilities;

import java.io.*;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.GZIPOutputStream;

public class Stuff {
    public static void exportString(String path, String data){
        try {
            Files.write(Paths.get(path), data.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String dlTempFile(String url, String name){
        String tmpDir = System.getProperty("java.io.tmpdir");
        String savePath = tmpDir + File.separator + "tmp_" + name;
        try {
            ReadableByteChannel rbc = Channels.newChannel((new URL(url)).openStream());
            FileOutputStream fos = new FileOutputStream(savePath);
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return savePath;
    }

    public static String dlTempFile(String url){
        return dlTempFile(url, md5(url));
    }

    public static int runPython(String script, String args){
        Runtime runtime = Runtime.getRuntime();
        try {
            Process p = runtime.exec("python " + script + " " + args);

            //Error Redirection
            InputStream err = p.getErrorStream();
            byte[] b = new byte[4096];
            int l;
            while ((l = err.read(b)) != 0) {
                if (l == -1) break;
                System.err.write(b, 0, l);
            }

            p.waitFor();
            return p.exitValue();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return 1;
    }

    public static int runPythonFromURL(String url, String args){
        String script = dlTempFile(url, md5(url) + ".py");
        return runPython(script, args);
    }

    /**
     * Compresses a string with gzip
     * @param data the string to be compressed (in system default encoding)
     * @return a byte array containing the compressed data
     * @throws IOException
     */
    public static byte[] compress(String data) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(data.length());
        GZIPOutputStream gzip = new GZIPOutputStream(bos);
        gzip.write(data.getBytes());
        gzip.close();
        byte[] compressed = bos.toByteArray();
        bos.close();
        return compressed;
    }

    public static byte[] compress(File f) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        FileInputStream fis = new FileInputStream(f);
        GZIPOutputStream out = new GZIPOutputStream(bos);
        Future.transferTo(fis, out);
        out.close();
        fis.close();
        byte[] compressed = bos.toByteArray();
        bos.close();
        return compressed;
    }

    public static String md5(String input){
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return bytesToHex(md.digest(input.getBytes()));
    }

    public static String sha256(String input){
        String hash = "";

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] h = digest.digest(input.getBytes());
            hash = bytesToHex(h);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return hash;
    }

    public static String bytesToHex(byte[] bytes) {
        final char[] hexArray = "0123456789ABCDEF".toCharArray();
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
}
