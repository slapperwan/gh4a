package com.gh4a.fragment;

import android.os.Bundle;
import android.support.v4.content.Loader;
import android.support.v7.widget.RecyclerView;

import com.gh4a.R;
import com.gh4a.adapter.RootAdapter;
import com.gh4a.adapter.UserAdapter;
import com.gh4a.loader.LoaderResult;
import com.gh4a.loader.OrganizationMemberListLoader;

import org.eclipse.egit.github.core.User;

import java.util.List;

public class OrganizationMemberListFragment extends ListDataBaseFragment<User> {
    public static OrganizationMemberListFragment newInstance(String organization) {
        OrganizationMemberListFragment f = new OrganizationMemberListFragment();
        Bundle args = new Bundle();
        args.putString("org", organization);
        f.setArguments(args);
        return f;
    }

    @Override
    protected Loader<LoaderResult<List<User>>> onCreateLoader() {
        String organization = getArguments().getString("org");
        return new OrganizationMemberListLoader(getActivity(), organization);
    }

    @Override
    protected RootAdapter<User, ? extends RecyclerView.ViewHolder> onCreateAdapter() {
        return new UserAdapter(getActivity());
    }

    @Override
    protected int getEmptyTextResId() {
        return R.string.no_org_members_found;
    }
}
