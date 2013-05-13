package search;

import com.google.gson.Gson;

import common.Links;

abstract public class JsonReply {

	protected final Links links = new Links();
	protected final Gson gson = new Gson();

	public Links getLinks() {
		return links;
	}
}
