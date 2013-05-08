import java.util.ArrayList;

import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;

@SuppressWarnings("serial")
public class DA extends Agent {
	private ArrayList<String> relations = new ArrayList<String>();

	protected void setup() {
		System.out.println("DIPRE Agent " + getAID().getName() + " is ready.");

		// Get the relations as a start-up argument
		Object[] args = getArguments();
		if (args != null) {
			for (int i = 0; i < args.length; i++) {
				relations.add(args[i].toString());
			}
		}
		System.out.println("Relations of agent " + getLocalName() + ":");
		for (String s : relations) {
			System.out.println(s);
		}

		addBehaviour(new TickerBehaviour(this, 3000) {
			protected void onTick() {
				System.out.println("This is DIPRE Agent! My name is "
						+ getLocalName() + ".");
			}
		});
	}

	protected void takeDown() {
		// Printout a dismissal message
		System.out.println("Agent " + getAID().getName() + " terminating.");
	}
}
