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

/**
 * WatchEvent payload model class.
 */
public class WatchPayload extends EventPayload implements Serializable {

	private static final long serialVersionUID = -1600566006173513492L;

	public static final String ACTION_START = "started";

	private String action;

	/**
	 * @return action
	 */
	public String getAction() {
		return action;
	}

	/**
	 * @param action
	 * @return this WatchPayload
	 */
	public WatchPayload setAction(String action) {
		this.action = action;
		return this;
	}
}
