package com.gh4a.loader;

import java.io.IOException;

import org.eclipse.egit.github.core.service.MarkdownService;

import android.content.Context;

import com.gh4a.Gh4Application;

public class MarkdownLoader extends BaseLoader<String> {

    private String mText;
    private String mMode;

    public MarkdownLoader(Context context, String text, String mode) {
        super(context);
        mText = text;
        mMode = mode;
    }

    @Override
    public String doLoadInBackground() throws IOException {
        MarkdownService markdownService = (MarkdownService)
                Gh4Application.get().getService(Gh4Application.MARKDOWN_SERVICE);
        return markdownService.getHtml(mText, mMode);
    }
}
