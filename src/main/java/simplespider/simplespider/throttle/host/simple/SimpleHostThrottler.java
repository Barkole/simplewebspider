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

package simplespider.simplespider.throttle.host.simple;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import simplespider.simplespider.throttle.host.HostThrottler;
import simplespider.simplespider.util.SimpleUrl;
import simplespider.simplespider.util.ValidityHelper;

public class SimpleHostThrottler implements HostThrottler {
	private static final Log				LOG												= LogFactory.getLog(SimpleHostThrottler.class);

	private static String					HOST_THROTTLER_HOSTS_AT_ONCE					= "throttler.host.hosts-at-once";
	private static int						HOST_THROTTLER_HOSTS_AT_ONCE_DEFAULT			= 20;

	private static String					HOST_THROTTLER_HOSTS_MAX_SIZE					= "throttler.host.hosts-max-size";
	private static int						HOST_THROTTLER_HOSTS_MAX_SIZE_DEFAULT			= 1024;

	private static String					HOST_THROTTLER_HOSTS_MAX_AGE_SECONDS			= "throttler.host.hosts-max-age-seconds";
	private static int						HOST_THROTTLER_HOSTS_MAX_AGE_SECONDS_DEFAULT	= 60 * 60;

	private final Configuration				configuration;
	private final Map<String, HostCounter>	hosts;
	/**
	 * Oldest one (with lowest {@link Date}/ {@link Date#getTime()} value) is the first one.
	 */
	private final SortedSet<HostCounter>	hostsByTimestampUpdated;
	private final Comparator<SimpleUrl>		simpleUrlFitnessComparator;

	public SimpleHostThrottler(final Configuration configuration) {
		ValidityHelper.checkNotNull("configuration", configuration);

		this.configuration = configuration;
		this.hosts = new HashMap<String, HostCounter>();
		this.hostsByTimestampUpdated = new TreeSet<HostCounter>(new HostComparatorByTimestampUpdated());
		this.simpleUrlFitnessComparator = new SimpleUrlSimpleFitnessComparator(this.hosts);
	}

	public int getUrlsAtOnce() {
		int hostsAtOnce = this.configuration.getInt(HOST_THROTTLER_HOSTS_AT_ONCE, HOST_THROTTLER_HOSTS_AT_ONCE_DEFAULT);
		if (hostsAtOnce < 1) {
			LOG.warn("Configuration " + HOST_THROTTLER_HOSTS_AT_ONCE + " is invalid. Using default value: " + HOST_THROTTLER_HOSTS_AT_ONCE_DEFAULT);
			hostsAtOnce = HOST_THROTTLER_HOSTS_AT_ONCE_DEFAULT;
		}
		return hostsAtOnce;
	}

	private int getHostsMaxSize() {
		int hostsMaxSize = this.configuration.getInt(HOST_THROTTLER_HOSTS_MAX_SIZE, HOST_THROTTLER_HOSTS_MAX_SIZE_DEFAULT);
		if (hostsMaxSize < 1) {
			LOG.warn("Configuration " + HOST_THROTTLER_HOSTS_MAX_SIZE + " is invalid. Using default value: " + HOST_THROTTLER_HOSTS_MAX_SIZE_DEFAULT);
			hostsMaxSize = HOST_THROTTLER_HOSTS_MAX_SIZE_DEFAULT;
		}
		return hostsMaxSize;
	}

	private long getHostsMaxAgeSeconds() {
		return this.configuration.getLong(HOST_THROTTLER_HOSTS_MAX_AGE_SECONDS, HOST_THROTTLER_HOSTS_MAX_AGE_SECONDS_DEFAULT);
	}

	public synchronized String getBestFitting(final List<String> urls) {
		// Required for internal algorithm
		final List<SimpleUrl> simpleUrls = new ArrayList<SimpleUrl>(urls.size());
		// Used to ensure return the exactly same String object
		final Map<SimpleUrl, String> simpleUrlsToUrlString = new HashMap<SimpleUrl, String>(urls.size());

		for (final String url : urls) {
			try {
				final SimpleUrl simpleUrl = new SimpleUrl(url);
				simpleUrls.add(simpleUrl);
				simpleUrlsToUrlString.put(simpleUrl, url);
			} catch (final MalformedURLException e) {
				LOG.warn("Ignore not valid url " + url + ": " + e);
			}
		}

		final SimpleUrl bestFitting = getBestFitting(simpleUrls);

		final String result = simpleUrlsToUrlString.get(bestFitting);
		if (result == null) {
			throw new IllegalStateException("Could not find " + simpleUrls + " in internal map");
		}
		return result;
	}

	public synchronized SimpleUrl getBestFitting(final List<SimpleUrl> urls) {
		/* Cleanup before using */
		removeOutOfDateEntries();

		/* Calculate the best fitting one */
		final SimpleUrl bestFittingUrl = getBestFittingInternal(urls);

		/* Updating hosts */
		updateHosts(bestFittingUrl);

		/* Cleanup after using */
		enforceMaxSize();

		return bestFittingUrl;
	}

	private void updateHosts(final SimpleUrl simpleUrl) {
		final String host = simpleUrl.getHost();
		HostCounter hostCounter = this.hosts.get(host);

		if (hostCounter == null) {
			hostCounter = new HostCounter(host);
			this.hosts.put(host, hostCounter);
		} else {
			this.hostsByTimestampUpdated.remove(hostCounter);
		}

		hostCounter.increaseUsageCounter();
		final boolean added = this.hostsByTimestampUpdated.add(hostCounter);
		if (!added) {
			throw new IllegalStateException("Failed to add hostCounter " + hostCounter);
		}
	}

	private SimpleUrl getBestFittingInternal(final List<SimpleUrl> urls) {
		ValidityHelper.checkNotEmpty("urls", urls);

		SimpleUrl bestFittingUrl = null;
		for (final SimpleUrl simpleUrl : urls) {
			if (bestFittingUrl == null) {
				bestFittingUrl = simpleUrl;
				continue;
			}

			// If new one has more fitness so remember this one
			if (this.simpleUrlFitnessComparator.compare(bestFittingUrl, simpleUrl) < 0) {
				bestFittingUrl = simpleUrl;
			}
		}
		return bestFittingUrl;
	}

	private void enforceMaxSize() {
		final int hostsMaxSize = getHostsMaxSize();
		if (this.hosts.size() != this.hostsByTimestampUpdated.size()) {
			throw new IllegalStateException("Size of hosts and hostsByTimestampUpdated is not equal: " + this.hosts.size() + " vs. "
					+ this.hostsByTimestampUpdated.size());
		}

		while (this.hostsByTimestampUpdated.size() > hostsMaxSize) {
			final HostCounter oldest = this.hostsByTimestampUpdated.first();
			this.hostsByTimestampUpdated.remove(oldest);
			this.hosts.remove(oldest.getHost());
		}

		if (this.hosts.size() != this.hostsByTimestampUpdated.size()) {
			throw new IllegalStateException("After trimming: Size of hosts and hostsByTimestampUpdated is not equal: " + this.hosts.size() + " vs. "
					+ this.hostsByTimestampUpdated.size());
		}
	}

	private void removeOutOfDateEntries() {
		final long hostsMaxAgeSeconds = getHostsMaxAgeSeconds();
		if (hostsMaxAgeSeconds < 1) {
			// No remove by age is requested
			return;
		}

		if (this.hosts.size() != this.hostsByTimestampUpdated.size()) {
			throw new IllegalStateException("Size of hosts and hostsByTimestampUpdated is not equal: " + this.hosts.size() + " vs. "
					+ this.hostsByTimestampUpdated.size());
		}

		final Date timestampMaxAge = new Date(System.currentTimeMillis() - hostsMaxAgeSeconds * 1000);
		final HostCounter referenceObject = new HostCounter(null, timestampMaxAge);
		final SortedSet<HostCounter> outOfDates = this.hostsByTimestampUpdated.headSet(referenceObject);
		// Create a new List to avoid modification on set, while iterate over it
		for (final HostCounter outOfDate : new ArrayList<HostCounter>(outOfDates)) {
			this.hosts.remove(outOfDate.getHost());
			this.hostsByTimestampUpdated.remove(outOfDate);
		}

		if (this.hosts.size() != this.hostsByTimestampUpdated.size()) {
			throw new IllegalStateException("After trimming: Size of hosts and hostsByTimestampUpdated is not equal: " + this.hosts.size() + " vs. "
					+ this.hostsByTimestampUpdated.size());
		}
	}

}
