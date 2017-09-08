package com.gh4a.loader;

import android.content.Context;

import com.gh4a.Gh4Application;

import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.egit.github.core.PullRequestMarker;
import org.eclipse.egit.github.core.Reference;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.service.DataService;

import java.io.IOException;

public class ReferenceLoader extends BaseLoader<Reference> {
    private final PullRequest mPullRequest;

    public ReferenceLoader(Context context, PullRequest pullRequest) {
        super(context);
        mPullRequest = pullRequest;
    }

    @Override
    public Reference doLoadInBackground() throws IOException {
        DataService dataService = (DataService)
                Gh4Application.get().getService(Gh4Application.DATA_SERVICE);

        PullRequestMarker head = mPullRequest.getHead();
        if (head.getRepo() == null) {
            return null;
        }
        String owner = head.getRepo().getOwner().getLogin();
        String repo = head.getRepo().getName();
        String ref = "heads/" + head.getRef();

        return dataService.getReference(new RepositoryId(owner, repo), ref);
    }
}
