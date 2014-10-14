package com.gh4a.fragment;

import java.util.List;

import org.eclipse.egit.github.core.Contributor;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.Loader;

import com.gh4a.Constants;
import com.gh4a.R;
import com.gh4a.adapter.ContributorAdapter;
import com.gh4a.adapter.RootAdapter;
import com.gh4a.loader.ContributorListLoader;
import com.gh4a.loader.LoaderResult;
import com.gh4a.utils.IntentUtils;

public class ContributorListFragment extends ListDataBaseFragment<Contributor> {
    public static ContributorListFragment newInstance(String repoOwner, String repoName) {
        ContributorListFragment f = new ContributorListFragment();

        Bundle args = new Bundle();
        args.putString(Constants.Repository.OWNER, repoOwner);
        args.putString(Constants.Repository.NAME, repoName);
        f.setArguments(args);

        return f;
    }

    @Override
    public Loader<LoaderResult<List<Contributor>>> onCreateLoader(int id, Bundle args) {
        String repoOwner = getArguments().getString(Constants.Repository.OWNER);
        String repoName = getArguments().getString(Constants.Repository.NAME);
        return new ContributorListLoader(getActivity(), repoOwner, repoName);
    }

    @Override
    protected int getEmptyTextResId() {
        return R.string.no_contributors_found;
    }

    @Override
    protected RootAdapter<Contributor> onCreateAdapter() {
        return new ContributorAdapter(getActivity());
    }

    @Override
    protected void onItemClick(Contributor item) {
        Intent intent = IntentUtils.getUserActivityIntent(getActivity(),
                item.getLogin(), item.getName());
        if (intent != null) {
            startActivity(intent);
        }
    }
}
