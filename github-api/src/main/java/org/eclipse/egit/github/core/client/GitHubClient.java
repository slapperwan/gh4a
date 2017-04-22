/*******************************************************************************
 *  Copyright (c) 2011 GitHub Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *    Christian Trutz             - HttpClient 4.1
 *******************************************************************************/
package org.eclipse.egit.github.core.client;

import static com.google.gson.stream.JsonToken.BEGIN_ARRAY;
import static java.net.HttpURLConnection.HTTP_ACCEPTED;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_CONFLICT;
import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.net.HttpURLConnection.HTTP_GONE;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_NO_CONTENT;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_RESET;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import static org.eclipse.egit.github.core.client.IGitHubConstants.AUTH_TOKEN;
import static org.eclipse.egit.github.core.client.IGitHubConstants.CHARSET_UTF8;
import static org.eclipse.egit.github.core.client.IGitHubConstants.CONTENT_TYPE_JSON;
import static org.eclipse.egit.github.core.client.IGitHubConstants.HOST_API;
import static org.eclipse.egit.github.core.client.IGitHubConstants.HOST_DEFAULT;
import static org.eclipse.egit.github.core.client.IGitHubConstants.HOST_GISTS;
import static org.eclipse.egit.github.core.client.IGitHubConstants.PROTOCOL_HTTPS;
import static org.eclipse.egit.github.core.client.IGitHubConstants.SEGMENT_V3_API;
import static org.eclipse.egit.github.core.service.GitHubService.ACCEPT_FULL;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.stream.JsonReader;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

import org.eclipse.egit.github.core.RequestError;
import org.eclipse.egit.github.core.okhttp.OkHttpProvider;
import org.eclipse.egit.github.core.util.EncodingUtils;

/**
 * Client class for interacting with GitHub HTTP/JSON API.
 */
public class GitHubClient {

	/**
	 * Create API v3 client from URL.
	 * <p>
	 * This creates an HTTPS-based client with a host that contains the host
	 * value of the given URL prefixed with 'api' if the given URL is github.com
	 * or gist.github.com
	 *
	 * @param url
	 * @return client
	 */
	public static GitHubClient createClient(String url) {
		try {
			String host = new URL(url).getHost();
			if (HOST_DEFAULT.equals(host) || HOST_GISTS.equals(host))
				host = HOST_API;
			return new GitHubClient(host);
		} catch (IOException e) {
			throw new IllegalArgumentException(e);
		}
	}

	/**
	 * Content-Type header
	 */
	protected static final String HEADER_CONTENT_TYPE = "Content-Type"; //$NON-NLS-1$

	/**
	 * Content-Encoding header
	 */
	protected static final String HEADER_CONTENT_ENCODING = "Content-Encoding"; //$NON-NLS-1$

	/**
	 * Accept header
	 */
	protected static final String HEADER_ACCEPT = "Accept"; //$NON-NLS-1$

	/**
	 * Accept-Encoding header
	 */
	protected static final String HEADER_ACCEPT_ENCODING = "Accept-Encoding"; //$NON-NLS-1$

	/**
	 * Authorization header
	 */
	protected static final String HEADER_AUTHORIZATION = "Authorization"; //$NON-NLS-1$

	/**
	 * User-Agent header
	 */
	protected static final String HEADER_USER_AGENT = "User-Agent"; //$NON-NLS-1$

	/**
	 * METHOD_GET
	 */
	protected static final String METHOD_GET = "GET"; //$NON-NLS-1$

	/**
	 * METHOD_PUT
	 */
	protected static final String METHOD_PUT = "PUT"; //$NON-NLS-1$

	/**
	 * METHOD_POST
	 */
	protected static final String METHOD_POST = "POST"; //$NON-NLS-1$

	/**
	 * METHOD_DELETE
	 */
	protected static final String METHOD_DELETE = "DELETE"; //$NON-NLS-1$

	/**
	 * Default user agent request header value
	 */
	protected static final String USER_AGENT = "GitHubJava/2.1.0"; //$NON-NLS-1$

	/**
	 * 422 status code for unprocessable entity
	 */
	protected static final int HTTP_UNPROCESSABLE_ENTITY = 422;

	/**
	 * Base URI
	 */
	protected final String baseUri;

	/**
	 * Prefix to apply to base URI
	 */
	protected final String prefix;

	/**
	 * {@link Gson} instance
	 */
	protected Gson gson = GsonUtils.getGson();

	private String user;

	private String credentials;

	private String userAgent = USER_AGENT;

	private String headerAccept = ACCEPT_FULL;

	private int bufferSize = 8192;

	private int requestLimit = -1;

	private int remainingRequests = -1;

	/**
	 * Create default client
	 */
	public GitHubClient() {
		this(HOST_API);
	}

	/**
	 * Create client for host name
	 *
	 * @param hostname
	 */
	public GitHubClient(String hostname) {
		this(hostname, -1, PROTOCOL_HTTPS);
	}

	/**
	 * Create client for host, port, and scheme
	 *
	 * @param hostname
	 * @param port
	 * @param scheme
	 */
	public GitHubClient(final String hostname, final int port,
			final String scheme) {
		final StringBuilder uri = new StringBuilder(scheme);
		uri.append("://"); //$NON-NLS-1$
		uri.append(hostname);
		if (port > 0)
			uri.append(':').append(port);
		baseUri = uri.toString();

		// Use URI prefix on non-standard host names
		if (HOST_API.equals(hostname))
			prefix = null;
		else
			prefix = SEGMENT_V3_API;
	}

	/**
	 * Set whether or not serialized data should include fields that are null.
	 *
	 * @param serializeNulls
	 * @return this client
	 */
	public GitHubClient setSerializeNulls(boolean serializeNulls) {
		gson = GsonUtils.getGson(serializeNulls);
		return this;
	}

	/**
	 * Set the value to set as the user agent header on every request created.
	 * Specifying a null or empty agent parameter will reset this client to use
	 * the default user agent header value.
	 *
	 * @param agent
	 * @return this client
	 */
	public GitHubClient setUserAgent(final String agent) {
		if (agent != null && agent.length() > 0)
			userAgent = agent;
		else
			userAgent = USER_AGENT;
		return this;
	}

	/**
	 * Set the value to set as the accept header on every request created.
	 * Specifying a null or empty header parameter will reset this client to use
	 * the default accept header value.
	 *
	 * @param header
	 * @return this client
	 */
	public GitHubClient setHeaderAccept(final String header) {
		if (header != null && header.length() > 0)
			headerAccept = header;
		else
			headerAccept = ACCEPT_FULL;
		return this;
	}

	/**
	 * Returns the accept header currently used for all requests.
	 *
	 * @return header
	 * @since 4.2
	 */
	public String getHeaderAccept() {
		if (headerAccept != null && headerAccept.length() > 0)
			return headerAccept;
		else
			return ACCEPT_FULL;
	}

	/**
	 * Configure request with standard headers
	 *
	 * @param request
	 * @return configured request
	 */
	protected HttpURLConnection configureRequest(final HttpURLConnection request) {
		if (credentials != null)
			request.setRequestProperty(HEADER_AUTHORIZATION, credentials);
		request.setRequestProperty(HEADER_USER_AGENT, userAgent);
		request.setRequestProperty(HEADER_ACCEPT, getHeaderAccept());
		request.setRequestProperty(HEADER_ACCEPT_ENCODING, "gzip;q=1.0, deflate;q=0.5");
		return request;
	}

	/**
	 * Configure request URI
	 *
	 * @param uri
	 * @return configured URI
	 */
	protected String configureUri(final String uri) {
		if (prefix == null || uri.startsWith(prefix))
			return uri;
		else
			return prefix + uri;
	}

	/**
	 * Create connection to URI
	 *
	 * @param uri
	 * @return connection
	 * @throws IOException
	 */
	protected HttpURLConnection createConnection(String uri) throws IOException {
		URL url = new URL(createUri(uri));
        return OkHttpProvider.getOkHttpClient().open(url);
	}

	/**
	 * Create connection to URI
	 *
	 * @param uri
	 * @param method
	 * @return connection
	 * @throws IOException
	 */
	protected HttpURLConnection createConnection(String uri, String method)
			throws IOException {
		HttpURLConnection connection = createConnection(uri);
		connection.setRequestMethod(method);
		return configureRequest(connection);
	}

	/**
	 * Create a GET request connection to the URI
	 *
	 * @param uri
	 * @return connection
	 * @throws IOException
	 */
	protected HttpURLConnection createGet(String uri) throws IOException {
		return createConnection(uri, METHOD_GET);
	}

	/**
	 * Create a POST request connection to the URI
	 *
	 * @param uri
	 * @return connection
	 * @throws IOException
	 */
	protected HttpURLConnection createPost(String uri) throws IOException {
		return createConnection(uri, METHOD_POST);
	}

	/**
	 * Create a PUT request connection to the URI
	 *
	 * @param uri
	 * @return connection
	 * @throws IOException
	 */
	protected HttpURLConnection createPut(String uri) throws IOException {
		return createConnection(uri, METHOD_PUT);
	}

	/**
	 * Create a DELETE request connection to the URI
	 *
	 * @param uri
	 * @return connection
	 * @throws IOException
	 */
	protected HttpURLConnection createDelete(String uri) throws IOException {
		return createConnection(uri, METHOD_DELETE);
	}

	/**
	 * Set credentials
	 *
	 * @param user
	 * @param password
	 * @return this client
	 */
	public GitHubClient setCredentials(final String user, final String password) {
		this.user = user;
		if (user != null && user.length() > 0 && password != null
				&& password.length() > 0)
			credentials = "Basic " //$NON-NLS-1$
					+ EncodingUtils.toBase64(user + ':' + password);
		else
			credentials = null;
		return this;
	}

	/**
	 * Set OAuth2 token
	 *
	 * @param token
	 * @return this client
	 */
	public GitHubClient setOAuth2Token(String token) {
		if (token != null && token.length() > 0)
			credentials = AUTH_TOKEN + ' ' + token;
		else
			credentials = null;
		return this;
	}

	/**
	 * Set buffer size used to send the request and read the response
	 *
	 * @param bufferSize
	 * @return this client
	 */
	public GitHubClient setBufferSize(int bufferSize) {
		if (bufferSize < 1)
			throw new IllegalArgumentException(
					"Buffer size must be greater than zero"); //$NON-NLS-1$

		this.bufferSize = bufferSize;
		return this;
	}

	/**
	 * Get the user that this client is currently authenticating as
	 *
	 * @return user or null if not authentication
	 */
	public String getUser() {
		return user;
	}

	/**
	 * Convert object to a JSON string
	 *
	 * @param object
	 * @return JSON string
	 * @throws IOException
	 */
	protected String toJson(Object object) throws IOException {
		try {
			return gson.toJson(object);
		} catch (JsonParseException jpe) {
			IOException ioe = new IOException(
					"Parse exception converting object to JSON"); //$NON-NLS-1$
			ioe.initCause(jpe);
			throw ioe;
		}
	}

	/**
	 * Parse JSON to specified type
	 *
	 * @param <V>
	 * @param stream
	 * @param type
	 * @return parsed type
	 * @throws IOException
	 */
	protected <V> V parseJson(InputStream stream, Type type) throws IOException {
		return parseJson(stream, type, null);
	}

	/**
	 * Parse JSON to specified type
	 *
	 * @param <V>
	 * @param stream
	 * @param type
	 * @param listType
	 * @return parsed type
	 * @throws IOException
	 */
	protected <V> V parseJson(InputStream stream, Type type, Type listType)
			throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				stream, CHARSET_UTF8), bufferSize);
		if (listType == null)
			try {
				return gson.fromJson(reader, type);
			} catch (JsonParseException jpe) {
				IOException ioe = new IOException(
						"Parse exception converting JSON to object"); //$NON-NLS-1$
				ioe.initCause(jpe);
				throw ioe;
			} finally {
				try {
					reader.close();
				} catch (IOException ignored) {
					// Ignored
				}
			}
		else {
			JsonReader jsonReader = new JsonReader(reader);
			try {
				if (jsonReader.peek() == BEGIN_ARRAY)
					return gson.fromJson(jsonReader, listType);
				else
					return gson.fromJson(jsonReader, type);
			} catch (JsonParseException jpe) {
				IOException ioe = new IOException(
						"Parse exception converting JSON to object"); //$NON-NLS-1$
				ioe.initCause(jpe);
				throw ioe;
			} finally {
				try {
					jsonReader.close();
				} catch (IOException ignored) {
					// Ignored
				}
			}
		}
	}

	/**
	 * Does status code denote an error
	 *
	 * @param code
	 * @return true if error, false otherwise
	 */
	protected boolean isError(final int code) {
		switch (code) {
		case HTTP_BAD_REQUEST:
		case HTTP_UNAUTHORIZED:
		case HTTP_FORBIDDEN:
		case HTTP_NOT_FOUND:
		case HTTP_CONFLICT:
		case HTTP_GONE:
		case HTTP_UNPROCESSABLE_ENTITY:
		case HTTP_INTERNAL_ERROR:
			return true;
		default:
			return false;
		}
	}

	/**
	 * Does status code denote a non-error response?
	 *
	 * @param code
	 * @return true if okay, false otherwise
	 */
	protected boolean isOk(final int code) {
		switch (code) {
		case HTTP_OK:
		case HTTP_CREATED:
		case HTTP_ACCEPTED:
		case HTTP_RESET:
			return true;
		default:
			return false;
		}
	}

	/**
	 * Is the response empty?
	 *
	 * @param code
	 * @return true if empty, false otherwise
	 */
	protected boolean isEmpty(final int code) {
		return HTTP_NO_CONTENT == code;
	}

	/**
	 * Parse error from response
	 *
	 * @param response
	 * @return request error
	 * @throws IOException
	 */
	protected RequestError parseError(InputStream response) throws IOException {
		return parseJson(response, RequestError.class);
	}

	/**
	 * Get body from response inputs stream
	 *
	 * @param request
	 * @param stream
	 * @return parsed body
	 * @throws IOException
	 */
	protected Object getBody(GitHubRequest request, InputStream stream)
			throws IOException {
		Type type = request.getType();
		if (type != null)
			return parseJson(stream, type, request.getArrayType());
		else
			return null;
	}

	/**
	 * Create error exception from response and throw it
	 *
	 * @param response
	 * @param code
	 * @param status
	 * @return non-null newly created {@link IOException}
	 */
	protected IOException createException(InputStream response, int code,
			String status) {
		if (isError(code)) {
			final RequestError error;
			try {
				error = parseError(response);
			} catch (IOException e) {
				return e;
			}
			if (error != null)
				return new RequestException(error, code);
		} else
			try {
				response.close();
			} catch (IOException ignored) {
				// Ignored
			}
		String message;
		if (status != null && status.length() > 0)
			message = status + " (" + code + ')'; //$NON-NLS-1$
		else
			message = "Unknown error occurred (" + code + ')'; //$NON-NLS-1$
		return new IOException(message);
	}

	/**
	 * Post to URI
	 *
	 * @param uri
	 * @throws IOException
	 */
	public void post(String uri) throws IOException {
		post(uri, null, null);
	}

	/**
	 * Put to URI
	 *
	 * @param uri
	 * @throws IOException
	 */
	public void put(String uri) throws IOException {
		put(uri, null, null);
	}

	/**
	 * Delete resource at URI. This method will throw an {@link IOException}
	 * when the response status is not a 204 (No Content).
	 *
	 * @param uri
	 * @throws IOException
	 */
	public void delete(String uri) throws IOException {
		delete(uri, null);
	}

	/**
	 * Send parameters to output stream of request
	 *
	 * @param request
	 * @param params
	 * @throws IOException
	 */
	protected void sendParams(HttpURLConnection request, Object params)
			throws IOException {
		request.setDoOutput(true);
		if (params != null) {
			request.setRequestProperty(HEADER_CONTENT_TYPE, CONTENT_TYPE_JSON
					+ "; charset=" + CHARSET_UTF8); //$NON-NLS-1$
			byte[] data = toJson(params).getBytes(CHARSET_UTF8);
			request.setFixedLengthStreamingMode(data.length);
			BufferedOutputStream output = new BufferedOutputStream(
					request.getOutputStream(), bufferSize);
			try {
				output.write(data);
				output.flush();
			} finally {
				try {
					output.close();
				} catch (IOException ignored) {
					// Ignored
				}
			}
		} else {
			request.setFixedLengthStreamingMode(0);
			request.setRequestProperty("Content-Length", "0");
		}
	}

	private <V> V sendJson(final HttpURLConnection request,
			final Object params, final Type type) throws IOException {
		sendParams(request, params);
		final int code = request.getResponseCode();
		updateRateLimits(request);
		if (isOk(code))
			if (type != null)
				return parseJson(getStream(request), type);
			else
				return null;
		if (isEmpty(code))
			return null;
		throw createException(getStream(request), code,
				request.getResponseMessage());
	}

	private String readStream(final InputStream stream) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				stream, CHARSET_UTF8));
		try {
			StringBuilder output = new StringBuilder();
			char[] buffer = new char[8192];
			int read;
			while ((read = reader.read(buffer)) != -1)
				output.append(buffer, 0, read);
			return output.toString();
		} finally {
			try {
				reader.close();
			} catch (IOException ignored) {
				// Ignored
			}
		}
	}

	/**
	 * Create full URI from path
	 *
	 * @param path
	 * @return uri
	 */
	protected String createUri(final String path) {
		return baseUri + configureUri(path);
	}

	/**
	 * Get response stream contents from GET to URI.
	 *
	 * @param request
	 * @return contents
	 * @throws IOException
	 */
	public String getStreamContents(final GitHubRequest request)
			throws IOException {
		HttpURLConnection connection = createGet(request.generateUri());
		try {
			return readStream(getResponseStream(connection));
		} finally {
			connection.disconnect();
		}
	}

	/**
	 * Get response stream contents from POST to URI.
	 *
	 * @param uri
	 * @param params
	 * @return contents
	 * @throws IOException
	 */
	public String postStreamContents(final String uri, final Object params)
			throws IOException {
		HttpURLConnection connection = createPost(uri);
		try {
			sendParams(connection, params);
			return readStream(getResponseStream(connection));
		} finally {
			connection.disconnect();
		}
	}

	/**
	 * Get response stream for request
	 *
	 * @param request
	 * @return stream
	 * @throws IOException
	 */
	protected InputStream getResponseStream(final HttpURLConnection request)
			throws IOException {
		InputStream stream = getStream(request);
		int code = request.getResponseCode();
		updateRateLimits(request);
		if (isOk(code))
			return stream;
		else
			throw createException(stream, code, request.getResponseMessage());
	}

	/**
	 * Get stream from request
	 *
	 * @param request
	 * @return stream
	 * @throws IOException
	 */
	protected InputStream getStream(HttpURLConnection request)
			throws IOException {
		InputStream stream = null;
		if (request.getResponseCode() >= HTTP_BAD_REQUEST)
			stream = request.getErrorStream();
		if (stream == null)
			stream = request.getInputStream();

		String encoding = request.getHeaderField(HEADER_CONTENT_ENCODING);
		if ("gzip".equals(encoding))
			return new GZIPInputStream(stream);
		else if ("deflate".equals(encoding))
			return new InflaterInputStream(stream);
		else
			return stream;
	}

	/**
	 * Get response from URI and bind to specified type
	 *
	 * @param request
	 * @return response
	 * @throws IOException
	 */
	public GitHubResponse get(GitHubRequest request) throws IOException {
		HttpURLConnection httpRequest = createGet(request.generateUri());
		try {
			String accept = request.getResponseContentType();
			if (accept != null)
				httpRequest.setRequestProperty(HEADER_ACCEPT, accept);
			final int code = httpRequest.getResponseCode();
			updateRateLimits(httpRequest);
			if (isOk(code))
				return new GitHubResponse(httpRequest, getBody(request,
						getStream(httpRequest)));
			if (isEmpty(code))
				return new GitHubResponse(httpRequest, null);
			throw createException(getStream(httpRequest), code,
					httpRequest.getResponseMessage());
		} finally {
			if (httpRequest != null) {
				httpRequest.disconnect();
			}
		}
	}

	/**
	 * Post data to URI
	 *
	 * @param <V>
	 * @param uri
	 * @param params
	 * @param type
	 * @return response
	 * @throws IOException
	 */
	public <V> V post(final String uri, final Object params, final Type type)
			throws IOException {
		HttpURLConnection request = createPost(uri);
		try {
			return sendJson(request, params, type);
		} finally {
			if (request != null) {
				request.disconnect();
			}
		}
	}

	/**
	 * Put data to URI
	 *
	 * @param uri
	 * @param params
	 * @throws IOException
	 */
	public void put(final String uri, final Object params)
			throws IOException {
		HttpURLConnection request = createPut(uri);
		try {
			if (params != null)
				sendParams(request, params);
			final int code = request.getResponseCode();
			updateRateLimits(request);
			if (!isOk(code))
			    throw new RequestException(parseError(getStream(request)), code);
		} finally {
			if (request != null) {
				request.disconnect();
			}
		}
	}

	/**
	 * Put data to URI
	 *
	 * @param <V>
	 * @param uri
	 * @param params
	 * @param type
	 * @return response
	 * @throws IOException
	 */
	public <V> V put(final String uri, final Object params, final Type type)
			throws IOException {
		HttpURLConnection request = createPut(uri);
		try {
			return sendJson(request, params, type);
		} finally {
			if (request != null) {
				request.disconnect();
			}
		}
	}

	/**
	 * Delete resource at URI. This method will throw an {@link IOException}
	 * when the response status is not a 204 (No Content).
	 *
	 * @param uri
	 * @param params
	 * @throws IOException
	 */
	public void delete(final String uri, final Object params)
			throws IOException {
		HttpURLConnection request = createDelete(uri);
		try {
			if (params != null)
				sendParams(request, params);
			final int code = request.getResponseCode();
			updateRateLimits(request);
			if (!isEmpty(code))
				throw new RequestException(parseError(getStream(request)), code);
		} finally {
			if (request != null) {
				request.disconnect();
			}
		}
	}

	/**
	 * Update rate limits present in response headers
	 *
	 * @param request
	 * @return this client
	 */
	protected GitHubClient updateRateLimits(HttpURLConnection request) {
		String limit = request.getHeaderField("X-RateLimit-Limit");
		if (limit != null && limit.length() > 0)
			try {
				requestLimit = Integer.parseInt(limit);
			} catch (NumberFormatException nfe) {
				requestLimit = -1;
			}
		else
			requestLimit = -1;

		String remaining = request.getHeaderField("X-RateLimit-Remaining");
		if (remaining != null && remaining.length() > 0)
			try {
				remainingRequests = Integer.parseInt(remaining);
			} catch (NumberFormatException nfe) {
				remainingRequests = -1;
			}
		else
			remainingRequests = -1;

		return this;
	}

	/**
	 * Get number of requests remaining before rate limiting occurs
	 * <p>
	 * This will be the value of the 'X-RateLimit-Remaining' header from the
	 * last request made
	 *
	 * @return remainingRequests or -1 if not present in the response
	 */
	public int getRemainingRequests() {
		return remainingRequests;
	}

	/**
	 * Get number of requests that {@link #getRemainingRequests()} counts down
	 * from as each request is made
	 * <p>
	 * This will be the value of the 'X-RateLimit-Limit' header from the last
	 * request made
	 *
	 * @return requestLimit or -1 if not present in the response
	 */
	public int getRequestLimit() {
		return requestLimit;
	}

	public <V> PagedRequest<V> createPagedRequest(int start, int size) {
		return new PagedRequest(start, size);
	}

	public <V> PageIterator<V> createPageIterator(PagedRequest<V> request) {
		return new PageIterator(request, this);
	}
}
