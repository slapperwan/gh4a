package com.gh4a.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.EditText;

import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.activities.WebViewerActivity;

import org.eclipse.egit.github.core.util.EncodingUtils;

public class MarkdownPreviewWebView extends WebView {

    private String mCssTheme;

    public MarkdownPreviewWebView(Context context) {
        super(context);
        initialize();
    }

    public MarkdownPreviewWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public MarkdownPreviewWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize();
    }

    private void initialize() {
        mCssTheme = Gh4Application.THEME == R.style.DarkTheme
                ? WebViewerActivity.DARK_CSS_THEME : WebViewerActivity.LIGHT_CSS_THEME;

        if (!isInEditMode()) {
            initWebViewSettings(getSettings());
            setContent("");
        }
    }

    public void setEditText(EditText editor) {
        editor.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                setContent(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void initWebViewSettings(WebSettings s) {
        s.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NORMAL);
        s.setLoadsImagesAutomatically(true);
        s.setJavaScriptEnabled(true);
        s.setUseWideViewPort(false);
    }

    private void setContent(String content) {
        String html = generateMarkdownHtml(EncodingUtils.toBase64(content), mCssTheme);
        loadDataWithBaseURL("file:///android_asset/", html, null, "utf-8", null);
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
