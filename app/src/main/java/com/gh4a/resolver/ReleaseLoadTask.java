package com.gh4a.resolver;

import android.content.Intent;
import android.net.Uri;

import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.FragmentActivity;

import com.gh4a.ServiceFactory;
import com.gh4a.activities.ReleaseInfoActivity;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.Optional;
import com.gh4a.utils.RxUtils;
import com.meisolsson.githubsdk.model.Release;
import com.meisolsson.githubsdk.service.repositories.RepositoryReleaseService;

import java.net.HttpURLConnection;

import io.reactivex.Single;
import retrofit2.Response;

public class ReleaseLoadTask extends UrlLoadTask {
    @VisibleForTesting
    protected final String mRepoOwner;
    @VisibleForTesting
    protected final String mRepoName;
    @VisibleForTesting
    protected final String mTagName;
    @VisibleForTesting
    protected final long mId;

    public ReleaseLoadTask(FragmentActivity activity, Uri urlToResolve,
            String repoOwner, String repoName, String tagName) {
        super(activity, urlToResolve);
        mRepoOwner = repoOwner;
        mRepoName = repoName;
        mTagName = tagName;
        mId = -1;
    }

    public ReleaseLoadTask(FragmentActivity activity, Uri urlToResolve,
            String repoOwner, String repoName, long id) {
        super(activity, urlToResolve);
        mRepoOwner = repoOwner;
        mRepoName = repoName;
        mId = id;
        mTagName = null;
    }

    @Override
    protected Single<Optional<Intent>> getSingle() {
        RepositoryReleaseService service = ServiceFactory.get(RepositoryReleaseService.class, false);
        final Single<Response<Release>> releaseSingle;
        if (mId >= 0) {
            releaseSingle = service.getRelease(mRepoOwner, mRepoName, mId);
        } else {
            releaseSingle = service.getRelaseByTagName(mRepoOwner, mRepoName, mTagName);
        }
        return releaseSingle
                .map(ApiHelpers::throwOnFailure)
                .map(r -> Optional.of(ReleaseInfoActivity.makeIntent(mActivity, mRepoOwner, mRepoName, r)))
                .compose(RxUtils.mapFailureToValue(HttpURLConnection.HTTP_NOT_FOUND, Optional.absent()));
    }
}
