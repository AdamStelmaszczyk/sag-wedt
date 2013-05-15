package dipre;

import jade.content.ContentElement;
import jade.content.lang.Codec;
import jade.content.lang.Codec.CodecException;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.content.onto.basic.Action;
import jade.content.onto.basic.Result;
import jade.core.AID;
import jade.core.Agent;
import jade.core.Location;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.JADEAgentManagement.QueryPlatformLocationsAction;
import jade.domain.mobility.MobilityOntology;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import search.SA;

import common.Links;

/**
 * DIPRE Agent.
 */
public class DA extends Agent {

	private class Relation implements Serializable {
		private static final long serialVersionUID = 1L;
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

	private static final long serialVersionUID = 1L;
	private final List<Relation> relations = new ArrayList<Relation>();
	private AID searchAgent = new AID();
	private final Codec codec = new SLCodec();
	private final Ontology ontology = MobilityOntology.getInstance();
	private boolean fooTest = true; // Uzyte w zaslepce getNewRelation

	@Override
	protected void setup() {
		// Get the relations as a start-up argument
		final Object[] args = getArguments();
		if (args != null) {
			for (final Object arg : args) {
				final String relation = arg.toString();
				final String[] elements = relation.split("#");
				if (elements.length != 2) {
					System.err
							.printf("Relation \"%s\" passed to %s has wrong format."
									+ " Only binary relations with elements separeted by # are correct.\n",
									relation, getLocalName());
					doDelete();
					return;
				}
				relations.add(new Relation(elements[0], elements[1]));
			}
		} else {
			System.err.printf(
					"At least one relation is required to start %s.\n",
					getLocalName());
			doDelete();
			return;
		}
		System.out.printf("%s is ready with relations: %s.\n", getLocalName(),
				relations.toString());

		// Initialize agent
		init();
	}

	@Override
	protected void takeDown() {
		// Printout a dismissal message
		System.out.printf("%s is terminating.\n", getAID().getName());
	}

	@Override
	protected void beforeMove() {
		System.out.printf("%s is now moving itself from %s.\n", getLocalName(),
				here().getName());
	}

	@Override
	protected void afterMove() {
		System.out.printf("%s has moved itself to %s.\n", getLocalName(),
				here().getName());
		// Initialize agent
		init();
	}

	protected void init() {
		// Register language and ontology
		getContentManager().registerLanguage(codec);
		getContentManager().registerOntology(ontology);

		final SequentialBehaviour seqBehaviour = new SequentialBehaviour();
		seqBehaviour.addSubBehaviour(new FindSearchAgentBehaviour());
		seqBehaviour.addSubBehaviour(new CommunicationBehaviour());
		addBehaviour(seqBehaviour);
	}

	/**
	 * Try to move agent to another container. If there is no other container
	 * stay at this.
	 */
	protected boolean tryToMoveAgent() {
		try {
			// Get available locations with AMS
			final Action action = new Action(getAMS(),
					new QueryPlatformLocationsAction());
			final ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
			request.setLanguage(codec.getName());
			request.setOntology(ontology.getName());
			getContentManager().fillContent(request, action);
			request.addReceiver(action.getActor());
			send(request);

			// Receive response from AMS
			final MessageTemplate mt = MessageTemplate.and(
					MessageTemplate.MatchSender(getAMS()),
					MessageTemplate.MatchPerformative(ACLMessage.INFORM));
			final ACLMessage resp = blockingReceive(mt);
			final ContentElement ce = getContentManager().extractContent(resp);
			final Result result = (Result) ce;
			final jade.util.leap.Iterator it = result.getItems().iterator();
			final List<Location> locations = new ArrayList<Location>();
			while (it.hasNext()) {
				final Location loc = (Location) it.next();
				locations.add(loc);
			}
			System.out.printf("%s see locations: %s.\n", getLocalName(),
					locations.toString());
			// Move to first different location
			locations.remove(here());
			if (locations.size() > 0) {
				final Random rand = new Random();
				doMove(locations.get(rand.nextInt(locations.size())));
				return true;
			} else {
				System.err
						.printf("Cannot move %s because there is no other container.\n",
								getLocalName());
				return false;
			}
		} catch (final CodecException e) {
			System.err
					.printf("Cannot move %s because of problem with codec. Reason: %s\n",
							getLocalName(), e.getMessage());
			return false;
		} catch (final OntologyException e) {
			System.err
					.printf("Cannot move %s because of problem with ontology. Reason: %s\n",
							getLocalName(), e.getMessage());
			return false;
		}
	}

	/**
	 * Inner class FindSearchAgentBehaviour. This is the behaviour used by DA to
	 * find SA.
	 */
	private class FindSearchAgentBehaviour extends Behaviour {

		private static final long serialVersionUID = 1L;
		private DFAgentDescription dfd = new DFAgentDescription();
		int attempt = 0;
		int step = 0;

		@Override
		public void action() {
			// Search with the DF for the name of the SA
			if (attempt == 3) {
				if (tryToMoveAgent()) {
					return;
				} else {
					attempt = 0;
				}
			}
			switch (step) {
			case 0:
				final ServiceDescription sd = new ServiceDescription();
				sd.setType(SA.AGENT_TYPE);
				sd.setName(here().getName());
				dfd.addServices(sd);
				++step;
				break;
			case 1:
				try {
					System.out.printf(
							"%s is searching for SA on the yellow pages.\n",
							getLocalName());
					final SearchConstraints c = new SearchConstraints();
					c.setMaxDepth(3L);
					final DFAgentDescription[] result = DFService.search(
							DA.this, dfd, c);
					if ((result != null) && (result.length > 0)) {
						dfd = result[0];
						searchAgent = dfd.getName();
						System.out.printf("%s found %s SA.\n", getLocalName(),
								searchAgent.getLocalName());
						++step;
					} else {
						System.err.printf("%s cannot find SA.\n",
								getLocalName());
						++attempt;
					}
				} catch (final FIPAException fe) {
					fe.printStackTrace();
					System.err.printf(
							"%s search with DF is not succeeded. Reason: %s\n",
							getLocalName(), fe.getMessage());
					++attempt;
				}
				break;
			}
		}

		@Override
		public boolean done() {
			return step == 2;
		}

	} // End of inner class FindSearchAgentBehaviour

	/**
	 * Inner class CommunicationBehaviour. This is the behaviour used by DA to
	 * serve and perform messages.
	 */
	private class CommunicationBehaviour extends CyclicBehaviour {

		private static final long serialVersionUID = 1L;
		private int step = 0;

		@Override
		public void action() {
			// DEBUG only START
			try {
				Thread.sleep(2000);
			} catch (final InterruptedException e) {
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
					if (msg.getPerformative() == ACLMessage.INFORM) {
						handleLinksPortion(msg);
					} else if (msg.getPerformative() == ACLMessage.REQUEST) {
						handleRelationsRequest(msg);
					} else if (msg.getPerformative() == ACLMessage.REFUSE) {
						handleRefuseMsg(msg);
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
			final String relation = nextRequest();
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
				System.err.printf(
						"%s cannot read INFORM message from %s. Reason: %s\n",
						getLocalName(), msg.getSender().getLocalName(),
						e.getMessage());
			} finally {
				step = 0;
				tryToMoveAgent(); // TODO: probably it's temporary
			}
		}

		private void handleRelationsRequest(final ACLMessage msg) {
			// REQUEST message received. Send reply.
			System.out.printf("%s received REQUEST message.\n", getLocalName());
			final ACLMessage reply = msg.createReply();
			reply.setPerformative(ACLMessage.INFORM);
			// TODO: send all relations possibly in many messages
			final StringBuilder sb = new StringBuilder();
			for (final DA.Relation r : relations) {
				sb.append(r.toString());
				sb.append("\n");
			}
			reply.setContent(sb.toString());
			send(reply);
		}

		private void handleRefuseMsg(final ACLMessage msg) {
			// REFUSE message received. Repeat query.
			System.out.printf(
					"%s received REFUSE message from %s with content: %s\n",
					getLocalName(), msg.getSender().getLocalName(),
					msg.getContent());
			step = 0;
			tryToMoveAgent();
		}

		private void handleUnexpectedMsg(final ACLMessage msg) {
			// Unexpected message received. Send reply.
			final String msgType = ACLMessage.getPerformative(msg
					.getPerformative());
			System.err
					.printf("%s received unexpected %s message from %s with content: %s\n",
							getLocalName(), msgType, msg.getSender()
									.getLocalName(), msg.getContent());
			final ACLMessage reply = msg.createReply();
			reply.setPerformative(ACLMessage.NOT_UNDERSTOOD);
			reply.setContent("Unexpected act: " + msgType);
			send(reply);
		}

	} // End of inner class CommunicationBehaviour

	protected String nextRequest() {
		// TODO: implement choosing next relation to query Search Agent
		if (relations.isEmpty()) {
			return null;
		} else {
			return relations.get(0).toString();
		}
	}

	protected void processLinks(Links links) {
		// TODO: implement link processing
		System.out.printf("%s received links: %s.\n", getLocalName(),
				links.toString());
	}

	/**
	 * Method to perform DIPRE algorithm on web pages given in links
	 */
	protected void dipre(Links links) {
		// Relation relation = relations.remove(0); // TODO: temporary remove
		final Relation relation = relations.get(0);
		for (final String link : links) {
			final String pattern = getPattern(link, relation);
			// TODO magic number:)
			final Links innerLinks = getInnerLinks(link, 2);
			for (final String innerLink : innerLinks) {
				final Relation newRelation = getNewRelation(innerLink, pattern);
				if (newRelation != null) {
					relations.add(newRelation);
					System.out.printf("%s got new relation: %s.\n",
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
