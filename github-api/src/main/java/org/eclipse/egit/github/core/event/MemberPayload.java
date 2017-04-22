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
 * MemberEvent payload model class.
 */
public class MemberPayload extends EventPayload implements Serializable {

	private static final long serialVersionUID = -4261757812093447848L;

	public static final String ACTION_ADD = "added";

	private User member;

	private String action;

	/**
	 * @return member
	 */
	public User getMember() {
		return member;
	}

	/**
	 * @param member
	 * @return this MemberPayload
	 */
	public MemberPayload setMember(User member) {
		this.member = member;
		return this;
	}

	/**
	 * @return action
	 */
	public String getAction() {
		return action;
	}

	/**
	 * @param action
	 * @return this MemberPayload
	 */
	public MemberPayload setAction(String action) {
		this.action = action;
		return this;
	}
}
