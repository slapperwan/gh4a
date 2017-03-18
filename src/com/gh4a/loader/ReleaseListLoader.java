package com.gh4a.loader;

import android.content.Context;

import com.gh4a.Gh4Application;

import org.eclipse.egit.github.core.Release;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.service.RepositoryService;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ReleaseListLoader extends BaseLoader<List<Release>> {

    private final String mRepoOwner;
    private final String mRepoName;

    public ReleaseListLoader(Context context, String repoOwner, String repoName) {
        super(context);
        mRepoOwner = repoOwner;
        mRepoName = repoName;
    }

    @Override
    public List<Release> doLoadInBackground() throws IOException {
        List<Release> releases = loadReleases(mRepoOwner, mRepoName);
        Collections.sort(releases, new Comparator<Release>() {
            @Override
            public int compare(Release lhs, Release rhs) {
                return rhs.getCreatedAt().compareTo(lhs.getCreatedAt());
            }
        });
        return releases;
    }

    public static List<Release> loadReleases(String repoOwner, String repoName) throws IOException {
        RepositoryService repoService = (RepositoryService)
                Gh4Application.get().getService(Gh4Application.REPO_SERVICE);
        return repoService.getReleases(new RepositoryId(repoOwner, repoName));
    }
}
