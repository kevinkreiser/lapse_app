package kevinkreiser.lapse;

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

public class Settings {
    private boolean enabled = false;
    private int interval = - 1;
    private int start_time = 0;
    private int end_time = 24 * 60;
    private boolean[] weekdays = {false, false, false, false, false, false, false};
    private JSONObject schedule = null;
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
            reset(new JSONObject());
        }
    }

    //keep the schedule info only if its complete
    public synchronized void reset(JSONObject json) {
        try {
            schedule = json.optJSONObject("schedule");
            enabled = schedule.getBoolean("enabled");

            interval = schedule.getInt("interval");
            if(interval < 1 || interval > 300)
                throw new Exception();

            JSONArray days = schedule.getJSONArray("weekdays");
            boolean any = false;
            for(int i = 0; i < 7; i++) {
                weekdays[i] = days.getBoolean(i);
                any = weekdays[i] || any;
            }
            if(!any)
                throw new Exception();

            String time = schedule.getString("daily_start_time");
            String[] pieces = time.split(":");
            start_time = Integer.parseInt(pieces[0]) * 60 + Integer.parseInt(pieces[1]);
            if(pieces.length != 2 || start_time > 24 * 60)
                throw new Exception();

            time = schedule.getString("daily_end_time");
            pieces = time.split(":");
            end_time = Integer.parseInt(pieces[0]) * 60 + Integer.parseInt(pieces[1]);
            if(pieces.length != 2 || end_time > 24 * 60 || start_time >= end_time)
                throw new Exception();
        }
        catch(Exception e) {
            enabled = false;
            interval = -1;
            start_time = 0;
            end_time = 24 * 60 - 1;
            weekdays = new boolean[] {false, false, false, false, false, false, false};
            try {
                schedule = new JSONObject("{\"enabled\":false,\"interval\":-1,\"weekdays\":[false,false,false,false,false,false,false],\"daily_start_time\":\"0:00\",\"daily_end_time\":\"23:59\"}");
            } catch(Exception f) { }
        }

        try {
            if(!file.getParentFile().exists() && !file.getParentFile().mkdirs())
                Log.e("File", "Couldn't make storage location");
            file.delete();
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.write(new JSONObject().put("schedule", schedule).toString());
            writer.close();
        }
        catch(Throwable t) {
            Log.e("Schedule", t.getMessage());
        }
    }

    public synchronized int getInterval() {
        if(!enabled)
            return -1;

        //how often
        if(interval < 1)
            return -1;

        //on what day
        Calendar calendar = new GregorianCalendar();
        int day = calendar.get(Calendar.DAY_OF_WEEK) - 2;
        if(day == -1)
            day = 6;
        if(!weekdays[day])
            return -1;

        //between what times
        int time = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE) + interval;
        if(start_time > time || time > end_time)
            return -1;

        return interval;
    }

    public synchronized JSONObject getSchedule() {
        return schedule;
    }
}
