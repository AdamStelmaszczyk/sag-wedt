import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.wrapper.ControllerException;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Abstraction for Search Agent.
 */
public abstract class SA extends Agent {
	private static final long serialVersionUID = 1L;

	@Override
	protected void setup() {
		// Call abstract method
		setupSA();
		// Register the SA service in the yellow pages
		final DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		final ServiceDescription sd = new ServiceDescription();
		sd.setType("SA");
		try {
			sd.setName(getContainerController().getContainerName());
		} catch (final ControllerException e) {
			System.err.println(getLocalName()
					+ " cannot get container name. Reason: " + e.getMessage());
			doDelete();
		}
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		} catch (final FIPAException fe) {
			System.err.println(getLocalName()
					+ " registration with DF unsucceeded. Reason: "
					+ fe.getMessage());
			doDelete();
		}
		// End registration with the DF
		System.out.println("Search Agent " + getLocalName()
				+ " succeeded in registration with DF.");

		addBehaviour(new ServeRequestsBehaviour());
	}

	@Override
	protected void takeDown() {
		// Call abstract method
		takeDownSA();
		// Deregister from the yellow pages
		try {
			DFService.deregister(this);
		} catch (final FIPAException fe) {
			System.err.println(getLocalName()
					+ " deregistration with DF unsucceeded. Reason: "
					+ fe.getMessage());
		}
		// Printout a dismissal message
		System.out.println("Search Agent " + getAID().getName()
				+ " terminating.");
	}

	/**
	 * Inner class ServeRequestsBehaviour. This is the behaviour used by SA to
	 * serve incoming requests from DA's.
	 */
	private class ServeRequestsBehaviour extends CyclicBehaviour {
		private static final long serialVersionUID = 1L;

		@Override
		public void action() {
			final MessageTemplate mt = MessageTemplate
					.MatchPerformative(ACLMessage.REQUEST);
			final ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {
				// REQUEST message received. Process it
				final String keywords = msg.getContent();
				System.out.printf("%s received REQUEST with content: %s",
						getLocalName(), keywords);
				final ACLMessage reply = msg.createReply();
				reply.setPerformative(ACLMessage.INFORM);
				try {
					reply.setContentObject(find(keywords));
				} catch (final IOException e) {
					e.printStackTrace();
				}
				myAgent.send(reply);
			} else {
				block();
			}
		}
	} // End of inner class ServeRequestsBehaviour

	protected abstract void setupSA();

	protected abstract void takeDownSA();

	protected abstract ArrayList<String> find(String keywords);

}
