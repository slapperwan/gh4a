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

/**
 * Parent class for event payloads
 *
 * @see <a href="http://developer.github.com/v3/events/types">GitHub Event types
 *      API documentation</a>
 */
public class EventPayload implements Serializable {

	private static final long serialVersionUID = 1022083387039340606L;
}
