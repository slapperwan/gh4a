package com.gh4a.loader;

import java.io.IOException;
import java.util.List;

import org.eclipse.egit.github.core.User;
import org.eclipse.egit.github.core.service.OrganizationService;

import android.content.Context;

import com.gh4a.Gh4Application;
import com.gh4a.utils.ApiHelpers;

public class OrganizationListLoader extends BaseLoader<List<User>> {
    private final String mUserLogin;

    public OrganizationListLoader(Context context, String userLogin) {
        super(context);
        mUserLogin = userLogin;
    }

    @Override
    public List<User> doLoadInBackground() throws IOException {
        Gh4Application app = Gh4Application.get();
        OrganizationService orgService = (OrganizationService)
                app.getService(Gh4Application.ORG_SERVICE);
        if (ApiHelpers.loginEquals(mUserLogin, app.getAuthLogin())) {
            return orgService.getOrganizations();
        } else {
            return orgService.getOrganizations(mUserLogin);
        }
    }
}
