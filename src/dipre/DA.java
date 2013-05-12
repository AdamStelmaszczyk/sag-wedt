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

/**
 * DIPRE Agent.
 */
public class DA extends Agent {

	private class Relation {
		public String first;
		public String second;

		public Relation(String f, String s) {
			first = f;
			second = s;
		}

		@Override
		public String toString() {
			return first + " " + second;
		}
	}

	private final int SLEEP_MS = 1000;
	private final List<Relation> relations = new ArrayList<Relation>();
	private AID searchAgent = new AID();
	private static final long serialVersionUID = 1L;
	private boolean fooTest = true; // Uzyte w zaslepce getNewRelation

	@Override
	protected void setup() {
		// Get the relations as a start-up argument
		final Object[] args = getArguments();
		if (args != null) {
			for (final Object o : args) {
				String s = o.toString();
				String[] parts = s.split("#");
				if (parts.length != 2) {
					System.err.printf("Bad relation [%s] passed to %s\n", s,
							getLocalName());
					doDelete();
					return;
				}
				relations.add(new Relation(parts[0], parts[1]));
			}
		} else {
			System.err.printf("At least one relation is required to %s\n",
					getLocalName());
			doDelete();
			return;
		}
		System.out.println("DIPRE Agent " + getLocalName()
				+ " is ready with relations:");
		for (final Relation r : relations) {
			System.out.println(r.toString());
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
			return;
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
			return;
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
			// DEBUG only START
			try {
				Thread.sleep(SLEEP_MS);
			} catch (InterruptedException e) {
			}
			// DEBUG only END
			switch (step) {
			case 0:
				requestLinksPortion();
				break;
			case 1:
				// Receive messages
				final ACLMessage msg = receive();
				if (msg != null) {
					if (msg.getPerformative() == ACLMessage.INFORM)
						handleLinksPortion(msg);
					else if (msg.getPerformative() == ACLMessage.REQUEST)
						handleRelationsRequest(msg);
					else
						handleUnexpectedMsg(msg);
				} else
					block();
				break;
			}
		}

		private void requestLinksPortion() {
			// Send the keywords to Search Agent
			final ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
			request.addReceiver(searchAgent);
			String relation = nextRequest();
			if (relation != null) {
				request.setContent(relation);
				send(request);
			} // TODO: else? which step?
			step = 1;
		}

		private void handleLinksPortion(final ACLMessage msg) {
			// INFORM message received. Process it.
			try {
				final Links links = (Links) msg.getContentObject();
				processLinks(links); // TODO to chyba do smieci
				dipre(links);
			} catch (final UnreadableException e) {
				System.err
						.printf(getLocalName()
								+ "%s cannot read INFORM message from %s. Reason: %s\n",
								msg.getSender().getLocalName(), e.getMessage());
			} finally {
				step = 0;
			}
		}

		private void handleUnexpectedMsg(final ACLMessage msg) {
			// Unexpected message received. Send reply.
			final String msgType = ACLMessage.getPerformative(msg
					.getPerformative());
			System.err.printf(
					"Agent %s - Unexpected message [%s] received from %s\n",
					getLocalName(), msgType, msg.getSender().getLocalName());
			final ACLMessage reply = msg.createReply();
			reply.setPerformative(ACLMessage.NOT_UNDERSTOOD);
			reply.setContent("Unexpected-act " + msgType);
			send(reply);
		}

		private void handleRelationsRequest(final ACLMessage msg) {
			// REQUEST message received. Send reply.
			System.out.printf("DIPRE Agent %s received REQUEST message\n",
					getLocalName());
			final ACLMessage reply = msg.createReply();
			reply.setPerformative(ACLMessage.INFORM);
			// TODO: send all relations possibly in many messages
			final StringBuilder sb = new StringBuilder();
			sb.append("Relations:"); // TODO: this line is temporary
			for (final DA.Relation r : DA.this.relations) {
				sb.append(r.toString());
				sb.append("\n");
			}
			reply.setContent(sb.toString());
			send(reply);
		}
	} // End of inner class CommunicationBehaviour

	protected String nextRequest() {
		// TODO: implement choosing next relation to query Search Agent
		if (relations.isEmpty())
			return null;
		else
			return relations.get(0).toString();
	}

	protected void processLinks(Links links) {
		// TODO: implement link processing
		System.out.printf("%s received links: %s\n", getLocalName(), links);
	}

	/**
	 * Method to perform DIPRE algorithm on web pages given in links
	 */
	protected void dipre(Links links) {
		Relation relation = relations.remove(0); // TODO: temporary remove
		for (String link : links) {
			String pattern = getPattern(link, relation);
			// TODO magic number:)
			Links innerLinks = getInnerLinks(link, 2);
			for (String innerLink : innerLinks) {
				Relation newRelation = getNewRelation(innerLink, pattern);
				if (newRelation != null) {
					relations.add(newRelation);
					System.out.printf("%s got new relation: %s\n",
							getLocalName(), newRelation.toString());
				}
			}
		}
	}

	/**
	 * @param page
	 *            link to page, it starts searching
	 * @param deepthLevel
	 *            max level of deepth
	 * @return Links to pages from the same domain found by DFS with max level
	 */
	protected Links getInnerLinks(String link, int deepthLevel) {
		// TODO zaslepka metody
		final Links links = new Links();
		links.add("http://www.microsoft.com/page_id/13");
		links.add("http://msdn.microsoft.com/page_id/14");
		links.add("http://msdn.microsoft.com/page_id/15");
		return links;
	}

	protected String getPattern(String link, Relation relation) {
		// TODO zaslepka metody
		return relation.first + "’s first book " + relation.second;
	}

	protected Relation getNewRelation(String link, String pattern) {
		// TODO zaslepka metody
		if (fooTest) {
			fooTest = false;
			return new Relation("Tolkien", "LOTR");
		}
		return null;
	}

}
