/**
 * Simple Web Spider - <http://simplewebspider.sourceforge.net/>
 * Copyright (C) 2009  <berendona@users.sourceforge.net>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package simplespider.simplespider.bot.http.apache;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import simplespider.simplespider.bot.http.HttpClient;
import simplespider.simplespider.util.ValidityHelper;

public class ApacheHttpClient implements HttpClient {
	private final org.apache.http.client.HttpClient	httpClient;
	private HttpGet									httpGet			= null;
	private HttpResponse							httpResponse	= null;

	ApacheHttpClient(final org.apache.http.client.HttpClient httpClient) {
		this.httpClient = httpClient;
	}

	@Override
	public void createConnection(final String url) throws ClientProtocolException, IOException {
		ValidityHelper.checkNotEmpty("url", url);

		if (this.httpGet != null) {
			throw new IllegalStateException("There is an already open connection");
		}

		this.httpGet = new HttpGet(url);

		// Create a local instance of cookie store
		final CookieStore cookieStore = new BasicCookieStore();
		// Create local HTTP context
		final HttpContext localContext = new BasicHttpContext();
		// Bind custom cookie store to the local context
		localContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);

		//		this.httpGet.setFollowRedirects(true);

		this.httpResponse = this.httpClient.execute(this.httpGet, localContext);
	}

	@Override
	public int getStatusCode() {
		return getStatusLine().getStatusCode();
	}

	@Override
	public StatusLine getStatusLine() {
		checkForOpenConnection();
		return this.httpResponse.getStatusLine();
	}

	@Override
	public String getStatusText() {
		return getStatusLine().getReasonPhrase();
	}

	@Override
	public String getRedirectedUrl() {
		checkForOpenConnection();
		final URI uri = this.httpGet.getURI();
		final String realBaseUrl = uri.toString();
		return realBaseUrl;
	}

	@Override
	public InputStream getResponseBodyAsStream() throws IOException {
		checkForOpenConnection();

		final HttpEntity entity = this.httpResponse.getEntity();
		if (entity == null) {
			return null;
		}

		return entity.getContent();
	}

	@Override
	public void releaseConnection() {
		checkForOpenConnection();
		this.httpGet.abort();
		this.httpGet = null;
		this.httpResponse = null;
	}

	private void checkForOpenConnection() {
		if (this.httpResponse == null || this.httpGet == null) {
			throw new IllegalStateException("There is no open connection");
		}
	}

	@Override
	public String getMimeType() {
		checkForOpenConnection();

		final HttpEntity entity = this.httpResponse.getEntity();
		if (entity == null) {
			return null;
		}

		final Header contentType = entity.getContentType();
		if (contentType == null) {
			return null;
		}

		final String contentTypeValue = contentType.getValue();
		if (contentTypeValue == null) {
			return null;
		}

		// Only the first part is the mime type: e.g. "text/html; charset=UTF-8"
		final String[] split = contentTypeValue.split(";", -1);
		return split[0];
	}

}
