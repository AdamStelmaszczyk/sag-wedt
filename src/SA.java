import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

public class SA extends Agent {
	private static final long serialVersionUID = 1L;

	protected void setup() {
		// Register the search-web service in the yellow pages
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("SA");
		sd.setName("SA");
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		} catch (FIPAException fe) {
			System.err.println(getLocalName()
					+ " registration with DF unsucceeded. Reason: "
					+ fe.getMessage());
			doDelete();
		}
		// End registration with the DF
		System.out.println(getLocalName()
				+ " succeeded in registration with DF");

		addBehaviour(new TickerBehaviour(this, 4000) {
			private static final long serialVersionUID = 1L;

			protected void onTick() {
				System.out.println("This is Search Agent! My name is "
						+ getLocalName() + ".");
			}
		});
	}

	protected void takeDown() {
		// Deregister from the yellow pages
		try {
			DFService.deregister(this);
		} catch (FIPAException fe) {
			System.err.println(getLocalName()
					+ " deregistration with DF unsucceeded. Reason: "
					+ fe.getMessage());
		}
		// Printout a dismissal message
		System.out.println("Agent " + getAID().getName() + " terminating.");
	}
}
