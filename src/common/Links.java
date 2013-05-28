package common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

public class Links implements Serializable, Iterable<String> {

	private final List<String> links = new ArrayList<String>();
	private final Set<String> uniqnes = new HashSet<String>();
	private static final long serialVersionUID = 1L;

	public void add(String link) {
		links.add(link);
	}

	public void add(Links links) {
		for (String link : links) {
			if (uniqnes.contains(link))
				continue;
			this.uniqnes.add(link);
			this.links.add(link);
		}
	}

	@Override
	public Iterator<String> iterator() {
		return links.iterator();
	}
	public int size(){
		return links.size();
	}

	@Override
	public String toString() {
		return links.toString();
	}
}
