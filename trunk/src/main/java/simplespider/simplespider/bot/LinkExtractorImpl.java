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

import simplespider.simplespider.util.UrlUtil;
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

		final ArrayList<String> links = new ArrayList<String>();
		links.addAll(getLinksBySimpleTag(baseUrl, tagNode, "a", "href"));
		links.addAll(getLinksBySimpleTag(baseUrl, tagNode, "frame", "src"));
		links.addAll(getLinksBySimpleTag(baseUrl, tagNode, "iframe", "src"));
		links.addAll(getLinksBySimpleTag(baseUrl, tagNode, "ilayer", "src"));
		return links;
	}

	private List<String> getLinksBySimpleTag(final String baseUrl, final TagNode tagNode, final String tagName, final String attributeName) {
		ValidityHelper.checkNotEmpty("baseUrl", baseUrl);
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
					final String newUrl = UrlUtil.concatUrls(baseUrl, reference);
					if (ValidityHelper.isEmpty(newUrl)) {
						LOG.debug("Ignoring reference \"" + reference + "\" based on URL \"" + baseUrl + "\"");
					} else {
						links.add(newUrl);
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
