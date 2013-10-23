package com.gh4a.loader;

import java.io.IOException;
import java.util.List;

import org.eclipse.egit.github.core.User;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.OrganizationService;

import android.content.Context;

import com.gh4a.Gh4Application;

public class OrganizationListLoader extends BaseLoader<List<User>> {

    private String mUserLogin;
    
    public OrganizationListLoader(Context context, String userLogin) {
        super(context);
        mUserLogin = userLogin;
    }
    
    @Override
    public List<User> doLoadInBackground() throws IOException {
        Gh4Application app = (Gh4Application) getContext().getApplicationContext();
        GitHubClient client = new GitHubClient();
        client.setOAuth2Token(app.getAuthToken());
        OrganizationService orgService = new OrganizationService(client);
        return orgService.getOrganizations(mUserLogin);
    }
}
