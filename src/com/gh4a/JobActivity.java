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
package com.gh4a;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import com.gh4a.holder.BreadCrumbHolder;
import com.gh4a.utils.StringUtils;
import com.github.api.v2.schema.Job.Type;

public class JobActivity extends BaseActivity {

    private LoadingDialog mLoadingDialog;
    private String mCompany;
    private String mLocation;
    private String mCompanyUrl;
    private String mTitle;
    private String mUrl;
    private String mId;
    private String mCompanyLogo;
    private String mType;
    private String mDescription;
    private String mHowToApply;
    
    /**
     * Called when the activity is first created.
     * 
     * @param savedInstanceState the saved instance state
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.web_viewer);
        setUpActionBar();
        
        mCompany = getIntent().getStringExtra(Constants.Job.COMPANY);
        mLocation = getIntent().getStringExtra(Constants.Job.LOCATION);
        mCompanyUrl = getIntent().getStringExtra(Constants.Job.COMPANY_URL);
        mTitle = getIntent().getStringExtra(Constants.Job.TITLE);
        mUrl = getIntent().getStringExtra(Constants.Job.URL);
        mId = getIntent().getStringExtra(Constants.Job.ID);
        mCompanyLogo = getIntent().getStringExtra(Constants.Job.COMPANY_LOGO);
        mType = getIntent().getStringExtra(Constants.Job.TYPE);
        mDescription = getIntent().getStringExtra(Constants.Job.DESCRIPTION);
        mHowToApply = getIntent().getStringExtra(Constants.Job.HOW_TO_APPLY);

        TextView tvHistoryFile = (TextView) findViewById(R.id.tv_view);
        tvHistoryFile.setVisibility(View.GONE);
        
        TextView tvViewRaw = (TextView) findViewById(R.id.tv_view_raw);
        tvViewRaw.setVisibility(View.GONE);

        TextView tvDownload = (TextView) findViewById(R.id.tv_download);
        tvDownload.setVisibility(View.GONE);
        
        TextView tvViewInBrowser = (TextView) findViewById(R.id.tv_in_browser);
        if (!StringUtils.isBlank(mUrl)) {
            tvViewInBrowser.setVisibility(View.VISIBLE);
            tvViewInBrowser.setOnClickListener(new OnClickListener() {
                
                @Override
                public void onClick(View arg0) {
                    getApplicationContext().openBrowser(JobActivity.this, mUrl);
                }
            });
        }
        else {
            tvViewInBrowser.setVisibility(View.GONE);
        }
        
        setBreadCrumb();

        mLoadingDialog = LoadingDialog.show(this, true, true);
        
        fillData();
    }
    
    protected void setBreadCrumb() {
        BreadCrumbHolder[] breadCrumbHolders = new BreadCrumbHolder[2];

        BreadCrumbHolder b = new BreadCrumbHolder();
        b.setLabel(getResources().getString(R.string.explore));
        b.setTag(Constants.EXPLORE);
        breadCrumbHolders[0] = b;
        
        b = new BreadCrumbHolder();
        b.setLabel(getResources().getString(R.string.jobs));
        b.setTag(Constants.Job.JOB);
        breadCrumbHolders[1] = b;
        
        createBreadcrumb(mTitle, breadCrumbHolders);
    }

    private void fillData() {
        
        WebView webView = (WebView) findViewById(R.id.web_view);

        WebSettings s = webView.getSettings();
        s.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NORMAL);
        s.setUseWideViewPort(false);
        s.setAllowFileAccess(true);
        s.setBuiltInZoomControls(false);
        s.setLightTouchEnabled(true);
        s.setLoadsImagesAutomatically(true);
        s.setPluginsEnabled(false);
        s.setSupportZoom(true);
        s.setSupportMultipleWindows(true);
        s.setJavaScriptEnabled(true);

        webView.setWebViewClient(webViewClient);
        
        StringBuilder sb = new StringBuilder();
        sb.append("<h3 style='margin:2px'>").append(mTitle).append("</h3>");
        
        sb.append("<h4 style='margin:2px'>").append(mCompany).append(" - ");
        if (Type.FULL_TIME.value().equals(mType)) {
            sb.append("<span style='color:#1D9A00'>").append(mType).append("</span>");
        }
        else {
            sb.append(mType);
        }
        sb.append("</h4>");
        
        sb.append("<h4 style='margin:2px'>" + mLocation + "</h4>");
        
        if (!StringUtils.isBlank(mCompanyLogo)) {
            sb.append("<div style='margin:10px 0 10px 0'><img src='" + mCompanyLogo + "' border='0'/></div>");
        }
        
        sb.append(mDescription);
        
        if (!StringUtils.isBlank(mHowToApply)) {
            sb.append("<h3 style='margin:2px'>").append("How to apply").append("</h3>");
            sb.append(mHowToApply);
        }
        
        webView.loadDataWithBaseURL("http://jobs.github.com", sb.toString(), "text/html", "utf-8", null);
    }

    /** The web view client. */
    private WebViewClient webViewClient = new WebViewClient() {

        @Override
        public void onPageFinished(WebView webView, String url) {
            if (mLoadingDialog != null && mLoadingDialog.isShowing()) {
                mLoadingDialog.dismiss();
            }
        }
        
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
            return true;
        }
    };

}
