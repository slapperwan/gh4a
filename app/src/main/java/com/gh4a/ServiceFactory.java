package com.gh4a;

import android.content.Context;
import android.util.Log;

import com.meisolsson.githubsdk.core.GitHubPaginationInterceptor;
import com.meisolsson.githubsdk.core.ServiceGenerator;
import com.meisolsson.githubsdk.core.StringResponseConverterFactory;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.Cache;
import okhttp3.CacheControl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.moshi.MoshiConverterFactory;

public class ServiceFactory {
    private static final String DEFAULT_HEADER_ACCEPT =
            "application/vnd.github.squirrel-girl-preview,application/vnd.github.v3.full+json";

    private final static HttpLoggingInterceptor LOGGING_INTERCEPTOR = new HttpLoggingInterceptor()
            .setLevel(HttpLoggingInterceptor.Level.BASIC);

    private final static Interceptor PAGINATION_INTRCEPTOR = new GitHubPaginationInterceptor();

    private final static Interceptor CACHE_STATUS_INTERCEPTOR = chain -> {
        Response response = chain.proceed(chain.request());
        Log.d("OkHttp", String.format(Locale.US, "For %s: network return code %d, cache %d",
                response.request().url().toString(),
                response.networkResponse() != null ? response.networkResponse().code() : -1,
                response.cacheResponse() != null ? response.cacheResponse().code() : -1));
        return response;
    };

    private final static Interceptor CACHE_BYPASS_INTERCEPTOR = chain -> {
        Request request = chain.request()
                .newBuilder()
                .addHeader("Cache-Control", "no-cache")
                .build();
        return chain.proceed(request);
    };

    // FIXME: The notifications endpoint currently returns invalid/empty ETags. GH support
    //        says they're looking into it and to use If-Modified-Since in the meantime.
    //        Unfortunately, the Last-Modified header can't be relied on either, as it's not
    //        updated when marking notifications as read :-(
    //        We thus check for the invalid ETag and prevent caching if we found it.
    //        Once this is fixed on server side, this interceptor should be removed.
    private final static CacheControl NO_STORE_CACHE_CONTROL =
            new CacheControl.Builder().noStore().build();
    private final static Interceptor ETAG_WORKAROUND_INTERCEPTOR = chain -> {
        Response response = chain.proceed(chain.request());
        String etag = response.header("ETag");
        if (etag != null && etag.contains("\"\"")) {
            return response.newBuilder()
                    .header("Cache-Control", NO_STORE_CACHE_CONTROL.toString())
                    .build();
        }
        return response;
    };

    private final static Interceptor CACHE_MAX_AGE_INTERCEPTOR = chain -> {
        Response response = chain.proceed(chain.request());
        CacheControl origCacheControl = CacheControl.parse(response.headers());
        // Github sends max-age=60, which leads to problems when we modify stuff and
        // reload data afterwards. Make sure to constrain max age to 2 seconds to only avoid
        // network calls in cases where the exact same data is loaded from multiple places
        // at the same time, and use ETags to avoid useless data transfers otherwise.
        if (origCacheControl.maxAgeSeconds() <= 2) {
            return response;
        }
        CacheControl.Builder newBuilder = new CacheControl.Builder()
                .maxAge(2, TimeUnit.SECONDS);
        if (origCacheControl.maxStaleSeconds() >= 0) {
            newBuilder.maxStale(origCacheControl.maxStaleSeconds(), TimeUnit.SECONDS);
        }
        if (origCacheControl.minFreshSeconds() >= 0) {
            newBuilder.minFresh(origCacheControl.minFreshSeconds(), TimeUnit.SECONDS);
        }
        if (origCacheControl.noCache()) {
            newBuilder.noCache();
        }
        if (origCacheControl.noStore()) {
            newBuilder.noStore();
        }
        if (origCacheControl.noTransform()) {
            newBuilder.noTransform();
        }
        return response.newBuilder()
                .header("Cache-Control", newBuilder.build().toString())
                .build();
    };

    private final static Retrofit.Builder RETROFIT_BUILDER = new Retrofit.Builder()
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .addConverterFactory(new StringResponseConverterFactory())
            .addConverterFactory(MoshiConverterFactory.create(ServiceGenerator.moshi));

    private static OkHttpClient sApiHttpClient;
    private static OkHttpClient sImageHttpClient;

    private final static HashMap<String, Object> sCache = new HashMap<>();

    public static <S> S get(Class<S> serviceClass, boolean bypassCache) {
        return get(serviceClass, bypassCache, null, null, null);
    }

    public static <S> S get(Class<S> serviceClass, boolean bypassCache, String acceptHeader,
            String token, Integer pageSize) {
        String key = makeKey(serviceClass, bypassCache, acceptHeader, token, pageSize);
        S service = (S) sCache.get(key);
        if (service == null) {
            service = createService(serviceClass, bypassCache, acceptHeader, token, pageSize);
            sCache.put(key, service);
        }
        return service;
    }

    private static String makeKey(Class<?> serviceClass, boolean bypassCache,
            String acceptHeader, String token, Integer pageSize) {
        return String.format(Locale.US, "%s-%d-%s-%s-%d",
                serviceClass.getSimpleName(), bypassCache ? 1 : 0,
                acceptHeader != null ? acceptHeader : "",
                token != null ? token : "", pageSize != null ? pageSize : 0);
    }

    private static <S> S createService(Class<S> serviceClass, final boolean bypassCache,
            final String acceptHeader, final String token, final Integer pageSize) {
        OkHttpClient.Builder clientBuilder = sApiHttpClient.newBuilder()
                .addInterceptor(PAGINATION_INTRCEPTOR)
                .addNetworkInterceptor(ETAG_WORKAROUND_INTERCEPTOR)
                .addNetworkInterceptor(CACHE_MAX_AGE_INTERCEPTOR)
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
                                .addQueryParameter("per_page", String.valueOf(pageSize))
                                .build());
                    }
                    if (original.header("Accept") == null) {
                        requestBuilder.addHeader("Accept", acceptHeader != null
                                ? acceptHeader : DEFAULT_HEADER_ACCEPT);
                    }

                    Request request = requestBuilder.build();
                    Gh4Application.trackVisitedUrl(request.url().toString());
                    return chain.proceed(request);
                });

        if (BuildConfig.DEBUG) {
            clientBuilder.addInterceptor(LOGGING_INTERCEPTOR);
            clientBuilder.addInterceptor(CACHE_STATUS_INTERCEPTOR);
        }
        if (bypassCache) {
            clientBuilder.addInterceptor(CACHE_BYPASS_INTERCEPTOR);
        }

        Retrofit retrofit = RETROFIT_BUILDER
                .baseUrl("https://api.github.com")
                .client(clientBuilder.build())
                .build();
        return retrofit.create(serviceClass);
    }

    public static OkHttpClient.Builder getHttpClientBuilder() {
        return sApiHttpClient.newBuilder();
    }

    public static OkHttpClient getImageHttpClient() {
        return sImageHttpClient;
    }

    static void initClient(Context context) {
        sApiHttpClient = new OkHttpClient.Builder()
                .cache(new Cache(new File(context.getCacheDir(), "api-http"), 20 * 1024 * 1024))
                .build();
        sImageHttpClient = new OkHttpClient.Builder()
                .cache(new Cache(new File(context.getCacheDir(), "image-http"), 20 * 1024 * 1024))
                .build();
    }
}