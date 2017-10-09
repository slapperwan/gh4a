package com.gh4a.loader;

import java.util.List;

import android.content.Context;

import com.gh4a.ApiRequestException;
import com.gh4a.Gh4Application;
import com.gh4a.utils.ApiHelpers;
import com.meisolsson.githubsdk.model.User;
import com.meisolsson.githubsdk.service.organizations.OrganizationMemberService;

public class OrganizationMemberListLoader extends BaseLoader<List<User>> {

    private final String mUserLogin;

    public OrganizationMemberListLoader(Context context, String userLogin) {
        super(context);
        mUserLogin = userLogin;
    }

    @Override
    public List<User> doLoadInBackground() throws ApiRequestException {
        final OrganizationMemberService service =
                Gh4Application.get().getGitHubService(OrganizationMemberService.class);
        return ApiHelpers.PageIterator
                .toSingle(page -> service.getMembers(mUserLogin, page))
                .blockingGet();
    }
}
