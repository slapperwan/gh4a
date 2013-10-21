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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;

import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.gh4a.adapter.DiscussionCommentFeedAdapter;
import com.gh4a.feeds.DiscussionHandler;
import com.gh4a.holder.Feed;

public class DiscussionActivity extends BaseActivity {
    
    private LoadingDialog mLoadingDialog;
    private String mUrl;
    private LinearLayout mHeader;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.discussion);

        mUrl = getIntent().getStringExtra(Constants.Discussion.URL);
        
//        mListView = (ListView) findViewById(R.id.list_view);
//        CommonFeedAdapter adapter = new CommonFeedAdapter(this, new ArrayList<Feed>(), false, true);
//        mListView.setAdapter(adapter);
//        mListView.setOnItemClickListener(new OnItemClickListener() {
//            @Override
//            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
//                Feed blog = (Feed) adapterView.getAdapter().getItem(position);
//                Intent intent = new Intent().setClass(DiscussionActivity.this, BlogActivity.class);
//                intent.putExtra(Constants.Blog.TITLE, blog.getTitle());
//                intent.putExtra(Constants.Blog.CONTENT, blog.getContent());
//                startActivity(intent);
//            }
//        });
        
        new LoadDiscussionsTask(this).execute("true");
    }

    private static class LoadDiscussionsTask extends
            AsyncTask<String, Void, List<Feed>> {

        /** The target. */
        private WeakReference<DiscussionActivity> mTarget;

        /** The exception. */
        private boolean mException;

        /** The hide main view. */
        private boolean mHideMainView;
        
        /**
         * Instantiates a new load tree list task.
         * 
         * @param activity the activity
         */
        public LoadDiscussionsTask(DiscussionActivity activity) {
            mTarget = new WeakReference<DiscussionActivity>(activity);
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#doInBackground(Params[])
         */
        @Override
        protected List<Feed> doInBackground(String... params) {
            if (mTarget.get() != null) {
                this.mHideMainView = Boolean.valueOf(params[0]);
                BufferedInputStream bis = null;
                try {
                    URL url = new URL(mTarget.get().mUrl);
                    HttpURLConnection request = (HttpURLConnection) url.openConnection();
                    
                    request.setRequestMethod("GET");
                    request.setDoOutput(true);
                    
//                    if (ApplicationConstants.CONNECT_TIMEOUT > -1) {
//                        request.setConnectTimeout(ApplicationConstants.CONNECT_TIMEOUT);
//                    }
//
//                    if (ApplicationConstants.READ_TIMEOUT > -1) {
//                        request.setReadTimeout(ApplicationConstants.READ_TIMEOUT);
//                    }
                    request.connect();
                    bis = new BufferedInputStream(request.getInputStream());
                    
                    SAXParserFactory factory = SAXParserFactory.newInstance();
                    SAXParser parser = factory.newSAXParser();
                    DiscussionHandler handler = new DiscussionHandler();
                    parser.parse(bis, handler);
                    return handler.getFeeds();
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
                mTarget.get().mLoadingDialog = LoadingDialog.show(mTarget.get(), true, true,
                        mHideMainView);
            }
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(List<Feed> result) {
            if (mTarget.get() != null) {
                if (mException) {
                    mTarget.get().showError();
                }
                else {
                    if (mTarget.get().mLoadingDialog != null && mTarget.get().mLoadingDialog.isShowing()) {
                        mTarget.get().mLoadingDialog.dismiss();
                    }
                    mTarget.get().fillData(result);
                }
            }
        }
    }

    private void fillData(List<Feed> result) {
        ListView lvComments = (ListView) findViewById(R.id.lv_comments);

        // set details inside listview header
        LayoutInflater infalter = getLayoutInflater();
        mHeader = (LinearLayout) infalter.inflate(R.layout.discussion_header, lvComments, false);

        lvComments.addHeaderView(mHeader, null, true);

        TextView tvLogin = (TextView) mHeader.findViewById(R.id.tv_login);
        TextView tvCreateAt = (TextView) mHeader.findViewById(R.id.tv_created_at);
        TextView tvTitle = (TextView) mHeader.findViewById(R.id.tv_title);
        TextView tvDesc = (TextView) mHeader.findViewById(R.id.tv_desc);

        Collections.sort(result, new Comparator<Feed>(){
            
            public int compare(Feed o1, Feed o2) {
               return o1.getPublished().compareTo(o2.getPublished());
            }
 
        });

        Feed feed = result.get(0);
        tvLogin.setText(feed.getAuthor());
        tvCreateAt.setText(DateFormat.getMediumDateFormat(this).format(feed.getPublished()));
        tvTitle.setText(feed.getTitle());
        tvDesc.setText(Html.fromHtml(feed.getContent()));
        
        result.remove(0);
        
        if (result != null) {
            DiscussionCommentFeedAdapter adapter = new DiscussionCommentFeedAdapter(DiscussionActivity.this, result);
            lvComments.setAdapter(adapter);
        }
    }
    
}
