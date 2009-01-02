package simplespider.simplespider.bot;

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

import simplespider.simplespider.util.SimpleUrl;
import simplespider.simplespider.util.ValidityHelper;

public class LinkExtractorImpl implements LinkExtractor {
	private static final Log	LOG			= LogFactory.getLog(LinkExtractorImpl.class);

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
						links.add(newUrl.toString());
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
						links.add(newUrl.toString());
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
