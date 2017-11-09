package com.gh4a.fragment;

import android.os.Bundle;

import com.gh4a.ServiceFactory;
import com.meisolsson.githubsdk.model.GitHubEvent;
import com.meisolsson.githubsdk.model.Page;
import com.meisolsson.githubsdk.service.activity.EventService;

import io.reactivex.Single;
import retrofit2.Response;

public class PublicEventListFragment extends EventListFragment {
    private String mLogin;

    public static PublicEventListFragment newInstance(String login) {
        PublicEventListFragment f = new PublicEventListFragment();
        Bundle args = new Bundle();
        args.putString("login", login);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLogin = getArguments().getString("login");
    }

    @Override
    protected Single<Response<Page<GitHubEvent>>> loadPage(int page) {
        final EventService service = ServiceFactory.get(EventService.class);
        return service.getPublicUserPerformedEvents(mLogin, page);
    }
}
