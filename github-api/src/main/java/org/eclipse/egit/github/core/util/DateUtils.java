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

import java.util.Date;

/**
 * Date utilities
 */
public abstract class DateUtils {

	/**
	 * Clone date if non-null
	 *
	 * @param date
	 * @return copied date
	 */
	public static Date clone(final Date date) {
		if (date == null)
			return null;
		return new Date(date.getTime());
	}
}
