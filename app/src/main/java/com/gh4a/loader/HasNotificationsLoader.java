package com.gh4a.loader;

import android.content.Context;

import com.gh4a.ServiceFactory;
import com.gh4a.utils.ApiHelpers;
import com.meisolsson.githubsdk.model.NotificationThread;
import com.meisolsson.githubsdk.model.Page;
import com.meisolsson.githubsdk.service.activity.NotificationService;

import java.io.IOException;
import java.util.HashMap;

public class HasNotificationsLoader extends BaseLoader<Boolean> {

    public HasNotificationsLoader(Context context) {
        super(context);
    }

    @Override
    public Boolean doLoadInBackground() throws IOException {
        NotificationService service = ServiceFactory.createService(
                NotificationService.class, null, null, 1);
        HashMap<String, Object> options = new HashMap<>();
        options.put("all", false);
        options.put("participating", false);
        Page<NotificationThread> page = ApiHelpers.throwOnFailure(
                service.getNotifications(options, 0).blockingGet());
        return !page.items().isEmpty();
    }
}
