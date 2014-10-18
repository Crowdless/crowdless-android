package com.crowdless;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.*;
import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import butterknife.OnItemClick;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.squareup.picasso.Picasso;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ChooseActivity extends Activity
{
    @InjectView(R.id.list_places)
    ListView listPlaces;

    @InjectView(R.id.btn_done)
    Button btnDone;

    private ProgressDialog dialog;
    private List<POI> checkedPois;
    private PoiAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose);
        ButterKnife.inject(this);
        checkedPois = new ArrayList<POI>();
        getActionBar().setTitle("Where to?");
    }

    @Override
    protected void onStart()
    {
        super.onStart();

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        if (adapter == null)
        {
            dialog = ProgressDialog.show(this, null, "Finding points of interest...");
            JsonObjectRequest jsonObjReq = new JsonObjectRequest("http://crowdless.nodejitsu.com/json", null, new Response.Listener<JSONObject>()
            {
                @Override
                public void onResponse(JSONObject response)
                {
                    Log.i("Crowdless", "got poi response: " + response);

                    List<POI> pois = new ArrayList<POI>();
                    pois.add(new POI("sakdjklasjd", "http://www.londonnet.co.uk/files/images/sightseeing/big-ben.jpg", "Big Ben", 51.5008, 0.1247, "A big ass clock", 4.7f, 99));
                    gotPois(pois);
                    dismissDialog();
                }
            }, null);
            Volley.newRequestQueue(this).add(jsonObjReq);
        }
    }

    @OnItemClick(R.id.list_places)
    void onPlaceClicked(int position)
    {
        int initialCount = checkedPois.size();
        POI poi = adapter.getItem(position);
        if (!checkedPois.remove(poi))
            checkedPois.add(poi);

        if (initialCount == 0 && checkedPois.size() > 0)
        {
            // animate button in
            btnDone.setTranslationY(btnDone.getHeight());
            btnDone.setVisibility(View.VISIBLE);

            ObjectAnimator anim = ObjectAnimator.ofFloat(btnDone, "translationY", btnDone.getHeight(), 0);
            anim.setInterpolator(new LinearInterpolator());
            anim.setDuration(200);
            anim.start();
        }
        else if (initialCount > 0 && checkedPois.size() == 0)
        {
            // animate button out
            ObjectAnimator anim = ObjectAnimator.ofFloat(btnDone, "translationY", 0, btnDone.getHeight());
            anim.setInterpolator(new LinearInterpolator());
            anim.setDuration(200);
            anim.start();
        }

        btnDone.setText(checkedPois.size() == 1 ? "Let's go to " + checkedPois.size() + " place" : "Let's go to " + checkedPois.size() + " places");
        adapter.notifyDataSetChanged();
    }

    @OnClick(R.id.btn_done)
    void onDoneClicked()
    {
        JSONArray pois = new JSONArray();
        for (POI poi : checkedPois)
            pois.put(poi.id);

        dismissDialog();
        dialog = ProgressDialog.show(this, null, "Generating your schedule...");
        JsonObjectRequest jsonObjReq = new JsonObjectRequest("http://crowdless.nodejitsu.com/json", null, new Response.Listener<JSONObject>()
        {
            @Override
            public void onResponse(JSONObject response)
            {
                ((CrowdlessApplication) getApplication()).schedule = null;
                startActivity(new Intent(ChooseActivity.this, ScheduleActivity.class));
                dismissDialog();
            }
        }, null);
        Volley.newRequestQueue(this).add(jsonObjReq);
    }

    @Override
    public void onBackPressed()
    {
        super.onBackPressed();

        if (!dismissDialog())
            finish();
    }

    public void gotPois(List<POI> poi)
    {
        checkedPois.clear();
        adapter = new PoiAdapter(poi);
        listPlaces.setAdapter(adapter);
    }

    private boolean dismissDialog()
    {
        if (dialog != null)
        {
            dialog.dismiss();
            dialog = null;
            return true;
        }
        return false;
    }

    private final class POI
    {
        public String id;
        public String name;
        public String description;
        public String imageUrl;
        public double latitude;
        public double longitude;
        public float rating;
        public float business;

        private POI(String id, String imageUrl, String name, double latitude, double longitude, String description, float rating, float business)
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
    }

    private final class PoiAdapter extends BaseAdapter
    {
        private List<POI> pois;

        private PoiAdapter(List<POI> pois)
        {
            this.pois = pois;
        }

        @Override
        public int getCount()
        {
            return pois.size();
        }

        @Override
        public POI getItem(int position)
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
            POI poi = getItem(position);

            if (view == null)
                view = LayoutInflater.from(ChooseActivity.this).inflate(R.layout.view_poi, parent, false);

            ((TextView) view.findViewById(R.id.txt_title)).setText(poi.name);
            ((TextView) view.findViewById(R.id.txt_description)).setText(poi.description);
            Picasso.with(ChooseActivity.this).cancelRequest(((ImageView) view.findViewById(R.id.img_poi)));
            Picasso.with(ChooseActivity.this).load(poi.imageUrl).into((ImageView) view.findViewById(R.id.img_poi));
            view.findViewById(R.id.img_check).setVisibility(checkedPois.contains(poi) ? View.VISIBLE : View.GONE);

            return view;
        }
    }

}
