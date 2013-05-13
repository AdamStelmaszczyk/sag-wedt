package search;

import java.io.IOException;
import java.net.URLEncoder;

import common.Links;

/**
 * Search Agent using Bing.
 */
public class BingSA extends SA {

	private static final String BASE_URL = "https://api.datamarket.azure.com/Bing/Search/Web?$format=json&Query=";
	private static final long serialVersionUID = 1L;

	@Override
	protected Links getLinks(String query) throws IOException {
		final String url = BASE_URL + URLEncoder.encode(query, "UTF-8");
		final String json = Network.doHttpRequest(url, "POST");
		final BingReply reply = new BingReply(json);
		return reply.getLinks();
	}

	private class BingReply {

		public BingReply(String json) {
			// TODO Auto-generated constructor stub
		}

		public Links getLinks() {
			// TODO Auto-generated method stub
			return null;
		}

	}

}
