package com.gh4a.fragment;

import android.os.Bundle;
import android.support.v7.widget.RecyclerView;

import com.gh4a.R;
import com.gh4a.ServiceFactory;
import com.gh4a.activities.UserActivity;
import com.gh4a.adapter.RootAdapter;
import com.gh4a.adapter.UserAdapter;
import com.meisolsson.githubsdk.model.Page;
import com.meisolsson.githubsdk.model.User;
import com.meisolsson.githubsdk.service.organizations.OrganizationMemberService;

import io.reactivex.Single;
import retrofit2.Response;

public class OrganizationMemberListFragment extends PagedDataBaseFragment<User> implements
        RootAdapter.OnItemClickListener<User> {
    private static final String EXTRA_ORG = "org";

    public static OrganizationMemberListFragment newInstance(String organization) {
        OrganizationMemberListFragment f = new OrganizationMemberListFragment();
        Bundle args = new Bundle();
        args.putString(EXTRA_ORG, organization);
        f.setArguments(args);
        return f;
    }

    @Override
    protected Single<Response<Page<User>>> loadPage(int page, boolean bypassCache) {
        String organization = getArguments().getString(EXTRA_ORG);
        final OrganizationMemberService service =
                ServiceFactory.get(OrganizationMemberService.class, bypassCache);
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
