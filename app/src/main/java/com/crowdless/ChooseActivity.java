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
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.squareup.picasso.Picasso;
import org.json.JSONArray;
import org.json.JSONException;
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
    private int[] peopleCounts;
    private float peopleAverage;

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
            JsonArrayRequest req = new JsonArrayRequest("http://crowdless.jitsu.com/landmarks", new Response.Listener<JSONArray>()
            {
                @Override
                public void onResponse(JSONArray response)
                {
                    Log.i("Crowdless", "got poi response: " + response);

                    int sum = 0;
                    peopleCounts = new int[response.length()];
                    List<POI> pois = new ArrayList<POI>();
                    for (int i=0; i<response.length(); i++)
                    {
                        try
                        {
                            JSONObject obj = response.getJSONObject(i);
                            peopleCounts[i] = obj.getInt("people");
                            sum += peopleCounts[i];
                            pois.add(new POI(obj));
                        }
                        catch (JSONException e)
                        {
                            e.printStackTrace();
                        }
                    }

                    peopleAverage = sum / (float) response.length();
                    gotPois(pois);
                    dismissDialog();
                }
            }, null);
            Volley.newRequestQueue(this).add(req);
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
        String ids = "";
        for (POI poi : checkedPois)
            ids += poi.id + ",";
        ids = ids.substring(0, ids.length()-1);

        dismissDialog();
        dialog = ProgressDialog.show(this, null, "Generating your schedule...");
        JsonArrayRequest req = new JsonArrayRequest("http://crowdless.jitsu.com/schedule?ids=" + ids, new Response.Listener<JSONArray>()
        {
            @Override
            public void onResponse(JSONArray response)
            {
                Log.i("Crowdless", "response: " + response);
                List<ScheduleEntry> entries = new ArrayList<ScheduleEntry>();
                for (int i=0; i<response.length(); i++)
                {
                    try
                    {
                        entries.add(new ScheduleEntry(response.getJSONObject(i)));
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }

                ((CrowdlessApplication) getApplication()).schedule = entries;
                startActivity(new Intent(ChooseActivity.this, ScheduleActivity.class));
                dismissDialog();
            }
        }, null);
        Volley.newRequestQueue(this).add(req);
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
            ((RatingBar) view.findViewById(R.id.rating_poi)).setRating(poi.rating);
            Picasso.with(ChooseActivity.this).cancelRequest(((ImageView) view.findViewById(R.id.img_poi)));
            Picasso.with(ChooseActivity.this).load(poi.imageUrl).into((ImageView) view.findViewById(R.id.img_poi));
            view.findViewById(R.id.img_check).setVisibility(checkedPois.contains(poi) ? View.VISIBLE : View.GONE);

            Log.i("Crowdless", "values: " + poi.business + " / " + peopleAverage);

            boolean show = poi.business >= peopleAverage / 2.0f;
            view.findViewById(R.id.img_flame_1).setVisibility(checkedPois.contains(poi) || !show ? View.GONE : View.VISIBLE);

            show = poi.business >= peopleAverage;
            view.findViewById(R.id.img_flame_2).setVisibility(checkedPois.contains(poi) || !show ? View.GONE : View.VISIBLE);

            show = poi.business >= peopleAverage * 2;
            view.findViewById(R.id.img_flame_3).setVisibility(checkedPois.contains(poi) || !show ? View.GONE : View.VISIBLE);

            return view;
        }
    }

}
