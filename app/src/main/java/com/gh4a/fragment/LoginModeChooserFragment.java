package com.gh4a.fragment;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.URLSpan;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.gh4a.R;
import com.gh4a.ServiceFactory;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.RxUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.meisolsson.githubsdk.model.User;
import com.meisolsson.githubsdk.service.users.UserService;

import java.util.regex.Pattern;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import io.reactivex.Single;

public class LoginModeChooserFragment extends DialogFragment implements
        RadioGroup.OnCheckedChangeListener, View.OnClickListener {
    public interface ParentCallback {
        void onLoginStartOauth();
        void onLoginFinished(String token, User user);
        void onLoginFailed(Throwable error);
        void onLoginCanceled();
    }

    public static LoginModeChooserFragment newInstance() {
        return new LoginModeChooserFragment();
    }

    public static final String SCOPES = "user,repo,gist,read:org,notifications";

    private RadioGroup mModeGroup;
    private View mOauthContainer;
    private View mTokenContainer;
    private View mProgressContainer;
    private WrappedEditor mToken;
    private Button mOkButton;

    private ParentCallback mCallback;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (!(context instanceof ParentCallback)) {
            throw new IllegalArgumentException("Activity must implement ParentCallback");
        }
        mCallback = (ParentCallback) context;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        View view = inflater.inflate(R.layout.login_dialog, null);

        mModeGroup = view.findViewById(R.id.login_mode);
        mModeGroup.setOnCheckedChangeListener(this);

        mOauthContainer = view.findViewById(R.id.oauth_container);
        mTokenContainer = view.findViewById(R.id.token_container);
        mProgressContainer = view.findViewById(R.id.progress_container);

        mToken = new WrappedEditor(view, R.id.token, R.id.token_wrapper) {
            private Pattern TOKEN_PATTERN = Pattern.compile("[A-Za-z0-9_]{8,255}");
            @Override
            protected int getTextErrorResId(Editable s) {
                int resId = super.getTextErrorResId(s);
                if (resId == 0 && !TOKEN_PATTERN.matcher(s).matches()) {
                    resId = R.string.credentials_error_invalid_token;
                }
                return resId;
            }
        };

        mModeGroup.check(R.id.oauth_button);

        TextView oauthHint = view.findViewById(R.id.oauth_hint);
        replaceURLSpansIfNeeded(oauthHint.getText());

        return new MaterialAlertDialogBuilder(getActivity())
                .setView(view)
                .setPositiveButton(R.string.login, null) // will be assigned later
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.cancel())
                .create();
    }

    @Override
    public void onResume() {
        super.onResume();

        final AlertDialog d = (AlertDialog) getDialog();
        mOkButton = d != null ? d.getButton(DialogInterface.BUTTON_POSITIVE) : null;
        if (mOkButton != null) {
            mOkButton.setOnClickListener(this);
            updateOkButtonState();
        }
    }

    @Override
    public void onCheckedChanged(RadioGroup group, @IdRes int checkedButtonId) {
        updateContainerVisibility(false);
    }

    @Override
    public void onCancel(@NonNull DialogInterface dialog) {
        super.onCancel(dialog);
        mCallback.onLoginCanceled();
    }

    @Override
    public void onClick(View v) {
        updateContainerVisibility(true);
        switch (mModeGroup.getCheckedRadioButtonId()) {
            case R.id.oauth_button:
                mCallback.onLoginStartOauth();
                dismissAllowingStateLoss();
                break;
            case R.id.token_button:
                handleTokenCheck(makeTokenCheckSingle(mToken.getText()));
                break;
        }
    }

    private void handleTokenCheck(Single<Pair<String, User>> checkSingle) {
        checkSingle.subscribe(pair -> {
            mCallback.onLoginFinished(pair.first, pair.second);
            dismissAllowingStateLoss();
        }, error -> {
            mCallback.onLoginFailed(error);
            dismissAllowingStateLoss();
        });
    }

    private void updateContainerVisibility(boolean busy) {
        @IdRes int checked = mModeGroup.getCheckedRadioButtonId();
        mOauthContainer.setVisibility(
                checked == R.id.oauth_button && !busy ? View.VISIBLE : View.GONE);
        mTokenContainer.setVisibility(
                checked == R.id.token_button && !busy ? View.VISIBLE : View.GONE);
        mProgressContainer.setVisibility(busy ? View.VISIBLE : View.GONE);
        updateOkButtonState();
    }

    private void updateOkButtonState() {
        final boolean enable;
        if (mProgressContainer.getVisibility() == View.VISIBLE) {
            enable = false;
        } else if (mModeGroup.getCheckedRadioButtonId() == R.id.token_button) {
            enable = !mToken.hasError();
        } else {
            enable = true;
        }
        if (mOkButton != null) {
            mOkButton.setEnabled(enable);
        }
    }

    private Single<Pair<String, User>> makeTokenCheckSingle(String token) {
        UserService userService = ServiceFactory.get(UserService.class, true, null, token, null);
        Single<User> userSingle = userService.getUser()
                .map(ApiHelpers::throwOnFailure)
                .compose(RxUtils::doInBackground);
        return Single.zip(Single.just(token), userSingle, Pair::create);
    }

    private void replaceURLSpansIfNeeded(CharSequence text) {
        if (!(text instanceof Spannable)) {
            return;
        }
        Spannable spannable = (Spannable) text;
        for (Object span : spannable.getSpans(0, spannable.length(), URLSpan.class)) {
            URLSpan urlSpan = (URLSpan) span;
            int start = spannable.getSpanStart(span);
            int end = spannable.getSpanEnd(span);
            int flags = spannable.getSpanFlags(span);
            spannable.removeSpan(span);
            spannable.setSpan(new URLSpan(urlSpan.getURL()) {
                @Override
                public void onClick(View widget) {
                    IntentUtils.openInCustomTabOrBrowser(getActivity(), Uri.parse(getURL()));
                }
            }, start, end, flags);
        }
    }
    private class WrappedEditor implements TextWatcher {
        private final TextInputEditText mEditor;
        private final TextInputLayout mWrapper;

        public WrappedEditor(View parent, @IdRes int editorResId, @IdRes int wrapperResId) {
            mEditor = parent.findViewById(editorResId);
            mWrapper = parent.findViewById(wrapperResId);
            mEditor.addTextChangedListener(this);
            afterTextChanged(mEditor.getText());
        }

        public String getText() {
            Editable editable = mEditor.getText();
            return editable != null ? editable.toString() : null;
        }

        public boolean hasError() {
            return mWrapper.isErrorEnabled();
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            int errorResId = getTextErrorResId(s);
            if (errorResId != 0) {
                mWrapper.setError(getString(errorResId));
            } else {
                mWrapper.setErrorEnabled(false);
            }
            updateOkButtonState();
        }

        protected int getTextErrorResId(Editable s) {
            if (TextUtils.isEmpty(s)) {
                return R.string.credentials_error_empty;
            }
            return 0;
        }
    }
}
