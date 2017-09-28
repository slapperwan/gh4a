/*******************************************************************************
 *  Copyright (c) 2011 GitHub Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *******************************************************************************/
package org.eclipse.egit.github.core;

import org.eclipse.egit.github.core.util.ObjectUtils;

import java.io.Serializable;
import java.util.Date;

/**
 * GitHub request error class
 */
public class BlockReason implements Serializable {
    /** serialVersionUID */
    private static final long serialVersionUID = -2491370428476460037L;

    private String reason;

    private Date createdAt;

    private String htmlUrl;

    /**
     * @return reason
     */
    public String getReason() {
        return reason;
    }

    /**
     * @return createdAt
     */
    public Date getCreatedAt() {
        return ObjectUtils.cloneDate(createdAt);
    }

    /**
     * @return htmlUrl
     */
    public String getHtmlUrl() {
        return htmlUrl;
    }
}
