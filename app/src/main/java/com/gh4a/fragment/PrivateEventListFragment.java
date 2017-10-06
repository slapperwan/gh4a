package com.gh4a.fragment;

import android.os.Bundle;

import com.gh4a.Gh4Application;
import com.gh4a.loader.PageIteratorLoader;
import com.gh4a.utils.ApiHelpers;
import com.meisolsson.githubsdk.model.GitHubEvent;
import com.meisolsson.githubsdk.model.Page;
import com.meisolsson.githubsdk.service.activity.EventService;

import java.io.IOException;

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
    protected PageIteratorLoader<GitHubEvent> onCreateLoader() {
        final EventService service = Gh4Application.get().getGitHubService(EventService.class);
        return new PageIteratorLoader<GitHubEvent>(getActivity()) {
            @Override
            protected Page<GitHubEvent> loadPage(int page) throws IOException {
                Response<Page<GitHubEvent>> response = mOrganization != null
                        ? service.getOrganizationEvents(mLogin, mOrganization, page).blockingGet()
                        : service.getUserReceivedEvents(mLogin, page).blockingGet();
                return ApiHelpers.throwOnFailure(response);
            }
        };
    }
}
