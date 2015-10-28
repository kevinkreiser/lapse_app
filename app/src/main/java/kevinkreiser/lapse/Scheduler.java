package kevinkreiser.lapse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.Timer;

public class Scheduler {
    private int interval = - 1;
    private int start_time = 0;
    private int end_time = 24 * 60;
    private boolean[] weekdays = {false, false, false, false, false, false, false};

    //keep the schedule info only if its complete
    public Scheduler(JSONObject json) {
        try {
            JSONObject schedule = json.optJSONObject("schedule");
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
            interval = - 1;
            start_time = 0;
            end_time = 24 * 60;
            weekdays = new boolean[] {false, false, false, false, false, false, false};
        }
    }

    public Date getNext() {
        if(interval == -1)
            return null;
        Date date = new Date();
        date.setTime(date.getTime() + interval * 1000);
        //TODO: check if this date isnt in range and go to next that is
        return date;
    }
}
