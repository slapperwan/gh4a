package com.gh4a.fragment;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.activities.WebViewerActivity;

import org.eclipse.egit.github.core.util.EncodingUtils;

public class CommentPreviewFragment extends Fragment {

    private WebView mPreviewWebView;
    private String mInitialData;
    private String mCssTheme;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mCssTheme = Gh4Application.THEME == R.style.DarkTheme
                ? WebViewerActivity.DARK_CSS_THEME : WebViewerActivity.LIGHT_CSS_THEME;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.comment_preview, container, false);
        mPreviewWebView = (WebView) v.findViewById(R.id.wv_preview);
        initWebViewSettings(mPreviewWebView.getSettings());

        if (mInitialData != null) {
            setContent(mInitialData);
            mInitialData = null;
        } else {
            setContent("");
        }

        return v;
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void initWebViewSettings(WebSettings s) {
        s.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NORMAL);
        s.setLoadsImagesAutomatically(true);
        s.setJavaScriptEnabled(true);
        s.setUseWideViewPort(false);
    }

    public void setContent(String content) {
        if (mPreviewWebView == null) {
            mInitialData = content;
            return;
        }

        String html = generateMarkdownHtml(EncodingUtils.toBase64(content), mCssTheme);
        mPreviewWebView.loadDataWithBaseURL("file:///android_asset/", html, null, "utf-8", null);
    }

    protected String generateMarkdownHtml(String base64Data, String cssTheme) {
        StringBuilder content = new StringBuilder();
        content.append("<html><head>");
        writeScriptInclude(content, "showdown");
        writeScriptInclude(content, "base64");
        writeCssInclude(content, "markdown", cssTheme);
        writeCssInclude(content, "mdpreview", cssTheme);
        content.append("</head>");

        content.append("<body>");
        content.append("<div id='content'></div>");

        content.append("<script>");
        content.append("var text = Base64.decode('");
        content.append(base64Data);
        content.append("');\n");
        content.append("var converter = new showdown.Converter();\n");
        content.append("converter.setFlavor('github');\n");
        content.append("var html = converter.makeHtml(text);\n");
        content.append("document.getElementById('content').innerHTML = html;");
        content.append("</script>");

        content.append("</body></html>");

        return content.toString();
    }

    protected static void writeScriptInclude(StringBuilder builder, String scriptName) {
        builder.append("<script src='file:///android_asset/");
        builder.append(scriptName);
        builder.append(".js' type='text/javascript'></script>");
    }

    protected static void writeCssInclude(StringBuilder builder, String cssType, String cssTheme) {
        builder.append("<link href='file:///android_asset/");
        builder.append(cssType);
        builder.append("-");
        builder.append(cssTheme);
        builder.append(".css' rel='stylesheet' type='text/css'/>");
    }
}
