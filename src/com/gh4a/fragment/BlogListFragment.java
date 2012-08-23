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
package com.gh4a.fragment;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.gh4a.BlogActivity;
import com.gh4a.Constants;
import com.gh4a.R;
import com.gh4a.adapter.CommonFeedAdapter;
import com.gh4a.feeds.FeedHandler;
import com.gh4a.holder.Feed;

public class BlogListFragment extends BaseFragment {

    private static final String BLOG = "https://github.com/blog.atom?page=";

    private int page = 1;
    private ListView mListView;
    private CommonFeedAdapter mAdapter;

    public static BlogListFragment newInstance() {

        BlogListFragment f = new BlogListFragment();
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.generic_list, container, false);
        mListView = (ListView) v.findViewById(R.id.list_view);
        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mAdapter = new CommonFeedAdapter(getSherlockActivity(),
                new ArrayList<Feed>());
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view,
                    int position, long id) {
                Feed blog = (Feed) adapterView.getAdapter().getItem(position);
                Intent intent = new Intent().setClass(getSherlockActivity(),
                        BlogActivity.class);
                intent.putExtra(Constants.Blog.TITLE, blog.getTitle());
                intent.putExtra(Constants.Blog.CONTENT, blog.getContent());
                intent.putExtra(Constants.Blog.LINK, blog.getLink());
                startActivity(intent);
            }
        });

        new LoadBlogsTask(this).execute();
    }

    private static class LoadBlogsTask extends
            AsyncTask<String, Void, List<Feed>> {

        private WeakReference<BlogListFragment> mTarget;

        public LoadBlogsTask(BlogListFragment activity) {
            mTarget = new WeakReference<BlogListFragment>(activity);
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
                } catch (MalformedURLException e) {
                    Log.e(Constants.LOG_TAG, e.getMessage(), e);
                    return null;
                } catch (IOException e) {
                    Log.e(Constants.LOG_TAG, e.getMessage(), e);
                    return null;
                } catch (ParserConfigurationException e) {
                    Log.e(Constants.LOG_TAG, e.getMessage(), e);
                    return null;
                } catch (SAXException e) {
                    Log.e(Constants.LOG_TAG, e.getMessage(), e);
                    return null;
                } catch (URISyntaxException e) {
                    Log.e(Constants.LOG_TAG, e.getMessage(), e);
                    return null;
                } finally {
                    if (bis != null) {
                        try {
                            bis.close();
                        } catch (IOException e) {
                            Log.e(Constants.LOG_TAG, e.getMessage(), e);
                        }
                    }
                }
            } else {
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
                mTarget.get().hideLoading();
                if (mTarget.get() != null && result != null) {
                    mTarget.get().fillData(result);
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