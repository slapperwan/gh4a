package com.gh4a.fragment;

import android.os.Bundle;

import com.gh4a.Gh4Application;

import org.eclipse.egit.github.core.client.PageIterator;
import org.eclipse.egit.github.core.event.Event;
import org.eclipse.egit.github.core.service.EventService;

public class PrivateEventListFragment extends EventListFragment {
    private String mOrganization;

    public static PrivateEventListFragment newInstance(String login, String organization) {
        PrivateEventListFragment f = new PrivateEventListFragment();
        Bundle args = makeArguments(login);
        args.putString("org", organization);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mOrganization = getArguments().getString("org");
    }

    @Override
    protected PageIterator<Event> onCreateIterator() {
        EventService eventService = (EventService)
                Gh4Application.get().getService(Gh4Application.EVENT_SERVICE);
        if (mOrganization != null) {
            return eventService.pageUserOrgEvents(mLogin, mOrganization);
        }
        return eventService.pageUserReceivedEvents(mLogin);
    }
}
