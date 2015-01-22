package au.com.wallaceit.httpsocketadaptor;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.URLDecoder;
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
        if (bundle!=null) {
            sourceport = Integer.parseInt(bundle.getString("sourceport"));
            desthost = bundle.getString("desthost");
            destport = Integer.parseInt(bundle.getString("destport"));
        }
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
        Notification.Builder mBuilder =
                new Notification.Builder(this)
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
        htserver = new Server(RelayService.this);
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
        private Service parent;

        public Server(Service service){
            super("127.0.0.1", sourceport);
            parent = service;
            System.out.println("Relay Started on port "+sourceport+"; Destination: "+desthost+":"+destport);
        }

        @Override
        public void stop(){
            super.stop();
            parent.stopSelf();
        }

        @Override
        public Response serve(IHTTPSession session) {
            Map<String, String> files = new HashMap<String, String>();
            Method method = session.getMethod();
            String responseBody = "1";
            if (Method.PUT.equals(method) || Method.POST.equals(method)) {
                try {
                    session.parseBody(files);
                } catch (IOException ioe) {
                    return new Response(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "SERVER INTERNAL ERROR: IOException: " + ioe.getMessage());
                } catch (ResponseException re) {
                    return new Response(re.getStatus(), MIME_PLAINTEXT, re.getMessage());
                }
                // get the POST body
                String postBody = session.getQueryParameterString();
                try {
                    postBody = URLDecoder.decode(postBody, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                // send to socket
                if (sendSocket(postBody)){
                    createNotification("Print Job Submitted");
                } else {
                    createNotification("Print Job Error!");
                }
            }
            if (Method.GET.equals(method)) {
                if (session.getUri().equals("/printwindow")){
                    responseBody = "<html><head><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"/><style>h1, h2 { color:#0078ae; font-family:helvetica; font-size:110%; }</style><script>window.addEventListener('message',sendData); function sendData(event){ if (event.data!='check') { var xmlhttp=new XMLHttpRequest(); xmlhttp.open('POST','http://127.0.0.1:8080/',false); xmlhttp.send(event.data); } else { event.source.postMessage('true', '*'); }  }</script></head>";
                    responseBody+= "<body style='text-align:center;'><h1 style='margin-top:50px;'>Connected to the Print Service</h1><h2>You can minimize this window, but leave it open for faster printing</h2><img style=\"margin-top:20px; width:50px;\" id=\"wscan-loader\" src=\"data:image/gif;base64,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\"/></body></html>";
                }
                if (session.getUri().equals("/stopserver")){
                    this.stop();
                }
            }
            //return super.serve(session);
            Response response = new Response(responseBody);
            response.addHeader("Access-Control-Allow-Origin", "*");
            response.addHeader("Access-Control-Allow-Methods", "POST, PUT, GET");
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
