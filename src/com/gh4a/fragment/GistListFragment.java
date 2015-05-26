package com.gh4a.fragment;

import android.os.Bundle;
import android.support.v4.content.Loader;

import com.gh4a.Constants;
import com.gh4a.R;
import com.gh4a.adapter.GistAdapter;
import com.gh4a.adapter.RootAdapter;
import com.gh4a.loader.GistListLoader;
import com.gh4a.loader.LoaderResult;
import com.gh4a.loader.StarredGistListLoader;
import com.gh4a.utils.IntentUtils;

import org.eclipse.egit.github.core.Gist;

import java.util.List;

public class GistListFragment extends ListDataBaseFragment<Gist> {
    private String mUserLogin;
    private boolean mShowStarred;

    public static GistListFragment newInstance(String userLogin, boolean starred) {
        Bundle args = new Bundle();
        args.putString(Constants.User.LOGIN, userLogin);
        args.putBoolean("starred", starred);

        GistListFragment f = new GistListFragment();
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mUserLogin = getArguments().getString(Constants.User.LOGIN);
        mShowStarred = getArguments().getBoolean("starred");
    }

    @Override
    public Loader<LoaderResult<List<Gist>>> onCreateLoader(int id, Bundle args) {
        if (mShowStarred) {
            return new StarredGistListLoader(getActivity());
        }
        return new GistListLoader(getActivity(), mUserLogin);
    }

    @Override
    protected RootAdapter<Gist> onCreateAdapter() {
        return new GistAdapter(getActivity(), mUserLogin);
    }

    @Override
    protected int getEmptyTextResId() {
        return mShowStarred ? R.string.no_starred_gists_found : R.string.no_gists_found;
    }

    @Override
    protected void onItemClick(Gist gist) {
        startActivity(IntentUtils.getGistActivityIntent(getActivity(), mUserLogin, gist.getId()));
    }
}