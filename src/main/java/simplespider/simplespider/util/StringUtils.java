package simplespider.simplespider.util;

public final class StringUtils {
	private StringUtils() {
		// Only static helpers
	}

	public static CharSequence clipping(final CharSequence value, final int maxLength) {
		if (ValidityHelper.isEmpty(value) // 
				|| (value.length() <= maxLength)) {
			return value;
		}

		if (maxLength <= 3) {
			return "...".substring(0, maxLength);
		}

		return value.subSequence(0, maxLength - 3) + "...";
	}
}
