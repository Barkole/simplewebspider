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
package simplespider.simplespider.bot;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.httpclient.CircularRedirectException;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import simplespider.simplespider.bot.extractor.LinkExtractor;
import simplespider.simplespider.bot.http.HttpClient;
import simplespider.simplespider.bot.http.HttpClientFactory;
import simplespider.simplespider.dao.DbHelper;
import simplespider.simplespider.dao.DbHelperFactory;
import simplespider.simplespider.dao.LinkDao;
import simplespider.simplespider.enity.Link;
import simplespider.simplespider.util.SimpleUrl;
import simplespider.simplespider.util.ValidityHelper;

public class CrawlerImpl implements Crawler {

	// TODO Sleeping on error should be solved not here
	private static final int		SLEEP_SECONDS_ON_ERROR	= 10;

	private static final Log		LOG						= LogFactory.getLog(CrawlerImpl.class);

	private final DbHelperFactory	dbHelperFactory;
	private final LinkExtractor		linkExtractor;
	private final HttpClientFactory	httpClientFactory;

	public CrawlerImpl(final DbHelperFactory dbHelperFactory, final LinkExtractor linkExtractor, final HttpClientFactory httpClientFactory) {
		this.dbHelperFactory = dbHelperFactory;
		this.linkExtractor = linkExtractor;
		this.httpClientFactory = httpClientFactory;
	}

	private HttpClient getHttpConnection(final String baseUrl) {
		final HttpClient httpClient = this.httpClientFactory.buildHttpClient();

		try {
			httpClient.createConnection(baseUrl);
		} catch (final Exception e) {
			if (e instanceof SocketTimeoutException) {
				LOG.info("Failed to load URL \"" + baseUrl + "\": " + e);
			} else if (e instanceof CircularRedirectException) {
				LOG.info("Failed to load URL \"" + baseUrl + "\": " + e);
			} else {
				LOG.info("Failed to load URL \"" + baseUrl + "\"", e);
			}
			return null;
		}

		final int statusCode = httpClient.getStatusCode();
		if (statusCode < 200 || statusCode >= 300) {
			LOG.info("Failed to load URL \"" + baseUrl + "\":" + httpClient.getStatusLine());
			httpClient.releaseConnection();
			return null;
		}

		return httpClient;
	}

	/*
	 * (non-Javadoc)
	 * @see simplespider.simplespider_core.bot.Crawler#crawl(java.lang.String)
	 */
	public void crawl(final String baseUrl) {
		ValidityHelper.checkNotEmpty("baseUrl", baseUrl);

		try {
			final HttpClient httpClient = getHttpConnection(baseUrl);
			if (httpClient == null) {
				// Error occurs, try it later
				setLinkUndone(baseUrl);
				// Slow down thread
				sleepOnError();
				return;
			}

			final List<String> urls;
			try {
				urls = getLinks(baseUrl, httpClient);
			} finally {
				// clean up the connection resources
				httpClient.releaseConnection();
			}

			if (urls == null) {
				// Error occurs, try it later
				setLinkUndone(baseUrl);
				// Slow down thread
				sleepOnError();
			} else {
				saveLinks(urls);
			}
		} catch (final Exception e) {
			LOG.warn("Failed to crawl URL \"" + baseUrl + "\"", e);
		}
	}

	private void sleepOnError() {
		try {
			TimeUnit.SECONDS.sleep(SLEEP_SECONDS_ON_ERROR);
		} catch (final InterruptedException e) {
			LOG.debug("Sleep was interrupted", e);
		}
	}

	private void saveLinks(final List<String> urls) throws SQLException {
		final DbHelper dbHelper = this.dbHelperFactory.buildDbHelper();
		try {
			final LinkDao linkDao = dbHelper.getLinkDao();

			for (final String url : urls) {
				SimpleUrl simpleUrl;
				try {
					simpleUrl = new SimpleUrl(url);
				} catch (final Exception e) {
					LOG.info("Ignoring malformed URL \"" + url + "\"", e);
					continue;
				}

				final String protocol = simpleUrl.getProtocol();
				if (!isSupportedProtocol(protocol)) {
					LOG.info("Ignoring URL without supported protocol: \"" + url + "\"");
					continue;
				}

				final String cleanedUrl = simpleUrl.toNormalform(false, true);
				if (linkDao.isAvailable(cleanedUrl)) {
					LOG.debug("URL is already available: \"" + url + "\"");
					continue;
				}

				final Link linkEntity = new Link(cleanedUrl);
				linkDao.save(linkEntity);
			}

			dbHelper.commitTransaction();
		} finally {
			dbHelper.close();
		}
	}

	private boolean isSupportedProtocol(final String protocol) {
		ValidityHelper.checkNotEmpty("protocol", protocol);

		return "http".equals(protocol) //
				|| "https".equals(protocol) //
				|| "ftp".equals(protocol);
	}

	private List<String> getLinks(final String baseUrl, final HttpClient httpClient) throws SQLException, MalformedURLException {
		final String realBaseUrl;
		try {
			realBaseUrl = httpClient.getRedirectedUrl();
		} catch (final URIException e) {
			LOG.warn("Failed to get URI after redirection for URL \"" + baseUrl + "\"", e);
			return null;
		}

		final String cleanedBasedUrl = new SimpleUrl(baseUrl).toNormalform(false, true);
		final String cleanedRealBaseUrl = new SimpleUrl(realBaseUrl).toNormalform(false, true);

		if (isRedirectDouble(cleanedBasedUrl, cleanedRealBaseUrl)) {
			// Was redirected to a URL, thats already available, so nothing is to do
			return new ArrayList<String>(0);
		}

		final InputStream bodyAsStream;
		try {
			bodyAsStream = httpClient.getResponseBodyAsStream();
		} catch (final IOException e) {
			LOG.warn("Failed to get body for url \"" + cleanedRealBaseUrl + "\"", e);
			return null;
		}

		if (bodyAsStream == null) {
			LOG.warn("Failed to get body for url \"" + cleanedRealBaseUrl + "\"");
			return null;
		}

		final String mimeType = httpClient.getMimeType();
		// Only supporting HTTP and mime type plain and html
		// If not mime type is defined, so hope it will be plain or html ;-)
		if (ValidityHelper.isEmpty(mimeType) //
				|| "text/plain".equalsIgnoreCase(mimeType) //
				|| "text/html".equalsIgnoreCase(mimeType) //
				|| "text/xml".equalsIgnoreCase(mimeType) //
				|| "text/x-opml".equalsIgnoreCase(mimeType) //
				|| "text/x-opml+xml".equalsIgnoreCase(mimeType) //
				|| "application/atom+xml".equalsIgnoreCase(mimeType) //
				|| "application/atomcoll+xml".equalsIgnoreCase(mimeType) //
				|| "application/atomserv+xml".equalsIgnoreCase(mimeType) //
				|| "application/html+xml".equalsIgnoreCase(mimeType) //
				|| "application/rdf+xml".equalsIgnoreCase(mimeType) //
				|| "application/rss+xml".equalsIgnoreCase(mimeType) //
				|| "application/xml".equalsIgnoreCase(mimeType) //
		) {
			try {
				return this.linkExtractor.getUrls(bodyAsStream, cleanedRealBaseUrl);
			} catch (final IOException e) {
				LOG.warn("Failed to extract links from body for url \"" + cleanedRealBaseUrl + "\"", e);
				return null;
			}
		} else {
			LOG.info("Not supporting mime type \"" + mimeType + "\": Ignoring URL \"" + baseUrl + "\"");
			return new ArrayList<String>(0);
		}
	}

	private boolean isRedirectDouble(final String cleanedBasedUrl, final String cleanedRealBaseUrl) {

		final DbHelper dbHelper;
		try {
			dbHelper = this.dbHelperFactory.buildDbHelper();
		} catch (final SQLException e) {
			throw new RuntimeException("Failed to get database helper", e);
		}

		try {
			final LinkDao linkDao = dbHelper.getLinkDao();

			if (!cleanedBasedUrl.equals(cleanedRealBaseUrl)) {
				if (linkDao.isAvailable(cleanedRealBaseUrl)) {
					return true;
				}

				final Link newLink = new Link();
				newLink.setUrl(cleanedRealBaseUrl);
				newLink.setDone(true);

				linkDao.save(newLink);
				try {
					dbHelper.commitTransaction();
				} catch (final SQLException e) {
					throw new RuntimeException("Failed to commit transaction");
				}
			}
		} finally {
			try {
				dbHelper.close();
			} catch (final SQLException e) {
				LOG.warn("Failed to close database connection", e);
			}
		}

		return false;
	}

	private void setLinkUndone(final String baseUrl) throws SQLException {
		final DbHelper dbHelper = this.dbHelperFactory.buildDbHelper();
		try {
			final LinkDao linkDao = dbHelper.getLinkDao();
			final Link link = linkDao.getByUrl(baseUrl);

			if (link == null) {
				throw new SQLException("Link with following url not available: \"" + baseUrl + "\"");
			}

			link.setDone(false);
			link.setErrors(link.getErrors() + 1);
			linkDao.save(link);
			dbHelper.commitTransaction();
		} finally {
			dbHelper.close();
		}
	}

}
