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

import org.eclipse.egit.github.core.Repository;

/**
 * ForkEvent payload model class.
 */
public class ForkPayload extends EventPayload implements Serializable {

	private static final long serialVersionUID = 2110456722558520113L;

	private Repository forkee;

	/**
	 * @return forkee
	 */
	public Repository getForkee() {
		return forkee;
	}

	/**
	 * @param forkee
	 * @return this ForkPayload
	 */
	public ForkPayload setForkee(Repository forkee) {
		this.forkee = forkee;
		return this;
	}
}
