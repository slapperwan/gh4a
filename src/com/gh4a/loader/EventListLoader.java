package com.gh4a.loader;

import java.util.List;

import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.event.Event;
import org.eclipse.egit.github.core.service.EventService;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;

import com.gh4a.Gh4Application;

public class EventListLoader extends AsyncTaskLoader<List<Event>> {

    private String mLogin;
    private boolean mIsPrivate;
    
    public EventListLoader(Context context, String login, boolean isPrivate) {
        super(context);
        this.mLogin = login;
        this.mIsPrivate = isPrivate;
    }
    
    @Override
    public List<Event> loadInBackground() {
        Gh4Application app = (Gh4Application) getContext().getApplicationContext();
        GitHubClient client = new GitHubClient();
        client.setOAuth2Token(app.getAuthToken());
        EventService eventService = new EventService(client);
        if (mIsPrivate) {
            return (List<Event>) eventService.pageUserReceivedEvents(mLogin, true).next();
        }
        else {
            return (List<Event>) eventService.pageUserEvents(mLogin, false).next();
        }
    }

}
