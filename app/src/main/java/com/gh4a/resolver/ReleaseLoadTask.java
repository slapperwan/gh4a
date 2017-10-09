package com.gh4a.resolver;

import android.content.Intent;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;

import com.gh4a.ApiRequestException;
import com.gh4a.activities.ReleaseInfoActivity;
import com.gh4a.loader.ReleaseListLoader;
import com.meisolsson.githubsdk.model.Release;

import java.util.List;

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
    protected Intent run() throws ApiRequestException {
        List<Release> releases = ReleaseListLoader.loadReleases(mRepoOwner, mRepoName);

        if (releases != null) {
            for (Release release : releases) {
                if (TextUtils.equals(release.tagName(), mTagName)) {
                    return ReleaseInfoActivity.makeIntent(mActivity, mRepoOwner, mRepoName,
                            release);
                }
            }
        }

        return null;
    }
}
