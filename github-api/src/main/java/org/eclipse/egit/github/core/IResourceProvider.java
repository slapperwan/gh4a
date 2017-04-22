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

import java.util.List;

/**
 * Interface for container classes that can provide a collection of resources of
 * the same type.
 * 
 * @param <V>
 */
public interface IResourceProvider<V> {

	/**
	 * Get collection of resources
	 * 
	 * @return non-null but possibly empty collection
	 */
	List<V> getResources();

}
