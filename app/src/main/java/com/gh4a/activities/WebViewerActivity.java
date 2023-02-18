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
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.gh4a.BaseActivity;
import com.gh4a.BuildConfig;
import com.gh4a.R;
import com.gh4a.fragment.SettingsFragment;
import com.gh4a.utils.FileUtils;
import com.gh4a.utils.HtmlUtils;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.StringUtils;
import com.gh4a.utils.UiUtils;
import com.gh4a.widget.FindActionModeCallback;
import com.gh4a.widget.SwipeRefreshLayout;

import java.io.IOException;
import java.util.ArrayList;

@SuppressLint("AddJavascriptInterface")
public abstract class WebViewerActivity extends BaseActivity implements
        SwipeRefreshLayout.ChildScrollDelegate, View.OnTouchListener {

    protected final Point mLastTouchDown = new Point();

    private WebView mWebView;
    private WebView mPrintWebView;
    private boolean mStarted;
    private boolean mHasData;
    private boolean mRequiresNativeClient;
    private boolean mPageFinished;
    private boolean mRenderingDone;
    private final Handler mHandler = new Handler();

    public static final String DARK_CSS_THEME = "dark";
    public static final String LIGHT_CSS_THEME = "light";
    public static final String PRINT_CSS_THEME = "print";

    private static final ArrayList<String> sLanguagePlugins = new ArrayList<>();
    private static final int[] ZOOM_SIZES = { 50, 75, 100, 150, 200 };

    private final WebViewClient mWebViewClient = new WebViewClient() {
        @Override
        public void onPageFinished(WebView view, String url) {
            mPageFinished = true;
            showContentIfDone();
        }

        @Override
        @TargetApi(24)
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            if (mStarted) {
                handleUrlLoad(request.getUrl());
            }
            return true;
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (mStarted) {
                handleUrlLoad(Uri.parse(url));
            }
            return true;
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (BuildConfig.DEBUG) {
            WebView.setWebContentsDebuggingEnabled(true);
        }

        setContentView(R.layout.web_viewer);

        setContentShown(false);
        setupWebView();
        setChildScrollDelegate(this);
    }

    @Override
    public boolean displayDetachAction() {
        return true;
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

    private void setupWebView() {
        mWebView = findViewById(R.id.web_view);

        WebSettings s = mWebView.getSettings();
        initWebViewSettings(s);
        addCommonJavascriptInterfaces(mWebView);

        SharedPreferences prefs = getSharedPreferences(SettingsFragment.PREF_NAME, MODE_PRIVATE);
        int initialZoomLevel = prefs.getInt(SettingsFragment.KEY_TEXT_SIZE, 2);
        if (initialZoomLevel >= 0 && initialZoomLevel < ZOOM_SIZES.length) {
            s.setTextZoom(ZOOM_SIZES[initialZoomLevel]);
        }

        mWebView.setBackgroundColor(Color.TRANSPARENT);
        mWebView.setWebViewClient(mWebViewClient);
        mWebView.setOnTouchListener(this);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void initWebViewSettings(WebSettings s) {
        s.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NORMAL);
        s.setAllowFileAccess(false);
        s.setBuiltInZoomControls(true);
        s.setDisplayZoomControls(false);
        s.setLoadsImagesAutomatically(true);
        s.setSupportZoom(true);
        s.setJavaScriptEnabled(true);
        s.setUseWideViewPort(false);
    }

    private void addCommonJavascriptInterfaces(WebView webView) {
        webView.addJavascriptInterface(new Base64JavascriptInterface(), "Base64");
        webView.addJavascriptInterface(new HtmlUtilsJavascriptInterface(), "HtmlUtils");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (mHasData) {
            getMenuInflater().inflate(R.menu.print_menu, menu);
            if (mPrintWebView != null) {
                menu.findItem(R.id.print).setEnabled(false);
            }
        } else {
            menu.removeItem(R.id.browser);
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
        } else if (itemId == R.id.print) {
            doPrint();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            mLastTouchDown.set((int) event.getX(), (int) event.getY());
        }
        return false;
    }

    private void doSearch() {
        if (mWebView == null) {
            return;
        }
        FindActionModeCallback findAction = new FindActionModeCallback(mWebView.getContext());
        startSupportActionMode(findAction);
        findAction.setWebView(mWebView);
        findAction.showSoftInput();
    }

    private void doPrint() {
        if (handlePrintRequest()) {
            return;
        }

        mPrintWebView = new WebView(this);
        initWebViewSettings(mPrintWebView.getSettings());
        addCommonJavascriptInterfaces(mPrintWebView);

        if (mRequiresNativeClient) {
            mPrintWebView.addJavascriptInterface(new PrintNativeClientJavascriptInterface(), "NativeClient");
        } else {
            mPrintWebView.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView webView, String url) {
                    doPrintHtml();
                }
            });
        }
        final String html = generateHtml(PRINT_CSS_THEME, true);
        mPrintWebView.loadDataWithBaseURL("file:///android_asset/", html, null, "utf-8", null);
        supportInvalidateOptionsMenu();
    }

    private void doPrintHtml() {
        if (!isFinishing()) {
            final String title = getDocumentTitle();
            PrintManager printManager = (PrintManager) getSystemService(Context.PRINT_SERVICE);
            PrintDocumentAdapter printAdapter = mPrintWebView.createPrintDocumentAdapter(title);
            printManager.print(title, printAdapter, new PrintAttributes.Builder().build());
        }
        mPrintWebView = null;
        supportInvalidateOptionsMenu();
    }

    @Override
    protected void setContentShown(boolean shown) {
        super.setContentShown(shown);
        if (!shown) {
            mHasData = false;
            mRenderingDone = false;
            mPageFinished = false;
            supportInvalidateOptionsMenu();
        }
    }

    private void showContentIfDone() {
        if (mPageFinished && (mRenderingDone || !mRequiresNativeClient)) {
            applyLineWrapping(shouldWrapLines());
            setContentShown(true);
        }
    }

    protected boolean shouldWrapLines() {
        return getPrefs().getBoolean("line_wrapping", false);
    }

    private void setLineWrapping(boolean enabled) {
        getPrefs().edit().putBoolean("line_wrapping", enabled).apply();
    }

    private void applyLineWrapping(boolean enabled) {
        mWebView.loadUrl("javascript:applyLineWrapping(" + enabled + ")");
    }

    protected void handleUrlLoad(Uri uri) {
        IntentUtils.openLinkInternallyOrExternally(this, uri);
    }

    protected void onLineTouched(int line, int x, int y) {
    }

    protected void onDataReady() {
        final String cssTheme = getResources().getBoolean(R.bool.is_dark_theme)
                ? DARK_CSS_THEME : LIGHT_CSS_THEME;
        final String html = generateHtml(cssTheme, false);
        if (mRequiresNativeClient) {
            mWebView.addJavascriptInterface(new NativeClientJavascriptInterface(), "NativeClient");
        }
        mWebView.loadDataWithBaseURL("file:///android_asset/", html, null, "utf-8", null);
        mHasData = true;
        supportInvalidateOptionsMenu();
    }

    private void loadLanguagePluginListIfNeeded() {
        if (!sLanguagePlugins.isEmpty()) {
            return;
        }

        AssetManager am = getAssets();
        try {
            String[] files = am.list("prettify-plugins");
            for (String filename : files) {
                if (filename.endsWith(".js")) {
                    int dotPosition = filename.lastIndexOf('.');
                    sLanguagePlugins.add("prettify-plugins/" + filename.substring(0, dotPosition));
                }
            }
        } catch (IOException e) {
            // retry next time
            sLanguagePlugins.clear();
        }
    }

    protected String generateMarkdownHtml(String base64Data,
            String repoOwner, String repoName, String ref, String folderPath,
            String cssTheme, boolean addTitleHeader) {
        String title = addTitleHeader ? getDocumentTitle() : null;
        StringBuilder content = new StringBuilder();
        content.append("<html><head><title>");
        if (title != null) {
            content.append(title);
        }
        content.append("</title>");
        HtmlUtils.writeScriptInclude(content, "showdown");
        HtmlUtils.writeCssInclude(content, "markdown", cssTheme);
        content.append("</head>");

        content.append("<body>");
        if (title != null) {
            content.append("<h2>").append(title).append("</h2>");
        }
        content.append("<div id='content'></div>");

        content.append("<script>");
        content.append("var text = Base64.decode('");
        content.append(base64Data.replaceAll("\\n", ""));
        content.append("');\n");
        content.append("var converter = new showdown.Converter();\n");
        content.append("converter.setFlavor('github');\n");
        content.append("var html = converter.makeHtml(text);\n");
        if (repoOwner != null && repoName != null) {
            String actualRef = ref == null ? "master" : ref;
            content.append(String.format("html = HtmlUtils.rewriteRelativeUrls(html, '%s', '%s', '%s', '%s');\n",
                    repoOwner, repoName, actualRef, folderPath));
        }
        content.append("document.getElementById('content').innerHTML = html;");
        content.append("</script>");

        content.append("</body></html>");

        mRequiresNativeClient = false;
        return content.toString();
    }

    protected String generateCodeHtml(String data, String fileName,
                int highlightStart, int highlightEnd,
                String cssTheme, boolean addTitleHeader) {
        String title = addTitleHeader ? getDocumentTitle() : null;
        StringBuilder content = new StringBuilder();
        content.append("<html><head><title>");
        if (title != null) {
            content.append(title);
        }
        content.append("</title>");
        HtmlUtils.writeScriptInclude(content, "codeutils");

        HtmlUtils.writeCssInclude(content, "prettify", cssTheme);
        HtmlUtils.writeScriptInclude(content, "prettify");
        loadLanguagePluginListIfNeeded();
        for (String plugin : sLanguagePlugins) {
            HtmlUtils.writeScriptInclude(content, plugin);
        }
        content.append("</head>");
        content.append("<body onload='prettyPrint(function() { highlightLines(");
        content.append(highlightStart).append(",").append(highlightEnd).append("); ");
        content.append("addClickListeners(); NativeClient.onRenderingDone(); })'");
        content.append(" onresize='scrollToHighlight();'>");
        if (title != null) {
            content.append("<h2>").append(title).append("</h2>");
        }
        content.append("<pre id='content' class='prettyprint linenums lang-");
        content.append(prettifyLanguageCodeFor(fileName, data)).append("'>");

        content.append(TextUtils.htmlEncode(data));
        content.append("</pre></body></html>");

        mRequiresNativeClient = true;
        return content.toString();
    }

    private String prettifyLanguageCodeFor(String fileName, String fileContent) {
        if (FileUtils.isMarkdown(fileName)) {
            // Markdown files can have HTML code in them, so this is the best compromise we can do
            // to overcome the absence of Markdown syntax highlighting in Prettify library
            return "html";
        }

        String extension = FileUtils.getFileExtension(fileName);
        if (!StringUtils.isBlank(extension)) {
            return extension;
        }

        boolean hasShebangLine = fileContent.startsWith("#!");
        return hasShebangLine
                ? ""      // default prettify code highlighting
                : "txt";  // plain text, no highlighting
    }

    protected static String wrapWithMarkdownStyling(String html, String cssTheme, String title) {
        StringBuilder content = new StringBuilder();
        HtmlUtils.writeCssInclude(content, "markdown", cssTheme);
        content.append("<body>");
        if (title != null) {
            content.append("<h2>").append(title).append("</h2>");
        }
        content.append(html);
        content.append("</body>");
        return content.toString();
    }

    @Override
    protected abstract boolean canSwipeToRefresh();

    protected boolean handlePrintRequest() {
        return false;
    }
    protected abstract String generateHtml(String cssTheme, boolean addTitleHeader);
    protected abstract String getDocumentTitle();

    private static class Base64JavascriptInterface {
        @JavascriptInterface
        public String decode(String base64) {
            return StringUtils.fromBase64(base64);
        }
    }

    private static class HtmlUtilsJavascriptInterface {
        @JavascriptInterface
        public String rewriteRelativeUrls(final String html, final String repoUser,
                final String repoName, final String branch, final String folderPath) {
            return HtmlUtils.rewriteRelativeUrls(html, repoUser, repoName, branch, folderPath);
        }
    }

    private class NativeClientJavascriptInterface {
        @JavascriptInterface
        public void onLineTouched(final int line) {
            mHandler.post(() -> WebViewerActivity.this.onLineTouched(line, mLastTouchDown.x, mLastTouchDown.y));
        }

        @JavascriptInterface
        public void onRenderingDone() {
            mHandler.post(() -> {
                mRenderingDone = true;
                showContentIfDone();
            });
        }
    }

    private class PrintNativeClientJavascriptInterface {
        @JavascriptInterface
        public void onLineTouched(int line) {
        }

        @JavascriptInterface
        public void onRenderingDone() {
            mHandler.post(() -> {
                mPrintWebView.loadUrl("javascript:applyLineWrapping(true)");
                doPrintHtml();
            });
        }
    }
}
