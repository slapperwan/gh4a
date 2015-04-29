package com.gh4a.loader;

import java.io.IOException;

import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.service.StarService;

import android.content.Context;

import com.gh4a.Gh4Application;

public class IsStarringLoader extends BaseLoader<Boolean> {

    private String mRepoOwner;
    private String mRepoName;

    public IsStarringLoader(Context context, String repoOwner, String repoName) {
        super(context);
        mRepoOwner = repoOwner;
        mRepoName = repoName;
    }

    @Override
    public Boolean doLoadInBackground() throws IOException {
        StarService starService = (StarService)
                Gh4Application.get().getService(Gh4Application.STAR_SERVICE);
        return starService.isStarring(new RepositoryId(mRepoOwner, mRepoName));
    }
}
