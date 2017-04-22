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
package org.eclipse.egit.github.core;

import java.io.Serializable;
import java.util.List;

/**
 * GitHub request error class
 */
public class RequestError implements Serializable {

	/** serialVersionUID */
	private static final long serialVersionUID = -7842670602124573940L;

	// This field is required for legacy v2 error support
	private String error;

	private String message;

	private List<FieldError> errors;

	/**
	 * @return message
	 */
	public String getMessage() {
		return message != null ? message : error;
	}

	/**
	 * Get errors
	 *
	 * @return list of errors
	 */
	public List<FieldError> getErrors() {
		return errors;
	}
}
