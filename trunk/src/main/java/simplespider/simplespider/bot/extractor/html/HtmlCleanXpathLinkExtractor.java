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
package simplespider.simplespider.bot.extractor.html;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.htmlcleaner.XPatherException;

import simplespider.simplespider.bot.extractor.LinkExtractor;
import simplespider.simplespider.util.SimpleUrl;
import simplespider.simplespider.util.ValidityHelper;

public class HtmlCleanXpathLinkExtractor implements LinkExtractor {
	private static final Log	LOG			= LogFactory.getLog(HtmlCleanXpathLinkExtractor.class);

	private HtmlCleaner			htmlCleaner	= new HtmlCleaner();

	private TagNode[] getTagByName(final TagNode tagNode, final String tagName) {
		ValidityHelper.checkNotNull("tagNode", tagNode);
		ValidityHelper.checkNotEmpty("tagName", tagName);

		final Object[] frames;
		try {
			frames = tagNode.evaluateXPath("//" + tagName);
		} catch (final XPatherException e) {
			throw new IllegalStateException("Failed to build XPath expression \"//frame\"", e);
		}
		final TagNode[] frames2 = Arrays.copyOf(frames, frames.length, TagNode[].class);
		return frames2;
	}

	@SuppressWarnings("unchecked")
	private String getAttribute(final TagNode anchor, final String attributeName) {
		ValidityHelper.checkNotNull("anchor", anchor);
		ValidityHelper.checkNotEmpty("attributeName", attributeName);

		final Map<String, String> attributes = anchor.getAttributes();
		final Set<Entry<String, String>> entrySet = attributes.entrySet();
		for (final Entry<String, String> entry : entrySet) {
			final String key = entry.getKey();
			if (attributeName.equalsIgnoreCase(key)) {
				return entry.getValue();
			}
		}

		return null;
	}

	public List<String> getUrls(final InputStream body, final String baseUrl) throws IOException {
		ValidityHelper.checkNotNull("body", body);

		final TagNode tagNode;
		try {
			tagNode = this.htmlCleaner.clean(body);
		} catch (final IOException e) {
			throw new IOException("Failed to load url \"" + baseUrl + "\"", e);
		} catch (final RuntimeException e) {
			throw new RuntimeException("Failed to load url \"" + baseUrl + "\"", e);
		}

		final SimpleUrl url = new SimpleUrl(baseUrl);

		final ArrayList<String> links = new ArrayList<String>();
		// html
		links.addAll(getLinksBySimpleTagAttribute(url, tagNode, "a", "href"));
		links.addAll(getLinksBySimpleTagAttribute(url, tagNode, "frame", "src"));
		links.addAll(getLinksBySimpleTagAttribute(url, tagNode, "iframe", "src"));
		links.addAll(getLinksBySimpleTagAttribute(url, tagNode, "ilayer", "src"));

		// rss & rdf
		links.addAll(getLinksBySimpleTagAttribute(url, tagNode, "atom:link", "href"));
		links.addAll(getLinksBySimpleTagAttribute(url, tagNode, "category", "domain"));
		links.addAll(getLinksBySimpleTagAttribute(url, tagNode, "item", "rdf:about"));
		links.addAll(getLinksBySimpleTagAttribute(url, tagNode, "rdf:li", "rdf:resource"));
		links.addAll(getLinksBySimpleTagAttribute(url, tagNode, "source", "url"));
		links.addAll(getLinksBySimpleTagAttribute(url, tagNode, "textinput", "rdf:resource"));
		links.addAll(getLinksBySimpleTagContent(url, tagNode, "comments"));
		links.addAll(getLinksBySimpleTagContent(url, tagNode, "docs"));
		links.addAll(getLinksBySimpleTagContent(url, tagNode, "link"));
		links.addAll(getLinksBySimpleTagContent(url, tagNode, "url"));
		links.addAll(getLinksBySimpleTagContent(url, tagNode, "wfw:commentRss"));

		// atom
		links.addAll(getLinksBySimpleTagAttribute(url, tagNode, "link", "href"));
		links.addAll(getLinksBySimpleTagAttribute(url, tagNode, "collection", "href"));
		links.addAll(getLinksBySimpleTagAttribute(url, tagNode, "member", "href"));
		links.addAll(getLinksBySimpleTagAttribute(url, tagNode, "member", "hrefreadonly"));
		links.addAll(getLinksBySimpleTagContent(url, tagNode, "id"));

		// opml
		links.addAll(getLinksBySimpleTagAttribute(url, tagNode, "outline", "htmlUrl"));
		links.addAll(getLinksBySimpleTagAttribute(url, tagNode, "outline", "url"));
		links.addAll(getLinksBySimpleTagAttribute(url, tagNode, "outline", "xmlUrl"));
		links.addAll(getLinksBySimpleTagContent(url, tagNode, "docs"));
		links.addAll(getLinksBySimpleTagContent(url, tagNode, "ownerId"));

		return links;
	}

	private List<String> getLinksBySimpleTagAttribute(final SimpleUrl baseUrl, final TagNode tagNode, final String tagName, final String attributeName) {
		ValidityHelper.checkNotNull("baseUrl", baseUrl);
		ValidityHelper.checkNotNull("tagNode", tagNode);
		ValidityHelper.checkNotEmpty("tagName", tagName);
		ValidityHelper.checkNotEmpty("attributeName", attributeName);

		final List<String> links = new ArrayList<String>();

		final TagNode[] frames = getTagByName(tagNode, tagName);
		for (final TagNode frame : frames) {
			final String reference = getAttribute(frame, attributeName);
			// Ignoring tags with empty attributes
			if (!ValidityHelper.isEmpty(reference)) {
				try {
					final SimpleUrl newUrl = SimpleUrl.newURL(baseUrl, reference);
					if (newUrl == null) {
						LOG.debug("Ignoring reference \"" + reference + "\" based on URL \"" + baseUrl + "\"");
					} else {
						final String normalformedUrl = newUrl.toNormalform(false, true);
						links.add(normalformedUrl);
					}
				} catch (final Exception e) {
					LOG.debug("Ignoring reference \"" + reference + "\" based on URL \"" + baseUrl + "\"", e);
				}
			}
		}

		return links;
	}

	// FIXME Most the same like getLinksBySimpleTagAttribute -> Refactoring
	private List<String> getLinksBySimpleTagContent(final SimpleUrl baseUrl, final TagNode tagNode, final String tagName) {
		ValidityHelper.checkNotNull("baseUrl", baseUrl);
		ValidityHelper.checkNotNull("tagNode", tagNode);
		ValidityHelper.checkNotEmpty("tagName", tagName);

		final List<String> links = new ArrayList<String>();

		final TagNode[] frames = getTagByName(tagNode, tagName);
		for (final TagNode frame : frames) {
			final StringBuffer reference = frame.getText();
			// Ignoring tags with empty attributes
			if (!ValidityHelper.isEmpty(reference)) {
				try {
					final SimpleUrl newUrl = SimpleUrl.newURL(baseUrl, reference.toString());
					if (newUrl == null) {
						LOG.debug("Ignoring reference \"" + reference + "\" based on URL \"" + baseUrl + "\"");
					} else {
						final String normalformedUrl = newUrl.toNormalform(false, true);
						links.add(normalformedUrl);
					}
				} catch (final Exception e) {
					LOG.debug("Ignoring reference \"" + reference + "\" based on URL \"" + baseUrl + "\"", e);
				}
			}
		}

		return links;
	}

	/**
	 * Only for JUnit. And prepare for Dependency Injection
	 * 
	 * @param htmlCleaner
	 *            must not be <code>null</code>
	 * @throws NullPointerException
	 *             if <code>htmlCleaner</code> is <code>null</code>
	 */
	public void setHtmlCleaner(final HtmlCleaner htmlCleaner) {
		ValidityHelper.checkNotNull("htmlCleaner", htmlCleaner);
		this.htmlCleaner = htmlCleaner;
	}
}
