package search;

import common.Links;

/**
 * Search Agent using Bing.
 */
public class BingSA extends SA {

	private static final long serialVersionUID = 1L;

	@Override
	protected Links getLinks(String keywords) {
		final Links links = new Links();
		links.add("http://www.microsoft.com/");
		links.add("http://msdn.microsoft.com/");
		return links;
	}

}
