package kevinkreiser.lapse;

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.CountDownLatch;

public class CameraThread implements Runnable {
    private volatile boolean running = true;
    private Camera camera;
    private CameraPreview preview;
    private Camera.AutoFocusCallback focus_callback;
    private Camera.PictureCallback image_callback;
    private CountDownLatch latch;
    private boolean refocus;

    public CameraThread(Context context, LinearLayout layout, final String picture_dir) {
        //setup the preview
        preview = new CameraPreview(context);
        layout.setVisibility(View.VISIBLE);
        layout.addView(preview);

        //try to get focus for the camera
        focus_callback = new Camera.AutoFocusCallback() {
            @Override
            public void onAutoFocus(boolean success, Camera camera) {
                refocus = !success;
                latch.countDown();
            }
        };

        //what to do with the jpeg bytes when a picture is taken
        image_callback = new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                //save the image
                try {
                    FileOutputStream fos = new FileOutputStream(out_file(picture_dir));
                    fos.write(data);
                    fos.close();
                } catch (Exception e) {
                    Log.e("Write Picture", "Couldn't save picture: " + e.getMessage());
                }
                //signal we are done
                latch.countDown();
            }
        };
    }

    public void abort() {
        running = false;
    }

    public void run() {
        while(running) {
            //wait for a valid interval to sleep
            int interval = Scheduler.getInstance().getInterval();
            while(interval < 1) {
                refocus = true;
                try { Thread.sleep(1000); } catch (Exception e) { break; }
                interval = Scheduler.getInstance().getInterval();
            }
            try { Thread.sleep(interval * 1000); } catch (Exception e) { break; }

            //ask for a picture and wait until image is saved
            if(tryCamera()) {
                preview.start(camera);
                latch = new CountDownLatch(1);
                camera.takePicture(null, null, image_callback);
                try { latch.await(); } catch (Exception e) { break; }
                preview.stop();
            }
        }
        if(camera != null) {
            camera.release();
            camera = null;
        }
    }

    private static File out_file(String root_dir) {
        //TODO: find and remove old empty directories
        //make a place to put these pictures
        Date now = new Date();
        File dir = new File(root_dir, new SimpleDateFormat("yyyy_MM").format(now) + File.separator +
            new SimpleDateFormat("dd_HH").format(now));
        if(!dir.exists() && !dir.mkdirs())
            return null;
        //make a reasonable file name
        return new File(dir, new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(new Date()) + ".JPG");
    }

    private boolean tryCamera() {
        try {
            //open a landscape camera
            if(camera == null) {
                refocus = true;
                camera = Camera.open();
                camera.setDisplayOrientation(0);
            }
            //make sure to focus
            if(camera != null && refocus) {
                preview.start(camera);
                latch = new CountDownLatch(1);
                camera.autoFocus(focus_callback);
                try { latch.await(); } catch (Exception e) { }
                preview.stop();
                Log.i("Camera", "Refocused");
            }
        }
        catch(Exception e) {
            camera = null;
            Log.e("Camera Troubles", e.getMessage());
        }
        return camera != null;
    }
}
