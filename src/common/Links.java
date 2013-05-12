package common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class Links implements Serializable, Iterable<String> {

	private final Collection<String> links = new ArrayList<String>();
	private static final long serialVersionUID = 1L;

	public void add(String link) {
		links.add(link);
	}

	@Override
	public Iterator<String> iterator() {
		return links.iterator();
	}

	@Override
	public String toString() {
		return links.toString();
	}
}
