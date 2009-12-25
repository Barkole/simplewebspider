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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

import simplespider.simplespider.util.SimpleUrl;

public class SimpleUrlSimpleFitnessComparatorTest {

	@Test
	public void testCompareWithEmptyInputMap() throws Exception {
		final HashMap<String, HostCounter> domains = new HashMap<String, HostCounter>();
		final SimpleUrlSimpleFitnessComparator testling = new SimpleUrlSimpleFitnessComparator(domains);

		{
			final SimpleUrl simpleUrl1 = new SimpleUrl("http://some/");
			final SimpleUrl simpleUrl2 = new SimpleUrl("http://other/");
			Assert.assertEquals("Compare is wrong", 0, testling.compare(simpleUrl1, simpleUrl2));
		}
	}

	@Test
	public void testCompareWithFilledInputMap() throws Exception {
		final HashMap<String, HostCounter> domains = new HashMap<String, HostCounter>();

		final String neverUsed = "http://neverused/";
		final String usedTimestampZero = "http://used/";
		final String neverUsed2TimestampZero = "http://neverused2/";
		final String neverUsed3TimestampZero = "http://neverused3/";
		final String notAvailable = "http://notavailable/";
		final String notAvailable2 = "http://notavailable2/";

		{
			final String host = new SimpleUrl(neverUsed).getHost();
			domains.put(host, new HostCounter(host));
		}
		{
			final String host = new SimpleUrl(usedTimestampZero).getHost();
			final HostCounter hostCounter = new HostCounter(host);
			final Field usageCounterField = HostCounter.class.getDeclaredField("usageCounter");
			usageCounterField.setAccessible(true);
			usageCounterField.set(hostCounter, Long.valueOf(19L));
			domains.put(host, hostCounter);
		}

		{
			final String host = new SimpleUrl(neverUsed2TimestampZero).getHost();
			final HostCounter hostCounter = new HostCounter(host, new Date(0L));
			domains.put(host, hostCounter);
		}
		{
			final String host = new SimpleUrl(neverUsed3TimestampZero).getHost();
			final HostCounter hostCounter = new HostCounter(host, new Date(0L));
			domains.put(host, hostCounter);
		}

		final SimpleUrlSimpleFitnessComparator testling = new SimpleUrlSimpleFitnessComparator(domains);

		{
			final SimpleUrl simpleUrl1 = new SimpleUrl(neverUsed);
			final SimpleUrl simpleUrl2 = new SimpleUrl(neverUsed);
			Assert.assertEquals("Compare is wrong", 0, testling.compare(simpleUrl1, simpleUrl2));
		}
		{
			final SimpleUrl simpleUrl1 = new SimpleUrl(usedTimestampZero);
			final SimpleUrl simpleUrl2 = new SimpleUrl(usedTimestampZero);
			Assert.assertEquals("Compare is wrong", 0, testling.compare(simpleUrl1, simpleUrl2));
		}
		{
			final SimpleUrl simpleUrl1 = new SimpleUrl(neverUsed2TimestampZero);
			final SimpleUrl simpleUrl2 = new SimpleUrl(neverUsed2TimestampZero);
			Assert.assertEquals("Compare is wrong", 0, testling.compare(simpleUrl1, simpleUrl2));
		}
		{
			final SimpleUrl simpleUrl1 = new SimpleUrl(neverUsed3TimestampZero);
			final SimpleUrl simpleUrl2 = new SimpleUrl(neverUsed3TimestampZero);
			Assert.assertEquals("Compare is wrong", 0, testling.compare(simpleUrl1, simpleUrl2));
		}
		{
			final SimpleUrl simpleUrl1 = new SimpleUrl(notAvailable);
			final SimpleUrl simpleUrl2 = new SimpleUrl(notAvailable);
			Assert.assertEquals("Compare is wrong", 0, testling.compare(simpleUrl1, simpleUrl2));
		}
		{
			final SimpleUrl simpleUrl1 = new SimpleUrl(notAvailable2);
			final SimpleUrl simpleUrl2 = new SimpleUrl(notAvailable2);
			Assert.assertEquals("Compare is wrong", 0, testling.compare(simpleUrl1, simpleUrl2));
		}
		{
			final SimpleUrl simpleUrl1 = new SimpleUrl(neverUsed);
			final SimpleUrl simpleUrl2 = new SimpleUrl(notAvailable);
			Assert.assertEquals("Compare is wrong", -1, testling.compare(simpleUrl1, simpleUrl2));
		}
		{
			final SimpleUrl simpleUrl1 = new SimpleUrl(notAvailable);
			final SimpleUrl simpleUrl2 = new SimpleUrl(neverUsed);
			Assert.assertEquals("Compare is wrong", 1, testling.compare(simpleUrl1, simpleUrl2));
		}
		{
			final SimpleUrl simpleUrl1 = new SimpleUrl(neverUsed);
			final SimpleUrl simpleUrl2 = new SimpleUrl(neverUsed2TimestampZero);
			Assert.assertEquals("Compare is wrong", -1, testling.compare(simpleUrl1, simpleUrl2));
		}
		{
			final SimpleUrl simpleUrl1 = new SimpleUrl(neverUsed2TimestampZero);
			final SimpleUrl simpleUrl2 = new SimpleUrl(neverUsed);
			Assert.assertEquals("Compare is wrong", 1, testling.compare(simpleUrl1, simpleUrl2));
		}
		{
			final SimpleUrl simpleUrl1 = new SimpleUrl(neverUsed2TimestampZero);
			final SimpleUrl simpleUrl2 = new SimpleUrl(notAvailable);
			Assert.assertEquals("Compare is wrong", -1, testling.compare(simpleUrl1, simpleUrl2));
		}
		{
			final SimpleUrl simpleUrl1 = new SimpleUrl(notAvailable);
			final SimpleUrl simpleUrl2 = new SimpleUrl(neverUsed2TimestampZero);
			Assert.assertEquals("Compare is wrong", 1, testling.compare(simpleUrl1, simpleUrl2));
		}
		{
			final SimpleUrl simpleUrl1 = new SimpleUrl(neverUsed2TimestampZero);
			final SimpleUrl simpleUrl2 = new SimpleUrl(usedTimestampZero);
			Assert.assertEquals("Compare is wrong", 1, testling.compare(simpleUrl1, simpleUrl2));
		}
		{
			final SimpleUrl simpleUrl1 = new SimpleUrl(usedTimestampZero);
			final SimpleUrl simpleUrl2 = new SimpleUrl(neverUsed2TimestampZero);
			Assert.assertEquals("Compare is wrong", -1, testling.compare(simpleUrl1, simpleUrl2));
		}
		{
			final SimpleUrl simpleUrl1 = new SimpleUrl(notAvailable);
			final SimpleUrl simpleUrl2 = new SimpleUrl(usedTimestampZero);
			Assert.assertEquals("Compare is wrong", 1, testling.compare(simpleUrl1, simpleUrl2));
		}
		{
			final SimpleUrl simpleUrl1 = new SimpleUrl(usedTimestampZero);
			final SimpleUrl simpleUrl2 = new SimpleUrl(notAvailable);
			Assert.assertEquals("Compare is wrong", -1, testling.compare(simpleUrl1, simpleUrl2));
		}
	}

	@Test
	public void testCompareWithFilledInputMapUsedToSortList() throws Exception {
		final HashMap<String, HostCounter> domains = new HashMap<String, HostCounter>();

		final String neverUsed = "http://neverused/";
		final String usedTimestampZero = "http://used/";
		final String neverUsed2TimestampZero = "http://neverused2/";
		final String neverUsed3TimestampZero = "http://neverused3/";
		final String notAvailable = "http://notavailable/";
		final String notAvailable2 = "http://notavailable2/";

		{
			final String host = new SimpleUrl(neverUsed).getHost();
			domains.put(host, new HostCounter(host));
		}
		{
			final String host = new SimpleUrl(usedTimestampZero).getHost();
			final HostCounter hostCounter = new HostCounter(host);
			final Field usageCounterField = HostCounter.class.getDeclaredField("usageCounter");
			usageCounterField.setAccessible(true);
			usageCounterField.set(hostCounter, Long.valueOf(19L));
			domains.put(host, hostCounter);
		}

		{
			final String host = new SimpleUrl(neverUsed2TimestampZero).getHost();
			final HostCounter hostCounter = new HostCounter(host, new Date(0L));
			domains.put(host, hostCounter);
		}
		{
			final String host = new SimpleUrl(neverUsed3TimestampZero).getHost();
			final HostCounter hostCounter = new HostCounter(host, new Date(0L));
			domains.put(host, hostCounter);
		}

		final SimpleUrlSimpleFitnessComparator testling = new SimpleUrlSimpleFitnessComparator(domains);

		final List<SimpleUrl> simpleUrlList = new ArrayList<SimpleUrl>();
		simpleUrlList.add(new SimpleUrl(neverUsed3TimestampZero));
		simpleUrlList.add(new SimpleUrl(neverUsed2TimestampZero));
		simpleUrlList.add(new SimpleUrl(notAvailable));
		simpleUrlList.add(new SimpleUrl(usedTimestampZero));
		simpleUrlList.add(new SimpleUrl(usedTimestampZero));
		simpleUrlList.add(new SimpleUrl(neverUsed2TimestampZero));
		simpleUrlList.add(new SimpleUrl(notAvailable));
		simpleUrlList.add(new SimpleUrl(neverUsed3TimestampZero));
		simpleUrlList.add(new SimpleUrl(notAvailable2));
		simpleUrlList.add(new SimpleUrl(neverUsed3TimestampZero));
		simpleUrlList.add(new SimpleUrl(neverUsed2TimestampZero));
		final SimpleUrl[] simpleUrls = simpleUrlList.toArray(new SimpleUrl[simpleUrlList.size()]);

		final List<SimpleUrl> simpleUrlListExpected = new ArrayList<SimpleUrl>();
		simpleUrlListExpected.add(new SimpleUrl(usedTimestampZero));
		simpleUrlListExpected.add(new SimpleUrl(usedTimestampZero));
		simpleUrlListExpected.add(new SimpleUrl(neverUsed3TimestampZero));
		simpleUrlListExpected.add(new SimpleUrl(neverUsed2TimestampZero));
		simpleUrlListExpected.add(new SimpleUrl(neverUsed2TimestampZero));
		simpleUrlListExpected.add(new SimpleUrl(neverUsed3TimestampZero));
		simpleUrlListExpected.add(new SimpleUrl(neverUsed3TimestampZero));
		simpleUrlListExpected.add(new SimpleUrl(neverUsed2TimestampZero));
		simpleUrlListExpected.add(new SimpleUrl(notAvailable));
		simpleUrlListExpected.add(new SimpleUrl(notAvailable));
		simpleUrlListExpected.add(new SimpleUrl(notAvailable2));
		final SimpleUrl[] simpleUrlsExpected = simpleUrlListExpected.toArray(new SimpleUrl[simpleUrlListExpected.size()]);

		Arrays.sort(simpleUrls, testling);

		for (int i = 0; i < simpleUrls.length; i++) {
			Assert.assertEquals("Ordering is incorrect at index " + i, simpleUrlsExpected[i], simpleUrls[i]);
		}
	}
}
