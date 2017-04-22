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
import java.util.List;

import org.eclipse.egit.github.core.GollumPage;

/**
 * GollumEvent payload model class.
 */
public class GollumPayload extends EventPayload implements Serializable {

	private static final long serialVersionUID = 7111499446827257290L;

	private List<GollumPage> pages;

	/**
	 * @return pages
	 */
	public List<GollumPage> getPages() {
		return pages;
	}

	/**
	 * @param pages
	 * @return this GollumPayload
	 */
	public GollumPayload setPages(List<GollumPage> pages) {
		this.pages = pages;
		return this;
	}
}
