import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;

import java.util.ArrayList;

public class DA extends Agent {
	private static final long serialVersionUID = 1L;
	private ArrayList<String> relations = new ArrayList<String>();
	private AID searchAgent = new AID();

	protected void setup() {
		// Get the relations as a start-up argument
		Object[] args = getArguments();
		if (args != null) {
			for (Object o : args) {
				relations.add(o.toString());
			}
		}
		System.out.println("DIPRE Agent " + getLocalName() + " is ready.");
		System.out.println("Relations of agent " + getLocalName() + ":");
		for (String s : relations) {
			System.out.println(s);
		}

		// Search with the DF for the name of the SA
		DFAgentDescription dfd = new DFAgentDescription();
		ServiceDescription sd = new ServiceDescription();
		sd.setType("SA");
		dfd.addServices(sd);
		try {
			while (true) {
				System.out.println(getLocalName()
						+ " waiting for an SA registering with the DF");
				SearchConstraints c = new SearchConstraints();
				c.setMaxDepth(new Long(3));
				DFAgentDescription[] result = DFService.search(this, dfd, c);
				if ((result != null) && (result.length > 0)) {
					dfd = result[0];
					searchAgent = dfd.getName();
					break;
				}
				Thread.sleep(10000);
			}
		} catch (Exception fe) {
			fe.printStackTrace();
			System.err.println(getLocalName()
					+ " search with DF is not succeeded because of "
					+ fe.getMessage());
			doDelete();
		}
		System.out.println(getLocalName() + " found "
				+ searchAgent.getLocalName() + " Search Agent.");

		addBehaviour(new TickerBehaviour(this, 4000) {
			private static final long serialVersionUID = 1L;

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
