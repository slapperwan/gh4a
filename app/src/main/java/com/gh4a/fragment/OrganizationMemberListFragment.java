package com.gh4a.fragment;

import android.os.Bundle;
import android.support.v4.content.Loader;
import android.support.v7.widget.RecyclerView;

import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.activities.UserActivity;
import com.gh4a.adapter.RootAdapter;
import com.gh4a.adapter.UserAdapter;
import com.gh4a.loader.LoaderResult;
import com.gh4a.loader.OrganizationMemberListLoader;
import com.gh4a.utils.ApiHelpers;
import com.meisolsson.githubsdk.model.Page;
import com.meisolsson.githubsdk.model.User;
import com.meisolsson.githubsdk.service.organizations.OrganizationMemberService;

import java.util.List;

import io.reactivex.Single;
import retrofit2.Response;

public class OrganizationMemberListFragment extends PagedDataBaseFragment<User> implements
        RootAdapter.OnItemClickListener<User> {
    public static OrganizationMemberListFragment newInstance(String organization) {
        OrganizationMemberListFragment f = new OrganizationMemberListFragment();
        Bundle args = new Bundle();
        args.putString("org", organization);
        f.setArguments(args);
        return f;
    }

    @Override
    protected Single<Response<Page<User>>> loadPage(int page) {
        String organization = getArguments().getString("org");
        final OrganizationMemberService service =
                Gh4Application.get().getGitHubService(OrganizationMemberService.class);
        return service.getMembers(organization, page);
    }

    @Override
    protected RootAdapter<User, ? extends RecyclerView.ViewHolder> onCreateAdapter() {
        UserAdapter adapter = new UserAdapter(getActivity());
        adapter.setOnItemClickListener(this);
        return adapter;
    }

    @Override
    protected int getEmptyTextResId() {
        return R.string.no_org_members_found;
    }

    @Override
    public void onItemClick(User item) {
        startActivity(UserActivity.makeIntent(getActivity(), item));
    }
}
