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

import org.eclipse.egit.github.core.okhttp.OkHttpProvider;

import static org.eclipse.egit.github.core.client.IGitHubConstants.CHARSET_UTF8;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Utilities for writing multiple HTTP requests
 */
public class MultiPartUtils {

	/**
	 * Post parts to URL
	 *
	 * @param url
	 * @param parts
	 * @return connection that was posted to
	 * @throws IOException
	 */
	public static HttpURLConnection post(String url, Map<String, Object> parts)
			throws IOException {
		HttpURLConnection post = OkHttpProvider.getOkHttpClient().open(new URL(url));
		post.setRequestMethod("POST"); //$NON-NLS-1$
		return post(post, parts);
	}

	/**
	 * Post parts to connection
	 *
	 * @param post
	 * @param parts
	 * @return connection that was posted to
	 * @throws IOException
	 */
	public static HttpURLConnection post(HttpURLConnection post,
			Map<String, Object> parts) throws IOException {
		String boundary = "00content0boundary00"; //$NON-NLS-1$
		post.setDoOutput(true);
		post.setRequestProperty("Content-Type", //$NON-NLS-1$
				"multipart/form-data; boundary=" + boundary); //$NON-NLS-1$
		BufferedOutputStream output = new BufferedOutputStream(
				post.getOutputStream());
		byte[] buffer = new byte[8192];
		byte[] boundarySeparator = ("--" + boundary + "\r\n") //$NON-NLS-1$ //$NON-NLS-2$
				.getBytes(CHARSET_UTF8);
		byte[] newline = "\r\n".getBytes(CHARSET_UTF8); //$NON-NLS-1$
		try {
			for (Entry<String, Object> part : parts.entrySet()) {
				output.write(boundarySeparator);
				StringBuilder partBuffer = new StringBuilder(
						"Content-Disposition: "); //$NON-NLS-1$
				partBuffer.append("form-data; name=\""); //$NON-NLS-1$
				partBuffer.append(part.getKey());
				partBuffer.append('"');
				output.write(partBuffer.toString().getBytes(CHARSET_UTF8));
				output.write(newline);
				output.write(newline);
				final Object value = part.getValue();
				if (value instanceof InputStream) {
					InputStream input = (InputStream) value;
					int read;
					while ((read = input.read(buffer)) != -1)
						output.write(buffer, 0, read);
					input.close();
				} else
					output.write(part.getValue().toString()
							.getBytes(CHARSET_UTF8));
				output.write(newline);
			}
			output.write(("--" + boundary + "--\r\n").getBytes(CHARSET_UTF8)); //$NON-NLS-1$ //$NON-NLS-2$
		} finally {
			output.close();
		}
		return post;
	}
}
