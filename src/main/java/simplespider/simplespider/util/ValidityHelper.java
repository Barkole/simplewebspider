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
