package com.gh4a.widget;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.TextView;
import android.widget.Toast;

import com.gh4a.R;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatTextView;

public class StyleableTextView extends AppCompatTextView {

    private static final LinkMovementMethod CHECKING_LINK_METHOD = new LinkMovementMethod() {
        @Override
        public boolean onTouchEvent(@NonNull TextView widget,
                @NonNull Spannable buffer, @NonNull MotionEvent event) {
            try {
                return super.onTouchEvent(widget, buffer, event);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(widget.getContext(), R.string.link_not_openable, Toast.LENGTH_LONG)
                        .show();
                return true;
            } catch (SecurityException e) {
                // some apps have intent filters set for the VIEW action for
                // internal, non-exported activities
                // -> ignore
                return true;
            }
        }
    };

    public StyleableTextView(Context context) {
        super(context, null);
    }

    public StyleableTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initAttributes(context, attrs, 0);
    }

    public StyleableTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initAttributes(context, attrs, defStyle);
    }

    private void initAttributes(Context context, AttributeSet attrs, int defStyle) {
        Resources.Theme theme = context.getTheme();
        boolean needsLinkHandling = false;

        // If text is selectable, TextView.onTouchEvent() triggers a link click action,
        // which might cause double link open actions. Since we don't need auto-linkification
        // (we install our own movement method), just turn off auto-installation of
        // LinkMovementMethod
        setLinksClickable(false);

        TypedArray a = theme.obtainStyledAttributes(attrs, R.styleable.StyleableTextView, defStyle, 0);
        for (int i = 0; i < a.getIndexCount(); i++) {
            int attr = a.getIndex(i);
            if (attr == R.styleable.StyleableTextView_needsLinkHandling) {
                needsLinkHandling = a.getBoolean(attr, false);
            }
        }
        a.recycle();

        if (needsLinkHandling) {
            setMovementMethod(CHECKING_LINK_METHOD);
        }
    }

    // workaround for https://code.google.com/p/android/issues/detail?id=208169
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (isTextSelectable() && isEnabled()) {
            MovementMethod method = getMovementMethod();
            setMovementMethod(null);
            setMovementMethod(method);
        }
    }

    // workaround for https://code.google.com/p/android/issues/detail?id=191430
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        int startSelection = getSelectionStart();
        int endSelection = getSelectionEnd();
        if (startSelection != endSelection && event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            final CharSequence text = getText();
            setText(null);
            setText(text);
        }
        return super.dispatchTouchEvent(event);
    }
}
