package com.gh4a;

import org.eclipse.egit.github.core.client.GitHubClient;

import java.net.HttpURLConnection;

public class DefaultClient extends GitHubClient {
    private static final String DEFAULT_HEADER_ACCEPT = "application/vnd.github.beta.full+json";
    private String headerAccept;

    public DefaultClient() {
        this(DEFAULT_HEADER_ACCEPT);
    }

    public DefaultClient(String headerAccept) {
        this.headerAccept = headerAccept;
    }

    @Override
    protected HttpURLConnection configureRequest(HttpURLConnection request) {
        super.configureRequest(request);
        request.setRequestProperty(HEADER_ACCEPT, headerAccept);
        return request;
    }
}
