package simplespider.simplespider.bot.http.apache;

import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.ProxyHost;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.httpclient.protocol.Protocol;

import simplespider.simplespider.bot.http.HttpClient;
import simplespider.simplespider.bot.http.HttpClientFactory;
import simplespider.simplespider.bot.http.apache.ssl.TrustAllSSLProtocolSocketFactory;
import simplespider.simplespider.util.ValidityHelper;

public class ApacheHttpClientFactory implements HttpClientFactory {

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
		final HttpConnectionManagerParams params = httpConnectionManager.getParams();
		params.setConnectionTimeout(CONNECTION_TIMEOUT_MILLISECONDS);
		params.setSoTimeout(CONNECTION_TIMEOUT_MILLISECONDS);

		// Get initial state object
		final HttpState initialState = new HttpState();
		httpClient.setState(initialState);

		// RFC 2101 cookie management spec is used per default
		// to parse, validate, format & match cookies
		httpClient.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
		// A different cookie management spec can be selected
		// when desired

		return new ApacheHttpClient(httpClient);
	}

}
