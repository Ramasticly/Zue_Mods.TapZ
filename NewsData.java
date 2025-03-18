package com.namcobandaigames.dragonballtap.apk;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class NewsData {

    private String dl_url = "";
    private String end = "";
    private String start = "";
    private String web_url = "";

    public NewsData(String data) {
        String[] d = data.split(",");
        if (d[0].length() > 0) {
            this.dl_url = d[0];
        }
        if (d[1].length() > 0) {
            this.start = d[1];
        }
        if (d[2].length() > 0) {
            this.end = d[2];
        }
        if (d.length > 3 && d[3].length() > 0) {
            this.web_url = d[3];
        }
    }

    public boolean checkDate(int local) {
        String _now = "";
        String _start = "";
        String _end = "";
        if (this.start.length() == 0 || this.start.startsWith("0")) {
            return true;
        }
        SimpleDateFormat sdf1;
        Date date1 = new Date();
        if (local == 0) {
            sdf1 = new SimpleDateFormat("yyyy/MM/dd", Locale.JAPANESE);
        } else {
            sdf1 = new SimpleDateFormat("yyyy/MM/dd", Locale.JAPANESE);
        }
        try {
            _now = sdf1.format(date1);
            _start = sdf1.format(SimpleDateFormat.getDateInstance().parse(this.start));
            if (this.end.length() > 1) {
                _end = sdf1.format(SimpleDateFormat.getDateInstance().parse(this.end));
            }
        } catch (Exception e) {
        }
        if (_now.compareTo(_start) < 0) {
            return false;
        }
        return _end.length() <= 1 || _now.compareTo(_end) <= 0;
    }

    public String getDataURL() {
        return this.dl_url;
    }

    public String getWebURL() {
        return this.web_url;
    }
}
