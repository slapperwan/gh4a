/*******************************************************************************
 * Copyright (c) 2015 Jon Ander Peñalba <jonan88@gmail.com>.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Jon Ander Peñalba - initial API and implementation
 *******************************************************************************/
package org.eclipse.egit.github.core;

import java.io.Serializable;

/**
 * Rename model class
 */
public class Rename implements Serializable {

	/** serialVersionUID */;
	private final static long serialVersionUID = -4700399891066053425L;

	private String from;

	private String to;

	/**
	 * @return from
	 */
	public String getFrom() {
		return from;
	}

	/**
	 * @param from
	 * @return this rename model
	 */
	public Rename setFrom(String from) {
		this.from = from;
		return this;
	}

	/**
	 * @return to
	 */
	public String getTo() {
		return to;
	}

	/**
	 * @param to
	 * @return this rename model
	 */
	public Rename setTo(String to) {
		this.to = to;
		return this;
	}
}
