package kevinkreiser.lapse;

import android.os.Bundle;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.Menu;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {
    private Camera camera;
    private CameraPreview preview;
    private PictureCallback image_callback;
    private Context context;
    private ServiceNode service_node;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        //where to show stuff
        context = this;
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        preview = new CameraPreview(context);
        LinearLayout layout = (LinearLayout)findViewById(R.id.camera_preview);
        layout.setVisibility(View.VISIBLE);
        layout.addView(preview);

        //what to do with the jpeg bytes when a picture is taken
        image_callback = new PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                //save the image
                try {
                    FileOutputStream fos = new FileOutputStream(out_file());
                    fos.write(data);
                    fos.close();
                } catch (Exception e) {
                    Toast.makeText(context, "Couldn't save picture: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
                //kill the preview
                preview.stop();
            }
        };

        //start the service thread
        if(service_node != null)
            service_node.abort();
        service_node = new ServiceNode(context, File.separator + "sdcard" + File.separator + "lapse");
        try {
            JSONObject options = new JSONObject(state.getString("options"));
            service_node.setOptions(options);
        } catch (Exception e) { }
        new Thread(service_node).start();
    }


    @Override
    public void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        if(service_node != null && service_node.getOptions() != null)
            state.putString("schedule", service_node.getOptions().toString());
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
            case R.id.take_picture:
                preview.start(camera);
                camera.takePicture(null, null, image_callback);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onResume() {
        //hook the camera back up and schedule a time to take a picture
        super.onResume();
        if(context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            try {
                camera = Camera.open();
                //TODO: run a timer to take a picture in so many seconds
                //service_node.scheduleNext();
            }
            catch(Exception e) {
                Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }//you dont so share the sad news
        else {
            Toast.makeText(context, "Sorry, your phone does not have a camera!", Toast.LENGTH_LONG).show();
        }
    };

    @Override
    protected void onPause() {
        //let other apps use the camera
        super.onPause();
        camera.release();
    }

    private static File out_file() {
        //make a place to put these pictures
        Date now = new Date();
        File dir = new File(File.separator + "sdcard" + File.separator + "lapse",
                            new SimpleDateFormat("yyyy_MM_dd").format(now));
        if(!dir.exists() && !dir.mkdirs())
            return null;
        //make a reasonable file name
        return new File(dir, new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(new Date()) + ".JPG");
    }
}