package com.gh4a.loader;

import java.io.IOException;
import java.util.List;

import org.eclipse.egit.github.core.User;
import org.eclipse.egit.github.core.service.OrganizationService;

import android.content.Context;

import com.gh4a.Gh4Application;

public class OrganizationMemberListLoader extends BaseLoader<List<User>> {

    private String mUserLogin;

    public OrganizationMemberListLoader(Context context, String userLogin) {
        super(context);
        mUserLogin = userLogin;
    }

    @Override
    public List<User> doLoadInBackground() throws IOException {
        OrganizationService orgService = (OrganizationService)
                Gh4Application.get().getService(Gh4Application.ORG_SERVICE);
        return orgService.getPublicMembers(mUserLogin);
    }
}
