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

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class Reactions implements Serializable {

	/** serialVersionUID */
	private static final long serialVersionUID = 6358575015023539051L;

	private int totalCount;

	@SerializedName("+1")
	private int plusOne;
	@SerializedName("-1")
	private int minusOne;
	private int laugh;
	private int hooray;
	private int confused;
	private int heart;

	public int getTotalCount() {
		return totalCount;
	}

	public int getPlusOne() {
		return plusOne;
	}

	public Reactions setPlusOne(int value) {
		plusOne = value;
		updateTotalCount();
		return this;
	}

	public int getMinusOne() {
		return minusOne;
	}

	public Reactions setMinusOne(int value) {
		minusOne = value;
		updateTotalCount();
		return this;
	}

	public int getLaugh() {
		return laugh;
	}

	public Reactions setLaugh(int value) {
		laugh = value;
		updateTotalCount();
		return this;
	}

	public int getHooray() {
		return hooray;
	}

	public Reactions setHooray(int value) {
		hooray = value;
		updateTotalCount();
		return this;
	}

	public int getConfused() {
		return confused;
	}

	public Reactions setConfused(int value) {
		confused = value;
		updateTotalCount();
		return this;
	}

	public int getHeart() {
		return heart;
	}

	public Reactions setHeart(int value) {
		heart = value;
		updateTotalCount();
		return this;
	}

	private void updateTotalCount() {
		totalCount = plusOne + minusOne + laugh + hooray + confused + heart;
	}
}
