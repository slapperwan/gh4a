package com.gh4a;

import java.io.IOException;

public class TwoFactorAuthException extends IOException {
    private static final long serialVersionUID = 1L;
    private IOException e;
	private String twoFactorAuthType;
	
	public TwoFactorAuthException(IOException e, String twoFactorAuthType) {
		this.e = e;
		this.twoFactorAuthType = twoFactorAuthType;
	}

}
