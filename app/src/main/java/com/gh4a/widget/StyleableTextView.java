package com.gh4a.widget;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Typeface;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatTextView;

import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.TextView;
import android.widget.Toast;

import com.gh4a.R;
import com.gh4a.utils.TypefaceCache;

public class StyleableTextView extends AppCompatTextView {
    private static final int[] TEXT_APPEARANCE_ATTRS = new int[] {
        android.R.attr.textAppearance
    };

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


    private int mTypefaceValue = TypefaceCache.TF_REGULAR;

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

    public int getTypefaceValue() {
        return mTypefaceValue;
    }

    private void initAttributes(Context context, AttributeSet attrs, int defStyle) {
        Resources.Theme theme = context.getTheme();
        boolean needsLinkHandling = false;
        TypedArray appearance = null;

        // If text is selectable, TextView.onTouchEvent() triggers a link click action,
        // which might cause double link open actions. Since we don't need auto-linkification
        // (we install our own movement method), just turn off auto-installation of
        // LinkMovementMethod
        setLinksClickable(false);

        if (attrs != null) {
            TypedArray a = theme.obtainStyledAttributes(attrs, TEXT_APPEARANCE_ATTRS, defStyle, 0);
            int ap = a.getResourceId(0, -1);
            if (ap != -1) {
                appearance = theme.obtainStyledAttributes(ap, R.styleable.StyleableTextView);
            }
            a.recycle();
        }

        if (appearance != null) {
            int n = appearance.getIndexCount();
            for (int i = 0; i < n; i++) {
                int attr = appearance.getIndex(i);

                switch (attr) {
                    case R.styleable.StyleableTextView_ghFont:
                        mTypefaceValue = appearance.getInt(attr, -1);
                        break;
                    case R.styleable.StyleableTextView_needsLinkHandling:
                        needsLinkHandling = appearance.getBoolean(attr, false);
                        break;
                }
            }
        }

        TypedArray a = theme.obtainStyledAttributes(attrs, R.styleable.StyleableTextView, defStyle, 0);
        int n = a.getIndexCount();

        for (int i = 0; i < n; i++) {
            int attr = a.getIndex(i);

            switch (attr) {
                case R.styleable.StyleableTextView_ghFont:
                    mTypefaceValue = a.getInt(attr, -1);
                    break;
                case R.styleable.StyleableTextView_needsLinkHandling:
                    needsLinkHandling = a.getBoolean(attr, false);
                    break;
            }
        }

        a.recycle();

        if (needsLinkHandling) {
            setMovementMethod(CHECKING_LINK_METHOD);
        }
        if (!isInEditMode()) {
            setTypeface(TypefaceCache.getTypeface(mTypefaceValue));
        }
    }

    @Override
    public void setTypeface(Typeface tf, int style) {
        if (tf == getTypeface()) {
            setTypeface(TypefaceCache.getTypeface(mTypefaceValue, style));
        } else {
            super.setTypeface(tf, style);
        }
    }

    // workaround for https://code.google.com/p/android/issues/detail?id=208169
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (isTextSelectable() && isEnabled()) {
            setEnabled(false);
            setEnabled(true);
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
