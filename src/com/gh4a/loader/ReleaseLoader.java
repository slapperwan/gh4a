package com.gh4a.loader;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.egit.github.core.Release;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.service.RepositoryService;

import android.content.Context;

import com.gh4a.Gh4Application;

public class ReleaseLoader extends BaseLoader<List<Release>> {

    private String mRepoOwner;
    private String mRepoName;

    public ReleaseLoader(Context context, String repoOwner, String repoName) {
        super(context);
        mRepoOwner = repoOwner;
        mRepoName = repoName;
    }

    @Override
    public List<Release> doLoadInBackground() throws IOException {
        RepositoryService repoService = (RepositoryService)
                Gh4Application.get().getService(Gh4Application.REPO_SERVICE);
        List<Release> releases = repoService.getReleases(new RepositoryId(mRepoOwner, mRepoName));
        Collections.sort(releases, new Comparator<Release>() {

            @Override
            public int compare(Release lhs, Release rhs) {
                return rhs.getCreatedAt().compareTo(lhs.getCreatedAt());
            }
            
        });
        return releases;
    }
}
