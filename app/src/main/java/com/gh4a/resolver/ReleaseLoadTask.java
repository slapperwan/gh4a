package com.gh4a.resolver;

import android.content.Intent;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;

import com.gh4a.ServiceFactory;
import com.gh4a.activities.ReleaseInfoActivity;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.Optional;
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
    protected Single<Optional<Intent>> getSingle() {
        RepositoryReleaseService service = ServiceFactory.get(RepositoryReleaseService.class, false);
        return ApiHelpers.PageIterator
                .toSingle(page -> service.getReleases(mRepoOwner, mRepoName, page))
                .compose(RxUtils.filterAndMapToFirst(r -> TextUtils.equals(r.tagName(), mTagName)))
                .map(releaseOpt -> releaseOpt.map(r -> ReleaseInfoActivity.makeIntent(mActivity,
                        mRepoOwner, mRepoName, r)));
    }
}
