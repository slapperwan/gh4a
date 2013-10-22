package com.gh4a.loader;

import java.io.InputStream;

import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.ContentsService;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;

import com.gh4a.Constants;
import com.gh4a.DefaultClient;
import com.gh4a.Gh4Application;
import com.gh4a.utils.StringUtils;

public class ReadmeLoader extends AsyncTaskLoader<String> {

    private String mRepoOwner;
    private String mRepoName;
    
    public ReadmeLoader(Context context, String repoOwner, String repoName) {
        super(context);
        mRepoOwner = repoOwner;
        mRepoName = repoName;
    }
    
    @Override
    public String loadInBackground() {
        Gh4Application app = (Gh4Application) getContext().getApplicationContext();
        GitHubClient client = new DefaultClient("application/vnd.github.beta.html");
        client.setOAuth2Token(app.getAuthToken());
        try {
            ContentsService contentService = new ContentsService(client);
            InputStream is = contentService.getReadmeStream(new RepositoryId(mRepoOwner, mRepoName));
            if (is != null) {
                return StringUtils.convertStreamToString(is);
            }
            else {
                return null;
            }
        } catch (Exception e) {
            Log.e(Constants.LOG_TAG, e.getMessage(), e);
            return null;
        }
    }
}
