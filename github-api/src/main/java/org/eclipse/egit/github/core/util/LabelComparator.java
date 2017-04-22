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
package org.eclipse.egit.github.core.util;

import java.io.Serializable;
import java.util.Comparator;

import org.eclipse.egit.github.core.Label;

/**
 * Label comparator using case-insensitive name comparisons.
 */
public class LabelComparator implements Comparator<Label>, Serializable {

	/**
	 * serialVersionUID
	 */
	private static final long serialVersionUID = -3185701121586168554L;

	/**
	 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
	 */
	public int compare(Label label1, Label label2) {
		return label1.getName().compareToIgnoreCase(label2.getName());
	}

}
