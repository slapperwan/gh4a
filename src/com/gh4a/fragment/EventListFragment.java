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
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;

import com.gh4a.Constants;
import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.activities.CompareActivity;
import com.gh4a.activities.ReleaseInfoActivity;
import com.gh4a.activities.WikiListActivity;
import com.gh4a.adapter.FeedAdapter;
import com.gh4a.adapter.RootAdapter;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.StringUtils;
import com.gh4a.utils.ToastUtils;
import com.gh4a.utils.UiUtils;

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
    private static final int MENU_OPEN_ISSUES = 4;
    private static final int MENU_ISSUE = 5;
    private static final int MENU_GIST = 6;
    private static final int MENU_COMMENT_COMMIT = 11;
    private static final int MENU_DOWNLOAD_START = 100;
    private static final int MENU_DOWNLOAD_END = 199;
    private static final int MENU_PUSH_COMMIT_START = 200;

    protected String mLogin;
    private FeedAdapter mAdapter;

    private static final String[] REPO_EVENTS = new String[] {
        Event.TYPE_PUSH, Event.TYPE_ISSUES, Event.TYPE_WATCH, Event.TYPE_CREATE,
        Event.TYPE_PULL_REQUEST, Event.TYPE_COMMIT_COMMENT, Event.TYPE_DELETE,
        Event.TYPE_DOWNLOAD, Event.TYPE_FORK_APPLY, Event.TYPE_PUBLIC,
        Event.TYPE_MEMBER, Event.TYPE_ISSUE_COMMENT
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLogin = getArguments().getString(Constants.User.LOGIN);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        registerForContextMenu(getListView());
    }

    @Override
    protected RootAdapter<Event> onCreateAdapter() {
        mAdapter = new FeedAdapter(getActivity());
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
    protected void onItemClick(Event event) {
        Gh4Application context = Gh4Application.get();

        if (FeedAdapter.hasInvalidPayload(event)) {
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
            ToastUtils.notFoundMessage(getActivity(), R.string.repository);
            return;
        }

        if (Event.TYPE_COMMIT_COMMENT.equals(eventType)) {
            CommitCommentPayload payload = (CommitCommentPayload) event.getPayload();
            CommitComment comment = payload.getComment();
            if (comment != null) {
                intent = IntentUtils.getCommitInfoActivityIntent(getActivity(),
                        repoOwner, repoName, comment.getCommitId());
            }

        } else if (Event.TYPE_CREATE.equals(eventType)) {
            CreatePayload payload = (CreatePayload) event.getPayload();
            String ref = null;
            if ("branch".equals(payload.getRefType()) || "tag".equals(payload.getRefType())) {
                ref = payload.getRef();
            }
            intent = IntentUtils.getRepoActivityIntent(getActivity(), repoOwner, repoName, ref);

        } else if (Event.TYPE_DELETE.equals(eventType)) {
            intent = IntentUtils.getRepoActivityIntent(getActivity(), repoOwner, repoName, null);

        } else if (Event.TYPE_DOWNLOAD.equals(eventType)) {
            DownloadPayload payload = (DownloadPayload) event.getPayload();
            Download download = payload.getDownload();
            UiUtils.enqueueDownload(getActivity(), download.getHtmlUrl(), download.getContentType(),
                    download.getName(), download.getDescription(), null);

        } else if (Event.TYPE_FOLLOW.equals(eventType)) {
            FollowPayload payload = (FollowPayload) event.getPayload();
            intent = IntentUtils.getUserActivityIntent(getActivity(), payload.getTarget());

        } else if (Event.TYPE_FORK.equals(eventType)) {
            ForkPayload payload = (ForkPayload) event.getPayload();
            Repository forkee = payload.getForkee();
            if (forkee != null) {
                IntentUtils.openRepositoryInfoActivity(getActivity(), forkee);
            } else {
                ToastUtils.notFoundMessage(getActivity(), R.string.repository);
            }

        } else if (Event.TYPE_FORK_APPLY.equals(eventType)) {
            intent = IntentUtils.getRepoActivityIntent(getActivity(), repoOwner, repoName, null);

        } else if (Event.TYPE_GIST.equals(eventType)) {
            GistPayload payload = (GistPayload) event.getPayload();
            String login = event.getActor().getLogin();
            if (StringUtils.isBlank(login) && payload.getGist() != null
                    && payload.getGist().getUser() != null) {
                login = payload.getGist().getUser().getLogin();
            }
            if (!StringUtils.isBlank(login)) {
                intent = IntentUtils.getGistActivityIntent(getActivity(),
                        login, payload.getGist().getId());
            }

        } else if (Event.TYPE_GOLLUM.equals(eventType)) {
            intent = new Intent(getActivity(), WikiListActivity.class);
            intent.putExtra(Constants.Repository.OWNER, repoOwner);
            intent.putExtra(Constants.Repository.NAME, repoName);
            GollumPayload payload = (GollumPayload) event.getPayload();
            if (!payload.getPages().isEmpty()) {
                intent.putExtra(Constants.Object.OBJECT_SHA, payload.getPages().get(0).getSha());
            }

        } else if (Event.TYPE_ISSUE_COMMENT.equals(eventType)) {
            IssueCommentPayload payload = (IssueCommentPayload) event.getPayload();
            Issue issue = payload.getIssue();
            PullRequest request = issue != null ? issue.getPullRequest() : null;

            if (request != null && request.getHtmlUrl() != null) {
                intent = IntentUtils.getPullRequestActivityIntent(getActivity(),
                        repoOwner, repoName, issue.getNumber());
            } else if (issue != null) {
                intent = IntentUtils.getIssueActivityIntent(getActivity(),
                        repoOwner, repoName, issue.getNumber());
            }

        } else if (Event.TYPE_ISSUES.equals(eventType)) {
            IssuesPayload payload = (IssuesPayload) event.getPayload();
            startActivity(IntentUtils.getIssueActivityIntent(getActivity(), repoOwner, repoName,
                    payload.getIssue().getNumber()));

        } else if (Event.TYPE_MEMBER.equals(eventType)) {
            intent = IntentUtils.getRepoActivityIntent(getActivity(), repoOwner, repoName, null);

        } else if (Event.TYPE_PUBLIC.equals(eventType)) {
            intent = IntentUtils.getRepoActivityIntent(getActivity(), repoOwner, repoName, null);

        } else if (Event.TYPE_PULL_REQUEST.equals(eventType)) {
            PullRequestPayload payload = (PullRequestPayload) event.getPayload();
            intent = IntentUtils.getPullRequestActivityIntent(getActivity(),
                    repoOwner, repoName, payload.getNumber());

        } else if (Event.TYPE_PULL_REQUEST_REVIEW_COMMENT.equals(eventType)) {
            PullRequestReviewCommentPayload payload =
                    (PullRequestReviewCommentPayload) event.getPayload();
            if (payload.getPullRequest() != null) {
                intent = IntentUtils.getPullRequestActivityIntent(getActivity(),
                        repoOwner, repoName, payload.getPullRequest().getNumber());
            } else if (payload.getComment() != null) {
                intent = IntentUtils.getCommitInfoActivityIntent(getActivity(),
                        repoOwner, repoName, payload.getComment().getCommitId());
            }

        } else if (Event.TYPE_PUSH.equals(eventType)) {
            PushPayload payload = (PushPayload) event.getPayload();
            List<Commit> commits = payload.getCommits();

            if (commits != null && !commits.isEmpty()) {
                if (commits.size() > 1) {
                    // if commit > 1, then show compare activity
                    intent = new Intent(context, CompareActivity.class);
                    intent.putExtra(Constants.Repository.OWNER, repoOwner);
                    intent.putExtra(Constants.Repository.NAME, repoName);
                    intent.putExtra(Constants.Repository.HEAD, payload.getHead());
                    intent.putExtra(Constants.Repository.BASE, payload.getBefore());
                } else {
                    // only 1 commit, then show the commit details
                    intent = IntentUtils.getCommitInfoActivityIntent(getActivity(),
                            repoOwner, repoName, payload.getCommits().get(0).getSha());
                }
            } else {
                intent = IntentUtils.getRepoActivityIntent(getActivity(), repoOwner, repoName, null);
            }

        } else if (Event.TYPE_RELEASE.equals(eventType)) {
            ReleasePayload payload = (ReleasePayload) event.getPayload();
            Release release = payload.getRelease();
            if (release != null) {
                intent = new Intent(getActivity(), ReleaseInfoActivity.class);
                intent.putExtra(Constants.Release.RELEASE, release);
                intent.putExtra(Constants.Release.RELEASER, event.getActor());
                intent.putExtra(Constants.Repository.OWNER, repoOwner);
                intent.putExtra(Constants.Repository.NAME, repoName);
                startActivity(intent);
            }

        } else if (Event.TYPE_WATCH.equals(eventType)) {
            intent = IntentUtils.getRepoActivityIntent(getActivity(), repoOwner, repoName, null);
        }
        if (intent != null) {
            startActivity(intent);
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        Event event = mAdapter.getItem(info.position);

        if (FeedAdapter.hasInvalidPayload(event)) {
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
                .setIntent(IntentUtils.getUserActivityIntent(getActivity(), event.getActor().getLogin()));
        if (repoOwner != null) {
            menu.add(getString(R.string.menu_repo, eventRepo.getName()))
                    .setIntent(IntentUtils.getRepoActivityIntent(getActivity(), repoOwner, repoName, null));
        }

        if (Event.TYPE_COMMIT_COMMENT.equals(eventType) && repoOwner != null) {
            CommitCommentPayload payload = (CommitCommentPayload) event.getPayload();
            String sha = payload.getComment().getCommitId();
            menu.add(getString(R.string.menu_commit, sha.substring(0, 7)))
                    .setIntent(IntentUtils.getCommitInfoActivityIntent(getActivity(), repoOwner, repoName, sha));

        } else if (Event.TYPE_DOWNLOAD.equals(eventType)) {
            DownloadPayload payload = (DownloadPayload) event.getPayload();
            menu.add(Menu.NONE, MENU_DOWNLOAD_START, Menu.NONE,
                    getString(R.string.menu_file, payload.getDownload().getName()));

        } else if (Event.TYPE_FOLLOW.equals(eventType)) {
            FollowPayload payload = (FollowPayload) event.getPayload();
            User target = payload.getTarget();
            if (target != null) {
                menu.add(getString(R.string.menu_user, target.getLogin()))
                        .setIntent(IntentUtils.getUserActivityIntent(getActivity(), target.getLogin()));
            }

        } else if (Event.TYPE_FORK.equals(eventType)) {
            ForkPayload payload = (ForkPayload) event.getPayload();
            Repository forkee = payload.getForkee();
            if (forkee != null) {
                menu.add(getString(R.string.menu_fork, forkee.getOwner().getLogin() + "/" + forkee.getName()))
                        .setIntent(IntentUtils.getRepoActivityIntent(getActivity(), forkee));
            }

        } else if (Event.TYPE_GIST.equals(eventType)) {
            GistPayload payload = (GistPayload) event.getPayload();
            String gistId = payload.getGist().getId();
            String user = payload.getGist().getUser().getLogin();
            menu.add(getString(R.string.menu_gist, gistId))
                    .setIntent(IntentUtils.getGistActivityIntent(getActivity(), user, gistId));

        } else if (Event.TYPE_GOLLUM.equals(eventType)) {
            GollumPayload payload = (GollumPayload) event.getPayload();
            List<GollumPage> pages = payload.getPages();
            if (pages != null && !pages.isEmpty()) { //TODO: now just open the first page
                menu.add(getString(R.string.menu_wiki))
                        .setIntent(new Intent(Intent.ACTION_VIEW, Uri.parse(pages.get(0).getHtmlUrl())));
            }

        } else if (Event.TYPE_ISSUE_COMMENT.equals(eventType)) {
            menu.add(getString(R.string.menu_issues))
                    .setIntent(IntentUtils.getIssueListActivityIntent(getActivity(),
                            repoOwner, repoName, Constants.Issue.STATE_OPEN));

        } else if (Event.TYPE_ISSUES.equals(eventType)) {
            IssuesPayload payload = (IssuesPayload) event.getPayload();
            int issueNumber = payload.getIssue().getNumber();
            menu.add(getString(R.string.menu_issue, issueNumber))
                    .setIntent(IntentUtils.getIssueActivityIntent(getActivity(), repoOwner, repoName, issueNumber));

        } else if (Event.TYPE_PULL_REQUEST.equals(eventType)) {
            PullRequestPayload payload = (PullRequestPayload) event.getPayload();
            menu.add(getString(R.string.menu_pull, payload.getNumber()))
                    .setIntent(IntentUtils.getPullRequestActivityIntent(getActivity(),
                            repoOwner, repoName, payload.getNumber()));

        } else if (Event.TYPE_PUSH.equals(eventType) && repoOwner != null) {
            PushPayload payload = (PushPayload) event.getPayload();
            Intent intent = new Intent(getActivity(), CompareActivity.class);
            intent.putExtra(Constants.Repository.OWNER, repoOwner);
            intent.putExtra(Constants.Repository.NAME, repoName);
            intent.putExtra(Constants.Repository.HEAD, payload.getHead());
            intent.putExtra(Constants.Repository.BASE, payload.getBefore());

            menu.add(getString(R.string.menu_compare, payload.getHead().substring(0, 7)))
                    .setIntent(intent);

            List<Commit> commits = payload.getCommits();
            for (int i = 0; i < commits.size(); i++) {
                String sha = commits.get(i).getSha();
                menu.add(getString(R.string.menu_commit, sha.substring(0, 7)))
                        .setIntent(IntentUtils.getCommitInfoActivityIntent(getActivity(), repoOwner, repoName, sha));
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
        AdapterView.AdapterContextMenuInfo info =
                (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        if (info.position >= mAdapter.getCount()) {
            return false;
        }

        int id = item.getItemId();
        Event event = mAdapter.getItem(info.position);

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
                UiUtils.enqueueDownload(getActivity(), download.getHtmlUrl(), download.getContentType(),
                        download.getName(), download.getDescription(), null);
            }
            return true;
        }

        return false;
    }
}
