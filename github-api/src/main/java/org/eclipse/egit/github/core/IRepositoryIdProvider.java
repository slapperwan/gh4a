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

/**
 * Interface to provide an ID for a repository. This refers to the unique
 * identifier of any GitHub repository that is currently the owner and name of
 * the repository joined by a single '/'.
 */
public interface IRepositoryIdProvider {

	/**
	 * Generate a repository id
	 * 
	 * @return repository id
	 */
	String generateId();

}
