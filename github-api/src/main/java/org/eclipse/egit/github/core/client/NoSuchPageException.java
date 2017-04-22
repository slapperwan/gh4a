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
package org.eclipse.egit.github.core.client;

import java.io.IOException;
import java.util.NoSuchElementException;

/**
 * Exception class to be thrown when iterating over pages fails. This exception
 * wraps an {@link IOException} that is the actual exception that occurred when
 * the page request was made.
 */
public class NoSuchPageException extends NoSuchElementException {

	/**
	 * serialVersionUID
	 */
	private static final long serialVersionUID = 6795637952359586293L;

	/**
	 * Cause exception
	 */
	protected final IOException cause;

	/**
	 * Create no such page exception
	 *
	 * @param cause
	 */
	public NoSuchPageException(IOException cause) {
		this.cause = cause;
	}

	@Override
	public String getMessage() {
		return cause != null ? cause.getMessage() : super.getMessage();
	}

	@Override
	public IOException getCause() {
		return cause;
	}
}
