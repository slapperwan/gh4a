package com.gh4a.fragment;

import android.os.Bundle;

import com.gh4a.ServiceFactory;
import com.meisolsson.githubsdk.model.GitHubEvent;
import com.meisolsson.githubsdk.model.Page;
import com.meisolsson.githubsdk.service.activity.EventService;

import io.reactivex.Single;
import retrofit2.Response;

public class PrivateEventListFragment extends EventListFragment {
    private static final String EXTRA_LOGIN = "login";
    private static final String EXTRA_ORG = "org";

    private String mLogin;
    private String mOrganization;

    public static PrivateEventListFragment newInstance(String login, String organization) {
        PrivateEventListFragment f = new PrivateEventListFragment();
        Bundle args = new Bundle();
        args.putString(EXTRA_LOGIN, login);
        args.putString(EXTRA_ORG, organization);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLogin = getArguments().getString(EXTRA_LOGIN);
        mOrganization = getArguments().getString(EXTRA_ORG);
    }

    @Override
    protected Single<Response<Page<GitHubEvent>>> loadPage(int page, boolean bypassCache) {
        final EventService service = ServiceFactory.get(EventService.class, bypassCache);
        return mOrganization != null
                ? service.getOrganizationEvents(mLogin, mOrganization, page)
                : service.getUserReceivedEvents(mLogin, page);
    }
}
