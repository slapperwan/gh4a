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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

import com.gh4a.adapter.TrendAdapter;
import com.gh4a.feeds.TrendHandler;
import com.gh4a.holder.Trend;
import com.github.api.v2.services.constant.ApplicationConstants;

public class TrendActivity extends BaseActivity {
    
    private static final String TODAY = "http://github-trends.oscardelben.com/explore/today.xml";
    private static final String WEEK = "http://github-trends.oscardelben.com/explore/week.xml";
    private static final String MONTH = "http://github-trends.oscardelben.com/explore/month.xml";
    private static final String FOREVER = "http://github-trends.oscardelben.com/explore/forever.xml";
    
    private LoadingDialog mLoadingDialog;
    private Button mBtnToday;
    private Button mBtnWeek;
    private Button mBtnMonth;
    private Button mBtnForever;
    /**
     * Called when the activity is first created.
     * 
     * @param savedInstanceState the saved instance state
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.trends);
        setUpActionBar();

        createBreadcrumb(getResources().getString(R.string.trend));
        setUpBottomButtons();
        setEnableButtons(new boolean[] {false, true, true, true});
        
        new LoadTrendsTask(this).execute(TODAY);
    }

    private void setUpBottomButtons() {
        mBtnToday = (Button) findViewById(R.id.btn_today);
        mBtnToday.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                new LoadTrendsTask(TrendActivity.this).execute(TODAY);
                setEnableButtons(new boolean[] {false, true, true, true});
            }
        });
        
        mBtnWeek = (Button) findViewById(R.id.btn_week);
        mBtnWeek.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                new LoadTrendsTask(TrendActivity.this).execute(WEEK);
                setEnableButtons(new boolean[] {true, false, true, true});
            }
        });
        
        mBtnMonth = (Button) findViewById(R.id.btn_month);
        mBtnMonth.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                new LoadTrendsTask(TrendActivity.this).execute(MONTH);
                setEnableButtons(new boolean[] {true, true, false, true});
            }
        });
        
        mBtnForever = (Button) findViewById(R.id.btn_forever);
        mBtnForever.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                new LoadTrendsTask(TrendActivity.this).execute(FOREVER);
                setEnableButtons(new boolean[] {true, true, true, false});
            }
        });
    }
    
    private void setEnableButtons(boolean...enabled) {
        mBtnToday.setEnabled(enabled[0]);
        mBtnWeek.setEnabled(enabled[1]);
        mBtnMonth.setEnabled(enabled[2]);
        mBtnForever.setEnabled(enabled[3]);
    }
    
    private static class LoadTrendsTask extends
            AsyncTask<String, Void, List<Trend>> {

        /** The target. */
        private WeakReference<TrendActivity> mTarget;

        /** The exception. */
        private boolean mException;

        /**
         * Instantiates a new load tree list task.
         * 
         * @param activity the activity
         */
        public LoadTrendsTask(TrendActivity activity) {
            mTarget = new WeakReference<TrendActivity>(activity);
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#doInBackground(Params[])
         */
        @Override
        protected List<Trend> doInBackground(String... params) {
            if (mTarget.get() != null) {
                BufferedInputStream bis = null;
                try {
                    URL url = new URL(params[0]);
                    HttpURLConnection request = (HttpURLConnection) url.openConnection();
                    
                    request.setRequestMethod("GET");
                    request.setDoOutput(true);
                    
                    if (ApplicationConstants.CONNECT_TIMEOUT > -1) {
                        request.setConnectTimeout(ApplicationConstants.CONNECT_TIMEOUT);
                    }

                    if (ApplicationConstants.READ_TIMEOUT > -1) {
                        request.setReadTimeout(ApplicationConstants.READ_TIMEOUT);
                    }
                    request.connect();
                    bis = new BufferedInputStream(request.getInputStream());
                    
                    SAXParserFactory factory = SAXParserFactory.newInstance();
                    SAXParser parser = factory.newSAXParser();
                    TrendHandler handler = new TrendHandler();
                    parser.parse(bis, handler);
                    return handler.getTrends();
                }
                catch (MalformedURLException e) {
                    Log.e(Constants.LOG_TAG, e.getMessage(), e);
                    mException = true;
                    return null;
                }
                catch (IOException e) {
                    Log.e(Constants.LOG_TAG, e.getMessage(), e);
                    mException = true;
                    return null;
                }
                catch (ParserConfigurationException e) {
                    Log.e(Constants.LOG_TAG, e.getMessage(), e);
                    mException = true;
                    return null;
                }
                catch (SAXException e) {
                    Log.e(Constants.LOG_TAG, e.getMessage(), e);
                    mException = true;
                    return null;
                }
                finally {
                    if (bis != null) {
                        try {
                            bis.close();
                        }
                        catch (IOException e) {
                            Log.e(Constants.LOG_TAG, e.getMessage(), e);
                        }
                    }
                }
            }
            else {
                return null;
            }
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPreExecute()
         */
        @Override
        protected void onPreExecute() {
            if (mTarget.get() != null) {
                mTarget.get().mLoadingDialog = LoadingDialog.show(mTarget.get(), true, true);
            }
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(List<Trend> result) {
            if (mTarget.get() != null) {
                if (mException) {
                    mTarget.get().showError();
                }
                else {
                    mTarget.get().mLoadingDialog.dismiss();
                    mTarget.get().fillData(result);
                }
            }
        }
    }

    private void fillData(List<Trend> trends) {
        if (trends != null) {
            ListView listView = (ListView) findViewById(R.id.main_content);
            TrendAdapter adapter = new TrendAdapter(this, trends);
            listView.setAdapter(adapter);
            listView.setOnItemClickListener(new OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                    Trend trend = (Trend) adapterView.getAdapter().getItem(position);
                    String[] repos = trend.getTitle().split("/");
                    getApplicationContext().openRepositoryInfoActivity(TrendActivity.this,
                            repos[0].trim(), repos[1].trim());
                }
            });
        }
    }
}
