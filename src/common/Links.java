package common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Links implements Serializable, Iterable<String> {

	private final List<String> links = new ArrayList<String>();
	private static final long serialVersionUID = 1L;

	public void add(String link) {
		links.add(link);
	}

	@Override
	public String toString() {
		return links.toString();
	}

	@Override
	public Iterator<String> iterator() {
		// TODO Auto-generated method stub
		return links.iterator();
	}
}
