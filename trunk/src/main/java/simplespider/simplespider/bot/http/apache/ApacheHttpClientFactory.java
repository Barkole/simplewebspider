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

import org.apache.commons.configuration.Configuration;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.ProxyHost;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import simplespider.simplespider.bot.http.HttpClient;
import simplespider.simplespider.bot.http.HttpClientFactory;
import simplespider.simplespider.bot.http.apache.ssl.TrustAllSSLProtocolSocketFactory;
import simplespider.simplespider.util.ValidityHelper;

public class ApacheHttpClientFactory implements HttpClientFactory {

	private static final Log	LOG												= LogFactory.getLog(ApacheHttpClientFactory.class);

	private static final String	HTTP_CLIENT_USER_AGENT_DEFAULT					= "Mozilla/4.0 (compatible; MSIE 7.0b; Windows NT 6.0)";
	private static final String	HTTP_CLIENT_USER_AGENT							= "http.client.user-agent";

	private static final String	HTTP_CLIENT_SOCKET_TIMEOUT_SECONDS				= "http.client.socket.timeout-seconds";
	private static final int	HTTP_CLIENT_SOCKET_TIMEOUT_SECONDS_DEFAULT		= 30;

	private static final String	HTTP_CLIENT_CONNECTION_TIMEOUT_SECONDS			= "http.client.connection.timeout-seconds";
	private static final int	HTTP_CLIENT_CONNECTION_TIMEOUT_SECONDS_DEFAULT	= 30;

	private final ProxyHost		proxyHost;
	private final Configuration	configuration;

	public ApacheHttpClientFactory(final Configuration configuration) {
		this.configuration = configuration;
		this.proxyHost = null;
		setupSsl();
	}

	public ApacheHttpClientFactory(final Configuration configuration, final String proxyServer, final int proxyPort) {
		ValidityHelper.checkNotEmpty("proxyServer", proxyServer);

		this.configuration = configuration;

		this.proxyHost = new ProxyHost(proxyServer, proxyPort);
		setupSsl();
	}

	private void setupSsl() {
		Protocol.registerProtocol("https", new Protocol("https", new TrustAllSSLProtocolSocketFactory(), 443));
	}

	/*
	 * (non-Javadoc)
	 * @see simplespider.simplespider_core.http.HttpClientFactory#buildHttpClient()
	 */
	public HttpClient buildHttpClient() {
		int connectionTimeoutSeconds = this.configuration.getInt(HTTP_CLIENT_CONNECTION_TIMEOUT_SECONDS,
				HTTP_CLIENT_CONNECTION_TIMEOUT_SECONDS_DEFAULT);
		if (connectionTimeoutSeconds <= 0) {
			LOG.warn("Configuration " + HTTP_CLIENT_CONNECTION_TIMEOUT_SECONDS + " is invalid. Using default value: "
					+ HTTP_CLIENT_CONNECTION_TIMEOUT_SECONDS_DEFAULT);
			connectionTimeoutSeconds = HTTP_CLIENT_CONNECTION_TIMEOUT_SECONDS_DEFAULT;
		}
		final int socketTimeoutSeconds = this.configuration.getInt(HTTP_CLIENT_SOCKET_TIMEOUT_SECONDS, HTTP_CLIENT_SOCKET_TIMEOUT_SECONDS_DEFAULT);
		if (socketTimeoutSeconds <= 0) {
			LOG.warn("Configuration " + HTTP_CLIENT_SOCKET_TIMEOUT_SECONDS + " is invalid. Using default value: "
					+ HTTP_CLIENT_SOCKET_TIMEOUT_SECONDS_DEFAULT);
			connectionTimeoutSeconds = HTTP_CLIENT_SOCKET_TIMEOUT_SECONDS_DEFAULT;
		}
		final String userAgent = this.configuration.getString(HTTP_CLIENT_USER_AGENT, HTTP_CLIENT_USER_AGENT_DEFAULT);

		final org.apache.commons.httpclient.HttpClient httpClient = new org.apache.commons.httpclient.HttpClient();

		if (this.proxyHost != null) {
			final HostConfiguration hostConfiguration = httpClient.getHostConfiguration();
			hostConfiguration.setProxyHost(this.proxyHost);
		}

		final HttpConnectionManager httpConnectionManager = httpClient.getHttpConnectionManager();
		final HttpConnectionManagerParams httpConnectionManagerParams = httpConnectionManager.getParams();
		httpConnectionManagerParams.setConnectionTimeout(connectionTimeoutSeconds * 1000);

		// Get initial state object
		final HttpState initialState = new HttpState();
		httpClient.setState(initialState);

		final HttpClientParams clientParams = httpClient.getParams();
		// More browser like behavior
		clientParams.setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
		// More browser like behavior
		clientParams.makeLenient();
		// Setting client global socket timeout
		clientParams.setSoTimeout(socketTimeoutSeconds * 1000);
		// Setting user agent
		clientParams.setParameter("http.useragent", userAgent);

		return new ApacheHttpClient(httpClient);
	}
}
