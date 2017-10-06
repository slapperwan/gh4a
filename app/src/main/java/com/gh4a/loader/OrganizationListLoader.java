package com.gh4a.loader;

import java.io.IOException;
import java.util.List;

import android.content.Context;

import com.gh4a.Gh4Application;
import com.gh4a.utils.ApiHelpers;
import com.meisolsson.githubsdk.model.Page;
import com.meisolsson.githubsdk.model.User;
import com.meisolsson.githubsdk.service.organizations.OrganizationService;

import retrofit2.Response;

public class OrganizationListLoader extends BaseLoader<List<User>> {
    private final String mUserLogin;

    public OrganizationListLoader(Context context, String userLogin) {
        super(context);
        mUserLogin = userLogin;
    }

    @Override
    public List<User> doLoadInBackground() throws IOException {
        final Gh4Application app = Gh4Application.get();
        final OrganizationService service = app.getGitHubService(OrganizationService.class);
        return ApiHelpers.Pager.fetchAllPages(new ApiHelpers.Pager.PageProvider<User>() {
            @Override
            public Page<User> providePage(long page) throws IOException {
                Response<Page<User>> response = ApiHelpers.loginEquals(mUserLogin, app.getAuthLogin())
                        ? service.getMyOrganizations(page).blockingGet()
                        : service.getUserPublicOrganizations(mUserLogin, page).blockingGet();
                return ApiHelpers.throwOnFailure(response);
            }
        });
    }
}
