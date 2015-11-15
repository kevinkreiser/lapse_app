package kevinkreiser.lapse;

import android.hardware.Camera;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

public class Settings {
    private CameraSettings camera_settings;
    private ScheduleSettings schedule_settings;
    private final File file = new File(File.separator + "sdcard" + File.separator + "lapse", "settings.json");

    //singleton
    private static final Settings instance = new Settings();
    public static Settings getInstance() {
        return instance;
    }

    protected Settings() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            reset(new JSONObject(reader.readLine()));
            reader.close();
        }
        catch (Exception e) {
            reset(null);
        }
    }
    public synchronized CameraSettings getCameraSettings() { return camera_settings; }
    public synchronized ScheduleSettings getScheduleSettings() { return schedule_settings; }
    @Override
    public synchronized String toString() {
        try {
            return new JSONObject().put("camera", camera_settings.camera).put("schedule", schedule_settings.schedule).toString();
        } catch(Exception e) { return ""; }
    };

    //keep the schedule info only if its complete
    public synchronized void reset(JSONObject json) {
        //get camera parameters
        camera_settings = new CameraSettings(json);
        //get the schedule pieces
        schedule_settings = new ScheduleSettings(json);

        try {
            if (!file.getParentFile().exists() && !file.getParentFile().mkdirs())
                Log.e("File", "Couldn't make storage location");
            file.delete();
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.write(toString());
            writer.close();
        } catch (Throwable t) {
            Log.e("Settings", t.getMessage());
        }
    }

    public class CameraSettings {
        private JSONObject camera = null;
        private int jpeg_quality;
        private int width;
        private int height;

        public CameraSettings(JSONObject json) {
            try {
                camera = json.getJSONObject("camera");
                jpeg_quality = camera.getInt("jpeg_quality");
                if(jpeg_quality < 1 || jpeg_quality > 100)
                    throw new Exception();

                JSONArray sizes = camera.getJSONArray("picture_sizes");
                String[] pieces = sizes.getString(0).split("X");
                width = Integer.parseInt(pieces[0]);
                height = Integer.parseInt(pieces[1]);
                if(width * height < 1 || width * height > 100000000)
                    throw new Exception();
            }
            catch(Exception e) {
                try {
                    camera = new JSONObject("{\"jpeg_quality\":85,\"picture_sizes\":[]}");
                    jpeg_quality = 85;
                    width = -1;
                    height = -1;
                } catch(Exception f) { }
            }
        }

        public Camera.Parameters adjustParameters(Camera.Parameters parameters) {
            if(width > 0 && height > 0)
                parameters.setPictureSize(width, height);
            else {
                TreeMap<Integer, Camera.Size> sizes = new TreeMap(Collections.reverseOrder());
                for(Camera.Size size : parameters.getSupportedPictureSizes())
                    sizes.put(size.width * size.height, size);
                JSONArray picture_sizes = new JSONArray();
                for(Map.Entry<Integer, Camera.Size> entry : sizes.entrySet())
                    picture_sizes.put(entry.getValue().width + "X" + entry.getValue().height);
                try { camera.put("picture_sizes", picture_sizes); } catch (Exception e) {}
            }
            parameters.setJpegQuality(jpeg_quality);
            return parameters;
        }
    }

    public class ScheduleSettings {
        private JSONObject schedule = null;
        private boolean enabled = false;
        private int interval = -1;
        private int start_time = 0;
        private int end_time = 24 * 60;
        private boolean[] weekdays = {false, false, false, false, false, false, false};

        public ScheduleSettings(JSONObject json) {
            try {
                schedule = json.getJSONObject("schedule");
                enabled = schedule.getBoolean("enabled");

                interval = schedule.getInt("interval");
                if (interval < 1 || interval > 300)
                    throw new Exception();

                JSONArray days = schedule.getJSONArray("weekdays");
                boolean any = false;
                for (int i = 0; i < 7; i++) {
                    weekdays[i] = days.getBoolean(i);
                    any = weekdays[i] || any;
                }
                if (!any)
                    throw new Exception();

                String time = schedule.getString("daily_start_time");
                String[] pieces = time.split(":");
                start_time = Integer.parseInt(pieces[0]) * 60 + Integer.parseInt(pieces[1]);
                if (pieces.length != 2 || start_time > 24 * 60)
                    throw new Exception();

                time = schedule.getString("daily_end_time");
                pieces = time.split(":");
                end_time = Integer.parseInt(pieces[0]) * 60 + Integer.parseInt(pieces[1]);
                if (pieces.length != 2 || end_time > 24 * 60 || start_time >= end_time)
                    throw new Exception();
            } catch (Exception e) {
                enabled = false;
                interval = -1;
                start_time = 0;
                end_time = 24 * 60 - 1;
                weekdays = new boolean[]{false, false, false, false, false, false, false};
                try {
                    schedule = new JSONObject("{\"enabled\":false,\"interval\":-1,\"weekdays\":[false,false,false,false,false,false,false],\"daily_start_time\":\"0:00\",\"daily_end_time\":\"23:59\"}");
                } catch (Exception f) { }
            }
        }

        public int getInterval() {
            if (!enabled)
                return -1;

            //how often
            if (interval < 1)
                return -1;

            //on what day
            Calendar calendar = new GregorianCalendar();
            int day = calendar.get(Calendar.DAY_OF_WEEK) - 2;
            if (day == -1)
                day = 6;
            if (!weekdays[day])
                return -1;

            //between what times
            int time = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE) + interval;
            if (start_time > time || time > end_time)
                return -1;

            return interval;
        }
    }
}
