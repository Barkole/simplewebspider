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
package simplespider.simplespider.util;

import java.util.Collection;

public final class ValidityHelper {

	public static void checkNotEmpty(final String name, final CharSequence value) {
		checkNotEmptyInternal("name", name);
		checkNotEmptyInternal(name, value);
	}

	public static void checkNotEmpty(final String name, final Collection<?> values) {
		checkNotEmptyInternal("name", name);
		checkNotNull(name, values);
		if (isEmpty(values)) {
			throw new IllegalArgumentException(name + " is empty");
		}
	}

	private static void checkNotEmptyInternal(final String name, final CharSequence value) {
		checkNotNullInternal(name, value);
		if (isEmpty(value)) {
			throw new IllegalArgumentException(name + " is empty");
		}
	}

	public static void checkNotNull(final String name, final Object value) {
		checkNotEmptyInternal("name", name);
		if (value == null) {
			throw new NullPointerException(name + " is null");
		}
	}

	private static void checkNotNullInternal(final String name, final Object value) {
		if (value == null) {
			throw new NullPointerException(name + " is null");
		}
	}

	public static boolean isEmpty(final CharSequence value) {
		return value == null || value.length() == 0;
	}

	public static boolean isEmpty(final Collection<?> values) {
		return values == null || values.size() == 0;
	}

	private ValidityHelper() {
		// Only static helpers
	}

}
