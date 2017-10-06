package com.gh4a.fragment;

import android.os.Bundle;
import android.support.v4.content.Loader;
import android.support.v7.widget.RecyclerView;

import com.gh4a.R;
import com.gh4a.activities.UserActivity;
import com.gh4a.adapter.RootAdapter;
import com.gh4a.adapter.UserAdapter;
import com.gh4a.loader.CollaboratorListLoader;
import com.gh4a.loader.LoaderResult;
import com.meisolsson.githubsdk.model.User;


import java.util.List;

public class CollaboratorListFragment extends ListDataBaseFragment<User> implements
        RootAdapter.OnItemClickListener<User> {
    public static CollaboratorListFragment newInstance(String owner, String repo) {
        CollaboratorListFragment f = new CollaboratorListFragment();
        Bundle args = new Bundle();
        args.putString("owner", owner);
        args.putString("repo", repo);
        f.setArguments(args);
        return f;
    }

    @Override
    protected Loader<LoaderResult<List<User>>> onCreateLoader() {
        String owner = getArguments().getString("owner");
        String repo = getArguments().getString("repo");
        return new CollaboratorListLoader(getActivity(), owner, repo);
    }

    @Override
    protected RootAdapter<User, ? extends RecyclerView.ViewHolder> onCreateAdapter() {
        UserAdapter adapter = new UserAdapter(getActivity());
        adapter.setOnItemClickListener(this);
        return adapter;
    }

    @Override
    protected int getEmptyTextResId() {
        return R.string.no_collaborators_found;
    }

    @Override
    public void onItemClick(User item) {
        startActivity(UserActivity.makeIntent(getActivity(), item));
    }
}
