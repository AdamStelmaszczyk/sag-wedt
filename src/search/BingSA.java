package search;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;

import javax.xml.bind.DatatypeConverter;

import common.Links;

/**
 * Search Agent using Bing.
 */
public class BingSA extends SA {

	private static final String BASE_URL = "https://api.datamarket.azure.com/Bing/Search/Web?$format=json&Query=";
	private static final String ACCOUNT_KEY = "DedMhSFgiGycNer19EUCE5EW3ug0qqjhM56/gzvbZX4";
	private static final long serialVersionUID = 1L;

	public static void main(String[] args) throws IOException {
		final BingSA bingSA = new BingSA();
		final Links links = bingSA.getLinks("test");
		System.out.println(links);
	}

	@Override
	protected Links getLinks(String query) throws IOException {
		final String url = BASE_URL
				+ URLEncoder.encode(String.format("'%s'", query), ENCODING);
		final String auth = String.format("%1$s:%1$s", ACCOUNT_KEY);
		final String authInBase64 = DatatypeConverter.printBase64Binary(auth
				.getBytes());
		final String json = Network.doHttpRequest(url, "GET", authInBase64);
		final BingReply reply = new BingReply(json);
		return reply.getLinks();
	}

	private class BingReply extends JsonReply {

		public BingReply(String json) {
			final Reply reply = gson.fromJson(json, Reply.class);
			for (final Result result : reply.d.results) {
				links.add(result.Url);
			}
		}

		private class Reply {
			private Data d;
		}

		private class Data {
			private ArrayList<Result> results;
		}

		private class Result {
			private String Url;
		}

	}

}
