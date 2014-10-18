package com.crowdless;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONException;
import org.json.JSONObject;

public class LoginActivity extends Activity
{
    @InjectView(R.id.webview)
    WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ButterKnife.inject(this);
    }

    @OnClick(R.id.btn_login_yammer)
    void onLoginYammerClicked()
    {
        webView.clearCache(true);
        CookieManager.getInstance().removeAllCookie();
        webView.setWebViewClient(new CustomWebViewClient());
        webView.loadUrl("https://www.yammer.com/dialog/oauth?client_id=kw9NUS1z7jWE3dNfGwAkA&redirect_uri=https://www.yammer.com");
        webView.setVisibility(View.VISIBLE);
    }

    void onLoggedIn(String token)
    {
        Log.i("Crowdless", "got token: " + token);
        PreferenceManager.getDefaultSharedPreferences(this).edit().putString("TOKEN", token).apply();
        startActivity(new Intent(this, ChooseActivity.class));
        finish();
    }

    void onCodeRetrieved(final String code)
    {
        Log.i("Crowdless", "got code: " + code);
        webView.setVisibility(View.GONE);

        String url = "https://www.yammer.com/oauth2/access_token.json?client_id=kw9NUS1z7jWE3dNfGwAkA&client_secret=cdyKBt9cc7mRgSsnbIHvKDG0YKZT7Zg62mUOPviNqY&code=" + code;

        final ProgressDialog dialog = ProgressDialog.show(this, null, "Logging in...");

        JsonObjectRequest jsonObjReq = new JsonObjectRequest(url, null, new Response.Listener<JSONObject>()
        {
            @Override
            public void onResponse(JSONObject response)
            {
                Log.i("Crowdless", "got token response: " + response);

                String token = null;
                try
                {
                    token = response.getJSONObject("access_token").getString("token");
                }
                catch (JSONException e)
                {
                    e.printStackTrace();
                }
                dialog.dismiss();
                onLoggedIn(token);
            }
        }, null);
        Volley.newRequestQueue(this).add(jsonObjReq);
    }

    public class CustomWebViewClient extends WebViewClient
    {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url)
        {
            Log.i("Crowdless", url);
            if (url.startsWith("https://www.yammer.com/?code="))
            {
                String code = url.substring(29);
                onCodeRetrieved(code);
            }

            return false;
        }
    }
}
