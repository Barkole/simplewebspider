package simplespider.simplespider.bot;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public interface LinkExtractor {

	public abstract List<String> getUrls(InputStream body, String baseUrl) throws IOException;

}