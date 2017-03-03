package com.gh4a.fragment;

import android.os.Bundle;

import com.gh4a.Gh4Application;

import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.client.PageIterator;
import org.eclipse.egit.github.core.event.Event;
import org.eclipse.egit.github.core.service.EventService;

public class RepositoryEventListFragment extends EventListFragment {
    private Repository mRepository;

    public static RepositoryEventListFragment newInstance(Repository repository) {
        RepositoryEventListFragment f = new RepositoryEventListFragment();
        Bundle args = new Bundle();
        args.putSerializable("repository", repository);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRepository = (Repository) getArguments().getSerializable("repository");
    }

    @Override
    protected PageIterator<Event> onCreateIterator() {
        EventService eventService = (EventService)
                Gh4Application.get().getService(Gh4Application.EVENT_SERVICE);
        return eventService.pageEvents(mRepository);
    }
}
