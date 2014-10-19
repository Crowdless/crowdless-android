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
            startTime = new Date(timeframe.getLong("start"));
            endTime = new Date(timeframe.getLong("end"));
        }
        catch (Exception e)
        {

        }
    }
}
