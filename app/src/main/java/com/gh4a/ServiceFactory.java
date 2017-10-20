package com.gh4a;

import com.meisolsson.githubsdk.core.GitHubPaginationInterceptor;
import com.meisolsson.githubsdk.core.ServiceGenerator;
import com.meisolsson.githubsdk.core.StringResponseConverterFactory;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.moshi.MoshiConverterFactory;

public class ServiceFactory {
    private static final String DEFAULT_HEADER_ACCEPT =
            "application/vnd.github.squirrel-girl-preview,application/vnd.github.v3.full+json";

    private final static HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor()
            .setLevel(HttpLoggingInterceptor.Level.BASIC);

    private final static Retrofit.Builder builder = new Retrofit.Builder()
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .addConverterFactory(new StringResponseConverterFactory())
            .addConverterFactory(MoshiConverterFactory.create(ServiceGenerator.moshi));

    private final static OkHttpClient httpClient = new OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(new GitHubPaginationInterceptor())
            .build();

    public static <S> S createService(Class<S> serviceClass, final String acceptHeader,
            final String token, final Integer pageSize) {
        OkHttpClient client = httpClient.newBuilder()
                .addInterceptor(chain -> {
                    Request original = chain.request();

                    Request.Builder requestBuilder = original.newBuilder()
                            .method(original.method(), original.body());

                    String tokenToUse = token != null
                            ? token : Gh4Application.get().getAuthToken();
                    if (tokenToUse != null) {
                        requestBuilder.header("Authorization", "Token " + tokenToUse);
                    }
                    if (pageSize != null) {
                        requestBuilder.url(original.url().newBuilder()
                                .addQueryParameter("page_size", String.valueOf(pageSize))
                                .build());
                    }
                    if (original.header("Accept") == null) {
                        requestBuilder.addHeader("Accept", acceptHeader != null
                                ? acceptHeader : DEFAULT_HEADER_ACCEPT);
                    }

                    Request request = requestBuilder.build();
                    Gh4Application.trackVisitedUrl(request.url().toString());
                    return chain.proceed(request);
                })
                .build();

        Retrofit retrofit = builder.baseUrl("https://api.github.com")
                .client(client)
                .build();
        return retrofit.create(serviceClass);
    }
}
