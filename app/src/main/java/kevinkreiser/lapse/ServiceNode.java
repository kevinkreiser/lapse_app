package kevinkreiser.lapse;

import org.json.JSONObject;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Socket;
import org.zeromq.ZContext;
import org.zeromq.ZBeacon;
import org.zeromq.ZPoller;
import org.zeromq.ZStar;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.text.format.Formatter;

import java.nio.charset.StandardCharsets;

import zmq.Poller;

public class ServiceNode extends BroadcastReceiver implements Runnable  {
    private Context context;
    private volatile boolean running = true;
    private boolean reset_connection = true;
    private ZContext zctx;
    private Socket service;
    private ZBeacon beacon;
    private byte[] beacon_msg;
    private JSONObject options;

    public ServiceNode(Context c) {
        context = c;
        //set up the beacon message ahead of time
        TelephonyManager tm = (TelephonyManager)c.getSystemService(Context.TELEPHONY_SERVICE);
        String id = tm.getDeviceId();
        beacon_msg = new byte[] { 'Z', 'R', 'E', 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        int offset = id.length() > 16 ? (id.length() - 16)/2 : 0;
        for(int i = offset; i < 16 && i < id.length(); i++)
            beacon_msg[(i - offset) + 4] = (byte)id.charAt(i);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
        if (networkInfo != null) {
            if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                Log.i("CONNECTION: ", "type:" + networkInfo.getType() + " state:" + networkInfo.getState());
                reset_connection = true;
            }
        }
    }

    public JSONObject getOptions() {
        return options;
    }

    public void setOptions(JSONObject json) {
        options = json;
    }

    public void getNext() {

    }

    public void abort() {
        running = false;
    }

    public void run() {
        while(running) {
            //if we have a connection we can listen for stuff
            if(resetConnection())
                listen();
            //maybe the connection will come back in a little
            try { Thread.sleep(120000); } catch(Exception e) { Log.e("Service Thread", "Insomniac"); }
        }
    }

    private void listen() {
        //make a poller for listening
        ZPoller poller = null;
        try {
            poller = new ZPoller(new ZStar.VerySimpleSelectorCreator().create());
            poller.register(service, ZPoller.IN);
        } catch (Exception e) {
            poller = null;
            Log.e("Service Poller", "Won't Poll");
        }

        //tell everyone we are listening
        beacon.start();

        //answer requests
        while (!reset_connection && running && poller != null) {
            if (poller.poll(500) == 1) {
                String message = service.recvStr();
                try {
                    JSONObject json = new JSONObject(message);
                    String type = json.getString("type");
                    /*switch (type) {
                        case "get_options":
                            break;
                        case "set_options":
                            break;
                        case "get_image":
                            break;
                    }*/
                } catch (Exception e) {
                    service.send("Malformed Request", ZMQ.DONTWAIT);
                    Log.e("Service Message", "Malformed request: " + message);
                }
            }
            try { Thread.sleep(500); } catch (Exception e) { Log.e("Listener Loop", "Insomniac"); }
        }

        //cleanup
        try { beacon.stop(); } catch (Exception e) { Log.e("Service Beacon", "Unstoppable"); }
        try { poller.close(); } catch (Exception e) { Log.e("Service Poller", "Unstoppable"); }
        service.close();
        zctx.close();
    }

    private boolean resetConnection() {
        reset_connection = false;
        ConnectivityManager connectivityManager = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (networkInfo.isConnected()) {
            //setup the service on a random port in suggested IANA range
            zctx = new ZContext();
            service = zctx.createSocket(ZMQ.REP);
            int port = service.bindToRandomPort("tcp://*", 49152, 65535);
            //setup the beacon
            beacon_msg[20] = (byte)((port >> 8) & 255);
            beacon_msg[21] = (byte)(port & 255);
            WifiManager wifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
            String broadcast_ip = Formatter.formatIpAddress(wifiManager.getDhcpInfo().gateway).replaceFirst("[0-9]+$", "255");
            beacon = new ZBeacon(broadcast_ip, 5670, beacon_msg, true);
            return true;
        }
        return false;
    }
}

