package com.gh4a.loader;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.egit.github.core.CommitStatus;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.service.CommitService;

import android.content.Context;

import com.gh4a.Gh4Application;

public class CommitStatusLoader extends BaseLoader<List<CommitStatus>> {
    private final String mRepoOwner;
    private final String mRepoName;
    private final String mSha;

    private static final Comparator<CommitStatus> TIMESTAMP_COMPARATOR = new Comparator<CommitStatus>() {
        @Override
        public int compare(CommitStatus lhs, CommitStatus rhs) {
            return rhs.getUpdatedAt().compareTo(lhs.getUpdatedAt());
        }
    };

    private static final Comparator<CommitStatus> STATUS_AND_CONTEXT_COMPARATOR = new Comparator<CommitStatus>() {
        @Override
        public int compare(CommitStatus lhs, CommitStatus rhs) {
            int lhsSeverity = getStateSeverity(lhs);
            int rhsSeverity = getStateSeverity(rhs);
            if (lhsSeverity != rhsSeverity) {
                return lhsSeverity < rhsSeverity ? 1 : -1;
            } else {
                return lhs.getContext().compareTo(rhs.getContext());
            }
        }

        private int getStateSeverity(CommitStatus status) {
            switch (status.getState()) {
                case CommitStatus.STATE_SUCCESS: return 0;
                case CommitStatus.STATE_ERROR:
                case CommitStatus.STATE_FAILURE: return 2;
                default: return 1;
            }
        }
    };

    public CommitStatusLoader(Context context, String repoOwner, String repoName, String sha) {
        super(context);
        mRepoOwner = repoOwner;
        mRepoName = repoName;
        mSha = sha;
    }

    @Override
    public List<CommitStatus> doLoadInBackground() throws IOException {
        CommitService commitService = (CommitService)
                Gh4Application.get().getService(Gh4Application.COMMIT_SERVICE);
        List<CommitStatus> statuses =
                commitService.getStatuses(new RepositoryId(mRepoOwner, mRepoName), mSha);

        // Sort by timestamps first, so the removal logic below keeps the newest status
        Collections.sort(statuses, TIMESTAMP_COMPARATOR);

        // Filter out outdated statuses, only keep the newest status per context
        Set<String> seenContexts = new HashSet<>();
        Iterator<CommitStatus> iter = statuses.iterator();
        while (iter.hasNext()) {
            CommitStatus status = iter.next();
            if (seenContexts.contains(status.getContext())) {
                iter.remove();
            } else {
                seenContexts.add(status.getContext());
            }
        }

        // sort by status, then context
        Collections.sort(statuses, STATUS_AND_CONTEXT_COMPARATOR);

        return statuses;
    }
}
