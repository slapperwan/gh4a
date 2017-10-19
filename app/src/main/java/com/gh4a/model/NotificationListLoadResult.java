package com.gh4a.model;

import java.util.Date;
import java.util.List;

public class NotificationListLoadResult {
    public final List<NotificationHolder> notifications;
    public final Date loadTime;

    public NotificationListLoadResult(List<NotificationHolder> notifications) {
        this.notifications = notifications;
        loadTime = new Date();
    }
}
