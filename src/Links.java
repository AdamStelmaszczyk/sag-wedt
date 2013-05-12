import java.io.Serializable;
import java.util.ArrayList;

public class Links implements Serializable {

	private final ArrayList<String> links = new ArrayList<String>();
	private static final long serialVersionUID = 1L;

	public void add(String link) {
		links.add(link);
	}

	@Override
	public String toString() {
		return links.toString();
	}
}
