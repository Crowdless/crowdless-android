package com.crowdless;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.DisplayMetrics;
import android.view.*;
import android.widget.*;
import butterknife.ButterKnife;
import butterknife.InjectView;
import com.esri.android.map.GraphicsLayer;
import com.esri.android.map.MapView;
import com.esri.android.map.ags.ArcGISFeatureLayer;
import com.esri.core.geometry.Point;
import com.esri.core.map.Graphic;
import com.esri.core.symbol.MarkerSymbol;
import com.esri.core.symbol.PictureMarkerSymbol;
import com.squareup.picasso.Picasso;

import java.util.List;

public class ScheduleActivity extends Activity
{
    @InjectView(R.id.map)
    MapView map;

    @InjectView(R.id.v_places)
    FrameLayout vPlaces;

    @InjectView(R.id.list_places)
    ListView listPlaces;

    private MenuItem menuItemList;
    private MenuItem menuItemMap;
    private boolean listShowing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule);
        ButterKnife.inject(this);
        getActionBar().setTitle("Let's go!");
        vPlaces.setVisibility(View.GONE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.schedule, menu);
        menuItemList = menu.findItem(R.id.action_list);
        menuItemMap = menu.findItem(R.id.action_map);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu)
    {
        menuItemList.setVisible(!listShowing);
        menuItemMap.setVisible(listShowing);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.action_list:
                vPlaces.setVisibility(View.VISIBLE);
                listShowing = !listShowing;
                invalidateOptionsMenu();
                return true;
            case R.id.action_map:
                vPlaces.setVisibility(View.GONE);
                listShowing = !listShowing;
                invalidateOptionsMenu();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart()
    {
        super.onStart();
        map.addLayer(new ArcGISFeatureLayer("http://services3.arcgis.com/hiVQEfGAv4lHzgdT/arcgis/rest/services/datafile/FeatureServer/0", ArcGISFeatureLayer.MODE.SNAPSHOT));

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        GraphicsLayer graphicsLayer = new GraphicsLayer();
        map.addLayer(graphicsLayer);
        Drawable d = getResources().getDrawable(R.drawable.marker);

        for (ScheduleEntry entry : ((CrowdlessApplication) getApplication()).schedule)
        {
            MarkerSymbol sym = new PictureMarkerSymbol(d);
            sym.setOffsetX((d.getIntrinsicWidth() / 2) / metrics.density);
            sym.setOffsetY((d.getIntrinsicHeight() / 2) / metrics.density);
            Point mapPoint = tomerc(new Point(entry.poi.longitude, entry.poi.latitude));
            graphicsLayer.addGraphic(new Graphic(mapPoint, sym));
        }

        listPlaces.setAdapter(new ScheduleAdapter(((CrowdlessApplication) getApplication()).schedule));

//        RouteTask routeTask = RouteTask.createOnlineRouteTask("http://sampleserver3.arcgisonline.com/ArcGIS/rest/services/Network/USA/NAServer/Route", null);
    }

    private Point tomerc(Point pnt)
    {
        double mercatorX_lon = pnt.getX();
        double mercatorY_lat = pnt.getY();
        if ((Math.abs(mercatorX_lon) > 180 || Math.abs(mercatorY_lat) > 90))
            return pnt;

        double num = mercatorX_lon * 0.017453292519943295;
        double x = 6378137.0 * num;
        double a = mercatorY_lat * 0.017453292519943295;

        mercatorX_lon = x;
        mercatorY_lat = 3189068.5 * Math.log((1.0 + Math.sin(a)) / (1.0 - Math.sin(a)));
        return new Point(mercatorX_lon, mercatorY_lat);
    }

    private final class ScheduleAdapter extends BaseAdapter
    {
        private List<ScheduleEntry> pois;

        private ScheduleAdapter(List<ScheduleEntry> pois)
        {
            this.pois = pois;
        }

        @Override
        public int getCount()
        {
            return pois.size();
        }

        @Override
        public ScheduleEntry getItem(int position)
        {
            return pois != null ? pois.get(position) : null;
        }

        @Override
        public long getItemId(int position)
        {
            return position;
        }

        @Override
        public View getView(int position, View view, ViewGroup parent)
        {
            ScheduleEntry entry = getItem(position);
            POI poi = entry.poi;

            if (view == null)
                view = LayoutInflater.from(ScheduleActivity.this).inflate(R.layout.view_poi, parent, false);

            ((TextView) view.findViewById(R.id.txt_title)).setText(poi.name);
            ((TextView) view.findViewById(R.id.txt_description)).setText(DateFormat.format("dd/MM hh:mm", entry.startTime) + " - " + DateFormat.format("dd/MM hh:mm",entry.endTime));
            Picasso.with(ScheduleActivity.this).cancelRequest(((ImageView) view.findViewById(R.id.img_poi)));
            Picasso.with(ScheduleActivity.this).load(poi.imageUrl).into((ImageView) view.findViewById(R.id.img_poi));
            view.findViewById(R.id.img_check).setVisibility(View.GONE);
            view.findViewById(R.id.img_flame_1).setVisibility(View.GONE);
            view.findViewById(R.id.img_flame_2).setVisibility(View.GONE);
            view.findViewById(R.id.img_flame_3).setVisibility(View.GONE);
            view.findViewById(R.id.rating_poi).setVisibility(View.GONE);

            return view;
        }
    }

}
