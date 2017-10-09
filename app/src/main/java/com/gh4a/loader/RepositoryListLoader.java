package com.gh4a.loader;

import android.content.Context;

import com.gh4a.ApiRequestException;
import com.gh4a.Gh4Application;
import com.gh4a.ServiceFactory;
import com.gh4a.utils.ApiHelpers;
import com.meisolsson.githubsdk.model.Page;
import com.meisolsson.githubsdk.model.Repository;
import com.meisolsson.githubsdk.model.UserType;
import com.meisolsson.githubsdk.service.repositories.RepositoryService;

import java.util.Collection;
import java.util.Map;

import io.reactivex.Single;
import retrofit2.Response;

public class RepositoryListLoader extends BaseLoader<Collection<Repository>> {
    private final String mLogin;
    private final Map<String, String> mFilterData;
    private final int mSize;
    private final UserType mUserType;

    public RepositoryListLoader(Context context, String login, UserType userType,
            Map<String, String> filterData, int size) {
        super(context);
        this.mLogin = login;
        this.mFilterData = filterData;
        this.mSize = size;
        this.mUserType = userType;
    }

    @Override
    public Collection<Repository> doLoadInBackground() throws ApiRequestException {
        RepositoryService service = ServiceFactory.createService(
                RepositoryService.class, null, null, mSize);
        final Single<Response<Page<Repository>>> observable;

        if (ApiHelpers.loginEquals(mLogin, Gh4Application.get().getAuthLogin())) {
            observable = service.getUserRepositories(mFilterData, 0);
        } else if (mUserType == UserType.Organization) {
            observable = service.getOrganizationRepositories(mLogin, mFilterData, 0);
        } else {
            observable = service.getUserRepositories(mLogin, mFilterData, 0);
        }

        return ApiHelpers.throwOnFailure(observable.blockingGet()).items();
    }
}
