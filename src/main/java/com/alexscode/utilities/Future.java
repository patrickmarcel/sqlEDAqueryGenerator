package com.alexscode.utilities;



import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author Alexandre Chanson
 * @version 1.2
 * Reimplementation of useful static methods from next versions of java
 */

public final class Future {

    /**
     * Uses a buffer to channel any InputStream into any OutputStream
     *
     * @param in  The source stream aka data comes from here
     * @param out The destination stream aka data goes there
     * @return The number of bytes transferred before end of stream was encountered
     * @throws IOException in case of read or write errors on the streams
     */
    public static int transferTo(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[4096];
        int read = 0, count = 0;
        while ((read = in.read(buffer, 0, 4096)) >= 0) {
            out.write(buffer, 0, read);
            count += read;
        }
        return count;
    }

    /**
     * Utility method to join a list of strings
     * @param strings the strings to join like a list of names
     * @param separator a separator (will not be added at the end) eg ", "
     * @return eg: JP, Alex, PAUL, Pierre
     */
    public static String join(List<String> strings, String separator){
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < strings.size(); i++) {
            sb.append(strings.get(i));
            if (i != strings.size() - 1) {
                sb.append(separator);
            }
        }
        return sb.toString();
    }

    public static String arrayToString(boolean[] a, String sep){
        if (a == null)
            return "null";

        StringBuilder b = new StringBuilder();
        for (int i = 0; i<a.length; i++) {
            b.append(a[i] ? 1 : 0);
            if (i == a.length - 1)
                return b.toString();
            b.append(sep);
        }

        return "";
    }

    public static String arrayToString(int[] a, String sep){
        if (a == null)
            return "null";

        StringBuilder b = new StringBuilder();
        for (int i = 0; i<a.length; i++) {
            b.append(a[i]);
            if (i == a.length - 1)
                return b.toString();
            b.append(sep);
        }

        return "";
    }

    public static String arrayToString(double[] a, String sep){
        if (a == null)
            return "null";

        StringBuilder b = new StringBuilder();
        for (int i = 0; i<a.length; i++) {
            b.append(a[i]);
            if (i == a.length - 1)
                return b.toString();
            b.append(sep);
        }

        return "";
    }

    static final String accentedCharacters = "àèìòùÀÈÌÒÙáéíóúýÁÉÍÓÚÝâêîôûÂÊÎÔÛãñõÃÑÕäëïöüÿÄËÏÖÜŸçÇßØøÅåÆæœ";
    private static final Pattern cutMeas = Pattern.compile("[\\w \\-"+accentedCharacters+"]*");
    static String[] split = new String[]{"], \\[", "]\\.\\[", ", \\[", "\\(\\[", "\\{\\["};
    public static String joinMDX(List<String> strings){
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < strings.size(); i++) {
            String line = strings.get(i);
            sb.append(line);
            if (i != strings.size() - 1) {
                boolean doSpace = false;
                for (String sp : split){
                    var test = line.split(sp);
                    var m = cutMeas.matcher(test[test.length-1]);
                    if (m.matches()) {
                        doSpace = true;
                        break;
                    }
                }
                if (doSpace)
                    sb.append(" ");
                else
                    sb.append("\n");
            }
        }
        return sb.toString();
    }
}
