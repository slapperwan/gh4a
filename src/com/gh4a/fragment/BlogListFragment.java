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

import java.util.List;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.Loader;

import com.gh4a.Constants;
import com.gh4a.R;
import com.gh4a.activities.BlogActivity;
import com.gh4a.adapter.CommonFeedAdapter;
import com.gh4a.adapter.RootAdapter;
import com.gh4a.holder.Feed;
import com.gh4a.loader.FeedLoader;
import com.gh4a.loader.LoaderResult;

public class BlogListFragment extends ListDataBaseFragment<Feed> {
    private static final String BLOG = "https://github.com/blog.atom";

    public static BlogListFragment newInstance() {
        return new BlogListFragment();
    }

    @Override
    protected RootAdapter<Feed> onCreateAdapter() {
        return new CommonFeedAdapter(getActivity(), true);
    }

    @Override
    protected int getEmptyTextResId() {
        return R.string.no_blogs_found;
    }

    @Override
    public void onItemClick(Feed blog) {
        Intent intent = new Intent(getActivity(), BlogActivity.class);
        intent.putExtra(Constants.Blog.TITLE, blog.getTitle());
        intent.putExtra(Constants.Blog.CONTENT, blog.getContent());
        intent.putExtra(Constants.Blog.LINK, blog.getLink());
        startActivity(intent);
    }

    @Override
    public Loader<LoaderResult<List<Feed>>> onCreateLoader(int id, Bundle args) {
        return new FeedLoader(getActivity(), BLOG);
    }
}