package simplespider.simplespider.bot.http;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.StatusLine;
import org.apache.commons.httpclient.URIException;

public interface HttpClient {

	public abstract void createConnection(final String url) throws HttpException, IOException;

	public abstract int getStatusCode();

	public abstract StatusLine getStatusLine();

	public abstract String getStatusText();

	public abstract String getRedirectedUrl() throws URIException;

	public abstract InputStream getResponseBodyAsStream() throws IOException;

	public abstract void releaseConnection();

	public abstract String getMimeType();

}