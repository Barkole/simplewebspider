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

import org.apache.commons.configuration.Configuration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.HttpVersion;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.params.ConnPerRouteBean;
import org.apache.http.conn.params.ConnRouteParams;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HttpContext;

import simplespider.simplespider.bot.http.HttpClient;
import simplespider.simplespider.bot.http.HttpClientFactory;
import simplespider.simplespider.util.ValidityHelper;

public class ApacheHttpClientFactory implements HttpClientFactory {

	private static final Log		LOG												= LogFactory.getLog(ApacheHttpClientFactory.class);

	private static final String		HTTP_CLIENT_USER_AGENT_DEFAULT					= "Mozilla/4.0 (compatible; MSIE 7.0b; Windows NT 6.0)";
	private static final String		HTTP_CLIENT_USER_AGENT							= "http.client.user-agent";

	private static final String		HTTP_CLIENT_SOCKET_TIMEOUT_SECONDS				= "http.client.socket.timeout-seconds";
	private static final int		HTTP_CLIENT_SOCKET_TIMEOUT_SECONDS_DEFAULT		= 30;

	private static final String		HTTP_CLIENT_CONNECTION_TIMEOUT_SECONDS			= "http.client.connection.timeout-seconds";
	private static final int		HTTP_CLIENT_CONNECTION_TIMEOUT_SECONDS_DEFAULT	= 30;

	private static final String		HTTP_CLIENT_MAX_TOTAL_CONNECTIONS				= "http.client.connection.max-total";
	private static final int		HTTP_CLIENT_MAX_TOTAL_CONNECTIONS_DEFAULT		= 4;

	private static final String		HTTP_CLIENT_MAX_CONNECTIONS_PER_ROUTE			= "http.client.connection.per-route";
	private static final int		HTTP_CLIENT_MAX_CONNECTIONS_PER_ROUTE_DEFAULT	= 2;

	private final HttpHost			proxyHost;
	private final Configuration		configuration;

	private ClientConnectionManager	connectionManager;

	public ApacheHttpClientFactory(final Configuration configuration) {
		this.configuration = configuration;
		this.proxyHost = null;
		initConnectionManager();
	}

	public ApacheHttpClientFactory(final Configuration configuration, final String proxyServer, final int proxyPort) {
		ValidityHelper.checkNotEmpty("proxyServer", proxyServer);
		this.configuration = configuration;
		this.proxyHost = new HttpHost(proxyServer, proxyPort, "http");
		initConnectionManager();
	}

	private void initConnectionManager() {
		// general setup
		final SchemeRegistry supportedSchemes = new SchemeRegistry();

		// Register the "http" and "https" protocol schemes, they are
		// required by the default operator to look up socket factories.
		supportedSchemes.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
		supportedSchemes.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443));

		final HttpParams params = buildParameters();

		this.connectionManager = new ThreadSafeClientConnManager(params, supportedSchemes);
	}

	@Override
	public HttpClient buildHttpClient() {

		final HttpParams params = buildParameters();

		final DefaultHttpClient httpClient = new DefaultHttpClient(this.connectionManager, params);

		addGzipSupport(httpClient);

		return new ApacheHttpClient(httpClient);
	}

	private void addGzipSupport(final DefaultHttpClient httpClient) {
		httpClient.addRequestInterceptor(new HttpRequestInterceptor() {

			@Override
			public void process(final HttpRequest request, final HttpContext context) throws HttpException, IOException {
				if (!request.containsHeader("Accept-Encoding")) {
					request.addHeader("Accept-Encoding", "gzip");
				}
			}

		});

		httpClient.addResponseInterceptor(new HttpResponseInterceptor() {

			@Override
			public void process(final HttpResponse response, final HttpContext context) throws HttpException, IOException {
				final HttpEntity entity = response.getEntity();
				if (entity == null) {
					return;
				}

				final Header ceheader = entity.getContentEncoding();
				if (ceheader == null) {
					return;
				}

				for (final HeaderElement codec : ceheader.getElements()) {
					final String name = codec.getName();
					if (name != null && name.equalsIgnoreCase("gzip")) {
						response.setEntity(new GzipDecompressingEntity(entity));
						return;
					}
				}
			}

		});
	}

	private HttpParams buildParameters() {
		final String userAgent = this.configuration.getString(HTTP_CLIENT_USER_AGENT, HTTP_CLIENT_USER_AGENT_DEFAULT);

		int connectionTimeoutSeconds = this.configuration.getInt(HTTP_CLIENT_CONNECTION_TIMEOUT_SECONDS,
				HTTP_CLIENT_CONNECTION_TIMEOUT_SECONDS_DEFAULT);
		if (connectionTimeoutSeconds <= 0) {
			LOG.warn("Configuration " + HTTP_CLIENT_CONNECTION_TIMEOUT_SECONDS + " is invalid. Using default value: "
					+ HTTP_CLIENT_CONNECTION_TIMEOUT_SECONDS_DEFAULT);
			connectionTimeoutSeconds = HTTP_CLIENT_CONNECTION_TIMEOUT_SECONDS_DEFAULT;
		}

		int socketTimeoutSeconds = this.configuration.getInt(HTTP_CLIENT_SOCKET_TIMEOUT_SECONDS, HTTP_CLIENT_SOCKET_TIMEOUT_SECONDS_DEFAULT);
		if (socketTimeoutSeconds <= 0) {
			LOG.warn("Configuration " + HTTP_CLIENT_SOCKET_TIMEOUT_SECONDS + " is invalid. Using default value: "
					+ HTTP_CLIENT_SOCKET_TIMEOUT_SECONDS_DEFAULT);
			socketTimeoutSeconds = HTTP_CLIENT_SOCKET_TIMEOUT_SECONDS_DEFAULT;
		}

		int maxTotalConnections = this.configuration.getInt(HTTP_CLIENT_MAX_TOTAL_CONNECTIONS, HTTP_CLIENT_MAX_TOTAL_CONNECTIONS_DEFAULT);
		if (maxTotalConnections <= 0) {
			LOG.warn("Configuration " + HTTP_CLIENT_MAX_TOTAL_CONNECTIONS + " is invalid. Using default value: "
					+ HTTP_CLIENT_MAX_TOTAL_CONNECTIONS_DEFAULT);
			maxTotalConnections = HTTP_CLIENT_MAX_TOTAL_CONNECTIONS_DEFAULT;
		}

		int maxConnectionsPerRoute = this.configuration.getInt(HTTP_CLIENT_MAX_CONNECTIONS_PER_ROUTE, HTTP_CLIENT_MAX_CONNECTIONS_PER_ROUTE_DEFAULT);
		if (maxConnectionsPerRoute <= 0) {
			LOG.warn("Configuration " + HTTP_CLIENT_MAX_CONNECTIONS_PER_ROUTE + " is invalid. Using default value: "
					+ HTTP_CLIENT_MAX_CONNECTIONS_PER_ROUTE_DEFAULT);
			maxConnectionsPerRoute = HTTP_CLIENT_MAX_CONNECTIONS_PER_ROUTE_DEFAULT;
		}

		// prepare parameters
		final HttpParams params = new BasicHttpParams();
		HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
		HttpProtocolParams.setContentCharset(params, "UTF-8");
		HttpProtocolParams.setUseExpectContinue(params, true);

		ConnManagerParams.setMaxTotalConnections(params, maxTotalConnections);
		final ConnPerRouteBean connPerRoute = new ConnPerRouteBean(maxConnectionsPerRoute);
		ConnManagerParams.setMaxConnectionsPerRoute(params, connPerRoute);

		if (this.proxyHost != null) {
			ConnRouteParams.setDefaultProxy(params, this.proxyHost);
		}

		HttpClientParams.setCookiePolicy(params, CookiePolicy.BROWSER_COMPATIBILITY);
		HttpClientParams.setRedirecting(params, true);

		HttpConnectionParams.setConnectionTimeout(params, connectionTimeoutSeconds * 1000);
		HttpConnectionParams.setSoTimeout(params, socketTimeoutSeconds * 1000);

		HttpProtocolParams.setContentCharset(params, "UTF-8");
		HttpProtocolParams.setHttpElementCharset(params, "UTF-8");
		HttpProtocolParams.setUserAgent(params, userAgent);
		HttpProtocolParams.setVersion(params, new ProtocolVersion("HTTP", 1, 1));

		params.setParameter(CoreProtocolPNames.WAIT_FOR_CONTINUE, Integer.valueOf(connectionTimeoutSeconds));

		params.setParameter(ClientPNames.ALLOW_CIRCULAR_REDIRECTS, Boolean.TRUE);
		params.setParameter(ClientPNames.REJECT_RELATIVE_REDIRECT, Boolean.FALSE);
		return params;
	}
}
