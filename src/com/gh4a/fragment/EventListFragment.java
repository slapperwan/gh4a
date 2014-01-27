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

import java.util.Arrays;
import java.util.List;

import org.eclipse.egit.github.core.Commit;
import org.eclipse.egit.github.core.Download;
import org.eclipse.egit.github.core.GollumPage;
import org.eclipse.egit.github.core.Issue;
import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.egit.github.core.Release;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.client.PageIterator;
import org.eclipse.egit.github.core.event.CommitCommentPayload;
import org.eclipse.egit.github.core.event.DownloadPayload;
import org.eclipse.egit.github.core.event.Event;
import org.eclipse.egit.github.core.event.EventPayload;
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
import com.gh4a.utils.StringUtils;
import com.gh4a.utils.UiUtils;

public abstract class EventListFragment extends PagedDataBaseFragment<Event> {
    private static final int MENU_USER = 1;
    private static final int MENU_REPO = 2;
    private static final int MENU_OPEN_ISSUES = 4;
    private static final int MENU_ISSUE = 5;
    private static final int MENU_GIST = 6;
    private static final int MENU_FILE = 7;
    private static final int MENU_FORKED_REPO = 8;
    private static final int MENU_WIKI_IN_BROWSER = 9;
    private static final int MENU_PULL_REQ = 10;
    private static final int MENU_COMPARE = 11;
    private static final int MENU_COMMENT_COMMIT = 12;
    private static final int MENU_PUSH_COMMIT_START = 100;
    
    private String mLogin;
    private boolean mIsPrivate;
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
        mLogin = getArguments().getString(Constants.User.USER_LOGIN);
        mIsPrivate = getArguments().getBoolean(Constants.Event.IS_PRIVATE);
    }

    @Override
    protected RootAdapter<Event> onCreateAdapter() {
        return new FeedAdapter(getActivity());
    }
    
    @Override
    protected int getEmptyTextResId() {
        return R.string.no_events_found;
    }

    @Override
    protected PageIterator<Event> onCreateIterator() {
        EventService eventService = (EventService)
                Gh4Application.get(getActivity()).getService(Gh4Application.EVENT_SERVICE);
        if (mIsPrivate) {
            return eventService.pageUserReceivedEvents(mLogin, true);
        }
        return eventService.pageUserEvents(mLogin, false);
    }

    @Override
    protected void onItemClick(Event event) {
        Gh4Application context = Gh4Application.get(getActivity());
        
        if (event.getPayload() == null) {
            return;
        }
        
        //if payload is a base class, return void.  Think that it is an old event which not supported
        //by API v3.
        if (!(event.getPayload() instanceof EventPayload)) {
            return;
        }
        
        String eventType = event.getType();
        EventRepository eventRepo = event.getRepo();
        String repoOwner = "";
        String repoName = "";

        if (eventRepo != null) {
            String[] repoNamePart = eventRepo.getName().split("/");
            if (repoNamePart.length == 2) {
                repoOwner = repoNamePart[0];
                repoName = repoNamePart[1];
            }
        }

        if (Arrays.binarySearch(REPO_EVENTS, eventType) >= 0 && eventRepo == null) {
            context.notFoundMessage(getActivity(), R.plurals.repository);
            return;
        }

        if (Event.TYPE_COMMIT_COMMENT.equals(eventType)) {
            CommitCommentPayload payload = (CommitCommentPayload) event.getPayload();
            context.openCommitInfoActivity(getActivity(), repoOwner, repoName,
                    payload.getComment().getCommitId(), 0);

        } else if (Event.TYPE_CREATE.equals(eventType)) {
            context.openRepositoryInfoActivity(getActivity(), repoOwner, repoName, null, 0);

        } else if (Event.TYPE_DELETE.equals(eventType)) {
            context.openRepositoryInfoActivity(getActivity(), repoOwner, repoName, null, 0);

        } else if (Event.TYPE_DOWNLOAD.equals(eventType)) {
            DownloadPayload payload = (DownloadPayload) event.getPayload();
            Download download = payload.getDownload();
            UiUtils.enqueueDownload(getActivity(), download.getUrl(), download.getContentType(),
                    download.getName(), download.getDescription());

        } else if (Event.TYPE_FOLLOW.equals(eventType)) {
            FollowPayload payload = (FollowPayload) event.getPayload();
            if (payload.getTarget() != null) {
                context.openUserInfoActivity(getActivity(), payload.getTarget().getLogin(), null);
            }

        } else if (Event.TYPE_FORK.equals(eventType)) {
            ForkPayload payload = (ForkPayload) event.getPayload();
            Repository forkee = payload.getForkee();
            if (forkee != null) {
                context.openRepositoryInfoActivity(getActivity(), forkee);
            } else {
                context.notFoundMessage(getActivity(), R.plurals.repository);
            }

        } else if (Event.TYPE_FORK_APPLY.equals(eventType)) {
            context.openRepositoryInfoActivity(getActivity(), repoOwner, repoName, null, 0);

        } else if (Event.TYPE_GIST.equals(eventType)) {
            GistPayload payload = (GistPayload) event.getPayload();
            String login = event.getActor().getLogin();
            if (StringUtils.isBlank(login) && payload.getGist() != null
                    && payload.getGist().getUser() != null) {
                login = payload.getGist().getUser().getLogin();
            }
            if (!StringUtils.isBlank(login)) {
                context.openGistActivity(getActivity(), login, payload.getGist().getId(), 0);
            }

        } else if (Event.TYPE_GOLLUM.equals(eventType)) {
            Intent intent = new Intent(getActivity(), WikiListActivity.class);
            intent.putExtra(Constants.Repository.REPO_OWNER, repoOwner);
            intent.putExtra(Constants.Repository.REPO_NAME, repoName);
            GollumPayload payload = (GollumPayload) event.getPayload();
            if (!payload.getPages().isEmpty()) {
                intent.putExtra(Constants.Object.OBJECT_SHA, payload.getPages().get(0).getSha());
            }
            startActivity(intent);

        } else if (Event.TYPE_ISSUE_COMMENT.equals(eventType)) {
            IssueCommentPayload payload = (IssueCommentPayload) event.getPayload();
            Issue issue = payload.getIssue();
            PullRequest request = issue != null ? issue.getPullRequest() : null;

            if (request != null && request.getHtmlUrl() != null) {
                context.openPullRequestActivity(getActivity(),
                        repoOwner, repoName, issue.getNumber());
            } else if (issue != null) {
                context.openIssueActivity(getActivity(), repoOwner, repoName,
                        issue.getNumber(), issue.getState());
            }

        } else if (Event.TYPE_ISSUES.equals(eventType)) {
            IssuesPayload payload = (IssuesPayload) event.getPayload();
            context.openIssueActivity(getActivity(), repoOwner, repoName, payload.getIssue().getNumber());

        } else if (Event.TYPE_MEMBER.equals(eventType)) {
            context.openRepositoryInfoActivity(getActivity(), repoOwner, repoName, null, 0);

        } else if (Event.TYPE_PUBLIC.equals(eventType)) {
            context.openRepositoryInfoActivity(getActivity(), repoOwner, repoName, null, 0);

        } else if (Event.TYPE_PULL_REQUEST.equals(eventType)) {
            PullRequestPayload payload = (PullRequestPayload) event.getPayload();
            context.openPullRequestActivity(getActivity(), repoOwner, repoName, payload.getNumber());

        } else if (Event.TYPE_PULL_REQUEST_REVIEW_COMMENT.equals(eventType)) {
            PullRequestReviewCommentPayload payload = (PullRequestReviewCommentPayload) event.getPayload();
            context.openCommitInfoActivity(getActivity(), repoOwner, repoName, 
                    payload.getComment().getCommitId(), 0);

        } else if (Event.TYPE_PUSH.equals(eventType)) {
            PushPayload payload = (PushPayload) event.getPayload();
            List<Commit> commits = payload.getCommits();

            if (commits != null && !commits.isEmpty()) {
                if (commits.size() > 1) {
                    // if commit > 1, then show compare activity
                    Intent intent = new Intent(context, CompareActivity.class);
                    intent.putExtra(Constants.Repository.REPO_OWNER, repoOwner);
                    intent.putExtra(Constants.Repository.REPO_NAME, repoName);
                    intent.putExtra(Constants.Repository.HEAD, payload.getHead());
                    intent.putExtra(Constants.Repository.BASE, payload.getBefore());
                    startActivity(intent);
                } else {
                    // only 1 commit, then show the commit details
                    context.openCommitInfoActivity(getActivity(), repoOwner, repoName,
                            payload.getCommits().get(0).getSha(), 0);
                }
            } else {
                context.openRepositoryInfoActivity(getActivity(), repoOwner, repoName, null, 0);
            }

        } else if (Event.TYPE_RELEASE.equals(eventType)) {
            ReleasePayload payload = (ReleasePayload) event.getPayload();
            Release release = payload.getRelease();
            if (release != null) {
                Intent intent = new Intent(getActivity(), ReleaseInfoActivity.class);
                intent.putExtra(Constants.Release.RELEASE, release);
                intent.putExtra(Constants.Release.RELEASER, event.getActor());
                intent.putExtra(Constants.Repository.REPO_OWNER, repoOwner);
                intent.putExtra(Constants.Repository.REPO_NAME, repoName);
                startActivity(intent);
            }

        } else if (Event.TYPE_WATCH.equals(eventType)) {
            context.openRepositoryInfoActivity(getActivity(), repoOwner, repoName, null, 0);
        }
    }
    
    public abstract int getMenuGroupId();
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        
        if (v.getId() != R.id.list_view) {
            return;
        }

        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        Event event = (Event) mAdapter.getItem(info.position);
        int groupId = getMenuGroupId();

        //if payload is a base class, return void.  Think that it is an old event which not supported
        //by API v3.
        if (!(event.getPayload() instanceof EventPayload)) {
            return;
        }

        String eventType = event.getType();
        EventRepository eventRepo = event.getRepo();
        String[] repoNamePart = eventRepo.getName().split("/");
        String repoOwner = repoNamePart.length == 2 ? repoNamePart[0] : null;

        menu.setHeaderTitle(R.string.go_to);

        /** Common menu */
        menu.add(groupId, MENU_USER, Menu.NONE, getString(R.string.menu_user, event.getActor().getLogin()));
        if (repoOwner != null) {
            menu.add(groupId, MENU_REPO, Menu.NONE, getString(R.string.menu_repo, eventRepo.getName()));
        }

        if (Event.TYPE_COMMIT_COMMENT.equals(eventType) && repoOwner != null) {
            CommitCommentPayload payload = (CommitCommentPayload) event.getPayload();
            menu.add(groupId, MENU_COMMENT_COMMIT, Menu.NONE,
                    getString(R.string.menu_commit, payload.getComment().getCommitId().substring(0, 7)));

        } else if (Event.TYPE_DOWNLOAD.equals(eventType)) {
            DownloadPayload payload = (DownloadPayload) event.getPayload();
            menu.add(groupId, MENU_FILE, Menu.NONE,
                    getString(R.string.menu_file, payload.getDownload().getName()));

        } else if (Event.TYPE_FOLLOW.equals(eventType)) {
            FollowPayload payload = (FollowPayload) event.getPayload();
            if (payload.getTarget() != null) {
                menu.add(groupId, MENU_USER, Menu.NONE,
                        getString(R.string.menu_user, payload.getTarget().getLogin()));
            }

        } else if (Event.TYPE_FORK.equals(eventType)) {
            ForkPayload payload = (ForkPayload) event.getPayload();
            Repository forkee = payload.getForkee();
            if (forkee != null) {
                menu.add(groupId, MENU_FORKED_REPO, Menu.NONE,
                        getString(R.string.menu_fork, forkee.getOwner().getLogin() + "/" + forkee.getName()));
            }

        } else if (Event.TYPE_GIST.equals(eventType)) {
            GistPayload payload = (GistPayload) event.getPayload();
            menu.add(groupId, MENU_GIST, Menu.NONE,
                    getString(R.string.menu_gist, payload.getGist().getId()));

        } else if (Event.TYPE_GOLLUM.equals(eventType)) {
            menu.add(groupId, MENU_WIKI_IN_BROWSER, Menu.NONE, getString(R.string.menu_wiki));

        } else if (Event.TYPE_ISSUE_COMMENT.equals(eventType)) {
            menu.add(groupId, MENU_OPEN_ISSUES, Menu.NONE, getString(R.string.menu_issues));

        } else if (Event.TYPE_ISSUES.equals(eventType)) {
            IssuesPayload payload = (IssuesPayload) event.getPayload();
            menu.add(groupId, MENU_ISSUE, Menu.NONE,
                    getString(R.string.menu_issue, payload.getIssue().getNumber()));


        } else if (Event.TYPE_PULL_REQUEST.equals(eventType)) {
            PullRequestPayload payload = (PullRequestPayload) event.getPayload();
            menu.add(groupId, MENU_PULL_REQ, Menu.NONE, getString(R.string.menu_pull, payload.getNumber()));

        } else if (Event.TYPE_PUSH.equals(eventType) && repoOwner != null) {
            PushPayload payload = (PushPayload) event.getPayload();
            menu.add(groupId, MENU_COMPARE, Menu.NONE,
                    getString(R.string.menu_compare, payload.getHead()));

            List<Commit> commits = payload.getCommits();
            for (int i = 0; i < commits.size(); i++) {
                menu.add(groupId, MENU_PUSH_COMMIT_START + i, Menu.NONE,
                        getString(R.string.menu_commit, commits.get(i).getSha()));
            }
        }

        // TODO: release event
    }
    
    public boolean open(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info =
                (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        
        Event event = (Event) mAdapter.getItem(info.position);
        String[] repoNamePart = event.getRepo().getName().split("/");
        String repoOwner = null;
        String repoName = null;
        if (repoNamePart.length == 2) {
            repoOwner = repoNamePart[0];
            repoName = repoNamePart[1];
        }
        
        Gh4Application app = Gh4Application.get(getActivity());
        int id = item.getItemId();

        if (id == MENU_USER) {
            app.openUserInfoActivity(getActivity(), repoOwner, null);
        } else if (id == MENU_REPO) {
            app.openRepositoryInfoActivity(getActivity(), repoOwner, repoName, null, 0);
        } else if (id >= MENU_PUSH_COMMIT_START || id == MENU_COMMENT_COMMIT) {
            if (repoOwner != null) {
                String sha = null;
                if (Event.TYPE_PUSH.equals(event.getType())) {
                    int offset = id - MENU_PUSH_COMMIT_START;
                    sha = ((PushPayload) event.getPayload()).getCommits().get(offset).getSha();
                } else if (Event.TYPE_COMMIT_COMMENT.equals(event.getType())) {
                    sha = ((CommitCommentPayload) event.getPayload()).getComment().getCommitId();
                }
                if (sha != null) {
                    app.openCommitInfoActivity(getActivity(), repoOwner, repoName, sha, 0);
                }
            } else {
                app.notFoundMessage(getActivity(), R.plurals.repository);
            }
        } else if (id == MENU_OPEN_ISSUES) {
            app.openIssueListActivity(getActivity(), repoOwner, repoName,
                    Constants.Issue.ISSUE_STATE_OPEN);
        } else if (id == MENU_ISSUE) {
            IssuesPayload payload = (IssuesPayload) event.getPayload();
            app.openIssueActivity(getActivity(), repoOwner, repoName, payload.getIssue().getNumber());
        } else if (id == MENU_GIST) {
            GistPayload payload = (GistPayload) event.getPayload();
            app.openGistActivity(getActivity(), payload.getGist().getUser().getLogin(),
                    payload.getGist().getId(), 0);
        } else if (id == MENU_FILE) {
            if (repoOwner != null) {
                DownloadPayload payload = (DownloadPayload) event.getPayload();
                Download download = payload.getDownload();
                UiUtils.enqueueDownload(getActivity(), download.getUrl(), download.getContentType(),
                        download.getName(), download.getDescription());
            } else {
                app.notFoundMessage(getActivity(), R.plurals.repository);
            }
        } else if (id == MENU_FORKED_REPO) {
            ForkPayload payload = (ForkPayload) event.getPayload();
            Repository forkee = payload.getForkee();
            if (forkee != null) {
                app.openRepositoryInfoActivity(getActivity(), forkee);
            } else {
                app.notFoundMessage(getActivity(), R.plurals.repository);
            }
        } else if (id == MENU_WIKI_IN_BROWSER) {
            GollumPayload payload = (GollumPayload) event.getPayload();
            List<GollumPage> pages = payload.getPages();
            if (pages != null && !pages.isEmpty()) { //TODO: now just open the first page
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(pages.get(0).getHtmlUrl()));
                startActivity(intent);
            }
        } else if (id == MENU_PULL_REQ) {
            PullRequestPayload payload = (PullRequestPayload) event.getPayload();
            app.openPullRequestActivity(getActivity(), repoOwner, repoName, payload.getNumber());
        } else if (id == MENU_COMPARE) {
            if (repoOwner != null) {
                PushPayload payload = (PushPayload) event.getPayload();
                
                Intent intent = new Intent(getActivity(), CompareActivity.class);
                intent.putExtra(Constants.Repository.REPO_OWNER, repoOwner);
                intent.putExtra(Constants.Repository.REPO_NAME, repoName);
                intent.putExtra(Constants.Repository.HEAD, payload.getHead());
                intent.putExtra(Constants.Repository.BASE, payload.getBefore());
                startActivity(intent);
            }
        }
        return true;
    }
}