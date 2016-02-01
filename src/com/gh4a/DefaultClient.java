package com.gh4a;

import org.eclipse.egit.github.core.client.GitHubClient;

import java.net.HttpURLConnection;

public class DefaultClient extends GitHubClient {
    private static final String DEFAULT_HEADER_ACCEPT = "application/vnd.github.beta.full+json";

    public DefaultClient() {
        this(DEFAULT_HEADER_ACCEPT);
    }

    public DefaultClient(String headerAccept) {
        super();
        setHeaderAccept(headerAccept);
    }

    @Override
    protected HttpURLConnection configureRequest(HttpURLConnection request) {
        super.configureRequest(request);
        Gh4Application.trackVisitedUrl(request.getURL().toExternalForm());
        return request;
    }

    @Override
    protected boolean isError(int code) {
        if (code == 401) {
            Gh4Application.get().logout();
        }
        return super.isError(code);
    }
}
