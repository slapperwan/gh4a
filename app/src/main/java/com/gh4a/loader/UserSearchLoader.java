package com.gh4a.loader;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.egit.github.core.SearchUser;
import org.eclipse.egit.github.core.service.UserService;

import android.content.Context;
import android.text.TextUtils;

import com.gh4a.Gh4Application;

public class UserSearchLoader extends BaseLoader<List<SearchUser>> {
    private final String mQuery;

    public UserSearchLoader(Context context, String query) {
        super(context);
        mQuery = query;
    }

    @Override
    public List<SearchUser> doLoadInBackground() throws Exception {
        if (TextUtils.isEmpty(mQuery)) {
            return new ArrayList<>();
        }

        UserService userService = (UserService)
                Gh4Application.get().getService(Gh4Application.USER_SERVICE);
        return userService.searchUsers(mQuery);
    }
}
