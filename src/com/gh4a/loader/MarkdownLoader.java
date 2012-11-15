package com.gh4a.loader;

import java.io.IOException;
import java.util.HashMap;

import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.MarkdownService;

import android.content.Context;

import com.gh4a.Constants.LoaderResult;
import com.gh4a.Gh4Application;

public class MarkdownLoader extends BaseLoader {

    private String mText;
    private String mMode;
    
    public MarkdownLoader(Context context, String text, String mode) {
        super(context);
        mText = text;
        mMode = mode;
    }
    
    @Override
    public void doLoadInBackground(HashMap<Integer, Object> result) throws IOException {
        Gh4Application app = (Gh4Application) getContext().getApplicationContext();
        GitHubClient client = new GitHubClient();
        client.setOAuth2Token(app.getAuthToken());
        MarkdownService markdownService = new MarkdownService(client);
        result.put(LoaderResult.DATA, markdownService.getHtml(mText, mMode));
    }
}
