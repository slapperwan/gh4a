/*
 * Copyright 2011 Azwan Adli Abdullah
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gh4a.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.gh4a.BaseActivity;
import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.activities.CommitActivity;
import com.gh4a.activities.CompareActivity;
import com.gh4a.activities.GistActivity;
import com.gh4a.activities.IssueActivity;
import com.gh4a.activities.IssueListActivity;
import com.gh4a.activities.PullRequestActivity;
import com.gh4a.activities.ReleaseInfoActivity;
import com.gh4a.activities.RepositoryActivity;
import com.gh4a.activities.UserActivity;
import com.gh4a.activities.WikiListActivity;
import com.gh4a.adapter.EventAdapter;
import com.gh4a.adapter.RootAdapter;
import com.gh4a.resolver.CommitCommentLoadTask;
import com.gh4a.resolver.PullRequestReviewCommentLoadTask;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.UiUtils;
import com.gh4a.widget.ContextMenuAwareRecyclerView;

import org.eclipse.egit.github.core.Commit;
import org.eclipse.egit.github.core.CommitComment;
import org.eclipse.egit.github.core.Download;
import org.eclipse.egit.github.core.GollumPage;
import org.eclipse.egit.github.core.Issue;
import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.egit.github.core.Release;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.User;
import org.eclipse.egit.github.core.client.PageIterator;
import org.eclipse.egit.github.core.event.CommitCommentPayload;
import org.eclipse.egit.github.core.event.CreatePayload;
import org.eclipse.egit.github.core.event.DownloadPayload;
import org.eclipse.egit.github.core.event.Event;
import org.eclipse.egit.github.core.event.EventRepository;
import org.eclipse.egit.github.core.event.FollowPayload;
import org.eclipse.egit.github.core.event.ForkPayload;
import org.eclipse.egit.github.core.event.GistPayload;
import org.eclipse.egit.github.core.event.GollumPayload;
import org.eclipse.egit.github.core.event.IssueCommentPayload;
import org.eclipse.egit.github.core.event.IssuesPayload;
import org.eclipse.egit.github.core.event.PullRequestPayload;
import org.eclipse.egit.github.core.event.PullRequestReviewCommentPayload;
import org.eclipse.egit.github.core.event.PushPayload;
import org.eclipse.egit.github.core.event.ReleasePayload;
import org.eclipse.egit.github.core.service.EventService;

import java.util.Arrays;
import java.util.List;

public abstract class EventListFragment extends PagedDataBaseFragment<Event> {
    private static final int MENU_DOWNLOAD_START = 100;
    private static final int MENU_DOWNLOAD_END = 199;

    protected String mLogin;
    private EventAdapter mAdapter;

    private static final String[] REPO_EVENTS = new String[] {
        Event.TYPE_PUSH, Event.TYPE_ISSUES, Event.TYPE_WATCH, Event.TYPE_CREATE,
        Event.TYPE_PULL_REQUEST, Event.TYPE_COMMIT_COMMENT, Event.TYPE_DELETE,
        Event.TYPE_DOWNLOAD, Event.TYPE_FORK_APPLY, Event.TYPE_PUBLIC,
        Event.TYPE_MEMBER, Event.TYPE_ISSUE_COMMENT
    };

    protected static Bundle makeArguments(String user) {
        Bundle args = new Bundle();
        args.putString("login", user);
        return args;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLogin = getArguments().getString("login");
    }

    @Override
    protected void onRecyclerViewInflated(RecyclerView view, LayoutInflater inflater) {
        super.onRecyclerViewInflated(view, inflater);
        registerForContextMenu(view);
    }

    @Override
    protected RootAdapter<Event, ? extends RecyclerView.ViewHolder> onCreateAdapter() {
        mAdapter = new EventAdapter(getActivity());
        mAdapter.setContextMenuSupported(true);
        return mAdapter;
    }

    @Override
    protected int getEmptyTextResId() {
        return R.string.no_events_found;
    }

    @Override
    protected PageIterator<Event> onCreateIterator() {
        EventService eventService = (EventService)
                Gh4Application.get().getService(Gh4Application.EVENT_SERVICE);
        return eventService.pageUserEvents(mLogin, false);
    }

    @Override
    public void onItemClick(Event event) {
        if (EventAdapter.hasInvalidPayload(event)) {
            return;
        }

        String eventType = event.getType();
        EventRepository eventRepo = event.getRepo();
        String repoOwner = "";
        String repoName = "";
        Intent intent = null;

        if (eventRepo != null) {
            String[] repoNamePart = eventRepo.getName().split("/");
            if (repoNamePart.length == 2) {
                repoOwner = repoNamePart[0];
                repoName = repoNamePart[1];
            }
        }

        if (Arrays.binarySearch(REPO_EVENTS, eventType) >= 0 && eventRepo == null) {
            Toast.makeText(getActivity(), R.string.repo_not_found_toast, Toast.LENGTH_LONG).show();
            return;
        }

        if (Event.TYPE_COMMIT_COMMENT.equals(eventType)) {
            CommitCommentPayload payload = (CommitCommentPayload) event.getPayload();
            CommitComment comment = payload.getComment();
            if (comment != null) {
                new CommitCommentLoadTask(getActivity(), repoOwner, repoName, comment.getCommitId(),
                        new IntentUtils.InitialCommentMarker(comment.getId()), false).schedule();
            }
        } else if (Event.TYPE_CREATE.equals(eventType)) {
            CreatePayload payload = (CreatePayload) event.getPayload();
            String ref = null;
            if ("branch".equals(payload.getRefType()) || "tag".equals(payload.getRefType())) {
                ref = payload.getRef();
            }
            intent = RepositoryActivity.makeIntent(getActivity(), repoOwner, repoName, ref);

        } else if (Event.TYPE_DELETE.equals(eventType)) {
            intent = RepositoryActivity.makeIntent(getActivity(), repoOwner, repoName);

        } else if (Event.TYPE_DOWNLOAD.equals(eventType)) {
            DownloadPayload payload = (DownloadPayload) event.getPayload();
            Download download = payload.getDownload();
            UiUtils.enqueueDownloadWithPermissionCheck((BaseActivity) getActivity(),
                    download.getHtmlUrl(), download.getContentType(),
                    download.getName(), download.getDescription(), null);

        } else if (Event.TYPE_FOLLOW.equals(eventType)) {
            FollowPayload payload = (FollowPayload) event.getPayload();
            intent = UserActivity.makeIntent(getActivity(), payload.getTarget());

        } else if (Event.TYPE_FORK.equals(eventType)) {
            ForkPayload payload = (ForkPayload) event.getPayload();
            Repository forkee = payload.getForkee();
            if (forkee != null) {
                intent = RepositoryActivity.makeIntent(getActivity(), forkee);
            } else {
                Toast.makeText(getActivity(), R.string.repo_not_found_toast, Toast.LENGTH_LONG).show();
            }

        } else if (Event.TYPE_FORK_APPLY.equals(eventType)) {
            intent = RepositoryActivity.makeIntent(getActivity(), repoOwner, repoName);

        } else if (Event.TYPE_GIST.equals(eventType)) {
            GistPayload payload = (GistPayload) event.getPayload();
            intent = GistActivity.makeIntent(getActivity(), payload.getGist().getId());

        } else if (Event.TYPE_GOLLUM.equals(eventType)) {
            GollumPayload payload = (GollumPayload) event.getPayload();
            intent = WikiListActivity.makeIntent(getActivity(), repoOwner, repoName,
                    payload.getPages().isEmpty() ? null : payload.getPages().get(0));

        } else if (Event.TYPE_ISSUE_COMMENT.equals(eventType)) {
            IssueCommentPayload payload = (IssueCommentPayload) event.getPayload();
            Issue issue = payload.getIssue();
            PullRequest request = issue != null ? issue.getPullRequest() : null;
            IntentUtils.InitialCommentMarker initialComment = payload.getComment() != null
                    ? new IntentUtils.InitialCommentMarker(payload.getComment().getId()) : null;

            if (request != null && request.getHtmlUrl() != null) {
                intent = PullRequestActivity.makeIntent(getActivity(),
                        repoOwner, repoName, issue.getNumber(),
                        initialComment != null ? PullRequestActivity.PAGE_CONVERSATION : -1,
                        initialComment);
            } else if (issue != null) {
                intent = IssueActivity.makeIntent(getActivity(),
                        repoOwner, repoName, issue.getNumber(), initialComment);
            }

        } else if (Event.TYPE_ISSUES.equals(eventType)) {
            IssuesPayload payload = (IssuesPayload) event.getPayload();
            startActivity(IssueActivity.makeIntent(getActivity(), repoOwner, repoName,
                    payload.getIssue().getNumber()));

        } else if (Event.TYPE_MEMBER.equals(eventType)) {
            intent = RepositoryActivity.makeIntent(getActivity(), repoOwner, repoName);

        } else if (Event.TYPE_PUBLIC.equals(eventType)) {
            intent = RepositoryActivity.makeIntent(getActivity(), repoOwner, repoName);

        } else if (Event.TYPE_PULL_REQUEST.equals(eventType)) {
            PullRequestPayload payload = (PullRequestPayload) event.getPayload();
            intent = PullRequestActivity.makeIntent(getActivity(),
                    repoOwner, repoName, payload.getNumber());

        } else if (Event.TYPE_PULL_REQUEST_REVIEW_COMMENT.equals(eventType)) {
            PullRequestReviewCommentPayload payload =
                    (PullRequestReviewCommentPayload) event.getPayload();
            PullRequest pr = payload.getPullRequest();
            CommitComment comment = payload.getComment();
            IntentUtils.InitialCommentMarker initialComment = comment != null
                    ? new IntentUtils.InitialCommentMarker(comment.getId()) : null;

            if (pr != null) {
                if (initialComment != null) {
                    new PullRequestReviewCommentLoadTask(getActivity(), repoOwner,
                            repoName, pr.getNumber(), initialComment, false).schedule();
                } else {
                    intent = PullRequestActivity.makeIntent(getActivity(), repoOwner, repoName,
                            pr.getNumber(), -1, null);
                }
            } else if (comment != null) {
                intent = CommitActivity.makeIntent(getActivity(), repoOwner, repoName,
                        comment.getCommitId(), initialComment);
            }

        } else if (Event.TYPE_PUSH.equals(eventType)) {
            PushPayload payload = (PushPayload) event.getPayload();
            List<Commit> commits = payload.getCommits();

            if (commits != null && !commits.isEmpty()) {
                if (commits.size() > 1) {
                    // if commit > 1, then show compare activity
                    intent = CompareActivity.makeIntent(getActivity(), repoOwner, repoName,
                            payload.getBefore(), payload.getHead());
                } else {
                    // only 1 commit, then show the commit details
                    intent = CommitActivity.makeIntent(getActivity(),
                            repoOwner, repoName, payload.getCommits().get(0).getSha());
                }
            } else {
                intent = RepositoryActivity.makeIntent(getActivity(), repoOwner, repoName);
            }

        } else if (Event.TYPE_RELEASE.equals(eventType)) {
            ReleasePayload payload = (ReleasePayload) event.getPayload();
            Release release = payload.getRelease();
            if (release != null) {
                intent = ReleaseInfoActivity.makeIntent(getActivity(),
                        repoOwner, repoName, release.getId());
            }

        } else if (Event.TYPE_WATCH.equals(eventType)) {
            intent = RepositoryActivity.makeIntent(getActivity(), repoOwner, repoName);
        }

        if (intent != null) {
            startActivity(intent);
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        ContextMenuAwareRecyclerView.RecyclerContextMenuInfo info =
                (ContextMenuAwareRecyclerView.RecyclerContextMenuInfo) menuInfo;
        Event event = mAdapter.getItemFromAdapterPosition(info.position);

        if (EventAdapter.hasInvalidPayload(event)) {
            return;
        }

        String eventType = event.getType();
        EventRepository eventRepo = event.getRepo();
        String[] repoNamePart = eventRepo.getName().split("/");
        String repoOwner = repoNamePart.length == 2 ? repoNamePart[0] : null;
        String repoName = repoNamePart.length == 2 ? repoNamePart[1] : null;

        menu.setHeaderTitle(R.string.go_to);

        /** Common menu */
        menu.add(getString(R.string.menu_user, event.getActor().getLogin()))
                .setIntent(UserActivity.makeIntent(getActivity(), event.getActor()));
        if (repoOwner != null) {
            menu.add(getString(R.string.menu_repo, eventRepo.getName()))
                    .setIntent(RepositoryActivity.makeIntent(getActivity(), repoOwner, repoName));
        }

        if (Event.TYPE_COMMIT_COMMENT.equals(eventType) && repoOwner != null) {
            CommitCommentPayload payload = (CommitCommentPayload) event.getPayload();
            String sha = payload.getComment().getCommitId();
            menu.add(getString(R.string.menu_commit, sha.substring(0, 7)))
                    .setIntent(CommitActivity.makeIntent(getActivity(), repoOwner, repoName, sha));

        } else if (Event.TYPE_DOWNLOAD.equals(eventType)) {
            DownloadPayload payload = (DownloadPayload) event.getPayload();
            menu.add(Menu.NONE, MENU_DOWNLOAD_START, Menu.NONE,
                    getString(R.string.menu_file, payload.getDownload().getName()));

        } else if (Event.TYPE_FOLLOW.equals(eventType)) {
            FollowPayload payload = (FollowPayload) event.getPayload();
            User target = payload.getTarget();
            if (target != null) {
                menu.add(getString(R.string.menu_user, target.getLogin()))
                        .setIntent(UserActivity.makeIntent(getActivity(), target));
            }

        } else if (Event.TYPE_FORK.equals(eventType)) {
            ForkPayload payload = (ForkPayload) event.getPayload();
            Repository forkee = payload.getForkee();
            if (forkee != null) {
                menu.add(getString(R.string.menu_fork, forkee.getOwner().getLogin() + "/" + forkee.getName()))
                        .setIntent(RepositoryActivity.makeIntent(getActivity(), forkee));
            }

        } else if (Event.TYPE_GIST.equals(eventType)) {
            GistPayload payload = (GistPayload) event.getPayload();
            String gistId = payload.getGist().getId();
            menu.add(getString(R.string.menu_gist, gistId))
                    .setIntent(GistActivity.makeIntent(getActivity(), gistId));

        } else if (Event.TYPE_GOLLUM.equals(eventType)) {
            GollumPayload payload = (GollumPayload) event.getPayload();
            List<GollumPage> pages = payload.getPages();
            if (pages != null && !pages.isEmpty()) { //TODO: now just open the first page
                menu.add(getString(R.string.menu_wiki))
                        .setIntent(new Intent(Intent.ACTION_VIEW, Uri.parse(pages.get(0).getHtmlUrl())));
            }

        } else if (Event.TYPE_ISSUE_COMMENT.equals(eventType)) {
            menu.add(getString(R.string.menu_issues))
                    .setIntent(IssueListActivity.makeIntent(getActivity(),repoOwner, repoName));

        } else if (Event.TYPE_ISSUES.equals(eventType)) {
            IssuesPayload payload = (IssuesPayload) event.getPayload();
            int issueNumber = payload.getIssue().getNumber();
            menu.add(getString(R.string.menu_issue, issueNumber))
                    .setIntent(IssueActivity.makeIntent(getActivity(), repoOwner, repoName, issueNumber));

        } else if (Event.TYPE_PULL_REQUEST.equals(eventType)) {
            PullRequestPayload payload = (PullRequestPayload) event.getPayload();
            menu.add(getString(R.string.menu_pull, payload.getNumber()))
                    .setIntent(PullRequestActivity.makeIntent(getActivity(),
                            repoOwner, repoName, payload.getNumber()));

        } else if (Event.TYPE_PUSH.equals(eventType) && repoOwner != null) {
            PushPayload payload = (PushPayload) event.getPayload();
            menu.add(getString(R.string.menu_compare, payload.getHead().substring(0, 7)))
                    .setIntent(CompareActivity.makeIntent(getActivity(), repoOwner, repoName,
                            payload.getBefore(), payload.getHead()));

            List<Commit> commits = payload.getCommits();
            for (int i = 0; i < commits.size(); i++) {
                String sha = commits.get(i).getSha();
                menu.add(getString(R.string.menu_commit, sha.substring(0, 7)))
                        .setIntent(CommitActivity.makeIntent(getActivity(), repoOwner, repoName, sha));
            }

        } else if (Event.TYPE_RELEASE.equals(eventType)) {
            ReleasePayload payload = (ReleasePayload) event.getPayload();
            List<Download> downloads = payload.getRelease().getAssets();
            int count = downloads != null ? downloads.size() : 0;

            for (int i = 0; i < count; i++) {
                menu.add(Menu.NONE, MENU_DOWNLOAD_START + i, Menu.NONE,
                        getString(R.string.menu_file, downloads.get(i).getName()));
            }
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        ContextMenuAwareRecyclerView.RecyclerContextMenuInfo info =
                (ContextMenuAwareRecyclerView.RecyclerContextMenuInfo) item.getMenuInfo();
        if (info.position >= mAdapter.getItemCount()) {
            return false;
        }

        int id = item.getItemId();
        Event event = mAdapter.getItemFromAdapterPosition(info.position);

        if (id >= MENU_DOWNLOAD_START && id <= MENU_DOWNLOAD_END) {
            final Download download;
            if (Event.TYPE_RELEASE.equals(event.getType())) {
                ReleasePayload payload = (ReleasePayload) event.getPayload();
                download = payload.getRelease().getAssets().get(id - MENU_DOWNLOAD_START);
            } else if (Event.TYPE_DOWNLOAD.equals(event.getType())) {
                DownloadPayload payload = (DownloadPayload) event.getPayload();
                download = payload.getDownload();
            } else {
                download = null;
            }

            if (download != null) {
                UiUtils.enqueueDownloadWithPermissionCheck((BaseActivity) getActivity(),
                        download.getHtmlUrl(), download.getContentType(),
                        download.getName(), download.getDescription(), null);
            }
            return true;
        }

        return false;
    }
}
