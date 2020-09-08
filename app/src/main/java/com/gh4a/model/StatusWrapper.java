package com.gh4a.model;

import android.content.Context;

import com.gh4a.R;
import com.meisolsson.githubsdk.model.CheckRun;
import com.meisolsson.githubsdk.model.Status;

import java.util.Date;

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

    public StatusWrapper(Context context, CheckRun checkRun) {
        mLabel = checkRun.name();
        mTargetUrl = checkRun.detailsUrl();

        switch (checkRun.state()) {
            case Requested:
            case Queued:
                mState = State.Unknown;
                mDescription = context.getString(R.string.check_pending_description);
                break;
            case InProgress:
                mState = State.Unknown;
                mDescription = context.getString(R.string.check_running_description);
                break;
            case Completed: {
                String runtime = formatTimeDelta(context, checkRun.startedAt(), checkRun.completedAt());
                String runtimeDesc = context.getString(R.string.check_runtime_description, runtime);
                String title = checkRun.output() != null ? checkRun.output().title() : null;
                if (title != null) {
                    mDescription = title + " — " + runtimeDesc;
                } else {
                    mDescription = runtimeDesc;
                }
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
                break;
            }
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

    private static String formatTimeDelta(Context context, Date start, Date end) {
        long deltaSeconds = (end.getTime() - start.getTime()) / 1000;
        long seconds = deltaSeconds % 60;
        long minutes = (deltaSeconds % 3600) / 60;
        long hours = deltaSeconds / 3600;
        int formatStringResId;

        if (hours == 0 && minutes == 0) {
            formatStringResId = R.string.check_runtime_format_seconds;
        } else if (hours == 0) {
            formatStringResId = R.string.check_runtime_format_minutes;
        } else {
            formatStringResId = R.string.check_runtime_format_hours;
        }

        return context.getString(formatStringResId, hours, minutes, seconds);
    }
}
