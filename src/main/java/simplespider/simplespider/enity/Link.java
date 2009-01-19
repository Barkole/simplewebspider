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
package simplespider.simplespider.enity;

public class Link {
	public static final String	GET_NEXT	= "LINK_GET_NEXT";

	private Long				id			= null;
	private String				url			= null;
	private boolean				done		= false;
	private int					errors		= 0;

	public Link() {
		// Default
	}

	public Link(final String url) {
		this.url = url;
	}

	public Long getId() {
		return this.id;
	}

	public String getUrl() {
		return this.url;
	}

	public boolean isDone() {
		return this.done;
	}

	public void setDone(final boolean done) {
		this.done = done;
	}

	public void setId(final Long id) {
		this.id = id;
	}

	public void setUrl(final String url) {
		this.url = url;
	}

	@Override
	public String toString() {
		return "Link [id:" + this.id + ",url:\"" + this.url + "\",done:" + this.done + "]";
	}

	public int getErrors() {
		return this.errors;
	}

	public void setErrors(final int errors) {
		this.errors = errors;
	}
}
