package com.gh4a.fragment;

import android.os.Bundle;

import com.gh4a.ServiceFactory;
import com.meisolsson.githubsdk.model.GitHubEvent;
import com.meisolsson.githubsdk.model.Page;
import com.meisolsson.githubsdk.service.activity.EventService;

import io.reactivex.Single;
import retrofit2.Response;

public class PublicEventListFragment extends EventListFragment {
    private static final String EXTRA_LOGIN = "login";
    private String mLogin;

    public static PublicEventListFragment newInstance(String login) {
        PublicEventListFragment f = new PublicEventListFragment();
        Bundle args = new Bundle();
        args.putString(EXTRA_LOGIN, login);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLogin = getArguments().getString(EXTRA_LOGIN);
    }

    @Override
    protected Single<Response<Page<GitHubEvent>>> loadPage(int page, boolean bypassCache) {
        final EventService service = ServiceFactory.get(EventService.class, bypassCache);
        return service.getPublicUserPerformedEvents(mLogin, page);
    }
}
