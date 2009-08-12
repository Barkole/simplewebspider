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
package simplespider.simplespider.dao.db4o;

class Link {
	private String				url			= null;

	public final static String	RANDOMIZER	= "randomizer";
	private long				randomizer	= 0;

	public Link() {
		// Default
	}

	public Link(final String url, final long randomizer) {
		this.url = url;
		this.randomizer = randomizer;
	}

	public String getUrl() {
		return this.url;
	}

	public void setUrl(final String url) {
		this.url = url;
	}

	@Override
	public String toString() {
		return "[" + this.url + ";" + this.randomizer + "]";
	}

	public void setRandomizer(final long randomizer) {
		this.randomizer = randomizer;
	}

	public long getRandomizer() {
		return this.randomizer;
	}
}
