package search;

import java.io.IOException;
import java.net.URLEncoder;

import common.Links;

/**
 * Search Agent using Faroo.com.
 */
public class FarooSA extends SA {

	private static final String BASE_URL = "http://www.faroo.com/api?q=";
	private static final long serialVersionUID = 1L;

	public static void main(String[] args) throws IOException {
		final FarooSA farooSA = new FarooSA();
		final Links links = farooSA.getLinks("test");
		System.out.println(links);
	}

	@Override
	protected Links getLinks(String query) throws IOException {
		final String url = BASE_URL + URLEncoder.encode(query, "UTF-8");
		final String json = Network.doHttpGet(url);
		final FarooReply reply = new FarooReply(json);
		return reply.getLinks();
	}

}