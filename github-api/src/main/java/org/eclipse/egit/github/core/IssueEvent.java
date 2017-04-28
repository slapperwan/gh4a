/******************************************************************************
 *  Copyright (c) 2011 GitHub Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *****************************************************************************/
package org.eclipse.egit.github.core;

import java.io.Serializable;
import java.util.Date;

import org.eclipse.egit.github.core.util.ObjectUtils;

/**
 * Issue event model class
 */
public class IssueEvent implements Serializable {

    /**
    * Closed event
    */
    public static final String TYPE_CLOSED = "closed"; //$NON-NLS-1$

    /**
    * Reopened event
    */
    public static final String TYPE_REOPENED = "reopened"; //$NON-NLS-1$

    /**
    * Subscribed event
    */
    public static final String TYPE_SUBSCRIBED = "subscribed"; //$NON-NLS-1$

    /**
    * Merged event
    */
    public static final String TYPE_MERGED = "merged"; //$NON-NLS-1$

    /**
    * Referenced event
    */
    public static final String TYPE_REFERENCED = "referenced"; //$NON-NLS-1$

    /**
    * Mentioned event
    */
    public static final String TYPE_MENTIONED = "mentioned"; //$NON-NLS-1$

    /**
    * Assigned event
    */
    public static final String TYPE_ASSIGNED = "assigned"; //$NON-NLS-1$

    /**
    * Unassigned event
    */
    public static final String TYPE_UNASSIGNED = "unassigned"; //$NON-NLS-1$

    /**
    * Labeled event
    */
    public static final String TYPE_LABELED = "labeled"; //$NON-NLS-1$

    /**
    * Unlabeled event
    */
    public static final String TYPE_UNLABELED = "unlabeled"; //$NON-NLS-1$

    /**
    * Milestoned event
    */
    public static final String TYPE_MILESTONED = "milestoned"; //$NON-NLS-1$

    /**
    * Demilestoned event
    */
    public static final String TYPE_DEMILESTONED = "demilestoned"; //$NON-NLS-1$

    /**
    * Renamed event
    */
    public static final String TYPE_RENAMED = "renamed"; //$NON-NLS-1$

    /**
    * Locked event
    */
    public static final String TYPE_LOCKED = "locked"; //$NON-NLS-1$

    /**
    * Unlocked event
    */
    public static final String TYPE_UNLOCKED = "unlocked"; //$NON-NLS-1$

    /**
    * HEAD ref deleted event
    */
    public static final String TYPE_HEAD_REF_DELETED = "head_ref_deleted"; //$NON-NLS-1$

    /**
    * HEAD ref restored event
    */
    public static final String TYPE_HEAD_REF_RESTORED = "head_ref_restored"; //$NON-NLS-1$

    /** serialVersionUID */
    private static final long serialVersionUID = -842754108817725707L;

    private long id;

    private String url;

    private User actor;

    private String commitId;

    private String commitUrl;

    private String event;

    private Date createdAt;

    private Label label;

    private User assignee;

    private User assigner;

    private Milestone milestone;

    private Rename rename;

    private Issue issue;

    /**
     * @return id
     */
    public long getId() {
        return id;
    }

    /**
     * @param id
     * @return this issue event
     */
    public IssueEvent setId(long id) {
        this.id = id;
        return this;
    }

    /**
     * @return url
     */
    public String getUrl() {
        return url;
    }

    /**
     * @param url
     * @return this issue event
     */
    public IssueEvent setUrl(String url) {
        this.url = url;
        return this;
    }

    /**
     * @return actor
     */
    public User getActor() {
        return actor;
    }

    /**
     * @param actor
     * @return this issue event
     */
    public IssueEvent setActor(User actor) {
        this.actor = actor;
        return this;
    }

    /**
     * @return commitId
     */
    public String getCommitId() {
        return commitId;
    }

    /**
     * @param commitId
     * @return this issue event
     */
    public IssueEvent setCommitId(String commitId) {
        this.commitId = commitId;
        return this;
    }

    /**
     * @return commitUrl
     */
    public String getCommitUrl() {
        return commitUrl;
    }

    /**
     * @param commitUrl
     * @return this issue event
     */
    public IssueEvent setCommitUrl(String commitUrl) {
        this.commitUrl = commitUrl;
        return this;
    }

    /**
     * @return event
     */
    public String getEvent() {
        return event;
    }

    /**
     * @param event
     * @return this issue event
     */
    public IssueEvent setEvent(String event) {
        this.event = event;
        return this;
    }

    /**
    * @return createdAt
    */
    public Date getCreatedAt() {
        return ObjectUtils.cloneDate(createdAt);
    }

    /**
    * @param createdAt
    * @return this issue event
    */
    public IssueEvent setCreatedAt(Date createdAt) {
        this.createdAt = ObjectUtils.cloneDate(createdAt);
        return this;
    }

    /**
     * @return label
     */
    public Label getLabel() {
        return label;
    }

    /**
     * @param label
     * @return this issue event
     */
    public IssueEvent setLabel(Label label) {
        this.label = label;
        return this;
    }

    /**
     * @return assignee
     */
    public User getAssignee() {
        return assignee;
    }

    /**
     * @param assignee
     * @return this issue event
     */
    public IssueEvent setAssignee(User assignee) {
        this.assignee = assignee;
        return this;
    }

    /**
     * @return assigner
     */
    public User getAssigner() {
        return assigner;
    }

    /**
     * @param assigner
     * @return this issue event
     */
    public IssueEvent setAssigner(User assigner) {
        this.assigner = assigner;
        return this;
    }

    /**
     * @return milestone
     */
    public Milestone getMilestone() {
        return milestone;
    }

    /**
     * @param milestone
     * @return this issue event
     */
    public IssueEvent setMilestone(Milestone milestone) {
        this.milestone = milestone;
        return this;
    }

    /**
     * @return rename
     */
    public Rename getRename() {
        return rename;
    }

    /**
     * @param rename
     * @return this issue event
     */
    public IssueEvent setRename(Rename rename) {
        this.rename = rename;
        return this;
    }

    /**
    * @return issue
    */
    public Issue getIssue() {
        return issue;
    }

    /**
    * @param issue
    * @return this issue event
    */
    public IssueEvent setIssue(Issue issue) {
        this.issue = issue;
        return this;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof IssueEvent) {
            return this.id == ((IssueEvent) other).id;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return ObjectUtils.hashCodeForLong(this.id);
    }
}
