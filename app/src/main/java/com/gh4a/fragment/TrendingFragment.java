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

import android.os.Bundle;
import android.support.annotation.StringRes;
import android.support.v7.widget.RecyclerView;

import com.gh4a.R;
import com.gh4a.activities.RepositoryActivity;
import com.gh4a.adapter.RootAdapter;
import com.gh4a.adapter.TrendAdapter;
import com.gh4a.model.Trend;
import com.gh4a.utils.SingleFactory;

import java.util.List;

import io.reactivex.Single;

public class TrendingFragment extends ListDataBaseFragment<Trend> implements
        RootAdapter.OnItemClickListener<Trend> {
    public static final String TYPE_DAILY = "daily";
    public static final String TYPE_WEEKLY = "weekly";
    public static final String TYPE_MONTHLY = "monthly";

    private String mType;
    private @StringRes int mStarsTemplate;

    public static TrendingFragment newInstance(String type) {
        if (type == null) {
            return null;
        }

        TrendingFragment f = new TrendingFragment();
        Bundle args = new Bundle();
        args.putString("type", type);
        switch (type) {
            case TYPE_DAILY: args.putInt("stars_template", R.string.trend_stars_today); break;
            case TYPE_WEEKLY: args.putInt("stars_template", R.string.trend_stars_week); break;
            case TYPE_MONTHLY: args.putInt("stars_template", R.string.trend_stars_month); break;
            default: throw new IllegalArgumentException();
        }
        f.setArguments(args);

        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mType = getArguments().getString("type");
        mStarsTemplate = getArguments().getInt("stars_template", 0);
    }

    @Override
    protected RootAdapter<Trend, ? extends RecyclerView.ViewHolder> onCreateAdapter() {
        TrendAdapter adapter = new TrendAdapter(getActivity(), mStarsTemplate);
        adapter.setOnItemClickListener(this);
        return adapter;
    }

    @Override
    protected int getEmptyTextResId() {
        return R.string.no_trends_found;
    }

    @Override
    public void onItemClick(Trend trend) {
        String owner = trend.getRepoOwner();
        String name = trend.getRepoName();
        if (owner != null && name != null) {
            startActivity(RepositoryActivity.makeIntent(getActivity(), owner, name));
        }
    }

    @Override
    protected Single<List<Trend>> onCreateDataSingle(boolean bypassCache) {
        return SingleFactory.loadTrends(mType);
    }
}
