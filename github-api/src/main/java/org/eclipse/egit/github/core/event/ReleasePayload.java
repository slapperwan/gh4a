/*******************************************************************************
 * Copyright (c) 2015 Jon Ander Peñalba <jonander.penalba@gmail.com>.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Jon Ander Peñalba - initial API and implementation
 *******************************************************************************/
package org.eclipse.egit.github.core.event;

import java.io.Serializable;

import org.eclipse.egit.github.core.Release;

/**
 * ReleaseEvent payload model class.
 * @since 4.2
 */
public class ReleasePayload extends EventPayload implements Serializable {

	private static final long serialVersionUID = 3309944674574815351L;

	public static final String ACTION_PUBLISH = "published";

	private String action;

	private Release release;

	/**
	 * @return action
	 */
	public String getAction() {
		return action;
	}

	/**
	 * @param action
	 * @return this ReleasePayload
	 */
	public ReleasePayload setAction(String action) {
		this.action = action;
		return this;
	}

	/**
	 * @return release
	 */
	public Release getRelease() {
		return release;
	}

	/**
	 * @param release
	 * @return this ReleasePayload
	 */
	public ReleasePayload setRelease(Release release) {
		this.release = release;
		return this;
	}
}
