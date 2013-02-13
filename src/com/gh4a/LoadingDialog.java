/*
 * Copyright 2011 Azwan Adli Abdullah
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gh4a;

import android.app.Dialog;
import android.content.Context;
import android.view.ViewGroup.LayoutParams;
import android.widget.ProgressBar;

/**
 * The Class LoadingDialog.
 */
public class LoadingDialog extends Dialog {

    /**
     * Show.
     *
     * @param context the context
     * @param title the title
     * @param message the message
     * @return the loading dialog
     */
    public static LoadingDialog show(Context context, CharSequence title, CharSequence message) {
        return show(context, title, message, false);
    }

    /**
     * Show.
     *
     * @param context the context
     * @param title the title
     * @param message the message
     * @param indeterminate the indeterminate
     * @return the loading dialog
     */
    public static LoadingDialog show(Context context, CharSequence title, CharSequence message,
            boolean indeterminate) {
        return show(context, title, message, indeterminate, false, null, true, android.R.style.Theme_Dialog);
    }

    /**
     * Show.
     *
     * @param context the context
     * @param title the title
     * @param message the message
     * @param indeterminate the indeterminate
     * @param cancelable the cancelable
     * @return the loading dialog
     */
    public static LoadingDialog show(Context context, CharSequence title, CharSequence message,
            boolean indeterminate, boolean cancelable) {
        return show(context, title, message, indeterminate, cancelable, null, false, android.R.style.Theme_Dialog);
    }

    /**
     * Show.
     *
     * @param context the context
     * @param indeterminate the indeterminate
     * @param cancelable the cancelable
     * @return the loading dialog
     */
    public static LoadingDialog show(Context context, boolean indeterminate, boolean cancelable) {
        return show(context, null, null, indeterminate, cancelable, null, true, R.style.NewDialog);
    }

    /**
     * Show.
     *
     * @param context the context
     * @param indeterminate the indeterminate
     * @param cancelable the cancelable
     * @param hideMainView the hide main view
     * @return the loading dialog
     */
    public static LoadingDialog show(Context context, boolean indeterminate, boolean cancelable,
            boolean hideMainView) {
        return show(context, null, null, indeterminate, cancelable, null, hideMainView, R.style.NewDialog);
    }

    /**
     * Show.
     *
     * @param context the context
     * @param title the title
     * @param message the message
     * @param indeterminate the indeterminate
     * @param cancelable the cancelable
     * @param cancelListener the cancel listener
     * @param hideMainView the hide main view
     * @return the loading dialog
     */
    public static LoadingDialog show(Context context, CharSequence title, CharSequence message,
            boolean indeterminate, boolean cancelable, OnCancelListener cancelListener,
            boolean hideMainView, int themeId) {
        LoadingDialog dialog = new LoadingDialog(context, themeId);
        dialog.setTitle(title);
        dialog.setCancelable(cancelable);
        dialog.setOnCancelListener(cancelListener);
        /* The next line will add the ProgressBar to the dialog. */
        dialog.addContentView(new ProgressBar(context), new LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT));
        dialog.show();

        return dialog;
    }

    /* (non-Javadoc)
     * @see android.app.Dialog#dismiss()
     */
    public void dismiss() {
        super.dismiss();
    }

    /**
     * Instantiates a new loading dialog.
     *
     * @param context the context
     */
    public LoadingDialog(Context context, int themeId) {
        super(context, themeId);
    }
}
