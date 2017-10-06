package com.gh4a.loader;

import android.content.Context;

import com.gh4a.Gh4Application;
import com.gh4a.utils.ApiHelpers;
import com.meisolsson.githubsdk.model.Page;
import com.meisolsson.githubsdk.model.Release;
import com.meisolsson.githubsdk.service.repositories.RepositoryReleaseService;

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
                return rhs.createdAt().compareTo(lhs.createdAt());
            }
        });
        return releases;
    }

    public static List<Release> loadReleases(final String repoOwner, final String repoName)
            throws IOException {
        final RepositoryReleaseService service =
                Gh4Application.get().getGitHubService(RepositoryReleaseService.class);
        return ApiHelpers.Pager.fetchAllPages(new ApiHelpers.Pager.PageProvider<Release>() {
            @Override
            public Page<Release> providePage(long page) throws IOException {
                return ApiHelpers.throwOnFailure(
                        service.getReleases(repoOwner, repoName, page).blockingGet());
            }
        });
    }
}
