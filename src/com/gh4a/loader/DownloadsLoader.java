package com.gh4a.loader;

import java.io.IOException;
import java.util.List;

import org.eclipse.egit.github.core.Download;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.service.DownloadService;

import android.content.Context;

import com.gh4a.Gh4Application;

public class DownloadsLoader extends BaseLoader<List<Download>> {

    private String mRepoOwner;
    private String mRepoName;

    public DownloadsLoader(Context context, String repoOwner, String repoName) {
        super(context);
        mRepoOwner = repoOwner;
        mRepoName = repoName;
    }

    @Override
    public List<Download> doLoadInBackground() throws IOException {
        DownloadService downloadService = (DownloadService)
                Gh4Application.get().getService(Gh4Application.DOWNLOAD_SERVICE);
        return downloadService.getDownloads(new RepositoryId(mRepoOwner, mRepoName));
    }
}
