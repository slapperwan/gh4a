/*******************************************************************************
 *  Copyright (c) 2011 GitHub Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *    Jason Tsay (GitHub Inc.) - initial API and implementation
 *******************************************************************************/
package org.eclipse.egit.github.core.event;

import java.io.Serializable;

import org.eclipse.egit.github.core.User;

/**
 * FollowEvent payload model class.
 */
public class FollowPayload extends EventPayload implements Serializable {

	private static final long serialVersionUID = -4345668254608800406L;

	private User target;

	/**
	 * @return target
	 */
	public User getTarget() {
		return target;
	}

	/**
	 * @param target
	 * @return this FollowPayload
	 */
	public FollowPayload setTarget(User target) {
		this.target = target;
		return this;
	}
}
