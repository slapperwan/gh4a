package com.gh4a.fragment;

import com.gh4a.Gh4Application;

import org.eclipse.egit.github.core.client.PageIterator;
import org.eclipse.egit.github.core.event.Event;
import org.eclipse.egit.github.core.service.EventService;

public class PrivateEventListFragment extends EventListFragment {
    public static PrivateEventListFragment newInstance(String login) {
        PrivateEventListFragment f = new PrivateEventListFragment();
        f.setArguments(makeArguments(login));
        return f;
    }

    @Override
    protected PageIterator<Event> onCreateIterator() {
        EventService eventService = (EventService)
                Gh4Application.get().getService(Gh4Application.EVENT_SERVICE);
        return eventService.pageUserReceivedEvents(mLogin);
    }
}
