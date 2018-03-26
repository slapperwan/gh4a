package com.gh4a.fragment;

import android.os.Bundle;

import com.gh4a.ServiceFactory;
import com.meisolsson.githubsdk.model.GitHubEvent;
import com.meisolsson.githubsdk.model.Page;
import com.meisolsson.githubsdk.model.User;
import com.meisolsson.githubsdk.model.UserType;
import com.meisolsson.githubsdk.service.activity.EventService;

import io.reactivex.Single;
import retrofit2.Response;

public class PublicEventListFragment extends EventListFragment {
    private String mLogin;
    private boolean mIsOrganization;

    public static PublicEventListFragment newInstance(User user) {
        PublicEventListFragment f = new PublicEventListFragment();
        Bundle args = new Bundle();
        args.putString("login", user.login());
        args.putBoolean("org", user.type() == UserType.Organization);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLogin = getArguments().getString("login");
        mIsOrganization = getArguments().getBoolean("org");
    }

    @Override
    protected Single<Response<Page<GitHubEvent>>> loadPage(int page, boolean bypassCache) {
        final EventService service = ServiceFactory.get(EventService.class, bypassCache);
        return mIsOrganization
                ? service.getPublicOrganizationEvents(mLogin, page)
                : service.getPublicUserPerformedEvents(mLogin, page);
    }
}
