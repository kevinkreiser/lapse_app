package kevinkreiser.lapse;

import java.io.File;

import android.os.Bundle;
import android.content.Context;

import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.Menu;
import android.view.WindowManager;
import android.widget.LinearLayout;

public class MainActivity extends AppCompatActivity {
    private Context context;
    private CameraThread camera;
    private ServiceNode service_node;
    private final String root_dir = new String(File.separator + "sdcard" + File.separator + "lapse");
    private final String picture_dir = new String(root_dir + File.separator + "photos");

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        //where to show stuff
        context = this;
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    public void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onResume() {
        //hook the camera back up and schedule a time to take a picture
        super.onResume();
        //start the service thread if we havent already
        if(service_node == null) {
            service_node = new ServiceNode(context, picture_dir);
            new Thread(service_node).start();
        }
        //a camera thread
        camera = new CameraThread(context, (LinearLayout)findViewById(R.id.camera_preview), picture_dir);
        new Thread(camera).start();
    };

    @Override
    protected void onPause() {
        //let other apps use the camera
        super.onPause();
        camera.abort();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        service_node.abort();
    }
}