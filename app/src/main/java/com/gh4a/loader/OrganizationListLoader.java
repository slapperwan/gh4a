package com.gh4a.loader;

import java.util.List;

import android.content.Context;

import com.gh4a.ApiRequestException;
import com.gh4a.Gh4Application;
import com.gh4a.utils.ApiHelpers;
import com.meisolsson.githubsdk.model.User;
import com.meisolsson.githubsdk.service.organizations.OrganizationService;

public class OrganizationListLoader extends BaseLoader<List<User>> {
    private final String mUserLogin;

    public OrganizationListLoader(Context context, String userLogin) {
        super(context);
        mUserLogin = userLogin;
    }

    @Override
    public List<User> doLoadInBackground() throws ApiRequestException {
        final Gh4Application app = Gh4Application.get();
        final OrganizationService service = app.getGitHubService(OrganizationService.class);
        return ApiHelpers.PageIterator
                .toSingle(page -> {
                    return ApiHelpers.loginEquals(mUserLogin, app.getAuthLogin())
                            ? service.getMyOrganizations(page)
                            : service.getUserPublicOrganizations(mUserLogin, page);
                })
                .blockingGet();
    }
}
