package search;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class Network {

	public static String doHttpRequest(String urlToRead, String requestMethod,
			String auth) throws IOException {
		final StringBuilder result = new StringBuilder();
		final URL url = new URL(urlToRead);
		final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod(requestMethod);
		if (auth != null) {
			conn.setRequestProperty("Authorization", "Basic " + auth);
		}
		final BufferedReader reader = new BufferedReader(new InputStreamReader(
				conn.getInputStream()));
		String line;
		while ((line = reader.readLine()) != null) {
			result.append(line);
		}
		reader.close();
		return result.toString();
	}

	public static String doHttpRequest(String url, String string)
			throws IOException {
		return doHttpRequest(url, string, null);
	}
}
