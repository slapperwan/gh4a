package com.gh4a.fragment;

import android.os.Bundle;

import com.gh4a.ApiRequestException;
import com.gh4a.Gh4Application;
import com.gh4a.loader.PageIteratorLoader;
import com.gh4a.utils.ApiHelpers;
import com.meisolsson.githubsdk.model.GitHubEvent;
import com.meisolsson.githubsdk.model.Page;
import com.meisolsson.githubsdk.service.activity.EventService;

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
    protected PageIteratorLoader<GitHubEvent> onCreateLoader() {
        final EventService service = Gh4Application.get().getGitHubService(EventService.class);
        return new PageIteratorLoader<GitHubEvent>(getActivity()) {
            @Override
            protected Page<GitHubEvent> loadPage(int page) throws ApiRequestException {
                return ApiHelpers.throwOnFailure(
                        service.getPublicUserPerformedEvents(mLogin, page).blockingGet());
            }
        };
    }
}
