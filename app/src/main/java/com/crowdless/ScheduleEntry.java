package com.crowdless;

import org.json.JSONObject;

import java.util.Date;

public class ScheduleEntry
{
    public POI poi;
    public Date startTime;
    public Date endTime;

    public ScheduleEntry(JSONObject obj)
    {
        try
        {
            poi = new POI(obj.getJSONObject("landmark"));
            JSONObject timeframe = obj.getJSONObject("timeframe");
            startTime = new Date(timeframe.getLong("start") - 86400000L);
            endTime = new Date(timeframe.getLong("end") - 86400000L);
        }
        catch (Exception e)
        {

        }
    }
}
