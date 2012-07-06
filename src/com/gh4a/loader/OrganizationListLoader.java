package com.gh4a.loader;

import java.io.IOException;
import java.util.List;

import org.eclipse.egit.github.core.User;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.OrganizationService;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;

import com.gh4a.Constants;
import com.gh4a.Gh4Application;

public class OrganizationListLoader extends AsyncTaskLoader<List<User>> {

    private String mUserLogin;
    
    public OrganizationListLoader(Context context, String userLogin) {
        super(context);
        mUserLogin = userLogin;
    }
    
    @Override
    public List<User> loadInBackground() {
        Gh4Application app = (Gh4Application) getContext().getApplicationContext();
        GitHubClient client = new GitHubClient();
        client.setOAuth2Token(app.getAuthToken());
        OrganizationService orgService = new OrganizationService(client);
        try {
            return orgService.getOrganizations(mUserLogin);
        } catch (IOException e) {
            Log.e(Constants.LOG_TAG, e.getMessage(), e);
            return null;
        }
    }

}
