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
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.gh4a.BaseActivity;
import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.fragment.SettingsFragment;
import com.gh4a.utils.FileUtils;
import com.gh4a.utils.ThemeUtils;
import com.gh4a.utils.UiUtils;
import com.gh4a.widget.SwipeRefreshLayout;

import java.io.IOException;
import java.util.ArrayList;

public abstract class WebViewerActivity extends BaseActivity implements
        SwipeRefreshLayout.ChildScrollDelegate {
    protected WebView mWebView;
    private boolean mStarted;

    private static ArrayList<String> sLanguagePlugins = new ArrayList<>();

    private int[] ZOOM_SIZES = new int[] {
        50, 75, 100, 150, 200
    };
    @SuppressWarnings("deprecation")
    private WebSettings.TextSize[] ZOOM_SIZES_API10 = new WebSettings.TextSize[] {
        WebSettings.TextSize.SMALLEST, WebSettings.TextSize.SMALLER,
        WebSettings.TextSize.NORMAL, WebSettings.TextSize.LARGER,
        WebSettings.TextSize.LARGEST
    };

    private WebViewClient mWebViewClient = new WebViewClient() {
        @Override
        public void onPageFinished(WebView webView, String url) {
            applyLineWrapping(shouldWrapLines());
            setContentShown(true);
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (mStarted && !handleUrlLoad(url)) {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    // ignore
                } catch (SecurityException e) {
                    // some apps (namely the Wikipedia one) have intent filters set
                    // for the VIEW action for internal, non-exported activities
                    // -> ignore
                }
            }
            return true;
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if ((getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
                WebView.setWebContentsDebuggingEnabled(true);
            }
        }

        // We also use the dark CAB for the light theme, so we have to inflate
        // the WebView using a dark theme
        Context inflateContext = new ContextThemeWrapper(this, R.style.DarkTheme);
        setContentView(LayoutInflater.from(inflateContext).inflate(R.layout.web_viewer, null));

        setContentShown(false);
        setupWebView();
        setChildScrollDelegate(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mStarted = true;
    }

    @Override
    protected void onStop() {
        mStarted = false;
        super.onStop();
    }

    @Override
    public boolean canChildScrollUp() {
        return UiUtils.canViewScrollUp(mWebView);
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
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem wrapItem = menu.findItem(R.id.wrap);
        if (wrapItem != null) {
            wrapItem.setChecked(shouldWrapLines());
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.search) {
            doSearch();
            return true;
        } else if (itemId == R.id.wrap) {
            boolean newState = !shouldWrapLines();
            item.setChecked(newState);
            setLineWrapping(newState);
            applyLineWrapping(newState);
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
        SharedPreferences prefs = getSharedPreferences(SettingsFragment.PREF_NAME, MODE_PRIVATE);
        int initialZoomLevel = prefs.getInt(SettingsFragment.KEY_TEXT_SIZE, -1);
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
        if (Gh4Application.THEME == R.style.DarkTheme) {
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

    private boolean shouldWrapLines() {
        return getPrefs().getBoolean("line_wrapping", false);
    }

    private void setLineWrapping(boolean enabled) {
        getPrefs().edit().putBoolean("line_wrapping", enabled).apply();
    }

    private void applyLineWrapping(boolean enabled) {
        mWebView.loadUrl("javascript:applyLineWrapping(" + enabled + ")");
    }

    protected boolean handleUrlLoad(String url) {
        return false;
    }

    protected void loadCode(String data, String fileName) {
        loadCode(data, fileName, null, null, null, -1, -1);
    }

    private void loadLanguagePluginListIfNeeded() {
        if (!sLanguagePlugins.isEmpty()) {
            return;
        }

        AssetManager am = getAssets();
        try {
            String[] files = am.list("");
            for (String f : files) {
                if (f.startsWith("lang-")) {
                    int pos = f.lastIndexOf('.');
                    if (pos > 0 && TextUtils.equals(f.substring(pos + 1), "js")) {
                        sLanguagePlugins.add(f.substring(0, pos));
                    }
                }
            }
        } catch (IOException e) {
            // retry next time
            sLanguagePlugins.clear();
        }
    }

    protected void loadCode(String data, String fileName,
            String repoOwner, String repoName, String ref,
            int highlightStart, int highlightEnd) {
        String ext = FileUtils.getFileExtension(fileName);
        boolean isMarkdown = FileUtils.isMarkdown(fileName);

        StringBuilder content = new StringBuilder();
        content.append("<html><head><title></title>");
        writeScriptInclude(content, "codeutils");

        if (isMarkdown) {
            writeScriptInclude(content, "showdown");
            writeCssInclude(content, "markdown");
            content.append("</head>");
            content.append("<body>");
            content.append("<div id='content'>");
        } else {
            writeCssInclude(content, "prettify");
            writeScriptInclude(content, "prettify");
            loadLanguagePluginListIfNeeded();
            for (String plugin : sLanguagePlugins) {
                writeScriptInclude(content, plugin);
            }
            content.append("</head>");
            content.append("<body onload='prettyPrint(function() { highlightLines(");
            content.append(highlightStart).append(",").append(highlightEnd).append("); })'");
            content.append(" onresize='scrollToHighlight();'>");
            content.append("<pre id='content' class='prettyprint linenums lang-");
            content.append(ext).append("'>");
        }

        content.append(TextUtils.htmlEncode(data));

        if (isMarkdown) {
            content.append("</div>");

            content.append("<script>");
            if (repoOwner != null && repoName != null) {
                content.append("var GitHub = new Object();");
                content.append("GitHub.nameWithOwner = \"");
                content.append(repoOwner).append("/").append(repoName).append("\";");
                if (ref != null) {
                    content.append("GitHub.branch = \"").append(ref).append("\";");
                }
            }
            content.append("var text = document.getElementById('content').innerHTML;");
            content.append("var converter = new Showdown.converter();");
            content.append("var html = converter.makeHtml(text);");
            content.append("document.getElementById('content').innerHTML = html;");
            content.append("</script>");
        } else {
            content.append("</pre>");
        }

        content.append("</body></html>");

        loadThemedHtml(content.toString());
    }

    protected static void writeScriptInclude(StringBuilder builder, String scriptName) {
        builder.append("<script src='file:///android_asset/");
        builder.append(scriptName);
        builder.append(".js' type='text/javascript'></script>");
    }

    protected static void writeCssInclude(StringBuilder builder, String cssType) {
        builder.append("<link href='file:///android_asset/");
        builder.append(cssType);
        builder.append("-");
        builder.append(ThemeUtils.getCssTheme(Gh4Application.THEME));
        builder.append(".css' rel='stylesheet' type='text/css'/>");
    }

    @Override
    protected abstract boolean canSwipeToRefresh();
}
