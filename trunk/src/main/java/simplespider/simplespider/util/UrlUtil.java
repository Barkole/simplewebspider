package simplespider.simplespider.util;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public final class UrlUtil {

	public static String cleanUrl(final String url) {
		if (ValidityHelper.isEmpty(url)) {
			return null;
		}

		final String urlLowerCase = url.toLowerCase();
		if (urlLowerCase.startsWith("javascript:") //
				|| urlLowerCase.startsWith("mailto:") //
				|| urlLowerCase.startsWith("about:")) {
			return null;
		}

		String urlOnCleaning = removePageAnchor(url);
		if (ValidityHelper.isEmpty(urlOnCleaning)) {
			return null;
		}

		// Replace all spaces
		urlOnCleaning = urlOnCleaning.replaceAll(" ", "%20");

		return urlOnCleaning;
	}

	public static String concatUrls(final String baseUrl, final String link) {
		ValidityHelper.checkNotEmpty("baseUrl", baseUrl);
		ValidityHelper.checkNotNull("link", link);

		final String cleanedLink = cleanUrl(link);
		if (ValidityHelper.isEmpty(cleanedLink)) {
			return null;
		}

		final String linkLowerCase = cleanedLink.toLowerCase();

		// If link is absolute, so return link only
		if (linkLowerCase.startsWith("http:") //
				|| linkLowerCase.startsWith("https:")) {
			return cleanedLink;
		}

		final URI baseUri;
		try {
			final String cleanUrl = setRootPath(cleanUrl(baseUrl));
			if (ValidityHelper.isEmpty(cleanUrl)) {
				throw new IllegalArgumentException("baseUrl \"" + baseUrl + "\" is not valid");
			}
			baseUri = new URL(cleanUrl).toURI();
		} catch (final MalformedURLException e) {
			throw new IllegalArgumentException("baseUrl \"" + baseUrl + "\" is invalid", e);
		} catch (final URISyntaxException e) {
			throw new IllegalArgumentException("baseUrl \"" + baseUrl + "\" is invalid", e);
		}

		final URI newUri = baseUri.resolve(cleanedLink);
		return newUri.toString();
	}

	public static String removePageAnchor(final String url) {
		ValidityHelper.checkNotNull("url", url);

		for (int i = url.length() - 1; i > 0; i--) {
			if (url.charAt(i) == '#') {
				return url.substring(0, i);
			}
		}
		return url;
	}

	public static String setRootPath(final String url) {
		ValidityHelper.checkNotNull("url", url);

		final URI uri;
		try {
			uri = new URL(url).toURI();
		} catch (final MalformedURLException e) {
			throw new IllegalArgumentException("url \"" + url + "\" is invalid", e);
		} catch (final URISyntaxException e) {
			throw new IllegalArgumentException("url \"" + url + "\" is invalid", e);
		}

		final String path = uri.getPath();
		if (path == null || path.length() == 0) {
			return url + "/";
		}

		return url;
	}

	private UrlUtil() {
		// Only static helpers
	}

}
