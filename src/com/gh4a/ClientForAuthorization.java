package com.gh4a;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;

import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.client.GitHubRequest;
import org.eclipse.egit.github.core.client.GitHubResponse;
import org.eclipse.egit.github.core.client.RequestException;


import com.gh4a.utils.StringUtils;

public class ClientForAuthorization extends GitHubClient {
    private String mOtpCode;

    public ClientForAuthorization(String otpCode) {
        mOtpCode = otpCode;
    }

    public GitHubResponse get(GitHubRequest request) throws IOException {
        HttpURLConnection conn = createGet(request.generateUri());
        try {
            if (!StringUtils.isBlank(mOtpCode)) {
                conn.setRequestProperty("X-GitHub-OTP", mOtpCode);
            }

            final int code = conn.getResponseCode();
            if (isOk(code)) {
                return new GitHubResponse(conn, getBody(request, getStream(conn)));
            } else if (isEmpty(code)) {
                return new GitHubResponse(conn, null);
            } else {
                throw createException(getStream(conn), code, conn.getResponseMessage());
            }
        } catch (IOException e) {
            String otpHeader = conn.getHeaderField("X-GitHub-OTP");
            if (!StringUtils.isBlank(otpHeader) && otpHeader.contains("required")) {
                throw getTwoFactorAuthException(e, otpHeader);
            }
            throw e;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    @Override
    public <V> V post(String uri, Object params, Type type) throws IOException {
        HttpURLConnection conn = createPost(uri);
        try {
            if (!StringUtils.isBlank(mOtpCode)) {
                conn.setRequestProperty("X-GitHub-OTP", mOtpCode);
            }
            return sendJson(conn, params, type);
        } catch (IOException e) {
            String otpHeader = conn.getHeaderField("X-GitHub-OTP");
            if (!StringUtils.isBlank(otpHeader)  && otpHeader.contains("required")) {
                throw getTwoFactorAuthException(e, otpHeader);
            }
            throw e;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    @Override
    public void delete(String uri, Object params) throws IOException {
        HttpURLConnection conn = createDelete(uri);
        try {
            if (!StringUtils.isBlank(mOtpCode)) {
                conn.setRequestProperty("X-GitHub-OTP", mOtpCode);
            }
            if (params != null) {
                sendParams(conn, params);
            }
            final int code = conn.getResponseCode();
            updateRateLimits(conn);
            if (!isEmpty(code)) {
                throw new RequestException(parseError(getStream(conn)), code);
            }
        } catch (IOException e) {
            String otpHeader = conn.getHeaderField("X-GitHub-OTP");
            if (!StringUtils.isBlank(otpHeader)  && otpHeader.contains("required")) {
                throw getTwoFactorAuthException(e, otpHeader);
            }
            throw e;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
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
        if (isOk(code)) {
            if (type != null) {
                return parseJson(getStream(request), type);
            }
            return null;
        } else if (isEmpty(code)) {
            return null;
        }
        throw createException(getStream(request), code, request.getResponseMessage());
    }
}
