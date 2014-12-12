package com.gh4a;

import java.io.IOException;

public class TwoFactorAuthException extends IOException {
    private static final long serialVersionUID = 1L;
	private String twoFactorAuthType;
	
	public TwoFactorAuthException(IOException e, String twoFactorAuthType) {
        super(e);
		this.twoFactorAuthType = twoFactorAuthType;
	}

    public String getTwoFactorAuthType() {
        return twoFactorAuthType;
    }
}
