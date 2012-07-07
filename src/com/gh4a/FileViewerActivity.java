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

import java.io.IOException;
import java.lang.ref.WeakReference;

import org.eclipse.egit.github.core.Content;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.ContentService;
import org.eclipse.egit.github.core.util.EncodingUtils;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import com.gh4a.utils.FileUtils;
import com.gh4a.utils.StringUtils;

public class FileViewerActivity extends BaseActivity {

    protected String mRepoOwner;
    protected String mRepoName;
    private String mPath;
    private String mRef;
    private String mSha;
    private String mName;
    private Content mContent;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.web_viewer);
        setUpActionBar();

        mRepoOwner = getIntent().getStringExtra(Constants.Repository.REPO_OWNER);
        mRepoName = getIntent().getStringExtra(Constants.Repository.REPO_NAME);
        mPath = getIntent().getStringExtra(Constants.Object.PATH);
        mRef = getIntent().getStringExtra(Constants.Object.REF);
        mSha = getIntent().getStringExtra(Constants.Object.OBJECT_SHA);
        mName = getIntent().getStringExtra(Constants.Object.NAME);
        
        TextView tvViewInBrowser = (TextView) findViewById(R.id.tv_in_browser);
        tvViewInBrowser.setVisibility(View.GONE);
        
        TextView tvHistoryFile = (TextView) findViewById(R.id.tv_view);
        tvHistoryFile.setText(getResources().getString(R.string.object_view_history));
        tvHistoryFile.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View view) {
                Intent intent = new Intent().setClass(FileViewerActivity.this, CommitHistoryActivity.class);
                intent.putExtra(Constants.Repository.REPO_OWNER, mRepoOwner);
                intent.putExtra(Constants.Repository.REPO_NAME, mRepoName);
                intent.putExtra(Constants.Object.OBJECT_SHA, mSha);
                intent.putExtra(Constants.Object.PATH, mPath);
                
                startActivity(intent);
            }
        });
        
        
        TextView tvViewRaw = (TextView) findViewById(R.id.tv_view_raw);
        tvViewRaw.setVisibility(View.VISIBLE);
        tvViewRaw.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View view) {
                TextView tvViewRaw = (TextView) view;
                if ("Raw".equals(tvViewRaw.getText())) {
                    new LoadContentTask(FileViewerActivity.this).execute(false);
                }
                else {
                    new LoadContentTask(FileViewerActivity.this).execute(true);
                }
            }
        });
        
        TextView tvDownload = (TextView) findViewById(R.id.tv_download);
        tvDownload.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View view) {
                String filename = mPath;
                int idx = mPath.lastIndexOf("/");
                
                if (idx != -1) {
                    filename = filename.substring(filename.lastIndexOf("/") + 1, filename.length());
                }

                String data = new String(EncodingUtils.fromBase64(mContent.getContent()));
                boolean success = FileUtils.save(filename, data);
                if (success) {
                    showMessage("File saved at " + Environment.getExternalStorageDirectory().getAbsolutePath() + "/download/" + filename, false);
                }
                else {
                    showMessage("Unable to save the file", false);
                }
            }
        });
        
        new LoadContentTask(this).execute(true);
    }

    private static class LoadContentTask extends AsyncTask<Boolean, Integer, Content> {

        private WeakReference<FileViewerActivity> mTarget;
        private boolean mException;
        private boolean mShowInBrowser;
        private boolean mHighlight;

        public LoadContentTask(FileViewerActivity activity) {
            mTarget = new WeakReference<FileViewerActivity>(activity);
        }

        @Override
        protected Content doInBackground(Boolean... params) {
            if (mTarget.get() != null) {
                mHighlight = params[0];
                try {
                    FileViewerActivity activity = mTarget.get();
                    GitHubClient client = new GitHubClient();
                    client.setOAuth2Token(mTarget.get().getAuthToken());
                    ContentService contentService = new ContentService(client);
                    return contentService.getContent(new RepositoryId(activity.mRepoOwner, activity.mRepoName), 
                            activity.mPath, activity.mRef);
                }
                catch (IOException e) {
                    Log.e(Constants.LOG_TAG, e.getMessage(), e);
                    mException = true;
                    return null;
                }
            }
            else {
                return null;
            }
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onPostExecute(Content result) {
            if (mTarget.get() != null) {
                if (mException) {
                    mTarget.get().showError();
                }
                else {
                    if (mShowInBrowser) {
                        String url = "https://github.com/" + mTarget.get().mRepoOwner + "/"
                                + mTarget.get().mRepoName + "/blob/" + mTarget.get().mRef + "/"
                                + mTarget.get().mPath;
                        mTarget.get().getApplicationContext().openBrowser(mTarget.get(), url);
                        mTarget.get().finish();
                    }
                    else {
                        mTarget.get().mContent = result;
                        try {
                            mTarget.get().fillData(result, mHighlight);
                        } catch (IOException e) {
                            mTarget.get().showError();
                        }
                    }
                }
            }
        }
    }

    protected void fillData(Content content, boolean highlight) throws IOException {
        String data = new String(EncodingUtils.fromBase64(content.getContent()));
        TextView tvViewRaw = (TextView) findViewById(R.id.tv_view_raw);
        if (highlight) {
            tvViewRaw.setText("Raw");
        }
        else {
            tvViewRaw.setText("Highlight");
        }
        
        WebView webView = (WebView) findViewById(R.id.web_view);

        WebSettings s = webView.getSettings();
        s.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NORMAL);
        s.setUseWideViewPort(false);
        s.setAllowFileAccess(true);
        s.setBuiltInZoomControls(true);
        s.setLightTouchEnabled(true);
        s.setLoadsImagesAutomatically(true);
        s.setPluginsEnabled(false);
        s.setSupportZoom(true);
        s.setSupportMultipleWindows(true);
        s.setJavaScriptEnabled(true);

        webView.getSettings().setUseWideViewPort(true);

        String highlighted = StringUtils.highlightSyntax(data, highlight, mName);
        webView.setWebViewClient(webViewClient);
        webView.loadDataWithBaseURL("file:///android_asset/", highlighted, "text/html", "utf-8", "");
    }

    private WebViewClient webViewClient = new WebViewClient() {

        @Override
        public void onPageFinished(WebView webView, String url) {
        }
        
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
            return true;
        }
    };

}
