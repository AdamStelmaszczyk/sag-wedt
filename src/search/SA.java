package search;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

import java.io.IOException;

import common.Links;
import common.Relation;
import common.SAResult;

/**
 * Abstraction for Search Agent.
 */
public abstract class SA extends Agent {

	public static final String AGENT_TYPE = "SA";
	protected static final String ENCODING = "UTF-8";
	private static final long serialVersionUID = 1L;

	protected abstract Links getLinks(String query) throws IOException;

	@Override
	protected void setup() {
		// Register the SA service in the yellow pages
		final DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		final ServiceDescription sd = new ServiceDescription();
		sd.setType(AGENT_TYPE);
		sd.setName(here().getName());
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		} catch (final FIPAException fe) {
			System.err.printf(
					"%s registration with DF unsucceeded. Reason: %s\n",
					getLocalName(), fe.getMessage());
			doDelete();
			return;
		}
		// End registration with the DF
		System.out.printf("%s succeeded in registration with DF.\n",
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
		System.out.printf("%s is terminating.\n", getAID().getName());
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
				Relation query = null;
				try {
					query = (Relation) msg.getContentObject();
				} catch (UnreadableException e1) {
					System.out
							.println("Warning::Error during deserialization of relation in SA");
					return; // TODO Not sure if it should be return or just
							// block()
				}
				System.out.printf("%s received REQUEST with content: %s\n",
						getLocalName(), query.toString());
				final ACLMessage reply = msg.createReply();
				reply.setPerformative(ACLMessage.INFORM);
				try {
					Links links = getLinks(query.toString());
					SAResult result = new SAResult(links, query);
					reply.setContentObject(result);
				} catch (final IOException e) {
					reply.setPerformative(ACLMessage.REFUSE);
					reply.setContent(e.getMessage());
				}
				myAgent.send(reply);
			} else {
				block();
			}
		}
	} // End of inner class ServeRequestsBehaviour

}
