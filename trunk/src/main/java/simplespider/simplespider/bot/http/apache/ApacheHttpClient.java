package simplespider.simplespider.bot.http.apache;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.StatusLine;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.methods.GetMethod;

import simplespider.simplespider.bot.http.HttpClient;
import simplespider.simplespider.util.ValidityHelper;

public class ApacheHttpClient implements HttpClient {
	private final org.apache.commons.httpclient.HttpClient	httpClient;
	private HttpMethod										method	= null;

	ApacheHttpClient(final org.apache.commons.httpclient.HttpClient httpClient) {
		this.httpClient = httpClient;
	}

	/*
	 * (non-Javadoc)
	 * @see simplespider.simplespider_core.http.HttpClient#createConnection(java.lang.String)
	 */
	public void createConnection(final String url) throws HttpException, IOException {
		ValidityHelper.checkNotEmpty("url", url);

		if (this.method != null) {
			throw new IllegalStateException("There is an already open connection");
		}

		this.method = new GetMethod(url);
		this.method.setFollowRedirects(true);

		try {
			this.httpClient.executeMethod(this.method);
		} catch (final HttpException e) {
			throw new HttpException("Failed to connect to url \"" + url + "\": " + e, e);
		} catch (final IOException e) {
			throw new IOException("Failed to connect to url \"" + url + "\": " + e, e);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see simplespider.simplespider_core.http.HttpClient#getStatusCode()
	 */
	public int getStatusCode() {
		checkForOpenConnection();
		return this.method.getStatusCode();
	}

	/*
	 * (non-Javadoc)
	 * @see simplespider.simplespider_core.http.HttpClient#getStatusLine()
	 */
	public StatusLine getStatusLine() {
		checkForOpenConnection();
		return this.method.getStatusLine();
	}

	/*
	 * (non-Javadoc)
	 * @see simplespider.simplespider_core.http.HttpClient#getStatusText()
	 */
	public String getStatusText() {
		checkForOpenConnection();
		return this.method.getStatusText();
	}

	/*
	 * (non-Javadoc)
	 * @see simplespider.simplespider_core.http.HttpClient#getRedirectedUrl()
	 */
	public String getRedirectedUrl() throws URIException {
		checkForOpenConnection();
		final URI uri = this.method.getURI();
		final String realBaseUrl = uri.getURI();
		return realBaseUrl;
	}

	/*
	 * (non-Javadoc)
	 * @see simplespider.simplespider_core.http.HttpClient#getResponseBodyAsStream()
	 */
	public InputStream getResponseBodyAsStream() throws IOException {
		checkForOpenConnection();
		return this.method.getResponseBodyAsStream();
	}

	/*
	 * (non-Javadoc)
	 * @see simplespider.simplespider_core.http.HttpClient#releaseConnection()
	 */
	public void releaseConnection() {
		checkForOpenConnection();
		this.method.releaseConnection();
		this.method = null;
	}

	private void checkForOpenConnection() {
		if (this.method == null) {
			throw new IllegalStateException("There is no open connection");
		}
	}

	public String getMimeType() {
		checkForOpenConnection();
		final Header contentTypeHeader = this.method.getResponseHeader("Content-Type");
		if (contentTypeHeader == null) {
			return null;
		}

		final String contentTypeValue = contentTypeHeader.getValue();
		if (contentTypeValue == null) {
			return null;
		}

		// Only the first part is the mime type: e.g. "text/html; charset=UTF-8"
		final String[] split = contentTypeValue.split(";", -1);
		return split[0];
	}

}
