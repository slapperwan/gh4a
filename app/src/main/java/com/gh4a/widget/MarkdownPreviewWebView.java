package com.gh4a.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import androidx.core.view.NestedScrollingChild2;
import androidx.core.view.NestedScrollingChildHelper;
import androidx.core.view.ViewCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.EditText;

import com.gh4a.R;
import com.gh4a.activities.WebViewerActivity;
import com.gh4a.utils.HtmlUtils;
import com.gh4a.utils.StringUtils;

public class MarkdownPreviewWebView extends WebView implements NestedScrollingChild2 {
    private final NestedScrollingChildHelper mChildHelper;
    private final int[] mScrollOffset = new int[2];
    private final int[] mScrollConsumed = new int[2];
    private int mNestedOffsetY;
    private int mLastY;
    private final String mCssTheme;

    public MarkdownPreviewWebView(Context context) {
        this(context, null);
    }

    public MarkdownPreviewWebView(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.webViewStyle);
    }

    public MarkdownPreviewWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mChildHelper = new NestedScrollingChildHelper(this);
        setNestedScrollingEnabled(true);

        mCssTheme = getResources().getBoolean(R.bool.is_dark_theme)
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

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        boolean result;

        MotionEvent event = MotionEvent.obtain(ev);
        final int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            mNestedOffsetY = 0;
        }

        int eventY = (int) event.getY();
        event.offsetLocation(0, mNestedOffsetY);

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mLastY = eventY;
                startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_TOUCH);
                result = super.onTouchEvent(event);
                break;
            case MotionEvent.ACTION_MOVE:
                int deltaY = mLastY - eventY;
                if (dispatchNestedPreScroll(0, deltaY, mScrollConsumed,
                        mScrollOffset, ViewCompat.TYPE_TOUCH)) {
                    deltaY -= mScrollConsumed[1];
                    mLastY = eventY - mScrollOffset[1];
                    event.offsetLocation(0, -mScrollOffset[1]);
                    mNestedOffsetY += mScrollOffset[1];
                }

                result = super.onTouchEvent(event);

                if (dispatchNestedScroll(0, mScrollOffset[1], 0, deltaY,
                        mScrollOffset, ViewCompat.TYPE_TOUCH)) {
                    event.offsetLocation(0, mScrollOffset[1]);
                    mNestedOffsetY += mScrollOffset[1];
                    mLastY -= mScrollOffset[1];
                }
                break;
            default:
                stopNestedScroll(ViewCompat.TYPE_TOUCH);
                result = super.onTouchEvent(event);
                break;
        }

        return result;
    }

    @Override
    public void setNestedScrollingEnabled(boolean enabled) {
        mChildHelper.setNestedScrollingEnabled(enabled);
    }

    @Override
    public boolean isNestedScrollingEnabled() {
        return mChildHelper.isNestedScrollingEnabled();
    }

    @Override
    public boolean startNestedScroll(int axes, int type) {
        return mChildHelper.startNestedScroll(axes, type);
    }

    @Override
    public void stopNestedScroll(int type) {
        mChildHelper.stopNestedScroll(type);
    }

    @Override
    public boolean hasNestedScrollingParent(int type) {
        return mChildHelper.hasNestedScrollingParent(type);
    }

    @Override
    public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed,
            int dxUnconsumed, int dyUnconsumed,  int[] offsetInWindow, int type) {
        return mChildHelper.dispatchNestedScroll(dxConsumed, dyConsumed,
                dxUnconsumed, dyUnconsumed, offsetInWindow, type);
    }

    @Override
    public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed,
            int[] offsetInWindow, int type) {
        return mChildHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow, type);
    }

    @Override
    public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed) {
        return mChildHelper.dispatchNestedFling(velocityX, velocityY, consumed);
    }

    @Override
    public boolean dispatchNestedPreFling(float velocityX, float velocityY) {
        return mChildHelper.dispatchNestedPreFling(velocityX, velocityY);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void initWebViewSettings(WebSettings s) {
        s.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NORMAL);
        s.setLoadsImagesAutomatically(true);
        s.setJavaScriptEnabled(true);
        s.setUseWideViewPort(false);
    }

    private void setContent(String content) {
        String html = generateMarkdownHtml(StringUtils.toBase64(content), mCssTheme);
        loadDataWithBaseURL("file:///android_asset/", html, null, "utf-8", null);
    }

    private String generateMarkdownHtml(String base64Data, String cssTheme) {
        StringBuilder content = new StringBuilder();
        content.append("<html><head>");
        HtmlUtils.writeScriptInclude(content, "showdown");
        HtmlUtils.writeCssInclude(content, "markdown", cssTheme);
        HtmlUtils.writeCssInclude(content, "mdpreview", cssTheme);
        content.append("</head>");

        content.append("<body>");
        content.append("<div id='content'></div>");

        addJavascriptInterface(new Base64JavascriptInterface(), "Base64");
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

    private static class Base64JavascriptInterface {
        @JavascriptInterface
        public String decode(String base64) {
            return StringUtils.fromBase64(base64);
        }
    }
}
