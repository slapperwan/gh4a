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

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.xml.sax.SAXException;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.gh4a.Constants;
import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.adapter.CommonFeedAdapter;
import com.gh4a.feeds.FeedHandler;
import com.gh4a.holder.Feed;

public class BlogListActivity extends BaseSherlockFragmentActivity {
    
    private static final String BLOG = "https://github.com/blog.atom?page=";
    
    private int page = 1;
    private ListView mListView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(Gh4Application.THEME);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.generic_list);
        
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(R.string.blog);
        actionBar.setSubtitle(R.string.explore);
        actionBar.setDisplayHomeAsUpEnabled(true);

        mListView = (ListView) findViewById(R.id.list_view);
        CommonFeedAdapter adapter = new CommonFeedAdapter(this);
        mListView.setAdapter(adapter);
        mListView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                Feed blog = (Feed) adapterView.getAdapter().getItem(position);
                Intent intent = new Intent().setClass(BlogListActivity.this, BlogActivity.class);
                intent.putExtra(Constants.Blog.TITLE, blog.getTitle());
                intent.putExtra(Constants.Blog.CONTENT, blog.getContent());
                intent.putExtra(Constants.Blog.LINK, blog.getLink());
                startActivity(intent);
            }
        });
        
        new LoadBlogsTask(this).execute();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.explore_menu, menu);
        menu.removeItem(R.id.refresh);
        return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (!isAuthorized()) {
                    Intent intent = new Intent().setClass(this, Github4AndroidActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                }
                else {
                    Gh4Application.get(this).openUserInfoActivity(this, getAuthLogin(), 
                            null, Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    return true;
                }
            case R.id.pub_timeline:
                Intent intent = new Intent().setClass(this, TimelineActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                return true;
            case R.id.trend:
                intent = new Intent().setClass(this, TrendingActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    private static class LoadBlogsTask extends
            AsyncTask<String, Void, List<Feed>> {

        private WeakReference<BlogListActivity> mTarget;
        private boolean mException;

        public LoadBlogsTask(BlogListActivity activity) {
            mTarget = new WeakReference<BlogListActivity>(activity);
        }

        @Override
        protected List<Feed> doInBackground(String... params) {
            if (mTarget.get() != null) {
                InputStream bis = null;
                try {
                    URL url = new URL(BLOG + mTarget.get().page);
                    HttpClient httpClient = new DefaultHttpClient();
                    HttpGet pageGet = new HttpGet(url.toURI());
                    HttpResponse response = httpClient.execute(pageGet);

                    bis = response.getEntity().getContent();
                    
                    SAXParserFactory factory = SAXParserFactory.newInstance();
                    SAXParser parser = factory.newSAXParser();
                    FeedHandler handler = new FeedHandler();
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
                } catch (URISyntaxException e) {
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

        @Override
        protected void onPreExecute() {
            if (mTarget.get() != null) {
            }
        }

        @Override
        protected void onPostExecute(List<Feed> result) {
            if (mTarget.get() != null) {
                if (mException) {
                    mTarget.get().showError();
                }
                else {
                    mTarget.get().hideLoading();
                    mTarget.get().fillData(result);
                    mTarget.get().page++;
                }
            }
        }
    }

    private void fillData(List<Feed> result) {
        if (result != null) {
            List<Feed> blogs = ((CommonFeedAdapter) mListView.getAdapter()).getObjects();
            blogs.addAll(result);
            ((CommonFeedAdapter) mListView.getAdapter()).notifyDataSetChanged();
        }
    }
    
}
