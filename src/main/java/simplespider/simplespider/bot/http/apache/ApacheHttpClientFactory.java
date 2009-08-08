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

import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.ProxyHost;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.httpclient.protocol.Protocol;

import simplespider.simplespider.bot.http.HttpClient;
import simplespider.simplespider.bot.http.HttpClientFactory;
import simplespider.simplespider.bot.http.apache.ssl.TrustAllSSLProtocolSocketFactory;
import simplespider.simplespider.util.ValidityHelper;

public class ApacheHttpClientFactory implements HttpClientFactory {

	// TODO Configure this
	private static final String	USER_AGENT						= "Mozilla/4.0 (compatible; MSIE 7.0b; Windows NT 6.0)";
	// TODO Configure this
	private static final int	CONNECTION_TIMEOUT_MILLISECONDS	= 30000;

	private final ProxyHost		proxyHost;

	public ApacheHttpClientFactory() {
		this.proxyHost = null;
		setupSsl();
	}

	public ApacheHttpClientFactory(final String proxyServer, final int proxyPort) {
		ValidityHelper.checkNotEmpty("proxyServer", proxyServer);
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
		final org.apache.commons.httpclient.HttpClient httpClient = new org.apache.commons.httpclient.HttpClient();

		if (this.proxyHost != null) {
			final HostConfiguration hostConfiguration = httpClient.getHostConfiguration();
			hostConfiguration.setProxyHost(this.proxyHost);
		}

		final HttpConnectionManager httpConnectionManager = httpClient.getHttpConnectionManager();
		final HttpConnectionManagerParams httpConnectionManagerParams = httpConnectionManager.getParams();
		httpConnectionManagerParams.setConnectionTimeout(CONNECTION_TIMEOUT_MILLISECONDS);

		// Get initial state object
		final HttpState initialState = new HttpState();
		httpClient.setState(initialState);

		final HttpClientParams clientParams = httpClient.getParams();
		// More browser like behavior
		clientParams.setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
		// More browser like behavior
		clientParams.makeLenient();
		// Setting client global socket timeout
		clientParams.setSoTimeout(CONNECTION_TIMEOUT_MILLISECONDS);
		// Setting user agent
		clientParams.setParameter("http.useragent", USER_AGENT);

		return new ApacheHttpClient(httpClient);
	}
}
