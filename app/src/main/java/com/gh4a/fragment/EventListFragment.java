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
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.util.Pair;
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
import com.gh4a.ServiceFactory;
import com.gh4a.activities.CommitActivity;
import com.gh4a.activities.CompareActivity;
import com.gh4a.activities.GistActivity;
import com.gh4a.activities.IssueActivity;
import com.gh4a.activities.IssueListActivity;
import com.gh4a.activities.PullRequestActivity;
import com.gh4a.activities.ReleaseInfoActivity;
import com.gh4a.activities.RepositoryActivity;
import com.gh4a.activities.ReviewActivity;
import com.gh4a.activities.UserActivity;
import com.gh4a.activities.WikiListActivity;
import com.gh4a.adapter.EventAdapter;
import com.gh4a.adapter.RootAdapter;
import com.gh4a.resolver.CommitCommentLoadTask;
import com.gh4a.resolver.PullRequestReviewCommentLoadTask;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.DownloadUtils;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.RxUtils;
import com.gh4a.widget.ContextMenuAwareRecyclerView;
import com.meisolsson.githubsdk.model.Commit;
import com.meisolsson.githubsdk.model.CommitCompare;
import com.meisolsson.githubsdk.model.Download;
import com.meisolsson.githubsdk.model.GitHubEvent;
import com.meisolsson.githubsdk.model.GitHubEventType;
import com.meisolsson.githubsdk.model.GitHubWikiPage;
import com.meisolsson.githubsdk.model.Issue;
import com.meisolsson.githubsdk.model.Page;
import com.meisolsson.githubsdk.model.PullRequest;
import com.meisolsson.githubsdk.model.ReferenceType;
import com.meisolsson.githubsdk.model.Release;
import com.meisolsson.githubsdk.model.ReleaseAsset;
import com.meisolsson.githubsdk.model.Repository;
import com.meisolsson.githubsdk.model.ReviewComment;
import com.meisolsson.githubsdk.model.User;
import com.meisolsson.githubsdk.model.git.GitComment;
import com.meisolsson.githubsdk.model.git.GitCommit;
import com.meisolsson.githubsdk.model.payload.CommitCommentPayload;
import com.meisolsson.githubsdk.model.payload.CreatePayload;
import com.meisolsson.githubsdk.model.payload.DownloadPayload;
import com.meisolsson.githubsdk.model.payload.FollowPayload;
import com.meisolsson.githubsdk.model.payload.ForkPayload;
import com.meisolsson.githubsdk.model.payload.GistPayload;
import com.meisolsson.githubsdk.model.payload.GitHubPayload;
import com.meisolsson.githubsdk.model.payload.GollumPayload;
import com.meisolsson.githubsdk.model.payload.IssueCommentPayload;
import com.meisolsson.githubsdk.model.payload.IssuesPayload;
import com.meisolsson.githubsdk.model.payload.PullRequestPayload;
import com.meisolsson.githubsdk.model.payload.PullRequestReviewCommentPayload;
import com.meisolsson.githubsdk.model.payload.PullRequestReviewPayload;
import com.meisolsson.githubsdk.model.payload.PushPayload;
import com.meisolsson.githubsdk.model.payload.ReleasePayload;
import com.meisolsson.githubsdk.service.pull_request.PullRequestService;
import com.meisolsson.githubsdk.service.repositories.RepositoryCommitService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.reactivex.Single;
import retrofit2.Response;

public abstract class EventListFragment extends PagedDataBaseFragment<GitHubEvent> {
    private static final int MENU_DOWNLOAD_START = 100;
    private static final int MENU_DOWNLOAD_END = 199;

    private EventAdapter mAdapter;

    private static final GitHubEventType[] REPO_EVENTS = new GitHubEventType[] {
        GitHubEventType.PushEvent, GitHubEventType.IssuesEvent, GitHubEventType.WatchEvent,
        GitHubEventType.CreateEvent, GitHubEventType.PullRequestEvent, GitHubEventType.CommitCommentEvent,
        GitHubEventType.DeleteEvent, GitHubEventType.DownloadEvent, GitHubEventType.ForkApplyEvent,
        GitHubEventType.PublicEvent, GitHubEventType.MemberEvent, GitHubEventType.IssueCommentEvent
    };

    @Override
    protected void onRecyclerViewInflated(RecyclerView view, LayoutInflater inflater) {
        super.onRecyclerViewInflated(view, inflater);
        registerForContextMenu(view);
    }

    @Override
    protected RootAdapter<GitHubEvent, ? extends RecyclerView.ViewHolder> onCreateAdapter() {
        mAdapter = new EventAdapter(getActivity());
        mAdapter.setContextMenuSupported(true);
        return mAdapter;
    }

    @Override
    protected int getEmptyTextResId() {
        return R.string.no_events_found;
    }

    @Override
    public void onItemClick(GitHubEvent event) {
        if (EventAdapter.hasInvalidPayload(event)) {
            return;
        }

        Pair<String, String> repoOwnerAndName = parseRepoIdentifierToOwnerAndName(event.repo());
        String repoOwner = repoOwnerAndName != null ? repoOwnerAndName.first : "";
        String repoName = repoOwnerAndName != null ? repoOwnerAndName.second : "";
        Intent intent = null;
        Single<Optional<Intent>> intentSingle = null;

        if (Arrays.binarySearch(REPO_EVENTS, event.type()) >= 0 && repoOwnerAndName == null) {
            Toast.makeText(getActivity(), R.string.repo_not_found_toast, Toast.LENGTH_LONG).show();
            return;
        }

        switch (event.type()) {
            case CommitCommentEvent: {
                CommitCommentPayload payload = (CommitCommentPayload) event.payload();
                GitComment comment = payload.comment();
                if (comment != null) {
                    intentSingle = CommitCommentLoadTask.load(getActivity(),
                            repoOwner, repoName, comment.commitId(),
                            new IntentUtils.InitialCommentMarker(comment.id()));
                }
                break;
            }

            case CreateEvent: {
                CreatePayload payload = (CreatePayload) event.payload();
                String ref = null;
                if (payload.refType() == ReferenceType.Branch
                        || payload.refType() == ReferenceType.Tag) {
                    ref = payload.ref();
                }
                intent = RepositoryActivity.makeIntent(getActivity(), repoOwner, repoName, ref);
                break;
            }

            case DeleteEvent:
                intent = RepositoryActivity.makeIntent(getActivity(), repoOwner, repoName);
                break;

            case DownloadEvent: {
                DownloadPayload payload = (DownloadPayload) event.payload();
                Download download = payload.download();
                DownloadUtils.enqueueDownloadWithPermissionCheck((BaseActivity) getActivity(), download);
                break;
            }

            case FollowEvent: {
                FollowPayload payload = (FollowPayload) event.payload();
                intent = UserActivity.makeIntent(getActivity(), payload.target());
                break;
            }

            case ForkEvent: {
                ForkPayload payload = (ForkPayload) event.payload();
                Repository forkee = payload.forkee();
                if (forkee != null) {
                    intent = RepositoryActivity.makeIntent(getActivity(), forkee);
                } else {
                    Toast.makeText(getActivity(), R.string.repo_not_found_toast, Toast.LENGTH_LONG)
                            .show();
                }
                break;
            }

            case ForkApplyEvent:
                intent = RepositoryActivity.makeIntent(getActivity(), repoOwner, repoName);
                break;

            case GistEvent: {
                GistPayload payload = (GistPayload) event.payload();
                intent = GistActivity.makeIntent(getActivity(), payload.gist().id());
                break;
            }

            case GollumEvent: {
                GollumPayload payload = (GollumPayload) event.payload();
                intent = WikiListActivity.makeIntent(getActivity(), repoOwner, repoName,
                        payload.pages().isEmpty() ? null : payload.pages().get(0));
                break;
            }

            case IssueCommentEvent: {
                IssueCommentPayload payload = (IssueCommentPayload) event.payload();
                Issue issue = payload.issue();
                PullRequest request = issue != null ? issue.pullRequest() : null;
                IntentUtils.InitialCommentMarker initialComment = payload.comment() != null
                        ? new IntentUtils.InitialCommentMarker(payload.comment().id()) : null;

                if (request != null && request.htmlUrl() != null) {
                    intent = PullRequestActivity.makeIntent(getActivity(),
                            repoOwner, repoName, issue.number(),
                            initialComment != null ? PullRequestActivity.PAGE_CONVERSATION : -1,
                            initialComment);
                } else if (issue != null) {
                    intent = IssueActivity.makeIntent(getActivity(),
                            repoOwner, repoName, issue.number(), initialComment);
                }
                break;
            }

            case IssuesEvent: {
                IssuesPayload payload = (IssuesPayload) event.payload();
                startActivity(IssueActivity.makeIntent(getActivity(), repoOwner, repoName,
                        payload.issue().number()));
                break;
            }

            case MemberEvent:
                intent = RepositoryActivity.makeIntent(getActivity(), repoOwner, repoName);
                break;

            case PublicEvent:
                intent = RepositoryActivity.makeIntent(getActivity(), repoOwner, repoName);
                break;

            case PullRequestEvent: {
                PullRequestPayload payload = (PullRequestPayload) event.payload();
                intent = PullRequestActivity.makeIntent(getActivity(),
                        repoOwner, repoName, payload.number());
                break;
            }

            case PullRequestReviewEvent: {
                PullRequestReviewPayload payload = (PullRequestReviewPayload) event.payload();
                intent = ReviewActivity.makeIntent(getActivity(), repoOwner, repoName,
                        payload.pullRequest().number(), payload.review(), null);
                break;
            }

            case PullRequestReviewCommentEvent: {
                PullRequestReviewCommentPayload payload =
                        (PullRequestReviewCommentPayload) event.payload();
                PullRequest pr = payload.pullRequest();
                ReviewComment comment = payload.comment();
                IntentUtils.InitialCommentMarker initialComment = comment != null
                        ? new IntentUtils.InitialCommentMarker(comment.id()) : null;

                if (pr != null) {
                    if (initialComment != null) {
                        intentSingle = PullRequestReviewCommentLoadTask.load(getActivity(),
                                repoOwner, repoName, pr.number(), initialComment);
                    } else {
                        intent = PullRequestActivity.makeIntent(getActivity(), repoOwner, repoName,
                                pr.number(), -1, null);
                    }
                } else if (comment != null) {
                    intent = CommitActivity.makeIntent(getActivity(), repoOwner, repoName,
                            comment.commitId(), initialComment);
                }
                break;
            }

            case PushEvent: {
                PushPayload payload = (PushPayload) event.payload();
                List<GitCommit> commits = payload.commits();

                if (commits != null && !commits.isEmpty()) {
                    if (commits.size() > 1) {
                        // if commit > 1, then show compare activity
                        intent = CompareActivity.makeIntent(getActivity(), repoOwner, repoName,
                                payload.before(), payload.head());
                    } else {
                        // only 1 commit, then show the commit details
                        intent = CommitActivity.makeIntent(getActivity(),
                                repoOwner, repoName, commits.get(0).sha());
                    }
                } else {
                    intent = RepositoryActivity.makeIntent(getActivity(), repoOwner, repoName);
                }
                break;
            }

            case ReleaseEvent: {
                ReleasePayload payload = (ReleasePayload) event.payload();
                Release release = payload.release();
                if (release != null) {
                    intent = ReleaseInfoActivity.makeIntent(getActivity(),
                            repoOwner, repoName, release.id());
                }
                break;
            }

            case WatchEvent:
                intent = RepositoryActivity.makeIntent(getActivity(), repoOwner, repoName);
                break;
        }

        if (intent != null) {
            startActivity(intent);
        } else if (intentSingle != null) {
            intentSingle
                    .compose(RxUtils::doInBackground)
                    .compose(RxUtils.wrapWithProgressDialog(getActivity(), R.string.loading_msg))
                    .subscribe(result -> {
                        if (result.isPresent() && isAdded()) {
                            startActivity(result.get());
                        }
                    }, error -> Log.d(Gh4Application.LOG_TAG, "Loading click intent failed", error));
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        ContextMenuAwareRecyclerView.RecyclerContextMenuInfo info =
                (ContextMenuAwareRecyclerView.RecyclerContextMenuInfo) menuInfo;
        GitHubEvent event = mAdapter.getItemFromAdapterPosition(info.position);

        if (EventAdapter.hasInvalidPayload(event)) {
            return;
        }

        Pair<String, String> repoOwnerAndName = parseRepoIdentifierToOwnerAndName(event.repo());
        String repoOwner = repoOwnerAndName != null ? repoOwnerAndName.first : null;
        String repoName = repoOwnerAndName != null ? repoOwnerAndName.second : null;

        menu.setHeaderTitle(R.string.go_to);

        /* Common menu */
        menu.add(getString(R.string.menu_user, event.actor().login()))
                .setIntent(UserActivity.makeIntent(getActivity(), event.actor()));
        if (repoOwnerAndName != null) {
            menu.add(getString(R.string.menu_repo, event.repo().repoWithUserName()))
                    .setIntent(RepositoryActivity.makeIntent(getActivity(), repoOwner, repoName));
        }

        switch (event.type()) {
            case CommitCommentEvent:
                if (repoOwnerAndName != null) {
                    CommitCommentPayload payload = (CommitCommentPayload) event.payload();
                    String sha = payload.comment().commitId();
                    menu.add(getString(R.string.menu_commit, sha.substring(0, 7)))
                            .setIntent(CommitActivity.makeIntent(getActivity(), repoOwnerAndName.first, repoOwnerAndName.second, sha));
                }
                break;

            case DownloadEvent: {
                DownloadPayload payload = (DownloadPayload) event.payload();
                menu.add(Menu.NONE, MENU_DOWNLOAD_START, Menu.NONE,
                        getString(R.string.menu_file, payload.download().name()));
                break;
            }

            case FollowEvent: {
                FollowPayload payload = (FollowPayload) event.payload();
                User target = payload.target();
                if (target != null) {
                    menu.add(getString(R.string.menu_user, target.login()))
                            .setIntent(UserActivity.makeIntent(getActivity(), target));
                }
                break;
            }

            case ForkEvent: {
                ForkPayload payload = (ForkPayload) event.payload();
                Repository forkee = payload.forkee();
                if (forkee != null) {
                    menu.add(getString(R.string.menu_fork, ApiHelpers.formatRepoName(getActivity(), forkee)))
                            .setIntent(RepositoryActivity.makeIntent(getActivity(), forkee));
                }
                break;
            }

            case GistEvent: {
                GistPayload payload = (GistPayload) event.payload();
                String gistId = payload.gist().id();
                menu.add(getString(R.string.menu_gist, gistId))
                        .setIntent(GistActivity.makeIntent(getActivity(), gistId));
                break;
            }

            case GollumEvent: {
                GollumPayload payload = (GollumPayload) event.payload();
                List<GitHubWikiPage> pages = payload.pages();
                if (pages != null && !pages.isEmpty()) { //TODO: now just open the first page
                    menu.add(getString(R.string.menu_wiki))
                            .setIntent(new Intent(Intent.ACTION_VIEW, Uri.parse(pages.get(0).htmlUrl())));
                }
                break;
            }

            case IssueCommentEvent: {
                IssueCommentPayload payload = (IssueCommentPayload) event.payload();
                boolean isPullRequest = payload.issue().pullRequest() != null;
                menu.add(getString(isPullRequest ? R.string.menu_pulls : R.string.menu_issues))
                        .setIntent(IssueListActivity.makeIntent(getActivity(),repoOwner, repoName, isPullRequest));
                break;
            }

            case IssuesEvent: {
                IssuesPayload payload = (IssuesPayload) event.payload();
                int issueNumber = payload.issue().number();
                menu.add(getString(R.string.menu_issue, issueNumber))
                        .setIntent(IssueActivity.makeIntent(getActivity(), repoOwner, repoName, issueNumber));
                break;
            }

            case PullRequestEvent: {
                PullRequestPayload payload = (PullRequestPayload) event.payload();
                menu.add(getString(R.string.menu_pull, payload.number()))
                        .setIntent(PullRequestActivity.makeIntent(getActivity(),
                                repoOwner, repoName, payload.number()));
                break;
            }

            case PushEvent: {
                if (repoOwnerAndName != null) {
                    PushPayload payload = (PushPayload) event.payload();
                    menu.add(getString(R.string.menu_compare, payload.head().substring(0, 7)))
                            .setIntent(CompareActivity.makeIntent(getActivity(), repoOwner, repoName,
                                    payload.before(), payload.head()));

                    for (GitCommit commit : payload.commits()) {
                        String sha = commit.sha();
                        menu.add(getString(R.string.menu_commit, sha.substring(0, 7)))
                                .setIntent(CommitActivity.makeIntent(getActivity(), repoOwner, repoName, sha));
                    }
                }
                break;
            }

            case ReleaseEvent: {
                ReleasePayload payload = (ReleasePayload) event.payload();
                List<ReleaseAsset> downloads = payload.release().assets();
                int count = downloads != null ? downloads.size() : 0;

                for (int i = 0; i < count; i++) {
                    menu.add(Menu.NONE, MENU_DOWNLOAD_START + i, Menu.NONE,
                            getString(R.string.menu_file, downloads.get(i).name()));
                }
                break;
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
        GitHubEvent event = mAdapter.getItemFromAdapterPosition(info.position);

        if (id >= MENU_DOWNLOAD_START && id <= MENU_DOWNLOAD_END) {
            if (event.type() == GitHubEventType.ReleaseEvent) {
                ReleasePayload payload = (ReleasePayload) event.payload();
                ReleaseAsset asset = payload.release().assets().get(id - MENU_DOWNLOAD_START);
                DownloadUtils.enqueueDownloadWithPermissionCheck((BaseActivity) getActivity(), asset);
            } else if (event.type() == GitHubEventType.DownloadEvent) {
                DownloadPayload payload = (DownloadPayload) event.payload();
                Download download = payload.download();
                DownloadUtils.enqueueDownloadWithPermissionCheck((BaseActivity) getActivity(), download);
                }
            return true;
        }

        Intent intent = item.getIntent();
        if (intent != null) {
            getActivity().startActivity(intent);
            return true;
        }

        return false;
    }

    @Override
    protected Single<Response<Page<GitHubEvent>>> loadPage(int page, boolean bypassCache) {
        return loadRawPage(page, bypassCache).flatMap(response -> {
            if (!response.isSuccessful()) {
                return Single.just(response);
            }
            Page<GitHubEvent> responsePage = response.body();
            if (responsePage == null) {
                return Single.just(response);
            }

            final PullRequestService
                    prService = ServiceFactory.get(PullRequestService.class, bypassCache);
            Map<PrIdentifier, Single<Response<PullRequest>>> prSingles = new HashMap<>();

            final RepositoryCommitService commitService =
                    ServiceFactory.get(RepositoryCommitService.class, bypassCache);
            Map<PushIdentifier, Single<Response<CommitCompare>>> pushSingles = new HashMap<>();

            for (GitHubEvent event : responsePage.items()) {
                Pair<String, String> repoOwnerAndName =
                        parseRepoIdentifierToOwnerAndName(event.repo());
                if (repoOwnerAndName == null) {
                    continue;
                }
                if (event.type() == GitHubEventType.PullRequestEvent) {
                    PullRequestPayload payload = (PullRequestPayload) event.payload();
                    Single<Response<PullRequest>> prSingle = prService.getPullRequest(
                            repoOwnerAndName.first, repoOwnerAndName.second, payload.number());
                    prSingles.put(new PrIdentifier(event.repo(), payload.number()), prSingle);
                } else if (event.type() == GitHubEventType.PushEvent) {
                    PushPayload payload = (PushPayload) event.payload();
                    Single<Response<CommitCompare>> pushSingle = commitService.compareCommits(
                            repoOwnerAndName.first, repoOwnerAndName.second,
                            payload.before(), payload.head());
                    pushSingles.put(new PushIdentifier(event.repo(),
                            payload.before(), payload.head()), pushSingle);
                }
            }

            if (prSingles.isEmpty() && pushSingles.isEmpty()) {
                return Single.just(response);
            }

            List<Single<Pair<?, Response<?>>>> singlesWithId = new ArrayList<>();
            for (Map.Entry<PrIdentifier, Single<Response<PullRequest>>> entry : prSingles.entrySet()) {
                singlesWithId.add(entry.getValue().map(prResp ->
                        Pair.create(entry.getKey(), prResp)));
            }
            for (Map.Entry<PushIdentifier, Single<Response<CommitCompare>>> entry : pushSingles.entrySet()) {
                singlesWithId.add(entry.getValue().map(commitsResp ->
                        Pair.create(entry.getKey(), commitsResp)));
            }

            return Single.zip(singlesWithId, responses -> {
                List<GitHubEvent> newItems = new ArrayList<>();

                for (GitHubEvent event : responsePage.items()) {
                    GitHubPayload<?> newPayload = null;
                    if (event.type() == GitHubEventType.PullRequestEvent && event.repo() != null) {
                        PullRequestPayload payload = (PullRequestPayload) event.payload();
                        for (Object entryRaw : responses) {
                            var entry = (Pair<?, Response<?>>) entryRaw;
                            if (entry.first instanceof PrIdentifier prId &&
                                    prId.repo.equals(event.repo()) &&
                                    prId.prNumber == payload.number()) {
                                if (entry.second.isSuccessful()) {
                                    newPayload = payload.toBuilder()
                                            .pullRequest((PullRequest) entry.second.body())
                                            .build();
                                }
                                break;
                            }
                        }
                    } else if (event.type() == GitHubEventType.PushEvent && event.repo() != null) {
                        PushPayload payload = (PushPayload) event.payload();
                        for (Object entryRaw : responses) {
                            var entry = (Pair<?, Response<?>>) entryRaw;
                            if (entry.first instanceof PushIdentifier pushId &&
                                    pushId.repo.equals(event.repo()) &&
                                    pushId.base.equals(payload.before()) &&
                                    pushId.head.equals(payload.head())) {
                                if (entry.second.isSuccessful()) {
                                    CommitCompare compare = (CommitCompare) entry.second.body();
                                    List<GitCommit> newCommits = new ArrayList<>();
                                    for (Commit commit : compare.commits()) {
                                        newCommits.add(commit.commit().toBuilder()
                                                .sha(commit.sha())
                                                .build());
                                    }
                                    newPayload = payload.toBuilder()
                                            .commits(newCommits)
                                            .size(compare.commits().size())
                                            .build();
                                }
                                break;
                            }

                        }
                    }
                    if (newPayload != null) {
                        newItems.add(event.toBuilder().payload(newPayload).build());
                    } else {
                        newItems.add(event);
                    }
                }

                Page<GitHubEvent> newPage = Page.<GitHubEvent>builder()
                        .items(newItems)
                        .first(responsePage.first())
                        .last(responsePage.last())
                        .next(responsePage.next())
                        .prev(responsePage.prev())
                        .build();
                return Response.success(newPage);
            });
        });
    }

    private static Pair<String, String> parseRepoIdentifierToOwnerAndName(GitHubEvent.RepoIdentifier identifier) {
        if (identifier == null) {
            return null;
        }
        String repoWithUserName = identifier.repoWithUserName();
        if (repoWithUserName == null) {
            return null;
        }
        String[] parts = repoWithUserName.split("/");
        if (parts.length != 2) {
            return null;
        }
        return Pair.create(parts[0], parts[1]);
    }

    protected abstract Single<Response<Page<GitHubEvent>>> loadRawPage(int page, boolean bypassCache);

    private record PrIdentifier(GitHubEvent.RepoIdentifier repo, int prNumber) {}

    private record PushIdentifier(GitHubEvent.RepoIdentifier repo, String base, String head) {}
}
