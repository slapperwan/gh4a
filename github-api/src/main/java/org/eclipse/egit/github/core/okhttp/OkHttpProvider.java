package org.eclipse.egit.github.core.okhttp;

import com.squareup.okhttp.OkHttpClient;

public class OkHttpProvider {
    private static final OkHttpClient sOkHttpClient = new OkHttpClient();

    public static OkHttpClient getOkHttpClient(){
        return sOkHttpClient;
    }
}
