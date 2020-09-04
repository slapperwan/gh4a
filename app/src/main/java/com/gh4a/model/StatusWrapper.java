package com.gh4a.model;

import com.meisolsson.githubsdk.model.CheckRun;
import com.meisolsson.githubsdk.model.Status;

public class StatusWrapper {
    public enum State {
        Success,
        Failed,
        Unknown
    }

    private String mLabel;
    private String mDescription;
    private State mState;
    private String mTargetUrl;

    public StatusWrapper(Status status) {
        mLabel = status.context();
        mDescription = status.description();
        mTargetUrl = status.targetUrl();
        switch (status.state()) {
            case Error:
            case Failure:
                mState = State.Failed;
                break;
            case Success:
                mState = State.Success;
                break;
            default:
                mState = State.Unknown;
                break;
        }
    }

    public StatusWrapper(CheckRun checkRun) {
        mLabel = checkRun.name();
        mDescription = checkRun.output() != null ? checkRun.output().title() : null;
        mTargetUrl = checkRun.detailsUrl();
        switch (checkRun.conclusion()) {
            case Failure:
            case TimedOut:
            case Cancelled:
                mState = State.Failed;
                break;
            case Success:
                mState = State.Success;
                break;
            default:
                mState = State.Unknown;
                break;
        }
    }

    public State state() {
        return mState;
    }

    public String label() {
        return mLabel;
    }

    public String description() {
        return mDescription;
    }

    public String targetUrl() {
        return mTargetUrl;
    }
}
