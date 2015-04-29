package com.gh4a.fragment;

import android.os.Bundle;
import android.view.MenuItem;

import com.gh4a.Constants;
import com.gh4a.Gh4Application;

import org.eclipse.egit.github.core.client.PageIterator;
import org.eclipse.egit.github.core.event.Event;
import org.eclipse.egit.github.core.service.EventService;

public class PrivateEventListFragment extends EventListFragment {
    public static PrivateEventListFragment newInstance(String login) {
        PrivateEventListFragment f = new PrivateEventListFragment();

        Bundle args = new Bundle();
        args.putString(Constants.User.LOGIN, login);
        f.setArguments(args);

        return f;
    }

    @Override
    protected PageIterator<Event> onCreateIterator() {
        EventService eventService = (EventService)
                Gh4Application.get().getService(Gh4Application.EVENT_SERVICE);
        return eventService.pageUserReceivedEvents(mLogin);
    }

    @Override
    public int getMenuGroupId() {
        return 1;
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (item.getGroupId() == 1) {
            open(item);
            return true;
        }
        return false;
    }
}
