package com.crowdless;

import android.app.Activity;
import android.os.Bundle;
import butterknife.ButterKnife;
import com.esri.android.map.MapView;
import com.esri.android.map.ags.ArcGISFeatureLayer;

public class ScheduleActivity extends Activity
{
//    @InjectView(R.id.map)
    MapView map;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule);
        ButterKnife.inject(this);
        map = (MapView) findViewById(R.id.map);
        getActionBar().setTitle("Let's go!");
    }

    @Override
    protected void onStart()
    {
        super.onStart();
        map.addLayer(new ArcGISFeatureLayer("http://services3.arcgis.com/hiVQEfGAv4lHzgdT/arcgis/rest/services/lats/FeatureServer/0", ArcGISFeatureLayer.MODE.SNAPSHOT));
//        JsonArrayRequest jsonObjReq = new JsonArrayRequest("http://crowdless.nodejitsu.com/json", null, new Response.Listener<JSONArray>()
//        {
//            @Override
//            public void onResponse(JSONArray response)
//            {
//                Log.i("Crowdless", "got response: " + response);
////                for (JSONObject object : response)
////                {
////
////                }
//            }
//        }, null);

    }
}
