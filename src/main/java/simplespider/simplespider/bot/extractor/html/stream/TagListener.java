// htmlFilterScraper.java 
// ---------------------------
// (C) by Michael Peter Christen; mc@yacy.net
// first published on http://www.anomic.de
// Frankfurt, Germany, 2004
// last major change: 18.02.2004
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

package simplespider.simplespider.bot.extractor.html.stream;

import java.util.Properties;

interface TagListener {

	public boolean isTagWithoutContent(String tag);

	public boolean isTagWithContent(String tag);

	public void scrapeTagWithoutContent(String tagname, Properties tagopts);

	public void scrapeTagWithContent(String tagname, Properties tagopts, char[] text);
}
