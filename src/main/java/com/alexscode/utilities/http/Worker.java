package com.alexscode.utilities.http;


import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.zip.GZIPOutputStream;


class Worker implements Runnable {

    private static String _404 = "<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"UTF-8\"><title>Page Not Found</title></head><body><h1>Erreur 404</h1><h2>Elsa n'a pas trouvée la page demandée !</h2><img src=\"https://media.giphy.com/media/xSlfy3sa3ecEw/giphy.gif\"></body></html>";

    private Socket socket;
    private HttpReq requete;
    private Handler handler;

    Worker(Socket socket, Handler h) {
        this.socket = socket;
        requete = new HttpReq();
        handler = h;
        new Thread(this).start();
    }

    public void run() {
        try {
            OutputStream out = socket.getOutputStream();
            requete.doParse(socket.getInputStream());
            //System.out.printf("DEBUG '%s' FROM '%s'%n", requete.getHead(), socket.getRemoteSocketAddress().toString());

            HttpAns ans = handler.handle(requete);

            if (requete.supportsGzip()){
                ans.setCompressed();
                sendHeader(out, ans);
                GZIPOutputStream compressor = new GZIPOutputStream(out);
                ans.bodyFactory.apply(compressor);
            }else {
                sendHeader(out, ans);
                ans.bodyFactory.apply(out);
            }

            socket.close();
        } catch (IOException e) {
            System.out.println("--- SOCKET EXCEPTION ---");
            e.printStackTrace();
        }
    }

    private static void sendHeader(OutputStream out, HttpAns ans) throws IOException {
        ans.printTo(out);
        out.write(new byte[]{0x0d, 0x0a});
    }

}
