import java.util.ArrayList;

public class BingSA extends SA {
	private static final long serialVersionUID = 1L;

	@Override
	protected void setupSA() {
		return;
	}

	@Override
	protected void takeDownSA() {
		return;
	}

	@Override
	protected ArrayList<String> find(String keywords) {
		ArrayList<String> links = new ArrayList<String>();
		links.add("http://www.microsoft.com/");
		links.add("http://msdn.microsoft.com/");
		return links;
	}

}