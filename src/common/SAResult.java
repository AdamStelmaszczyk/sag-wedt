package common;

import java.io.Serializable;

public class SAResult implements Serializable {
	private static final long serialVersionUID = 1L;
	private Links links;
	private Relation relation;

	public SAResult(Links l, Relation r) {
		this.links = l;
		this.relation = r;
	}

	public Links getLinks() {
		return links;
	}

	public Relation getRelation() {
		return relation;
	}

}
