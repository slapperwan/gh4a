package com.gh4a;

import java.net.HttpURLConnection;

import org.eclipse.egit.github.core.client.GitHubClient;

public class DefaultClient extends GitHubClient {

    private String headerAccept;

    public DefaultClient() {
        this.headerAccept = "application/vnd.github.beta.full+json";
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
