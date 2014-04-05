package com.gh4a;

import java.io.IOException;
import java.net.HttpURLConnection;

import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.client.GitHubRequest;
import org.eclipse.egit.github.core.client.GitHubResponse;

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
            return new GitHubResponse(conn, getBody(request,
                    getStream(conn)));
        } catch (IOException e) {
            String otpHeader = conn.getHeaderField("X-GitHub-OTP");
            if (!StringUtils.isBlank(otpHeader)
                    && otpHeader.contains("required")) {
                
                String twoFactorAuthType = null;
                if (otpHeader.contains("app")) {
                    twoFactorAuthType = "app";
                } else if (otpHeader.contains("sms")) {
                    twoFactorAuthType = "sms";
                }
                
                throw new TwoFactorAuthException(e, twoFactorAuthType);
            }
            throw e;
        }
    }
}
