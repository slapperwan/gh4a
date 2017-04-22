/*******************************************************************************
 *  Copyright (c) 2016 Danny Baumann
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.github.core;

import java.io.Serializable;
import java.util.List;

/**
 * GitHub issue model class.
 */
public class TextMatch implements Serializable {
	public static class MatchItem implements Serializable {
		private String text;
		private List<Integer> indices;

		/**
		 * @return text
		 */
		public String getText() {
			return text;
		}

		/**
		 * @return Start position of text in fragment
		 */
		public int getStartPos() {
			return indices != null && !indices.isEmpty() ? indices.get(0) : -1;
		}

		/**
		 * @return End position of text in fragment
		 */
		public int getEdndPos() {
			return indices != null && indices.size() >= 2 ? indices.get(1) : -1;
		}
	}

	private String objectType;

	private String property;

	private String fragment;

	private List<MatchItem> matches;

	/**
	 * @return fragment
	 */
	public String getFragment() {
		return fragment;
	}

	/**
	 * @return matches
	 */
	public List<MatchItem> getMatches() {
		return matches;
	}
}
