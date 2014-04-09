package com.gh4a;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;

import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.client.GitHubRequest;
import org.eclipse.egit.github.core.client.GitHubResponse;

import android.util.Log;

import com.gh4a.utils.StringUtils;

public class ClientForAuthorization extends GitHubClient {

    private String otpCode;

    public ClientForAuthorization(String otpCode) {
        this.otpCode = otpCode;
    }

    public GitHubResponse get(GitHubRequest request) throws IOException {
        HttpURLConnection conn = createGet(request.generateUri());
        try {
            if (!StringUtils.isBlank(otpCode)) {
                conn.setRequestProperty("X-GitHub-OTP", otpCode);
            }

            final int code = conn.getResponseCode();
            if (isOk(code)) {
                return new GitHubResponse(conn, getBody(request,
                        getStream(conn)));
            } else if (isEmpty(code)) {
                return new GitHubResponse(conn, null);
            } else {
                throw createException(getStream(conn), code,
                        conn.getResponseMessage());
            }
        } catch (IOException e) {
            String otpHeader = conn.getHeaderField("X-GitHub-OTP");
            if (!StringUtils.isBlank(otpHeader)
                    && otpHeader.contains("required")) {

                throw getTwoFactorAuthException(e, otpHeader);
            }
            throw e;
        }
    }

    @Override
    public <V> V post(String uri, Object params, Type type) throws IOException {
        HttpURLConnection conn = createPost(uri);
        try {
            if (!StringUtils.isBlank(otpCode)) {
                conn.setRequestProperty("X-GitHub-OTP", otpCode);
            }
            return sendJson(conn, params, type);
        } catch (IOException e) {
            String otpHeader = conn.getHeaderField("X-GitHub-OTP");
            if (!StringUtils.isBlank(otpHeader)
                    && otpHeader.contains("required")) {

                throw getTwoFactorAuthException(e, otpHeader);
            }
            throw e;
        }
    }
    
    private TwoFactorAuthException getTwoFactorAuthException(IOException e, String otpHeader) {
        String twoFactorAuthType = null;
        if (otpHeader.contains("app")) {
            twoFactorAuthType = "app";
        } else if (otpHeader.contains("sms")) {
            twoFactorAuthType = "sms";
        }

        return new TwoFactorAuthException(e, twoFactorAuthType);
    }
    
    private <V> V sendJson(final HttpURLConnection request,
            final Object params, final Type type) throws IOException {
        sendParams(request, params);
        final int code = request.getResponseCode();
        updateRateLimits(request);
        if (isOk(code))
            if (type != null)
                return parseJson(getStream(request), type);
            else
                return null;
        if (isEmpty(code))
            return null;
        throw createException(getStream(request), code,
                request.getResponseMessage());
    }
}
