package com.gh4a.model;

import io.reactivex.Single;
import retrofit2.Response;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface GitHubFeedService {
    @GET("{url}")
    Single<Response<GitHubFeed>> getFeed(@Path(value = "url", encoded = true) String url);

    @GET("https://github.blog/all.atom")
    Single<Response<GitHubFeed>> getBlogFeed();
}
