package search;

import java.util.ArrayList;

import com.google.gson.Gson;
import common.Links;

public class FarooReply {

	private final Links links = new Links();

	private class Reply {
		private ArrayList<Result> results;
	}

	private class Result {
		private String url;
	}

	public FarooReply(String json) {
		final Gson gson = new Gson();
		final Reply reply = gson.fromJson(json, Reply.class);
		for (final Result result : reply.results) {
			links.add(result.url);
		}
	}

	public Links getLinks() {
		return links;
	}

}
