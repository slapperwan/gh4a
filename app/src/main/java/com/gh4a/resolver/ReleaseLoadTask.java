package com.gh4a.resolver;

import android.content.Intent;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;

import com.gh4a.Gh4Application;
import com.gh4a.activities.ReleaseInfoActivity;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.RxUtils;
import com.meisolsson.githubsdk.service.repositories.RepositoryReleaseService;

import io.reactivex.Single;

public class ReleaseLoadTask extends UrlLoadTask {
    @VisibleForTesting
    protected final String mRepoOwner;
    @VisibleForTesting
    protected final String mRepoName;
    @VisibleForTesting
    protected final String mTagName;

    public ReleaseLoadTask(FragmentActivity activity, String repoOwner, String repoName,
            String tagName) {
        super(activity);
        mRepoOwner = repoOwner;
        mRepoName = repoName;
        mTagName = tagName;
    }

    @Override
    protected Single<Intent> getSingle() {
        RepositoryReleaseService service =
                Gh4Application.get().getGitHubService(RepositoryReleaseService.class);
        return ApiHelpers.PageIterator
                .toSingle(page -> service.getReleases(mRepoOwner, mRepoName, page))
                .compose(RxUtils.filterAndMapToFirstOrNull(r -> TextUtils.equals(r.tagName(), mTagName)))
                .map(r -> {
                    if (r == null) {
                        return null;
                    }
                    return ReleaseInfoActivity.makeIntent(mActivity, mRepoOwner, mRepoName, r);
                });
    }
}
