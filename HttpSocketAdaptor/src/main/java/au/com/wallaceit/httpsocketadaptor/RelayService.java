package au.com.wallaceit.httpsocketadaptor;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.HashMap;
import java.util.Map;
import fi.iki.elonen.NanoHTTPD;

public class RelayService extends Service {
    private static int notifyId = 1;
    private NotificationManager mNotificationManager;
    private Server htserver;
    private int sourceport;
    private String desthost;
    private int destport;

    public RelayService() {

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Bundle bundle = intent.getExtras();
        sourceport = Integer.parseInt(bundle.getString("sourceport"));
        desthost = bundle.getString("desthost");
        destport = Integer.parseInt(bundle.getString("destport"));
        if (startRelay()) {
            createNotification(null);
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy(){
        stopRelay();
        removeNotification();
    }

    private boolean started = false;
    private boolean even = true;
    private void createNotification(String tickertxt){
        //removeNotification();
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(android.R.drawable.ic_media_play)
                        .setContentTitle("Sever Running")
                        .setContentText("The Socket relay is running!");
        PendingIntent pe = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(pe);
        if (tickertxt!=null){
            mBuilder.setTicker(tickertxt+(even?"":" ")); // a hack to make ticker show each request even though text has not changed
            even = !even;
        }
        mBuilder.setOngoing(true);
        if (started) {
            mNotificationManager.notify(notifyId, mBuilder.build());
        } else {
            startForeground(notifyId, mBuilder.build());
            started = true;
        }
    }

    private void removeNotification(){
        mNotificationManager.cancel(notifyId);
    }

    private boolean startRelay(){
        htserver = new Server();
        try {
            htserver.start();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void stopRelay(){
        htserver.stop();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private class Server extends NanoHTTPD {

        public final String PRINT_IP = desthost;
        public final int PRINT_PORT = destport;

        public Server(){
            super("0.0.0.0", sourceport);
            System.out.println("Relay Started on port "+sourceport+"; Destination: "+desthost+":"+destport);
        }

        @Override
        public Response serve(IHTTPSession session) {
            Map<String, String> files = new HashMap<String, String>();
            Method method = session.getMethod();
            if (Method.PUT.equals(method) || Method.POST.equals(method)) {
                try {
                    session.parseBody(files);
                } catch (IOException ioe) {
                    return new Response(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "SERVER INTERNAL ERROR: IOException: " + ioe.getMessage());
                } catch (ResponseException re) {
                    return new Response(re.getStatus(), MIME_PLAINTEXT, re.getMessage());
                }
            }
            // get the POST body
            String postBody = session.getQueryParameterString();
            try {
                postBody = URLDecoder.decode(postBody, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            System.out.println("Received Data: " + postBody);
            // foward to the socket
            if (Method.PUT.equals(method) || Method.POST.equals(method)) {
                if (sendSocket(postBody)){
                    createNotification("Print Job Submitted");
                } else {
                    createNotification("Print Job Error!");
                }
            }
            //return super.serve(session);
            Response response = new Response("1");
            response.addHeader("Access-Control-Allow-Origin", "*");
            response.addHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, DELETE");
            response.addHeader("Access-Control-Max-Age", "3600");
            response.addHeader("Access-Control-Allow-Headers", "x-requested-with");
            return response;
        }

        private boolean sendSocket(String data){
            Socket sock;
            try {
                sock = new Socket(PRINT_IP, PRINT_PORT);
                DataOutputStream dataOutputStream = new DataOutputStream(sock.getOutputStream());
                dataOutputStream.writeBytes(data);
                dataOutputStream.flush();
                try {
                    sock.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.out.println("Forwarded data");
                return true;
            } catch (IOException e) {
                System.out.println("Forward failed");
                e.printStackTrace();
                return false;
            }
        }
    }

}
