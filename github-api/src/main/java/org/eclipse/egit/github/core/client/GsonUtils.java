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

import static com.google.gson.FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.Reader;
import java.lang.reflect.Type;
import java.util.Date;

import org.eclipse.egit.github.core.event.Event;

/**
 * Gson utilities.
 */
public abstract class GsonUtils {

	private static final Gson GSON = createGson(true);

	private static final Gson GSON_NO_NULLS = createGson(false);

	/**
	 * Create the standard {@link Gson} configuration
	 *
	 * @return created gson, never null
	 */
	public static final Gson createGson() {
		return createGson(true);
	}

	/**
	 * Create the standard {@link Gson} configuration
	 *
	 * @param serializeNulls
	 *            whether nulls should be serialized
	 *
	 * @return created gson, never null
	 */
	public static final Gson createGson(final boolean serializeNulls) {
		final GsonBuilder builder = new GsonBuilder();
		builder.registerTypeAdapter(Date.class, new DateFormatter());
		builder.registerTypeAdapter(Event.class, new EventFormatter());
		builder.setFieldNamingPolicy(LOWER_CASE_WITH_UNDERSCORES);
		if (serializeNulls)
			builder.serializeNulls();
		return builder.create();
	}

	/**
	 * Get reusable pre-configured {@link Gson} instance
	 *
	 * @return Gson instance
	 */
	public static final Gson getGson() {
		return GSON;
	}

	/**
	 * Get reusable pre-configured {@link Gson} instance
	 *
	 * @param serializeNulls
	 * @return Gson instance
	 */
	public static final Gson getGson(final boolean serializeNulls) {
		return serializeNulls ? GSON : GSON_NO_NULLS;
	}

	/**
	 * Convert object to json
	 *
	 * @param object
	 * @return json string
	 */
	public static final String toJson(final Object object) {
		return toJson(object, true);
	}

	/**
	 * Convert object to json
	 *
	 * @param object
	 * @param includeNulls
	 * @return json string
	 */
	public static final String toJson(final Object object,
			final boolean includeNulls) {
		return includeNulls ? GSON.toJson(object) : GSON_NO_NULLS
				.toJson(object);
	}

	/**
	 * Convert string to given type
	 *
	 * @param json
	 * @param type
	 * @return instance of type
	 */
	public static final <V> V fromJson(String json, Class<V> type) {
		return GSON.fromJson(json, type);
	}

	/**
	 * Convert string to given type
	 *
	 * @param json
	 * @param type
	 * @return instance of type
	 */
	public static final <V> V fromJson(String json, Type type) {
		return GSON.fromJson(json, type);
	}

	/**
	 * Convert content of reader to given type
	 *
	 * @param reader
	 * @param type
	 * @return instance of type
	 */
	public static final <V> V fromJson(Reader reader, Class<V> type) {
		return GSON.fromJson(reader, type);
	}

	/**
	 * Convert content of reader to given type
	 *
	 * @param reader
	 * @param type
	 * @return instance of type
	 */
	public static final <V> V fromJson(Reader reader, Type type) {
		return GSON.fromJson(reader, type);
	}
}
