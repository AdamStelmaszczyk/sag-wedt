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

	@Override
	protected Links getLinks(String query) throws IOException {
		final Links links = new Links();
		final String url = BASE_URL + URLEncoder.encode(query, "UTF-8");
		final String reply = Network.doHttpGet(url);
		System.out.println(reply);
		links.add("http://www.faroo.com/");
		return links;
	}

}