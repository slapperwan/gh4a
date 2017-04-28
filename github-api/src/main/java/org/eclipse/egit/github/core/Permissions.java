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

/**
 * Permission model class
 */
public class Permissions implements Serializable {
	/** serialVersionUID */
	static final long serialVersionUID = 3480134641776106842L;

	private boolean admin;

	private boolean push;

	private boolean pull;

	/**
	 * @return admin
	 */
	public boolean isAdmin() {
		return admin;
	}

	/**
	 * @return push
	 */
	public boolean hasPushAccess() {
		return push;
	}

	/**
	 * @return pull
	 */
	public boolean canPull() {
		return pull;
	}
}
