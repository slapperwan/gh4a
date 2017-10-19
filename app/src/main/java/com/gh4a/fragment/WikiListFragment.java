package com.gh4a.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;

import com.gh4a.R;
import com.gh4a.activities.WikiActivity;
import com.gh4a.adapter.CommonFeedAdapter;
import com.gh4a.adapter.RootAdapter;
import com.gh4a.model.Feed;
import com.gh4a.utils.SingleFactory;

import org.xml.sax.SAXException;

import java.io.FileNotFoundException;
import java.util.List;

import io.reactivex.Single;

public class WikiListFragment extends ListDataBaseFragment<Feed> implements
        RootAdapter.OnItemClickListener<Feed> {
    private String mUserLogin;
    private String mRepoName;
    private String mInitialPage;
    private CommonFeedAdapter mAdapter;

    public static WikiListFragment newInstance(String owner, String repo, String initialPage) {
        WikiListFragment f = new WikiListFragment();
        Bundle args = new Bundle();
        args.putString("owner", owner);
        args.putString("repo", repo);
        args.putString("initial_page", initialPage);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mUserLogin = getArguments().getString("owner");
        mRepoName = getArguments().getString("repo");
        mInitialPage = getArguments().getString("initial_page");
        getArguments().remove("initial_page");
    }

    @Override
    protected Single<List<Feed>> onCreateDataSingle() {
        String url = "https://github.com/" + mUserLogin + "/" + mRepoName + "/wiki.atom";
        return SingleFactory.loadFeed(url);
    }

    @Override
    protected boolean onLoaderError(Throwable e) {
        if (e instanceof SAXException || e instanceof FileNotFoundException) {
            mAdapter.clear();
            updateEmptyState();
            setContentShown(true);
            return true;
        }
        return false;
    }

    @Override
    protected RootAdapter<Feed, ? extends RecyclerView.ViewHolder> onCreateAdapter() {
        mAdapter = new CommonFeedAdapter(getActivity(), false);
        mAdapter.setOnItemClickListener(this);
        return mAdapter;
    }

    @Override
    protected int getEmptyTextResId() {
        return R.string.no_wiki_updates_found;
    }

    @Override
    public void onItemClick(Feed feed) {
        openViewer(feed);
    }

    @Override
    protected void onAddData(RootAdapter<Feed, ?> adapter, List<Feed> data) {
        super.onAddData(adapter, data);

        if (mInitialPage != null) {
            for (Feed feed : data) {
                if (mInitialPage.equals(feed.getId())) {
                    openViewer(feed);
                    break;
                }
            }
            mInitialPage = null;
        }
    }

    private void openViewer(Feed feed) {
        startActivity(WikiActivity.makeIntent(getActivity(), mUserLogin, mRepoName, feed));
    }
}
