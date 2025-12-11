package dan200.computer.core;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;


public class HTTPRequest {
	final Object m_lock = new Object();
	URL m_url;
	Thread m_thread;
	String m_urlString;
	boolean m_complete;
	boolean m_cancelled;
	boolean m_success;
	String m_result;

	public HTTPRequest(String s, final String _postText) throws HTTPRequestException {
		this.m_urlString = s;

		try {
			this.m_url = new URL(s);
			if (!this.m_url.getProtocol().equalsIgnoreCase("http")) {
				throw new HTTPRequestException("Not an HTTP URL");
			}
		} catch (MalformedURLException var4) {
			throw new HTTPRequestException("Invalid URL");
		}

		this.m_cancelled = false;
		this.m_complete = false;
		this.m_success = false;
		this.m_result = null;
		this.m_thread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					HttpURLConnection httpurlconnection = (HttpURLConnection) HTTPRequest.this.m_url.openConnection();
					if (_postText == null) {
						httpurlconnection.setRequestMethod("GET");
					}
					else {
						httpurlconnection.setRequestMethod("POST");
						httpurlconnection.setDoOutput(true);
						OutputStream outputstream = httpurlconnection.getOutputStream();
						OutputStreamWriter outputstreamwriter = new OutputStreamWriter(outputstream);
						BufferedWriter bufferedwriter = new BufferedWriter(outputstreamwriter);
						bufferedwriter.write(_postText, 0, _postText.length());
						bufferedwriter.close();
					}

					InputStream inputstream = httpurlconnection.getInputStream();
					InputStreamReader inputstreamreader = new InputStreamReader(inputstream);
					BufferedReader bufferedreader = new BufferedReader(inputstreamreader);
					StringBuilder stringbuilder = new StringBuilder();

					while (true) {
						synchronized (HTTPRequest.this.m_lock) {
							if (HTTPRequest.this.m_cancelled) {
								break;
							}
						}

						String s1 = bufferedreader.readLine();
						if (s1 == null) {
							break;
						}

						stringbuilder.append(s1);
						stringbuilder.append('\n');
					}

					bufferedreader.close();
					synchronized (HTTPRequest.this.m_lock) {
						if (HTTPRequest.this.m_cancelled) {
							HTTPRequest.this.m_complete = true;
							HTTPRequest.this.m_success = false;
							HTTPRequest.this.m_result = null;
						}
						else {
							HTTPRequest.this.m_complete = true;
							HTTPRequest.this.m_success = true;
							HTTPRequest.this.m_result = stringbuilder.toString();
						}
					}
				} catch (IOException var10) {
					synchronized (HTTPRequest.this.m_lock) {
						HTTPRequest.this.m_complete = true;
						HTTPRequest.this.m_success = false;
						HTTPRequest.this.m_result = null;
					}
				}
			}
		});
		this.m_thread.start();
	}

	String getURL() {
		return this.m_urlString;
	}

	void cancel() {
		synchronized (this.m_lock) {
			this.m_cancelled = true;
		}
	}

	public boolean isComplete() {
		synchronized (this.m_lock) {
			return this.m_complete;
		}
	}

	public boolean wasSuccessful() {
		synchronized (this.m_lock) {
			return this.m_success;
		}
	}

	public BufferedReader getContents() {
		String s = null;
		synchronized (this.m_lock) {
			s = this.m_result;
		}

		return s != null ? new BufferedReader(new StringReader(s)) : null;
	}
}
