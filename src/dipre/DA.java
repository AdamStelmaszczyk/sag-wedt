package dipre;

import jade.content.ContentElement;
import jade.content.lang.Codec;
import jade.content.lang.Codec.CodecException;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.content.onto.UngroundedException;
import jade.content.onto.basic.Action;
import jade.content.onto.basic.Result;
import jade.core.AID;
import jade.core.Agent;
import jade.core.Location;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.JADEAgentManagement.QueryPlatformLocationsAction;
import jade.domain.mobility.MobilityOntology;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import jade.wrapper.ControllerException;

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

	private final int SLEEP_MS = 1000;
	private final List<Relation> relations = new ArrayList<Relation>();
	private AID searchAgent = new AID();
	private final Codec codec = new SLCodec();
	private final Ontology ontology = MobilityOntology.getInstance();
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

		init();
	}

	@Override
	protected void takeDown() {
		// Printout a dismissal message
		System.out.printf("DIPRE Agent %s terminating\n", getAID().getName());
	}

	@Override
	protected void beforeMove() {
		System.out.printf("%s is now moving itself from location: %s\n",
				getLocalName(), here().getName());
	}

	@Override
	protected void afterMove() {
		System.out.printf("%s has moved itself. New location: %s\n",
				getLocalName(), here().getName());
		init();
	}

	protected void init() {
		registerLanguageAndOntology();

		SequentialBehaviour seqBehaviour = new SequentialBehaviour();
		seqBehaviour.addSubBehaviour(new FindSearchAgentBehaviour());
		seqBehaviour.addSubBehaviour(new CommunicationBehaviour());
		addBehaviour(seqBehaviour);
	}

	protected void registerLanguageAndOntology() {
		// Register language and ontology
		getContentManager().registerLanguage(codec);
		getContentManager().registerOntology(ontology);
	}

	/*
	 * Try to move agent to another container. If there is no other container
	 * stay at this. TODO: improvements needed
	 */
	protected void tryToMoveAgent() {
		// Get available locations with AMS
		Action action = new Action(getAMS(), new QueryPlatformLocationsAction());
		ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
		request.setLanguage(codec.getName());
		request.setOntology(ontology.getName());
		try {
			getContentManager().fillContent(request, action);
		} catch (CodecException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.err.println(e.getMessage());
		} catch (OntologyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.err.println(e.getMessage());
		}
		request.addReceiver(action.getActor());
		send(request);

		// Receive response from AMS
		MessageTemplate mt = MessageTemplate.and(
				MessageTemplate.MatchSender(getAMS()),
				MessageTemplate.MatchPerformative(ACLMessage.INFORM));
		ACLMessage resp = blockingReceive(mt);
		try {
			ContentElement ce = getContentManager().extractContent(resp);
			Result result = (Result) ce;
			jade.util.leap.Iterator it = result.getItems().iterator();
			ArrayList<Location> locations = new ArrayList<Location>();
			while (it.hasNext()) {
				Location loc = (Location) it.next();
				locations.add(loc);
			}
			System.out.printf("%s see locations:\n%s\n", getLocalName(),
					locations.toString());
			// Move to first different location
			Location current = here();
			locations.remove(current);
			Random rand = new Random();
			if (locations.size() > 0)
				doMove(locations.get(rand.nextInt(locations.size())));
		} catch (UngroundedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.err.println(e.getMessage());
		} catch (CodecException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.err.println(e.getMessage());
		} catch (OntologyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.err.println(e.getMessage());
		}
	}

	/**
	 * Inner class FindSearchAgentBehaviour. This is the behaviour used by DA to
	 * find SA.
	 */
	private class FindSearchAgentBehaviour extends Behaviour {

		public static final int MAX_ATTEMPT = 3;

		private static final long serialVersionUID = 1L;
		private DFAgentDescription dfd = new DFAgentDescription();
		int attempt = 0;
		int step = 0;

		@Override
		public void action() {
			// Search with the DF for the name of the SA
			if (attempt == MAX_ATTEMPT) {
				tryToMoveAgent();
				attempt = 0;
			}
			switch (step) {
			case 0:
				final ServiceDescription sd = new ServiceDescription();
				sd.setType(SA.AGENT_TYPE);
				try {
					sd.setName(getContainerController().getContainerName());
				} catch (final ControllerException e) {
					System.err.printf(
							"%s cannot get container name. Reason: %s\n",
							getLocalName(), e.getMessage());
					++attempt;
					break;
				}
				dfd.addServices(sd);
				attempt = 0;
				++step;
				break;
			case 1:
				try {
					System.out.printf(
							"%s waiting for an SA registering with the DF\n",
							getLocalName());
					final SearchConstraints c = new SearchConstraints();
					c.setMaxDepth(3L);
					final DFAgentDescription[] result = DFService.search(
							DA.this, dfd, c);
					if ((result != null) && (result.length > 0)) {
						dfd = result[0];
						searchAgent = dfd.getName();
					} else {
						++attempt;
						break;
					}
				} catch (final Exception fe) {
					fe.printStackTrace();
					System.err
							.printf("%s search with DF is not succeeded because of %s\n",
									getLocalName(), fe.getMessage());
					++attempt;
					break;
				}
				System.out.printf("%s found %s Search Agent\n", getLocalName(),
						searchAgent.getLocalName());
				++step;
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
					else if (msg.getPerformative() == ACLMessage.REFUSE)
						handleRefuseMsg(msg);
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

		private void handleRefuseMsg(final ACLMessage msg) {
			// REFUSE message received. Repeat query.
			System.out
					.printf("DIPRE Agent %s received REFUSE message from %s with content: %s\n",
							getLocalName(), msg.getSender().getLocalName(),
							msg.getContent());
			step = 0;
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
		// Relation relation = relations.remove(0); // TODO: temporary remove
		Relation relation = relations.get(0);
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
