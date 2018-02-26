package com.gh4a.fragment;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.DialogFragment;
import android.support.v4.util.Pair;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import com.gh4a.ApiRequestException;
import com.gh4a.R;
import com.gh4a.ServiceFactory;
import com.gh4a.ServiceFactory.LoginService;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.RxUtils;
import com.meisolsson.githubsdk.model.User;
import com.meisolsson.githubsdk.service.users.UserService;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.Locale;

import io.reactivex.Single;
import io.reactivex.processors.PublishProcessor;

public class UserPasswordLoginDialogFragment extends DialogFragment implements
        View.OnClickListener {
    public interface ParentCallback {
        void onLoginFinished(String token, User user);
        void onLoginFailed(Throwable error);
    }

    public static UserPasswordLoginDialogFragment newInstance(String scopes) {
        UserPasswordLoginDialogFragment f = new UserPasswordLoginDialogFragment();
        Bundle args = new Bundle();
        args.putString("scopes", scopes);
        f.setArguments(args);
        return f;
    }

    private WrappedEditor mUserName;
    private WrappedEditor mPassword;
    private WrappedEditor mOtpCodeEditor;
    private View mCredentialsContainer;
    private View mProgressContainer;
    private View mOtpCodeContainer;
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

        mCredentialsContainer = view.findViewById(R.id.cred_container);
        mOtpCodeContainer = view.findViewById(R.id.otp_container);
        mProgressContainer = view.findViewById(R.id.progress_container);

        mUserName = new WrappedEditor(view, R.id.user_name, R.id.user_wrapper);
        mPassword = new WrappedEditor(view, R.id.password, R.id.password_wrapper);
        mOtpCodeEditor = new WrappedEditor(view, R.id.otp_code, R.id.otp_wrapper);

        updateContainerVisibility(false);

        return new AlertDialog.Builder(getActivity())
                .setView(view)
                .setPositiveButton(R.string.login, null) // will be assigned later
                .setNegativeButton(R.string.cancel, null)
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
    public void onClick(View v) {
        if (mWaitingForOtpCode) {
            mOtpCode = mOtpCodeEditor.getText();
            mRetryProcessor.onNext(0);
        } else {
            makeRequestSingle()
                    .flatMap(request -> makeLoginSingle(request))
                    .subscribe(pair -> {
                        mCallback.onLoginFinished(pair.first, pair.second);
                        dismiss();
                    }, error -> {
                        mCallback.onLoginFailed(error);
                        dismiss();
                    });
        }
        updateContainerVisibility(true);
    }

    private void updateOkButtonState() {
        boolean enable =
                mProgressContainer.getVisibility() == View.VISIBLE ? false :
                mWaitingForOtpCode ? !mOtpCodeEditor.hasError() :
                !mUserName.hasError() && !mPassword.hasError();
        if (mOkButton != null) {
            mOkButton.setEnabled(enable);
        }
    }
    private void updateContainerVisibility(boolean busy) {
        mCredentialsContainer.setVisibility(!mWaitingForOtpCode && !busy ? View.VISIBLE : View.GONE);
        mOtpCodeContainer.setVisibility(mWaitingForOtpCode && !busy ? View.VISIBLE : View.GONE);
        mProgressContainer.setVisibility(busy ? View.VISIBLE : View.GONE);
        updateOkButtonState();
    }

    private LoginService getService() {
        return ServiceFactory.createLoginService(mUserName.getText(), mPassword.getText(),
                () -> mOtpCode);
    }

    private Single<LoginService.AuthorizationRequest> makeRequestSingle() {
        String description = "Octodroid - " + Build.MANUFACTURER + " " + Build.MODEL;
        String fingerprint = getHashedDeviceId();
        LoginService service = getService();
        String scopes = getArguments().getString("scopes");

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
                            LoginService.AuthorizationRequest dummyRequest =
                                    new LoginService.AuthorizationRequest("", "dummy", "");
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
                    Iterator<LoginService.AuthorizationResponse> iter =
                            existingAuthorizations.iterator();
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
                            new LoginService.AuthorizationRequest(scopes, finalDescription, fingerprint);
                    if (deleteSingle != null) {
                        return deleteSingle.map(response -> request);
                    } else {
                        return Single.just(request);
                    }
                });
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

    private Single<Pair<String, User>> makeLoginSingle(LoginService.AuthorizationRequest request) {
        return getService().createAuthorization(request)
                .map(ApiHelpers::throwOnFailure)
                .compose(RxUtils::doInBackground)
                .flatMap(response -> {
                    UserService userService = ServiceFactory.get(UserService.class, true,
                            null, response.token(), null);
                    Single<User> userSingle = userService.getUser()
                            .map(ApiHelpers::throwOnFailure)
                            .compose(RxUtils::doInBackground);
                    return Single.zip(Single.just(response), userSingle,
                            (r, user) -> Pair.create(r.token(), user));
                });
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
            if (TextUtils.isEmpty(s)) {
                mWrapper.setError(getString(R.string.credentials_error_empty));
            } else {
                mWrapper.setErrorEnabled(false);
            }
            updateOkButtonState();
        }
    }
}