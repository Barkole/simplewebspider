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

import java.util.Date;

import org.junit.Assert;
import org.junit.Test;

public class HostCounterTest {

	@Test
	public void testGetHost() {
		final String host = "Köler$%$2";
		Assert.assertEquals("Host name must not be changing", host, new HostCounter(host).getHost());
	}

	@Test
	public void testGetTimestampUpdated() {
		{
			final Date date = new Date(0L);
			final HostCounter hostCounter = new HostCounter(null, date);
			Assert.assertEquals("Without modification timestamp must not differ", date, hostCounter.getTimestampUpdated());
		}
		{
			final Date date = new Date();
			final HostCounter hostCounter = new HostCounter(null, date);
			Assert.assertEquals("Without modification timestamp must not differ", date, hostCounter.getTimestampUpdated());
		}
		{
			final Date date = new Date(0L);
			final HostCounter hostCounter = new HostCounter(null, date);
			hostCounter.increaseUsageCounter();
			Assert.assertNotSame("With modification timestamp must differ", date, hostCounter.getTimestampUpdated());
		}
	}

	@Test
	public void testGetTimestampUpdatedLong() {
		{
			final long dateLong = 0L;
			final HostCounter hostCounter = new HostCounter(null, new Date(dateLong));
			Assert.assertEquals("Without modification timestamp must not differ", dateLong, hostCounter.getTimestampUpdatedLong());
			Assert.assertEquals("getTimestampUpdated and getTimestampUpdatedLong must be equalse", dateLong, hostCounter.getTimestampUpdated()
					.getTime());
		}
		{
			final Date date = new Date();
			final long dateLong = date.getTime();
			final HostCounter hostCounter = new HostCounter(null, date);
			Assert.assertEquals("Without modification timestamp must not differ", dateLong, hostCounter.getTimestampUpdatedLong());
			Assert.assertEquals("getTimestampUpdated and getTimestampUpdatedLong must be equalse", dateLong, hostCounter.getTimestampUpdated()
					.getTime());
		}
		{
			final long dateLong = 0L;
			final Date date = new Date(dateLong);
			final HostCounter hostCounter = new HostCounter(null, date);
			hostCounter.increaseUsageCounter();
			Assert.assertFalse("With modification timestamp must differ", dateLong == hostCounter.getTimestampUpdatedLong());
			Assert.assertEquals("getTimestampUpdated and getTimestampUpdatedLong must be equalse", hostCounter.getTimestampUpdatedLong(), hostCounter
					.getTimestampUpdated().getTime());
		}
	}

	@Test
	public void testIncreaseUsageCounter() {
		final HostCounter hostCounter = new HostCounter("Someone");
		Assert.assertEquals("Usage counter init value", 0, hostCounter.getUsageCounter());
		final long counter = hostCounter.increaseUsageCounter();
		Assert.assertEquals("Usage counter return after increasing ", 1, counter);
		Assert.assertEquals("Usage counter after increasing ", 1, hostCounter.getUsageCounter());
	}

	@Test
	public void testEqualsObject() {
		{
			final HostCounter hostCounter1 = new HostCounter("Someone");
			Assert.assertTrue("Same object has to be equals", hostCounter1.equals(hostCounter1));
		}
		{
			final HostCounter hostCounter1 = new HostCounter("Someone");
			final HostCounter hostCounter2 = new HostCounter("Someone");
			Assert.assertTrue("Same hostname has to be equals", hostCounter1.equals(hostCounter2));
			Assert.assertTrue("Same hostname has to be equals", hostCounter2.equals(hostCounter1));
		}
		{
			final HostCounter hostCounter1 = new HostCounter("Someone");
			final HostCounter hostCounter2 = new HostCounter("Someone");
			hostCounter2.increaseUsageCounter();
			Assert.assertTrue("Same hostname has to be equals and has to ignore update and counter", hostCounter1.equals(hostCounter2));
			Assert.assertTrue("Same hostname has to be equals and has to ignore update and counter", hostCounter2.equals(hostCounter1));
		}
		{
			final HostCounter hostCounter1 = new HostCounter("Someone");
			final HostCounter hostCounter2 = new HostCounter("Someone2");
			Assert.assertFalse("Different hostname has not to be equals", hostCounter1.equals(hostCounter2));
			Assert.assertFalse("Different hostname has not to be equals", hostCounter2.equals(hostCounter1));
		}
	}

}
