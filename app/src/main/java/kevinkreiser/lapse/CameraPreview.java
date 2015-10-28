package kevinkreiser.lapse;

import android.content.Context;
import android.hardware.Camera;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private SurfaceHolder holder = null;
    private Camera camera = null;

    public CameraPreview(Context context) {
        super(context);
    }

    public void start(Camera cam) {
        camera = cam;
        holder = getHolder();
        start();
    }

    public void start() {
        if(camera == null || holder == null)
            return;
        holder.addCallback(this);
        // deprecated setting, but required on Android versions prior to 3.0
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        if (holder.getSurface() == null || camera == null)
            return;
        try { camera.stopPreview(); } catch (Exception e) { }
        try {
            camera.setPreviewDisplay(holder);
            camera.startPreview();
        } catch (Exception e) {
            Toast.makeText(getContext(), "Couldn't show preview: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public void stop() {
        camera.stopPreview();
        holder.removeCallback(this);
        camera = null;
        holder = null;
    }

    @Override
    public void surfaceCreated(SurfaceHolder h) {
        holder = h;
        start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder h, int format, int width, int height) {
        holder = h;
        start();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder h) {
        stop();
    }
}

