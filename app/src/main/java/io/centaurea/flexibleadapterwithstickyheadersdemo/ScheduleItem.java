package io.centaurea.flexibleadapterwithstickyheadersdemo;

import java.util.Date;

/**
 * Created by Natasha on 23.07.16.
 */
public class ScheduleItem {
    private String title;
    private Date startTime;
    private String _id;

    public void setTitle(String title) {
        this.title = title;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public void set_id(String _id) {
        this._id = _id;
    }

    public Date getStartTime() {
        return startTime;
    }

    public String get_id() {
        return _id;
    }

    public String getTitle() {
        return title;
    }
}
