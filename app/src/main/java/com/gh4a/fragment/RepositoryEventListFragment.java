package com.gh4a.fragment;

import android.os.Bundle;

import com.gh4a.Gh4Application;
import com.gh4a.loader.PageIteratorLoader;
import com.gh4a.utils.ApiHelpers;
import com.meisolsson.githubsdk.model.GitHubEvent;
import com.meisolsson.githubsdk.model.Page;
import com.meisolsson.githubsdk.model.Repository;
import com.meisolsson.githubsdk.service.activity.EventService;

import java.io.IOException;

public class RepositoryEventListFragment extends EventListFragment {
    private Repository mRepository;

    public static RepositoryEventListFragment newInstance(Repository repository) {
        RepositoryEventListFragment f = new RepositoryEventListFragment();
        Bundle args = new Bundle();
        args.putParcelable("repository", repository);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRepository = getArguments().getParcelable("repository");
    }

    @Override
    protected PageIteratorLoader<GitHubEvent> onCreateLoader() {
        final EventService service = Gh4Application.get().getGitHubService(EventService.class);
        return new PageIteratorLoader<GitHubEvent>(getActivity()) {
            @Override
            protected Page<GitHubEvent> loadPage(int page) throws IOException {
                return ApiHelpers.throwOnFailure(service.getRepositoryEvents(
                        mRepository.owner().login(), mRepository.name(), page).blockingGet());
            }
        };
    }
}
