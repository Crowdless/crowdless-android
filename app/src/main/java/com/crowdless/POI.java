package com.crowdless;

import org.json.JSONObject;

final class POI
{
    public String id;
    public String name;
    public String description;
    public String imageUrl;
    public double latitude;
    public double longitude;
    public float rating;
    public float business;

    POI(String id, String imageUrl, String name, double latitude, double longitude, String description, float rating, float business)
    {
        this.id = id;
        this.imageUrl = imageUrl;
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.description = description;
        this.rating = rating;
        this.business = business;
    }

    public POI(JSONObject obj)
    {
        try
        {
            this.id = obj.getString("_id");
            this.imageUrl = obj.getString("image_url");
            this.name = obj.getString("name");
            this.latitude = obj.getJSONArray("coords").getDouble(0);
            this.longitude = obj.getJSONArray("coords").getDouble(1);
            this.description = obj.getString("description");
            this.rating = (float) obj.getDouble("rating") / 2.0f;
            this.business = obj.getInt("people");
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
