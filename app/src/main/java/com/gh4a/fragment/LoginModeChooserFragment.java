package com.gh4a.fragment;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
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

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.gh4a.ApiRequestException;
import com.gh4a.R;
import com.gh4a.ServiceFactory;
import com.gh4a.ServiceFactory.LoginService;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.RxUtils;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.meisolsson.githubsdk.model.User;
import com.meisolsson.githubsdk.service.users.UserService;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.Locale;
import java.util.regex.Pattern;

import io.reactivex.Single;
import io.reactivex.processors.PublishProcessor;

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
    private View mCredentialsContainer;
    private View mOtpCodeContainer;
    private View mTokenContainer;
    private View mProgressContainer;
    private WrappedEditor mUserName;
    private WrappedEditor mPassword;
    private WrappedEditor mOtpCodeEditor;
    private WrappedEditor mToken;
    private Button mOkButton;

    private ParentCallback mCallback;

    private final PublishProcessor<Integer> mRetryProcessor = PublishProcessor.create();
    private boolean mWaitingForOtpCode;
    private String mOtpCode;
    private Handler mHandler = new Handler();

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
        mCredentialsContainer = view.findViewById(R.id.cred_container);
        mOtpCodeContainer = view.findViewById(R.id.otp_container);
        mTokenContainer = view.findViewById(R.id.token_container);
        mProgressContainer = view.findViewById(R.id.progress_container);

        mUserName = new WrappedEditor(view, R.id.user_name, R.id.user_wrapper);
        mPassword = new WrappedEditor(view, R.id.password, R.id.password_wrapper);
        mOtpCodeEditor = new WrappedEditor(view, R.id.otp_code, R.id.otp_wrapper);
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

        return new AlertDialog.Builder(getActivity())
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
            case R.id.user_pw_button:
                if (mWaitingForOtpCode) {
                    mOtpCode = mOtpCodeEditor.getText();
                    mRetryProcessor.onNext(0);
                } else {
                    handleTokenCheck(makeAuthRequestSingle()
                            .flatMap(request -> makeUserPasswordLoginSingle(request)));
                }
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
        mCredentialsContainer.setVisibility(
                checked == R.id.user_pw_button && !mWaitingForOtpCode && !busy ? View.VISIBLE : View.GONE);
        mOtpCodeContainer.setVisibility(
                checked == R.id.user_pw_button && mWaitingForOtpCode && !busy ? View.VISIBLE : View.GONE);
        mTokenContainer.setVisibility(
                checked == R.id.token_button && !busy ? View.VISIBLE : View.GONE);
        mProgressContainer.setVisibility(busy ? View.VISIBLE : View.GONE);
        updateOkButtonState();
    }

    private void updateOkButtonState() {
        final boolean enable;
        if (mProgressContainer.getVisibility() == View.VISIBLE) {
            enable = false;
        } else if (mModeGroup.getCheckedRadioButtonId() == R.id.login_button) {
            enable = mWaitingForOtpCode
                    ? !mOtpCodeEditor.hasError()
                    : !mUserName.hasError() && !mPassword.hasError();
        } else if (mModeGroup.getCheckedRadioButtonId() == R.id.token_button) {
            enable = !mToken.hasError();
        } else {
            enable = true;
        }
        if (mOkButton != null) {
            mOkButton.setEnabled(enable);
        }
    }

    private String getHashedDeviceId() {
        String androidId = Settings.Secure.getString(getActivity().getContentResolver(),
                Settings.Secure.ANDROID_ID);
        if (androidId == null) {
            // shouldn't happen, do a lame fallback in that case
            androidId = Build.FINGERPRINT;
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] result = digest.digest(androidId.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : result) {
                sb.append(String.format(Locale.US, "%02X", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            // won't happen
            return androidId;
        }
    }

    private LoginService getUserPasswordLoginService() {
        return ServiceFactory.createLoginService(mUserName.getText(), mPassword.getText(),
                () -> mOtpCode);
    }

    private Single<LoginService.AuthorizationRequest> makeAuthRequestSingle() {
        String description = "Octodroid - " + Build.MANUFACTURER + " " + Build.MODEL;
        String fingerprint = getHashedDeviceId();
        ServiceFactory.LoginService service = getUserPasswordLoginService();

        return service.getAuthorizations()
                .map(ApiHelpers::throwOnFailure)
                .compose(RxUtils::doInBackground)
                .retryWhen(handler -> handler.flatMap(error -> {
                    if (error instanceof ApiRequestException) {
                        ApiRequestException are = (ApiRequestException) error;
                        if (are.getStatus() == 401 && are.getResponse().message().contains("OTP code")) {
                            mWaitingForOtpCode = true;
                            mHandler.post(() -> updateContainerVisibility(false));
                            // getAuthorizations() doesn't trigger the OTP SMS for whatever reason,
                            // so make a dummy create request (which we know will fail) just to
                            // actually trigger SMS sending
                            ServiceFactory.LoginService.AuthorizationRequest dummyRequest =
                                    new ServiceFactory.LoginService.AuthorizationRequest("", "dummy", "");
                            service.createAuthorization(dummyRequest)
                                    .compose(RxUtils::doInBackground)
                                    .subscribe(ignoredResponse -> {}, ignoredError -> {});
                        }
                    }
                    if (!mWaitingForOtpCode) {
                        mRetryProcessor.onError(error);
                    }
                    return mRetryProcessor;
                }))
                .compose(RxUtils.filter(authorization -> {
                    String note = authorization.note();
                    return note != null && note.startsWith(description);
                }))
                .flatMap(existingAuthorizations -> {
                    Single<Void> deleteSingle = null;
                    Iterator<LoginService.AuthorizationResponse> iter = existingAuthorizations.iterator();
                    while (iter.hasNext()) {
                        LoginService.AuthorizationResponse auth = iter.next();
                        if (fingerprint.equals(auth.fingerprint())) {
                            deleteSingle = service.deleteAuthorization(auth.id())
                                    .map(ApiHelpers::throwOnFailure)
                                    .compose(RxUtils::doInBackground);
                            iter.remove();
                        }
                    }

                    String finalDescription = description;
                    if (!existingAuthorizations.isEmpty()) {
                        finalDescription += " #" + (existingAuthorizations.size() + 1);
                    }
                    LoginService.AuthorizationRequest request =
                            new LoginService.AuthorizationRequest(SCOPES, finalDescription, fingerprint);
                    if (deleteSingle != null) {
                        return deleteSingle.map(response -> request);
                    } else {
                        return Single.just(request);
                    }
                });
    }

    private Single<Pair<String, User>> makeUserPasswordLoginSingle(LoginService.AuthorizationRequest request) {
        return getUserPasswordLoginService().createAuthorization(request)
                .map(ApiHelpers::throwOnFailure)
                .compose(RxUtils::doInBackground)
                .flatMap(response -> makeTokenCheckSingle(response.token()));
    }

    private Single<Pair<String, User>> makeTokenCheckSingle(String token) {
        UserService userService = ServiceFactory.get(UserService.class, true, null, token, null);
        Single<User> userSingle = userService.getUser()
                .map(ApiHelpers::throwOnFailure)
                .compose(RxUtils::doInBackground);
        return Single.zip(Single.just(token), userSingle, (t, user) -> Pair.create(t, user));
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
        private TextInputEditText mEditor;
        private TextInputLayout mWrapper;

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
