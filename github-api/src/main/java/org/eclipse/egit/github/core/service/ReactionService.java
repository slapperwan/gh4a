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
package org.eclipse.egit.github.core.service;

import static org.eclipse.egit.github.core.client.IGitHubConstants.SEGMENT_REACTIONS;

import java.io.IOException;

import org.eclipse.egit.github.core.client.GitHubClient;

/**
 * Service for interacting with reactions
 *
 * @see <a href="http://developer.github.com/v3/reactions">GitHub reactions API
 *      documentation</a>
 */
public class ReactionService extends GitHubService {
	public ReactionService() {
		super();
	}

	public ReactionService(GitHubClient client) {
		super(client);
	}

	public void deleteReaction(int id) throws IOException {
		StringBuilder uri = new StringBuilder(SEGMENT_REACTIONS);
		uri.append('/').append(id);
		client.delete(uri.toString());
	}
}
