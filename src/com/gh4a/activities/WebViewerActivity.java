/*
 * Copyright 2011 Azwan Adli Abdullah
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gh4a.activities;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.gh4a.Gh4Application;
import com.gh4a.LoadingFragmentActivity;
import com.gh4a.R;
import com.gh4a.utils.ThemeUtils;

public abstract class WebViewerActivity extends LoadingFragmentActivity {
    protected WebView mWebView;

    private int[] ZOOM_SIZES = new int[] {
        50, 75, 100, 150, 200
    };
    private WebSettings.TextSize[] ZOOM_SIZES_API10 = new WebSettings.TextSize[] {
        WebSettings.TextSize.SMALLEST, WebSettings.TextSize.SMALLER,
        WebSettings.TextSize.NORMAL, WebSettings.TextSize.LARGER,
        WebSettings.TextSize.LARGEST
    };

    private WebViewClient mWebViewClient = new WebViewClient() {
        @Override
        public void onPageFinished(WebView webView, String url) {
            setContentShown(true);
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (handleUrlLoad(url)) {
                return true;
            }
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
            return true;
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (hasErrorView()) {
            return;
        }

        ActionBar actionBar = getSupportActionBar();
        // Inflate from action bar context to get the correct foreground color
        // when using the DarkActionBar theme
        LayoutInflater inflater = LayoutInflater.from(actionBar.getThemedContext());
        setContentView(inflater.inflate(R.layout.web_viewer, null));

        setContentShown(false);
        setupWebView();
    }

    @SuppressWarnings("deprecation")
    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        mWebView = (WebView) findViewById(R.id.web_view);

        WebSettings s = mWebView.getSettings();
        s.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NORMAL);
        s.setAllowFileAccess(true);
        s.setBuiltInZoomControls(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            s.setDisplayZoomControls(false);
        }
        s.setLightTouchEnabled(true);
        s.setLoadsImagesAutomatically(true);
        s.setSupportZoom(true);
        s.setJavaScriptEnabled(true);
        s.setUseWideViewPort(false);
        applyDefaultTextSize(s);

        mWebView.setBackgroundColor(ThemeUtils.getWebViewBackgroundColor(Gh4Application.THEME));
        mWebView.setWebViewClient(mWebViewClient);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            menu.removeItem(R.id.search);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.search) {
            doSearch();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("deprecation")
    @TargetApi(11)
    private void doSearch() {
        if (mWebView != null) {
            mWebView.showFindDialog(null, true);
        }
    }

    @SuppressWarnings("deprecation")
    private void applyDefaultTextSize(WebSettings s) {
        SharedPreferences prefs = getSharedPreferences(SettingsActivity.PREF_NAME, MODE_PRIVATE);
        int initialZoomLevel = prefs.getInt(SettingsActivity.KEY_TEXT_SIZE, -1);
        if (initialZoomLevel < 0 || initialZoomLevel >= ZOOM_SIZES.length) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            s.setTextZoom(ZOOM_SIZES[initialZoomLevel]);
        } else {
            s.setTextSize(ZOOM_SIZES_API10[initialZoomLevel]);
        }
    }

    protected void loadUnthemedHtml(String html) {
        if (Gh4Application.THEME == R.style.DefaultTheme) {
            html = "<style type=\"text/css\">" +
                    "body { color: #A3A3A5 !important }" +
                    "a { color: #4183C4 !important }</style><body>" +
                    html + "</body>";
        }
        loadThemedHtml(html);
    }

    protected void loadThemedHtml(String html) {
        mWebView.loadDataWithBaseURL("file:///android_asset/", html, null, "utf-8", null);
    }

    protected boolean handleUrlLoad(String url) {
        return false;
    }
}
