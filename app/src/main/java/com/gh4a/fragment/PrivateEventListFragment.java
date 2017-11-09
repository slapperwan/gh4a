package com.gh4a.fragment;

import android.os.Bundle;

import com.gh4a.ServiceFactory;
import com.meisolsson.githubsdk.model.GitHubEvent;
import com.meisolsson.githubsdk.model.Page;
import com.meisolsson.githubsdk.service.activity.EventService;

import io.reactivex.Single;
import retrofit2.Response;

public class PrivateEventListFragment extends EventListFragment {
    private String mLogin;
    private String mOrganization;

    public static PrivateEventListFragment newInstance(String login, String organization) {
        PrivateEventListFragment f = new PrivateEventListFragment();
        Bundle args = new Bundle();
        args.putString("login", login);
        args.putString("org", organization);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLogin = getArguments().getString("login");
        mOrganization = getArguments().getString("org");
    }

    @Override
    protected Single<Response<Page<GitHubEvent>>> loadPage(int page, boolean bypassCache) {
        final EventService service = ServiceFactory.get(EventService.class, bypassCache);
        return mOrganization != null
                ? service.getOrganizationEvents(mLogin, mOrganization, page)
                : service.getUserReceivedEvents(mLogin, page);
    }
}
