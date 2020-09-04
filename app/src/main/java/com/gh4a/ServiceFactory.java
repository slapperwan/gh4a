package com.gh4a;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.gh4a.utils.Optional;
import com.meisolsson.githubsdk.core.GitHubPaginationInterceptor;
import com.meisolsson.githubsdk.core.ServiceGenerator;
import com.meisolsson.githubsdk.core.StringResponseConverterFactory;
import com.meisolsson.githubsdk.service.checks.ChecksService;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import io.reactivex.Single;
import okhttp3.Cache;
import okhttp3.CacheControl;
import okhttp3.ConnectionSpec;
import okhttp3.Credentials;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.TlsVersion;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.moshi.MoshiConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public class ServiceFactory {
    private static final String DEFAULT_HEADER_ACCEPT =
            "application/vnd.github.squirrel-girl-preview," // reactions API preview
            + "application/vnd.github.v3.full+json";
    private static final String CHECKS_API_HEADER_ACCEPT =
            "application/vnd.github.antiope-preview," // checks API preview
            + "application/vnd.github.v3.full+json";

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
                        final String header;
                        if (acceptHeader != null) {
                            header = acceptHeader;
                        } else if (serviceClass == ChecksService.class) {
                            header = CHECKS_API_HEADER_ACCEPT;
                        } else {
                            header = DEFAULT_HEADER_ACCEPT;
                        }
                        requestBuilder.addHeader("Accept", header);
                    }

                    return chain.proceed(requestBuilder.build());
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

    public static LoginService createLoginService(String userName, String password,
            Optional.Supplier<String> otpCodeSupplier) {
        OkHttpClient.Builder clientBuilder = sApiHttpClient.newBuilder()
                .addInterceptor(chain -> {
                    String otpCode = otpCodeSupplier.get();
                    Request request = chain.request();
                    if (otpCode != null) {
                        request = request.newBuilder()
                                .header("X-GitHub-OTP", otpCode)
                                .build();
                    }
                    return chain.proceed(request);
                })
                .authenticator((route, response) -> {
                    if (response.priorResponse() != null) {
                        return null;
                    }
                    return response.request().newBuilder()
                            .header("Authorization", Credentials.basic(userName, password))
                            .build();
                });

        if (BuildConfig.DEBUG) {
            clientBuilder.addInterceptor(LOGGING_INTERCEPTOR);
            clientBuilder.addInterceptor(CACHE_STATUS_INTERCEPTOR);
        }
        clientBuilder.addInterceptor(CACHE_BYPASS_INTERCEPTOR);

        Retrofit retrofit = RETROFIT_BUILDER
                .baseUrl("https://api.github.com")
                .client(clientBuilder.build())
                .build();
        return retrofit.create(LoginService.class);
    }

    public static OkHttpClient.Builder getHttpClientBuilder() {
        return sApiHttpClient.newBuilder();
    }

    public static OkHttpClient getImageHttpClient() {
        return sImageHttpClient;
    }

    static void initClient(Context context) {
        sApiHttpClient = enableTls12IfNeeded(new OkHttpClient.Builder())
                .cache(new Cache(new File(context.getCacheDir(), "api-http"), 20 * 1024 * 1024))
                .build();
        sImageHttpClient = new OkHttpClient.Builder()
                .cache(new Cache(new File(context.getCacheDir(), "image-http"), 20 * 1024 * 1024))
                .build();
    }

    private static OkHttpClient.Builder enableTls12IfNeeded(OkHttpClient.Builder builder) {
        if (Build.VERSION.SDK_INT < 22) {
            try {
                SSLContext sc = SSLContext.getInstance("TLSv1.2");
                sc.init(null, null, null);

                TrustManagerFactory tmf = TrustManagerFactory.getInstance(
                        TrustManagerFactory.getDefaultAlgorithm());
                tmf.init((KeyStore) null);
                TrustManager[] trustManagers = tmf.getTrustManagers();
                if (trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager)) {
                    throw new IllegalStateException("Unexpected default trust managers:"
                            + Arrays.toString(trustManagers));
                }
                X509TrustManager tm = (X509TrustManager) trustManagers[0];

                ConnectionSpec cs = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                        .tlsVersions(TlsVersion.TLS_1_2)
                        .build();

                List<ConnectionSpec> specs = new ArrayList<>();
                specs.add(cs);
                specs.add(ConnectionSpec.COMPATIBLE_TLS);
                specs.add(ConnectionSpec.CLEARTEXT);

                builder.sslSocketFactory(new Tls12SocketFactory(sc.getSocketFactory()), tm);
                builder.connectionSpecs(specs);
            } catch (Exception exc) {
                Log.e("OkHttpTLSCompat", "Error while setting TLS 1.2", exc);
            }
        }
        return builder;
    }

    public interface LoginService {
        @GET("/authorizations")
        Single<retrofit2.Response<List<AuthorizationResponse>>> getAuthorizations();
        @POST("/authorizations")
        Single<retrofit2.Response<AuthorizationResponse>> createAuthorization(
                @Body AuthorizationRequest request);
        @DELETE("/authorizations/{id}")
        Single<retrofit2.Response<Void>> deleteAuthorization(@Path("id") int id);

        class AuthorizationRequest {
            private String fingerprint;
            private String[] scopes;
            private String note;

            public AuthorizationRequest(String scopes, String note, String fingerprint) {
                this.scopes = scopes.split(",");
                this.note = note;
                this.fingerprint = fingerprint;
            }
        }

        class AuthorizationResponse {
            private int id;
            private String token;
            private String note;
            private String fingerprint;

            public int id() {
                return id;
            }
            public String token() {
                return token;
            }
            public String note() {
                return note;
            }
            public String fingerprint() {
                return fingerprint;
            }
        }
    }

    private static class Tls12SocketFactory extends SSLSocketFactory {
        private static final String[] TLS_V12_ONLY = {"TLSv1.2"};

        final SSLSocketFactory delegate;

        public Tls12SocketFactory(SSLSocketFactory base) {
            this.delegate = base;
        }

        @Override
        public String[] getDefaultCipherSuites() {
            return delegate.getDefaultCipherSuites();
        }

        @Override
        public String[] getSupportedCipherSuites() {
            return delegate.getSupportedCipherSuites();
        }

        @Override
        public Socket createSocket(Socket s, String host, int port, boolean autoClose)
                throws IOException {
            return patch(delegate.createSocket(s, host, port, autoClose));
        }

        @Override
        public Socket createSocket(String host, int port) throws IOException {
            return patch(delegate.createSocket(host, port));
        }

        @Override
        public Socket createSocket(String host, int port, InetAddress localHost, int localPort)
                throws IOException {
            return patch(delegate.createSocket(host, port, localHost, localPort));
        }

        @Override
        public Socket createSocket(InetAddress host, int port) throws IOException {
            return patch(delegate.createSocket(host, port));
        }

        @Override
        public Socket createSocket(InetAddress address, int port,
                InetAddress localAddress, int localPort) throws IOException {
            return patch(delegate.createSocket(address, port, localAddress, localPort));
        }

        private Socket patch(Socket s) {
            if (s instanceof SSLSocket) {
                ((SSLSocket) s).setEnabledProtocols(TLS_V12_ONLY);
            }
            return s;
        }
    }
}