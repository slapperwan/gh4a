package com.gh4a.utils;

import android.app.Dialog;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.EdgeEffect;
import android.widget.EditText;
import android.widget.MultiAutoCompleteTextView;
import android.widget.TextView;

import com.gh4a.R;
import com.gh4a.widget.IssueLabelSpan;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.meisolsson.githubsdk.model.Label;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

public class UiUtils {
    public static void hideImeForView(View view) {
        if (view == null) {
            return;
        }
        InputMethodManager imm = (InputMethodManager)
                view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
    }

    public static void showImeForView(View view) {
        if (view == null) {
            return;
        }
        InputMethodManager imm = (InputMethodManager)
                view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
    }

    public static int textColorForBackground(Context context, int backgroundColor) {
        int red = Color.red(backgroundColor);
        int green = Color.green(backgroundColor);
        int blue = Color.blue(backgroundColor);
        int luminance = Math.round(0.213F * red + 0.715F * green + 0.072F * blue);

        if (luminance >= 128) {
            return ContextCompat.getColor(context, R.color.label_fg_dark);
        }
        return ContextCompat.getColor(context, R.color.label_fg_light);
    }

    public static void updateViewVisibility(View view, boolean animate, boolean show) {
        int visibility = show ? View.VISIBLE : View.GONE;
        if (view.getVisibility() == visibility) {
            return;
        }

        if (animate) {
            Animation anim = AnimationUtils.loadAnimation(view.getContext(),
                    show ? android.R.anim.fade_in : android.R.anim.fade_out);
            view.startAnimation(anim);
        } else {
            view.clearAnimation();
        }
        view.setVisibility(visibility);
    }

    public static void trySetListOverscrollColor(RecyclerView view, int color) {
        RecyclerViewEdgeColorHelper helper =
                (RecyclerViewEdgeColorHelper) view.getTag(R.id.EdgeColorHelper);
        if (helper == null) {
            helper = new RecyclerViewEdgeColorHelper(view);
            view.setTag(R.id.EdgeColorHelper, helper);
        }
        helper.setColor(color);
    }

    private static class RecyclerViewEdgeColorHelper implements ViewTreeObserver.OnGlobalLayoutListener {
        private final RecyclerView mView;
        private int mColor;
        private EdgeEffect mTopEffect, mBottomEffect;

        private static Method sTopEnsureMethod, sBottomEnsureMethod;
        private static Field sTopEffectField, sBottomEffectField;

        public RecyclerViewEdgeColorHelper(RecyclerView view) {
            mView = view;
            mColor = 0;
            mView.getViewTreeObserver().addOnGlobalLayoutListener(this);
        }
        public void setColor(int color) {
            mColor = color;
            applyIfPossible();
        }
        @Override
        public void onGlobalLayout() {
            applyIfPossible();
        }

        private void applyIfPossible() {
            if (!ensureStaticMethodsAndFields()) {
                return;
            }
            try {
                Object topEffect = sTopEffectField.get(mView);
                Object bottomEffect = sBottomEffectField.get(mView);
                if (topEffect == null || bottomEffect == null) {
                    sTopEnsureMethod.invoke(mView);
                    sBottomEnsureMethod.invoke(mView);

                    mTopEffect = (EdgeEffect) sTopEffectField.get(mView);
                    mBottomEffect = (EdgeEffect) sBottomEffectField.get(mView);
                } else {
                    mTopEffect = (EdgeEffect) topEffect;
                    mBottomEffect = (EdgeEffect) bottomEffect;
                }
            } catch (IllegalAccessException | InvocationTargetException e) {
                mTopEffect = mBottomEffect = null;
            }
            applyColor(mTopEffect);
            applyColor(mBottomEffect);
        }

        private void applyColor(EdgeEffect effect) {
            if (effect != null) {
                final int alpha = Color.alpha(effect.getColor());
                effect.setColor(Color.argb(alpha, Color.red(mColor),
                        Color.green(mColor), Color.blue(mColor)));
            }
        }

        private boolean ensureStaticMethodsAndFields() {
            if (sTopEnsureMethod != null && sBottomEnsureMethod != null) {
                return true;
            }
            try {
                sTopEnsureMethod = RecyclerView.class.getDeclaredMethod("ensureTopGlow");
                sTopEnsureMethod.setAccessible(true);
                sBottomEnsureMethod = RecyclerView.class.getDeclaredMethod("ensureBottomGlow");
                sBottomEnsureMethod.setAccessible(true);
                sTopEffectField = RecyclerView.class.getDeclaredField("mTopGlow");
                sTopEffectField.setAccessible(true);
                sBottomEffectField = RecyclerView.class.getDeclaredField("mBottomGlow");
                sBottomEffectField.setAccessible(true);
                return true;
            } catch (NoSuchMethodException | NoSuchFieldException e) {
                // ignored
            }
            return false;
        }
    }

    public static int mixColors(int startColor, int endColor, float fraction) {
        // taken from ArgbEvaluator.evaluate
        int startA = (startColor >> 24) & 0xff;
        int startR = (startColor >> 16) & 0xff;
        int startG = (startColor >> 8) & 0xff;
        int startB = startColor & 0xff;

        int endA = (endColor >> 24) & 0xff;
        int endR = (endColor >> 16) & 0xff;
        int endG = (endColor >> 8) & 0xff;
        int endB = endColor & 0xff;

        return ((startA + (int)(fraction * (endA - startA))) << 24) |
                ((startR + (int)(fraction * (endR - startR))) << 16) |
                ((startG + (int)(fraction * (endG - startG))) << 8) |
                ((startB + (int)(fraction * (endB - startB))));
    }

    public static boolean canViewScrollUp(View view) {
        if (view == null) {
            return false;
        }
        return view.canScrollVertically(-1);
    }

    public static Dialog createProgressDialog(Context context, @StringRes int messageResId) {
        View content = LayoutInflater.from(context).inflate(R.layout.progress_dialog, null);
        TextView message = content.findViewById(R.id.message);

        message.setText(messageResId);
        return new MaterialAlertDialogBuilder(context)
                .setView(content)
                .create();
    }

    public static @ColorInt int resolveColor(Context context, @AttrRes int styledAttributeId) {
        TypedArray a = context.obtainStyledAttributes(new int[] {
            styledAttributeId
        });
        int color = a.getColor(0, 0);
        a.recycle();
        return color;
    }

    public static abstract class EmptinessWatchingTextWatcher implements TextWatcher {
        public EmptinessWatchingTextWatcher(EditText editor) {
            afterTextChanged(editor.getText());
        }
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }
        @Override
        public void afterTextChanged(Editable s) {
            onIsEmpty(s == null || s.length() == 0);
        }
        public abstract void onIsEmpty(boolean isEmpty);
    }

    public static CharSequence getSelectedText(TextView view) {
        int min = 0;
        int max = view.length();

        if (view.isFocused()) {
            int selectionStart = view.getSelectionStart();
            int selectionEnd = view.getSelectionEnd();

            min = Math.max(0, Math.min(selectionStart, selectionEnd));
            max = Math.max(0, Math.max(selectionStart, selectionEnd));
        }

        return view.getText().subSequence(min, max);
    }

    public static abstract class QuoteActionModeCallback implements ActionMode.Callback {
        private final TextView mView;

        public QuoteActionModeCallback(TextView view) {
            mView = view;
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.comment_selection_menu, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            if (item.getItemId() != R.id.quote) {
                return false;
            }

            onTextQuoted(UiUtils.getSelectedText(mView));
            mode.finish();
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
        }

        public abstract void onTextQuoted(CharSequence text);
    }

    public static class WhitespaceTokenizer implements MultiAutoCompleteTextView.Tokenizer {
        @Override
        public int findTokenStart(CharSequence text, int cursor) {
            while (cursor > 0 && !Character.isWhitespace(text.charAt(cursor - 1))) {
                cursor--;
            }

            return cursor;
        }

        @Override
        public int findTokenEnd(CharSequence text, int cursor) {
            int len = text.length();

            while (cursor < len) {
                if (Character.isWhitespace(text.charAt(cursor))) {
                    return cursor;
                } else {
                    cursor++;
                }
            }

            return len;
        }

        @Override
        public CharSequence terminateToken(CharSequence text) {
            return text;
        }
    }

    @NonNull
    public static SpannableStringBuilder formatLabelList(Context context, List<Label> labels) {
        SpannableStringBuilder builder = new SpannableStringBuilder();
        for (Label label : labels) {
            int pos = builder.length();
            IssueLabelSpan span = new IssueLabelSpan(context, label, true);
            builder.append(label.name());
            builder.setSpan(span, pos, pos + label.name().length(), 0);
        }
        return builder;
    }

    public static int limitViewHeight(int heightMeasureSpec, int maxHeight) {
        int heightSize = View.MeasureSpec.getSize(heightMeasureSpec);
        int heightMode = View.MeasureSpec.getMode(heightMeasureSpec);

        switch (heightMode) {
            case View.MeasureSpec.AT_MOST:
            case View.MeasureSpec.EXACTLY:
                heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(
                        Math.min(heightSize, maxHeight), heightMode);
                break;
            case View.MeasureSpec.UNSPECIFIED:
                heightMeasureSpec =
                        View.MeasureSpec.makeMeasureSpec(maxHeight, View.MeasureSpec.AT_MOST);
                break;
        }

        return heightMeasureSpec;
    }

    public static void setMenuItemText(Context context, MenuItem item, String title,
            String subtitle) {
        SpannableStringBuilder builder = new SpannableStringBuilder();
        builder.append(title).append("\n");

        int start = builder.length();
        builder.append(subtitle);

        int secondaryTextColor = UiUtils.resolveColor(context, android.R.attr.textColorSecondary);
        builder.setSpan(new ForegroundColorSpan(secondaryTextColor), start, builder.length(), 0);

        item.setTitle(builder);
    }
}
