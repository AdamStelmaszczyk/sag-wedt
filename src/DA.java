import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;
import jade.wrapper.ControllerException;

import java.util.ArrayList;

/**
 * DIPRE Agent.
 */
public class DA extends Agent {
	private static final long serialVersionUID = 1L;

	private final ArrayList<String> relations = new ArrayList<String>();
	private AID searchAgent = new AID();

	@Override
	protected void setup() {
		// Get the relations as a start-up argument
		final Object[] args = getArguments();
		if (args != null) {
			for (final Object o : args) {
				relations.add(o.toString());
			}
		}
		System.out.println("DIPRE Agent " + getLocalName()
				+ " is ready with relations:");
		for (final String s : relations) {
			System.out.println(s);
		}

		// Search with the DF for the name of the SA
		DFAgentDescription dfd = new DFAgentDescription();
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
			while (true) {
				System.out.println(getLocalName()
						+ " waiting for an SA registering with the DF");
				final SearchConstraints c = new SearchConstraints();
				c.setMaxDepth(new Long(3));
				final DFAgentDescription[] result = DFService.search(this, dfd,
						c);
				if ((result != null) && (result.length > 0)) {
					dfd = result[0];
					searchAgent = dfd.getName();
					break;
				}
				Thread.sleep(10000);
			}
		} catch (final Exception fe) {
			fe.printStackTrace();
			System.err.println(getLocalName()
					+ " search with DF is not succeeded because of "
					+ fe.getMessage());
			doDelete();
		}
		System.out.println(getLocalName() + " found "
				+ searchAgent.getLocalName() + " Search Agent.");

		addBehaviour(new CommunicationBehaviour(this));
	}

	@Override
	protected void takeDown() {
		// Printout a dismissal message
		System.out.println("DIPRE Agent " + getAID().getName()
				+ " terminating.");
	}

	/**
	 * Inner class CommunicationBehaviour. This is the behaviour used by SA to
	 * serve incoming requests from DA's.
	 */
	private class CommunicationBehaviour extends CyclicBehaviour {
		private static final long serialVersionUID = 1L;

		private final DA myAgent;

		private int step = 0;

		public CommunicationBehaviour(DA agent) {
			myAgent = agent;
		}

		@Override
		public void action() {
			switch (step) {
			case 0:
				// Send the keywords to Search Agent
				final ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
				request.addReceiver(searchAgent);
				request.setContent(nextRequest());
				myAgent.send(request);
				step = 1;
				break;
			case 1:
				// Receive messages
				final ACLMessage msg = myAgent.receive();
				if (msg != null) {
					if (msg.getPerformative() == ACLMessage.INFORM) {
						// INFORM message received. Process it.
						try {
							@SuppressWarnings("unchecked")
							final ArrayList<String> links = (ArrayList<String>) msg
									.getContentObject();
							processLinks(links);
							step = 0;
						} catch (final UnreadableException e) {
							System.err.println(getLocalName()
									+ " cannot read INFORM message from "
									+ msg.getSender().getLocalName()
									+ ". Reason: " + e.getMessage());
							step = 0;
						}
					} else if (msg.getPerformative() == ACLMessage.REQUEST) {
						// REQUEST message received. Send reply.
						System.out.println("DIPRE Agent " + getLocalName()
								+ " received REQUEST message.");
						final ACLMessage reply = msg.createReply();
						reply.setPerformative(ACLMessage.INFORM);
						// TODO: send all relations possibly in many messages
						final StringBuilder sb = new StringBuilder();
						for (final String s : myAgent.relations) {
							sb.append(s);
							sb.append("\n");
						}
						reply.setContent(sb.toString());
						send(reply);
					} else {
						// Unexpected message received. Send reply.
						System.err.println("Agent "
								+ getLocalName()
								+ " - Unexpected message ["
								+ ACLMessage.getPerformative(msg
										.getPerformative())
								+ "] received from "
								+ msg.getSender().getLocalName());
						final ACLMessage reply = msg.createReply();
						reply.setPerformative(ACLMessage.NOT_UNDERSTOOD);
						reply.setContent("Unexpected-act "
								+ ACLMessage.getPerformative(msg
										.getPerformative()));
						send(reply);
					}
				} else {
					block();
				}
				break;
			}
		}
	} // End of inner class CommunicationBehaviour

	protected String nextRequest() {
		// TODO: implement choosing next relation to query Search Agent
		if (relations.isEmpty()) {
			return "relation";
		} else {
			return relations.get(0);
		}
	}

	protected void processLinks(ArrayList<String> links) {
		// TODO: implement link processing
		System.out.println(getLocalName() + " received links:");
		for (final String l : links) {
			System.out.println(l);
		}
	}

}
