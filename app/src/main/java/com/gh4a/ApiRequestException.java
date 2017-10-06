package com.gh4a;

import com.meisolsson.githubsdk.core.ServiceGenerator;
import com.meisolsson.githubsdk.model.ClientErrorResponse;

import java.io.IOException;

import retrofit2.Response;

public class ApiRequestException extends IOException {
    private static final long serialVersionUID = -4331443972707730572L;

    private final ClientErrorResponse mResponse;
    private final int mStatus;

    public ApiRequestException(Response response) {
        mStatus = response.code();

        ClientErrorResponse error = null;
        try {
            error = ServiceGenerator.moshi
                    .adapter(ClientErrorResponse.class)
                    .fromJson(response.errorBody().source());
        } catch (IOException e) {
            // ignored
        }
        mResponse = error;
    }

    public int getStatus() {
        return mStatus;
    }

    public ClientErrorResponse getResponse() {
        return mResponse;
    }
}
