package simplespider.simplespider.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SimpleUrlTest {

	@Test
	public void testGetHostMainSecondLevel() throws Exception {
        // Given
        String inputUrl = "http://google.com/";
        String expectedHostMain = "google.com";

        // When
        SimpleUrl actual = new SimpleUrl(inputUrl);

        // Then
        assertEquals(expectedHostMain, actual.getHostMain());
	}

	@Test
	public void testGetHostMainThirdLevel() throws Exception {
        // Given
        String inputUrl = "http://www.google.com/";
        String expectedHostMain = "google.com";

        // When
        SimpleUrl actual = new SimpleUrl(inputUrl);

        // Then
        assertEquals(expectedHostMain, actual.getHostMain());
	}

}