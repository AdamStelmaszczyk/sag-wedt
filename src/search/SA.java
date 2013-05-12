package search;
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

import common.Links;

/**
 * Abstraction for Search Agent.
 */
public abstract class SA extends Agent {

	public static final String AGENT_TYPE = "SA";
	private static final long serialVersionUID = 1L;

	@Override
	protected void setup() {
		// Register the SA service in the yellow pages
		final DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		final ServiceDescription sd = new ServiceDescription();
		sd.setType(AGENT_TYPE);
		try {
			sd.setName(getContainerController().getContainerName());
		} catch (final ControllerException e) {
			System.err.printf("%s cannot get container name. Reason: %s\n",
					getLocalName(), e.getMessage());
			doDelete();
		}
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		} catch (final FIPAException fe) {
			System.err.printf(
					"%s registration with DF unsucceeded. Reason: %s\n",
					getLocalName(), fe.getMessage());
			doDelete();
		}
		// End registration with the DF
		System.out.printf(
				"Search Agent %s succeeded in registration with DF\n",
				getLocalName());

		addBehaviour(new ServeRequestsBehaviour());
	}

	@Override
	protected void takeDown() {
		// Deregister from the yellow pages
		try {
			DFService.deregister(this);
		} catch (final FIPAException fe) {
			System.err.printf(
					"%s deregistration with DF unsucceeded. Reason: %s\n",
					getLocalName(), fe.getMessage());
		}
		// Printout a dismissal message
		System.out.printf("Search Agent %s terminating\n", getAID().getName());
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
				System.out.printf("%s received REQUEST with content: %s\n",
						getLocalName(), keywords);
				final ACLMessage reply = msg.createReply();
				reply.setPerformative(ACLMessage.INFORM);
				try {
					reply.setContentObject(getLinks(keywords));
				} catch (final IOException e) {
					// TODO: we should do sth with this for example sending
					// REFUSE message instead of
					// printStackTrace
					e.printStackTrace();
				}
				myAgent.send(reply);
			} else {
				block();
			}
		}
	} // End of inner class ServeRequestsBehaviour

	protected abstract Links getLinks(String keywords) throws IOException;

}
