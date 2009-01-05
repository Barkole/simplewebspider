package simplespider.simplespider.bot.http.apache.ssl;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import org.apache.commons.httpclient.ConnectTimeoutException;
import org.apache.commons.httpclient.HttpClientError;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;
import org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class EasySSLProtocolSocketFactory implements ProtocolSocketFactory {

	/** Log object for this class. */
	private static final Log	LOG			= LogFactory.getLog(EasySSLProtocolSocketFactory.class);

	private SSLContext			sslcontext	= null;

	/**
	 * Constructor for EasySSLProtocolSocketFactory.
	 */
	public EasySSLProtocolSocketFactory() {
		super();
	}

	private static SSLContext createEasySSLContext() {
		try {
			final SSLContext context = SSLContext.getInstance("SSL");
			context.init(null, new TrustManager[] { new EasyX509TrustManager(null) }, null);
			return context;
		} catch (final Exception e) {
			LOG.error(e.getMessage(), e);
			throw new HttpClientError(e.toString());
		}
	}

	private SSLContext getSSLContext() {
		if (this.sslcontext == null) {
			this.sslcontext = createEasySSLContext();
		}
		return this.sslcontext;
	}

	/**
	 * @see SecureProtocolSocketFactory#createSocket(java.lang.String,int,java.net.InetAddress,int)
	 */
	public Socket createSocket(final String host, final int port, final InetAddress clientHost, final int clientPort) throws IOException,
			UnknownHostException {

		return getSSLContext().getSocketFactory().createSocket(host, port, clientHost, clientPort);
	}

	/**
	 * Attempts to get a new socket connection to the given host within the given time limit.
	 * <p>
	 * To circumvent the limitations of older JREs that do not support connect timeout a controller thread is executed. The controller thread attempts
	 * to create a new socket within the given limit of time. If socket constructor does not return until the timeout expires, the controller
	 * terminates and throws an {@link ConnectTimeoutException}
	 * </p>
	 * 
	 * @param host
	 *            the host name/IP
	 * @param port
	 *            the port on the host
	 * @param clientHost
	 *            the local host name/IP to bind the socket to
	 * @param clientPort
	 *            the port on the local machine
	 * @param params
	 *            {@link HttpConnectionParams Http connection parameters}
	 * @return Socket a new socket
	 * @throws IOException
	 *             if an I/O error occurs while creating the socket
	 * @throws UnknownHostException
	 *             if the IP address of the host cannot be determined
	 */
	public Socket createSocket(final String host, final int port, final InetAddress localAddress, final int localPort,
			final HttpConnectionParams params) throws IOException, UnknownHostException, ConnectTimeoutException {
		if (params == null) {
			throw new IllegalArgumentException("Parameters may not be null");
		}
		final int timeout = params.getConnectionTimeout();
		final SocketFactory socketfactory = getSSLContext().getSocketFactory();
		if (timeout == 0) {
			return socketfactory.createSocket(host, port, localAddress, localPort);
		} else {
			final Socket socket = socketfactory.createSocket();
			final SocketAddress localaddr = new InetSocketAddress(localAddress, localPort);
			final SocketAddress remoteaddr = new InetSocketAddress(host, port);
			socket.bind(localaddr);
			socket.connect(remoteaddr, timeout);
			return socket;
		}
	}

	/**
	 * @see SecureProtocolSocketFactory#createSocket(java.lang.String,int)
	 */
	public Socket createSocket(final String host, final int port) throws IOException, UnknownHostException {
		return getSSLContext().getSocketFactory().createSocket(host, port);
	}

	/**
	 * @see SecureProtocolSocketFactory#createSocket(java.net.Socket,java.lang.String,int,boolean)
	 */
	public Socket createSocket(final Socket socket, final String host, final int port, final boolean autoClose) throws IOException,
			UnknownHostException {
		return getSSLContext().getSocketFactory().createSocket(socket, host, port, autoClose);
	}

	@Override
	public boolean equals(final Object obj) {
		return ((obj != null) && obj.getClass().equals(EasySSLProtocolSocketFactory.class));
	}

	@Override
	public int hashCode() {
		return EasySSLProtocolSocketFactory.class.hashCode();
	}

}