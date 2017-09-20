package com.gh4a.job;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobCreator;

public class Gh4JobCreator implements JobCreator {
    @Override
    public Job create(String tag) {
        switch (tag) {
            case NotificationsJob.TAG:
                return new NotificationsJob();
            default:
                return null;
        }
    }
}
