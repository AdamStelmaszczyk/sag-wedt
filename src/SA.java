import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;

@SuppressWarnings("serial")
public class SA extends Agent {
	protected void setup() {
		addBehaviour(new TickerBehaviour(this, 3000) {
			protected void onTick() {
				System.out.println("This is Search Agent! My name is " + getLocalName() + ".");
			}
		});
	}
}
