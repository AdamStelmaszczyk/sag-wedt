package dipre;
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
import java.util.List;

import search.SA;

import common.Links;

import search.SA;

import common.Links;

/**
 * DIPRE Agent.
 */
public class DA extends Agent {

	private class Relation {
		public String first;
		public String second;
		public Relation(String f, String s){
			first=f;
			second=s;
		}
		@Override
		public String toString() {
			return first+" "+second;
		}
	}

	private final int SLEEP_MS = 10000;
	private final List<Relation> relations = new ArrayList<Relation>();
	private AID searchAgent = new AID();
	private static final long serialVersionUID = 1L;

	@Override
	protected void setup() {
		// Get the relations as a start-up argument
		final Object[] args = getArguments();
		if (args != null && args.length==2) {
				relations.add(new Relation(args[0].toString(), args[1].toString()));
			
		}
		System.out.printf("DIPRE Agent %s is ready with relations:\n",	
				getLocalName());
		
		
		
		for (final Relation s : relations) {
			System.out.println(s);
		}

		// Search with the DF for the name of the SA
		DFAgentDescription dfd = new DFAgentDescription();
		final ServiceDescription sd = new ServiceDescription();
		sd.setType(SA.AGENT_TYPE);
		try {
			sd.setName(getContainerController().getContainerName());
		} catch (final ControllerException e) {
			System.err.printf("%s cannot get container name. Reason: %s\n",
					getLocalName(), e.getMessage());
			doDelete();
		}
		dfd.addServices(sd);
		try {
			while (true) {
				System.out.printf(
						"%s waiting for an SA registering with the DF\n",
						getLocalName());
				final SearchConstraints c = new SearchConstraints();
				c.setMaxDepth(3L);
				final DFAgentDescription[] result = DFService.search(this, dfd,
						c);
				if ((result != null) && (result.length > 0)) {
					dfd = result[0];
					searchAgent = dfd.getName();
					break;
				}
				Thread.sleep(SLEEP_MS);
			}
		} catch (final Exception fe) {
			fe.printStackTrace();
			System.err.printf(
					"%s search with DF is not succeeded because of %s\n",
					getLocalName(), fe.getMessage());
			doDelete();
		}
		System.out.printf("%s found %s Search Agent\n", getLocalName(),
				searchAgent.getLocalName());

		addBehaviour(new CommunicationBehaviour());
	}

	@Override
	protected void takeDown() {
		// Printout a dismissal message
		System.out.printf("DIPRE Agent %s terminating\n", getAID().getName());
	}

	/**
	 * Inner class CommunicationBehaviour. This is the behaviour used by SA to
	 * serve incoming requests from DA's.
	 */
	private class CommunicationBehaviour extends CyclicBehaviour {

		private static final long serialVersionUID = 1L;
		private int step = 0;

		@Override
		public void action() {
			switch (step) {
			case 0:
				requestLinksPortion();
				break;
			case 1:
				// Receive messages
				final ACLMessage msg = receive();
				if (msg != null) {
					if (msg.getPerformative() == ACLMessage.INFORM) {
						handleLinksPortion(msg);
					} else if (msg.getPerformative() == ACLMessage.REQUEST) {
						handleRelationsRequest(msg);
					} else {
						handleUnexpectedMsg(msg);
					}
				} else {
					block();
				}
				break;
			}
		}

		private void requestLinksPortion() {
			// Send the keywords to Search Agent
			final ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
			request.addReceiver(searchAgent);
			String relation = nextRequest();
			if(relation!=null){
				request.setContent(relation);
				send(request);
			}
			step = 1;
		}

		private void handleLinksPortion(final ACLMessage msg) {
			// INFORM message received. Process it.
			try {
				final Links links = (Links) msg.getContentObject();
				processLinks(links);
			} catch (final UnreadableException e) {
				System.err
						.printf(getLocalName()
								+ "%s cannot read INFORM message from %s. Reason: %s\n",
								msg.getSender().getLocalName(),
								e.getMessage());
			} finally {
				step = 0;
			}
		}

		private void handleUnexpectedMsg(final ACLMessage msg) {
			// Unexpected message received. Send reply.
			final String msgText = ACLMessage.getPerformative(msg
					.getPerformative());
			System.err
					.printf("Agent %s - Unexpected message [%s] received from %s\n",
							getLocalName(), msgText, msg
									.getSender().getLocalName());
			final ACLMessage reply = msg.createReply();
			reply.setPerformative(ACLMessage.NOT_UNDERSTOOD);
			reply.setContent("Unexpected-act " + msgText);
			send(reply);
		}

		private void handleRelationsRequest(final ACLMessage msg) {
			// REQUEST message received. Send reply.
			System.out.printf(
					"DIPRE Agent %s received REQUEST message\n",
					getLocalName());
			final ACLMessage reply = msg.createReply();
			reply.setPerformative(ACLMessage.INFORM);
			// TODO: send all relations possibly in many messages
			final StringBuilder sb = new StringBuilder();
			sb.append("Relations:");
			for (final DA.Relation s : relations) {
				sb.append(s);
				sb.append("\n");
			}
			
			reply.setContent(sb.toString());
			send(reply);
		}
	} // End of inner class CommunicationBehaviour

	protected String nextRequest() {
		// TODO: implement choosing next relation to query Search Agent
		if (relations.isEmpty()) {
			return null;
		} else {
			return relations.remove(0).toString();
		}
	}

	protected void processLinks(Links links) {
		// TODO: implement link processing
		System.out.printf("%s received links: %s\n", getLocalName(), links);
	}

}
