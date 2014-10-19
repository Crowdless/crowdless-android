package com.crowdless;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.ProgressDialog;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.text.format.DateFormat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.*;
import android.view.animation.LinearInterpolator;
import android.widget.*;
import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.esri.android.map.GraphicsLayer;
import com.esri.android.map.MapView;
import com.esri.android.map.ags.ArcGISFeatureLayer;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.Polyline;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.io.UserCredentials;
import com.esri.core.map.Graphic;
import com.esri.core.symbol.MarkerSymbol;
import com.esri.core.symbol.PictureMarkerSymbol;
import com.esri.core.symbol.SimpleLineSymbol;
import com.esri.core.tasks.na.*;
import com.squareup.picasso.Picasso;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ScheduleActivity extends Activity
{
    @InjectView(R.id.map)
    MapView map;

    @InjectView(R.id.v_places)
    FrameLayout vPlaces;

    @InjectView(R.id.list_places)
    ListView listPlaces;

    @InjectView(R.id.txt_directions)
    TextView txtDirections;

    private MenuItem menuItemList;
    private MenuItem menuItemMap;
    private boolean listShowing = false;
    private GraphicsLayer graphicsLayer;
    private List<String> directions;
    private int directionIndex;
    private ProgressDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule);
        ButterKnife.inject(this);
        getActionBar().setTitle("Let's go!");
        vPlaces.setVisibility(View.GONE);
        txtDirections.setVisibility(View.GONE);
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
        map.addLayer(new ArcGISFeatureLayer("http://services3.arcgis.com/hiVQEfGAv4lHzgdT/arcgis/rest/services/dataexporttweets_(1)/FeatureServer/0", ArcGISFeatureLayer.MODE.SNAPSHOT));

        dialog = ProgressDialog.show(this, null, "Loading your route...");

//        DisplayMetrics metrics = new DisplayMetrics();
//        getWindowManager().getDefaultDisplay().getMetrics(metrics);
//
//        graphicsLayer = new GraphicsLayer();
//        map.addLayer(graphicsLayer);
//        Drawable d = getResources().getDrawable(R.drawable.marker);

        final StopGraphic[] points = new StopGraphic[((CrowdlessApplication) getApplication()).schedule.size()];
        int i = 0;

        for (ScheduleEntry entry : ((CrowdlessApplication) getApplication()).schedule)
        {
//            MarkerSymbol sym = new PictureMarkerSymbol(d);
//            sym.setOffsetX((d.getIntrinsicWidth() / 2) / metrics.density);
//            sym.setOffsetY((d.getIntrinsicHeight() / 2) / metrics.density);
            Point latlon = new Point(entry.poi.longitude, entry.poi.latitude);
            Point mapPoint = tomerc(latlon);
//            Graphic marker = new Graphic(mapPoint, sym);
//            graphicsLayer.addGraphic(marker);

            points[i] = new StopGraphic(mapPoint);
            points[i].setRouteName("route");
            points[i].setName(entry.poi.name);
            i++;
        }

        listPlaces.setAdapter(new ScheduleAdapter(((CrowdlessApplication) getApplication()).schedule));

        final JsonObjectRequest req = new JsonObjectRequest("https://www.arcgis.com/sharing/oauth2/token?client_id=eSZtcJr46c3BHD3X&grant_type=client_credentials&client_secret=1986ae12fc93450cb893925153e6dae3&f=pjson", null, new Response.Listener<JSONObject>()
        {
            @Override
            public void onResponse(JSONObject response)
            {
                try
                {
                    String token = response.getString("access_token");
                    new CrowdlessRouteTask(token).execute(points);
                }
                catch (Exception e)
                {

                }
            }
        }, null);
        Volley.newRequestQueue(this).add(req);
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

    @OnClick(R.id.txt_directions)
    void onTextDirectionsClicked()
    {
        if (directions == null || directionIndex == directions.size() - 1)
            return;
        txtDirections.setText(Html.fromHtml(directions.get(++directionIndex)));
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
            ((TextView) view.findViewById(R.id.txt_description)).setText(DateFormat.format("dd/MM hh:mm", entry.startTime) + " - " + DateFormat.format("dd/MM hh:mm", entry.endTime));
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

    public final class CrowdlessRouteTask extends AsyncTask<StopGraphic, Void, Graphic>
    {
        private final String token;

        public CrowdlessRouteTask(String token)
        {
            this.token = token;
        }

        @Override
        protected Graphic doInBackground(StopGraphic... points)
        {
            try
            {
                UserCredentials auth = new UserCredentials();
                auth.setUserToken(token, "eSZtcJr46c3BHD3X");
                String routeTaskURL = "http://route.arcgis.com/arcgis/rest/services/World/Route/NAServer/Route_World";
                RouteTask routeTask = RouteTask.createOnlineRouteTask(routeTaskURL, auth);

                RouteParameters routeParams = routeTask.retrieveDefaultRouteTaskParameters();
                NetworkDescription description = routeTask.getNetworkDescription();
                List<CostAttribute> costAttributes = description.getCostAttributes();

                // Assign the first cost attribute as the impedance
                if (costAttributes.size() > 0)
                    routeParams.setImpedanceAttributeName(costAttributes.get(0).getName());
                else
                    routeParams.setImpedanceAttributeName("Time");

                NAFeaturesAsFeature naFeatures = new NAFeaturesAsFeature();
                naFeatures.setSpatialReference(SpatialReference.create(102100));
                naFeatures.addFeatures(points);
                routeParams.setStops(naFeatures);
                routeParams.setReturnRoutes(true);
                routeParams.setReturnStops(true);
                routeParams.setReturnDirections(true);
                routeParams.setReturnPointBarriers(true);
                routeParams.setReturnPolygonBarriers(true);
                routeParams.setReturnPolylineBarriers(true);
                routeParams.setReturnZ(true);

                RouteResult results = routeTask.solve(routeParams);

                Log.i("Crowdless", "results: " + results);
                directions = new ArrayList<String>();

                Route route = results.getRoutes().get(0);
                Log.i("Crowdless", "route: " + route);
                for (RouteDirection r : route.getRoutingDirections())
                {
                    Log.i("Crowdless", "RouteDirection: " + r);
                    directions.add(r.getText()
                            .replace("east", "<b>east</b>")
                            .replace("west", "<b>west</b>")
                            .replace("north", "<b>north</b>")
                            .replace("south", "<b>south</b>")
                            .replace("arrive", "<b>arrive</b>")
                            .replace("Arrive", "<b>Arrive</b>")
                            .replace("depart", "<b>depart</b>")
                            .replace("Depart", "<b>Depart</b>")
                            .replace("Start", "<b>Start</b>")
                            .replace("Finish", "<b>Finish</b>")
                            .replace("left", "<b>left</b>")
                            .replace("right", "<b>right</b>"));
                }
                Polyline routeGeom = (Polyline) route.getRouteGraphic().getGeometry();
                for (int i=0; i<routeGeom.getPointCount(); i++)
                {
                    Point p = routeGeom.getPoint(i);
                    routeGeom.setPoint(i, tomerc(p));
                }

                Log.i("Crowdless", "routeGeom: " + routeGeom);
                Graphic symbolGraphic = new Graphic(routeGeom, new SimpleLineSymbol(getResources().getColor(R.color.primary_dark), 10));
                Log.i("Crowdless", "symbolGraphic: " + symbolGraphic);
                return symbolGraphic;
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Graphic graphic)
        {
            dialog.dismiss();

            DisplayMetrics metrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(metrics);

            graphicsLayer = new GraphicsLayer();
            map.addLayer(graphicsLayer);
            Drawable d = getResources().getDrawable(R.drawable.marker);

            if (graphic != null)
            {
//                GraphicsLayer graphicsLayer = new GraphicsLayer();
//                map.addLayer(graphicsLayer);
                graphicsLayer.addGraphic(graphic);
            }

            for (ScheduleEntry entry : ((CrowdlessApplication) getApplication()).schedule)
            {
                MarkerSymbol sym = new PictureMarkerSymbol(d);
                sym.setOffsetX((d.getIntrinsicWidth() / 2) / metrics.density);
                sym.setOffsetY((d.getIntrinsicHeight() / 2) / metrics.density);
                Point latlon = new Point(entry.poi.longitude, entry.poi.latitude);
                Point mapPoint = tomerc(latlon);
                Graphic marker = new Graphic(mapPoint, sym);
                graphicsLayer.addGraphic(marker);
            }

            directionIndex = -1;
            onTextDirectionsClicked();

            txtDirections.setVisibility(View.VISIBLE);
            txtDirections.setTranslationY(txtDirections.getHeight());
            ObjectAnimator anim = ObjectAnimator.ofFloat(txtDirections, "translationY", txtDirections.getHeight(), 0);
            anim.setInterpolator(new LinearInterpolator());
            anim.setDuration(200);
            anim.start();
        }
    }
}
