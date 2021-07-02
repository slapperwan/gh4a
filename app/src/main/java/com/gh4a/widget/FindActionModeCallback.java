/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.gh4a.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.text.Editable;
import android.text.Selection;
import android.text.Spannable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.view.ActionMode;
import androidx.core.view.MenuItemCompat;

import com.gh4a.R;
import com.gh4a.utils.UiUtils;

public class FindActionModeCallback implements ActionMode.Callback, TextWatcher, WebView.FindListener {
    private View mCustomView;
    private EditText mEditText;
    private TextView mMatches;
    private WebView mWebView;
    private MenuItem mPrevItem;
    private MenuItem mNextItem;
    private boolean mHasStartedSearch;
    private int mNumberOfMatches;

    public FindActionModeCallback(Context context) {
        mCustomView = LayoutInflater.from(context).inflate(R.layout.webview_find, null);
        mEditText = mCustomView.findViewById(R.id.edit);
        setText("");
        mMatches = mCustomView.findViewById(R.id.matches);
    }

    /**
     * Place text in the text field so it can be searched for.  Need to press
     * the find next or find previous button to find all of the matches.
     */
    public void setText(String text) {
        mEditText.setText(text);
        Spannable span = mEditText.getText();
        int length = span.length();
        // Ideally, we would like to set the selection to the whole field,
        // but this brings up the Text selection CAB, which dismisses this
        // one.
        Selection.setSelection(span, length, length);
        // Necessary each time we set the text, so that this will watch
        // changes to it.
        span.setSpan(this, 0, length, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        mHasStartedSearch = false;
    }

    /**
     * Set the WebView to search.
     *
     * @param webView an implementation of WebView
     */
    public void setWebView(@NonNull WebView webView) {
        if (null == webView) {
            throw new AssertionError("WebView supplied to "
                    + "FindActionModeCallback cannot be null");
        }
        if (mWebView != null) {
            mWebView.setFindListener(null);
        }
        mWebView = webView;
        mWebView.setFindListener(this);
    }

    @Override
    public void onFindResultReceived(int activeMatchOrdinal, int numberOfMatches, boolean isDoneCounting) {
        if (isDoneCounting) {
            int activeMatch = numberOfMatches > 0 ? activeMatchOrdinal + 1 : 0;
            mNumberOfMatches = numberOfMatches;
            mMatches.setSelected(numberOfMatches == 0);
            mMatches.setText(String.format("%d/%d", activeMatch, numberOfMatches));
            mMatches.setVisibility(View.VISIBLE);
            updatePrevNextItemState();
        }
    }

    /**
     * Move the highlight to the next match.
     * @param next If {@code true}, find the next match further down in the document.
     *             If {@code false}, find the previous match, up in the document.
     */
    private void findNext(boolean next) {
        if (mWebView == null) {
            throw new AssertionError(
                    "No WebView for FindActionModeCallback::findNext");
        }
        if (!mHasStartedSearch) {
            findAll();
        } else if (mNumberOfMatches == 0) {
            // There are no matches, so moving to the next match will not do anything.
        } else {
            mWebView.findNext(next);
        }
    }

    /**
     * Highlight all the instances of the string from mEditText in mWebView.
     */
    public void findAll() {
        if (mWebView == null) {
            throw new AssertionError(
                    "No WebView for FindActionModeCallback::findAll");
        }
        String find = mEditText.getText().toString();
        mNumberOfMatches = 0;
        if (find.isEmpty()) {
            mWebView.clearMatches();
            mMatches.setVisibility(View.GONE);
        } else {
            mWebView.findAllAsync(find);
            mMatches.setVisibility(View.INVISIBLE);
            mHasStartedSearch = true;
        }
        updatePrevNextItemState();
    }

    public void showSoftInput() {
        if (mEditText.requestFocus()) {
            UiUtils.showImeForView(mEditText);
        }
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        mode.setCustomView(mCustomView);
        mode.getMenuInflater().inflate(R.menu.webview_find, menu);
        Editable edit = mEditText.getText();
        Selection.setSelection(edit, edit.length());

        final ColorStateList iconTintList = AppCompatResources.getColorStateList(
                mCustomView.getContext(), R.color.webview_find_icon);

        mPrevItem = menu.findItem(R.id.find_prev);
        MenuItemCompat.setIconTintList(mPrevItem, iconTintList);
        mNextItem = menu.findItem(R.id.find_next);
        MenuItemCompat.setIconTintList(mNextItem, iconTintList);

        mMatches.setVisibility(View.GONE);
        updatePrevNextItemState();
        mHasStartedSearch = false;
        mEditText.requestFocus();
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        UiUtils.hideImeForView(mEditText);
        mWebView.clearMatches();
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        if (mWebView == null) {
            throw new AssertionError(
                    "No WebView for FindActionModeCallback::onActionItemClicked");
        }
        UiUtils.hideImeForView(mWebView);
        switch(item.getItemId()) {
            case R.id.find_prev:
                findNext(false);
                break;
            case R.id.find_next:
                findNext(true);
                break;
            default:
                return false;
        }
        return true;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        // Does nothing.  Needed to implement TextWatcher.
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        // Does nothing.  Needed to implement TextWatcher.
    }

    @Override
    public void afterTextChanged(Editable s) {
        findAll();
    }

    private void updatePrevNextItemState() {
        mPrevItem.setEnabled(mNumberOfMatches > 0);
        mNextItem.setEnabled(mNumberOfMatches > 0);
    }
}
