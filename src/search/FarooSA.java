package search;

import common.Links;

/**
 * Search Agent using Faroo.com.
 */
public class FarooSA extends SA {

	private static final long serialVersionUID = 1L;

	@Override
	protected void setup() {
		// TODO Auto-generated method stub
		super.setup();
	}

	@Override
	protected void takeDown() {
		// TODO Auto-generated method stub
		super.takeDown();
	}

	@Override
	protected Links getLinks(String keywords) {
		final Links links = new Links();
		links.add("http://www.entireweb.com/");
		return links;
	}

}
