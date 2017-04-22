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
package org.eclipse.egit.github.core.util;

import java.io.Serializable;
import java.util.Comparator;

import org.eclipse.egit.github.core.Milestone;

/**
 * Milestone comparator using case-insensitive name comparisons.
 */
public class MilestoneComparator implements Comparator<Milestone>, Serializable {

	/**
	 * serialVersionUID
	 */
	private static final long serialVersionUID = 7166479273639101758L;

	public int compare(Milestone m1, Milestone m2) {
		return m1.getTitle().compareToIgnoreCase(m2.getTitle());
	}

}
