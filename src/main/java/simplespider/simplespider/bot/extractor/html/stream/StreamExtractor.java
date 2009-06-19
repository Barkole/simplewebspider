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
package simplespider.simplespider.bot.extractor.html.stream;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import simplespider.simplespider.bot.extractor.LinkExtractor;
import simplespider.simplespider.util.SimpleUrl;
import simplespider.simplespider.util.StringUtils;
import simplespider.simplespider.util.ValidityHelper;

public class StreamExtractor implements LinkExtractor {
	private static final Log	LOG					= LogFactory.getLog(StreamExtractor.class);

	private static final int	DEFAULT_BUFFER_SIZE	= 16384;

	public List<String> getUrls(final InputStream body, final String baseUrl) throws IOException {
		ValidityHelper.checkNotNull("body", body);

		final TagListenerImpl listener = new TagListenerImpl();
		final HtmlWriter htmlWriter = new HtmlWriter(true, listener);

		parse(body, htmlWriter, baseUrl);

		final List<String> links = getLinks(baseUrl, listener.getLinks());

		return links;
	}

	private List<String> getLinks(final String baseUrl, final List<String> extractedLinks) throws MalformedURLException {
		final SimpleUrl url = new SimpleUrl(baseUrl);
		final List<String> links = new ArrayList<String>(extractedLinks.size());
		for (final String reference : extractedLinks) {
			if (reference.contains("<") || reference.contains(">")) {
				LOG.warn("Ignoring possible invalid reference based on URL \"" + baseUrl + "\":\n" + StringUtils.clipping(reference, 128));
				continue;
			}
			try {
				final SimpleUrl newUrl = SimpleUrl.newURL(url, reference);
				if (newUrl == null) {
					LOG.debug("Ignoring reference \"" + reference + "\" based on URL \"" + baseUrl + "\", because it contains nothing");
					continue;
				}
				final String normalformedUrl = newUrl.toNormalform(false, true);
				links.add(normalformedUrl);
			} catch (final Exception e) {
				LOG.warn("Ignoring reference \"" + reference + "\" based on URL \"" + baseUrl + "\"", e);
			}
		}
		return links;
	}

	private void parse(final InputStream sourceStream, final HtmlWriter target, final String baseUrl) throws IOException {
		final Reader source = new InputStreamReader(sourceStream);
		final char[] buffer = new char[DEFAULT_BUFFER_SIZE];
		long count = 0;

		for (int n = 0; -1 != (n = source.read(buffer));) {
			target.write(buffer, 0, n);
			count += n;

			if (target.binarySuspect()) {
				LOG.info("Skip binary content: \"" + baseUrl + "\"");
				break;
			}
		}
		target.flush();

		if (LOG.isDebugEnabled()) {
			LOG.debug("Loaded url \"" + baseUrl + "\": " + count + " bytes");
		}

		target.close();
	}

}
