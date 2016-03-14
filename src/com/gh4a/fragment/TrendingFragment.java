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
import android.support.v4.content.Loader;
import android.support.v7.widget.RecyclerView;

import com.gh4a.R;
import com.gh4a.adapter.RootAdapter;
import com.gh4a.adapter.TrendAdapter;
import com.gh4a.holder.Trend;
import com.gh4a.loader.LoaderResult;
import com.gh4a.loader.TrendLoader;
import com.gh4a.utils.IntentUtils;

import java.util.List;
import java.util.Locale;

public class TrendingFragment extends ListDataBaseFragment<Trend> {
    private static final String API_URL_TEMPLATE =
            "https://api.import.io/store/connector/%s/_query?_user=%s&_apikey=%s";
    private static final String API_EXTRACTOR_GUID = "f275068d-5237-432d-829c-e37a175270aa";
    private static final String API_KEY = "41fbbdaa94d045ebb652a065fade27714f92e6fe1cbc2c962ce67"
            + "e7cfcd20793397fe894275f8b9ecf20141404d95c4a215897f53fde"
            + "62f3a20bfad968bc5c9fd1af829bd368e46182a92adcbe6f246e";
    private static final String API_USER_GUID = "41fbbdaa-94d0-45eb-b652-a065fade2771";

    private static final String API_URL = String.format(Locale.US, API_URL_TEMPLATE,
            API_EXTRACTOR_GUID, API_USER_GUID, API_KEY);

    private static final String TREND_URL_TEMPLATE = "https://github.com/trending?since=%s";
    public static final String TYPE_DAILY = "daily";
    public static final String TYPE_WEEKLY = "weekly";
    public static final String TYPE_MONTHLY = "monthly";

    private String mUrl;

    public static TrendingFragment newInstance(String type) {
        if (type == null) {
            return null;
        }

        TrendingFragment f = new TrendingFragment();
        Bundle args = new Bundle();
        args.putString("url", String.format(Locale.US, TREND_URL_TEMPLATE, type));
        f.setArguments(args);

        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mUrl = getArguments().getString("url");
    }

    @Override
    protected RootAdapter<Trend, ? extends RecyclerView.ViewHolder> onCreateAdapter() {
        return new TrendAdapter(getActivity());
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
            startActivity(IntentUtils.getRepoActivityIntent(getActivity(), owner, name, null));
        }
    }

    @Override
    public Loader<LoaderResult<List<Trend>>> onCreateLoader() {
        return new TrendLoader(getActivity(), API_URL, mUrl);
    }
}
