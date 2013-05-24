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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import search.SA;

import common.Links;
import common.Relation;
import search.Network;

/**
 * DIPRE Agent.
 */
public class DA extends Agent {

	private static final long serialVersionUID = 1L;
	private final int maxRelationsSearchedBeforeMove = 1; // Probably it will
															// be program
															// parameter
	private final int maxLinkLevel = 2;
	private boolean willBeMoved = false;
	private final List<Relation> relationsToSearch = new ArrayList<Relation>();
	private final List<Relation> relationsArchive = new ArrayList<Relation>();
	private final List<Relation> relationsSecondChance = new ArrayList<Relation>();
	private AID searchAgent = new AID();
	private final Codec codec = new SLCodec();
	private final Ontology ontology = MobilityOntology.getInstance();
	private int relationsSearchedAfertMove = 0; // Total number of relations
												// searched
												// in SA after move. Probably
												// will be
												// used to decide, when to move.

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
				relationsToSearch.add(new Relation(elements[0], elements[1]));
			}
		} else {
			System.err.printf(
					"At least one relation is required to start %s.\n",
					getLocalName());
			doDelete();
			return;
		}
		System.out.printf("%s is ready with relations: %s.\n", getLocalName(),
				relationsToSearch.toString());

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
		// Insert all relations, that didn't gave any results
		// in current SA, into the begin of list. They will be checked in
		// new SA.

		relationsToSearch.addAll(0, relationsSecondChance);
		relationsSecondChance.clear();
		searchAgent = null;

	}

	@Override
	protected void afterMove() {
		System.out.printf("%s has moved itself to %s.\n", getLocalName(),
				here().getName());
		// System.out.println("Relacje do wyszukiwania");
		System.out.println("afterMove");
		// Initialize agent
		relationsSearchedAfertMove = 0;
		getContentManager().registerLanguage(codec);
		getContentManager().registerOntology(ontology);
		addBehaviour(new FindSearchAgentBehaviour());
		willBeMoved = false;
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
			System.out.println("FindSearchAgentBehaviour");
			// Search with the DF for the name of the SA
			if (attempt == 3) {
				if (tryToMoveAgent()) {
					willBeMoved = true;
					step = 2;
					return;
				} else {
					attempt = 0;
				}
			}

			switch (step) {
			case 0:
				System.out.println("dupa0");
				final ServiceDescription sd = new ServiceDescription();
				sd.setType(SA.AGENT_TYPE);
				sd.setName(here().getName());
				dfd.addServices(sd);
				System.out.println("dupa0k");
				++step;
				break;
			case 1:
				System.out.println("dupa1");
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
		private static final int stateRequestLinks = 0;
		private static final int stateHandleMsg = 1;
		private static final int stateTryMove = 2;

		private int state = stateRequestLinks;

		@Override
		public void action() {

			if (willBeMoved || searchAgent == null)
				return; // Wait for steady state
			switch (state) {
			case stateRequestLinks:

				if (relationsSearchedAfertMove == maxRelationsSearchedBeforeMove)
					state = stateTryMove;
				else
					requestLinksPortion();
				break;
			case stateHandleMsg:
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
			case stateTryMove:
				if (!willBeMoved && tryToMoveAgent()) {
					willBeMoved = true;
					state = stateRequestLinks;
				}
				return;
			}
		}

		private void requestLinksPortion() {
			// Send the keywords to Search Agent
			final ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
			request.addReceiver(searchAgent);
			final Relation relation = nextRequest();
			if (relation != null) {
				request.setContent(relation.toString());
				send(request);
				relationsSearchedAfertMove++;
				state = stateHandleMsg; // Wait for response

			} else {
				state = stateTryMove; // We should change SA
			}

		}

		private void handleLinksPortion(final ACLMessage msg) {
			// INFORM message received. Process it.
			try {
				if (!msg.getSender().getName().equals(searchAgent.getName())) {
					// TODO to powinno byc docelowo niepotrzebne
					System.out.println("INFO::Recived message from wrong SA");
					return;
				}
				final Links links = (Links) msg.getContentObject();
				// System.out.println(links);
				dipre(links);

			} catch (final UnreadableException e) {
				System.err.printf(
						"%s cannot read INFORM message from %s. Reason: %s\n",
						getLocalName(), msg.getSender().getLocalName(),
						e.getMessage());
			} finally {
				state = stateRequestLinks;
			}
		}

		private void handleRelationsRequest(final ACLMessage msg) {
			// REQUEST message received. Send reply.
			final ACLMessage reply = msg.createReply();
			reply.setPerformative(ACLMessage.INFORM);
			// TODO: send all relations possibly in many messages
			final StringBuilder sb = new StringBuilder();
			sb.append("--Archive:\n");
			for (final Relation r : relationsArchive) {
				sb.append(r.toString());
				sb.append("\n");
			}
			sb.append("--To be searched:\n");
			for (final Relation r : relationsToSearch) {
				sb.append(r.toString());
				sb.append("\n");
			}
			sb.append("--Waiting for a second chance in another SA:\n");
			for (final Relation r : relationsSecondChance) {
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
			state = stateRequestLinks;
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

	protected Relation nextRequest() {
		if (relationsToSearch.isEmpty()) {
			return null;
		} else {
			return relationsToSearch.get(0);
		}
	}

	/**
	 * Method to perform DIPRE algorithm on web pages given in links
	 */
	protected void dipre(Links links) {
		Relation relation = relationsToSearch.remove(0);
		int numOfNewRelations = 0;
		for (final String link : links) {
			final String pattern = getPattern(link, relation);
			if (pattern == null)
				continue;
			// TODO magic number:)
			final Links innerLinks = getInnerLinks(link, maxLinkLevel);
			for (final String innerLink : innerLinks) {
				final List<Relation> newRelations = getNewRelations(innerLink,
						pattern);
				if (newRelations.size() == 0)
					continue;
				numOfNewRelations += newRelations.size();
				relationsToSearch.addAll(newRelations);
				System.out.printf("%s got new relations: %s.\n",
						getLocalName(), newRelations.toString());

			}// for innerLink
		}// for link
		if (numOfNewRelations == 0) // No new relations found in current links
			relationsSecondChance.add(relation);
		else
			relationsArchive.add(relation);

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
		links.add(link);
		return links;
	}

	protected String getPattern(String link, Relation relation) {

		final int minLength = 4;
		final int maxLength = 150;

		String pageContent = null;
		try {
			pageContent = Network.doHttpRequest(link, "GET");
		} catch (IOException e) {

			System.out
					.printf("Warning:: During creation of pattern by %s can not do http request\n",
							getLocalName());
			System.out.printf("Processed link was %s\n", link);
			return null;
		}
		int iFirst = pageContent.indexOf(relation.first, 0);
		while (iFirst != -1) {
			int iSecond = pageContent.indexOf(relation.second, iFirst);
			while (iSecond != -1) {
				if (iSecond - iFirst > maxLength) {
					iSecond = pageContent.indexOf(relation.second, iSecond + 1);
					continue;
				}

				String body = pageContent.substring(
						iFirst + relation.first.length(), iSecond);
				String prefix = pageContent.substring(iFirst - 5, iFirst);
				prefix = prefix.replaceAll("[^A-Za-z0-9 ]", ".");
				body = body.replaceAll("[^A-Za-z0-9 ]", ".");
				String rule = new String(prefix + "([a-zA-Z0-9 ]{" + minLength
						+ "," + maxLength + "})" + body + "([a-zA-Z0-9 ]{"
						+ minLength + "," + maxLength + "})");
				return rule;

			}
			iFirst = pageContent.indexOf(relation.first, iFirst + 1);
		}
		return null;
	}

	protected List<Relation> getNewRelations(String link, String pattern) {
		List<Relation> result = new ArrayList<Relation>();
		String pageContent = null;
		try {
			pageContent = Network.doHttpRequest(link, "GET");
		} catch (IOException e) {
			System.out
					.printf("Warning:: During finding new relations by %s can not do http request\n",
							getLocalName());
			System.out.printf("Processed link was %s\n", link);
			return result;
		}

		Matcher m = Pattern.compile(pattern).matcher(pageContent);
		System.out.println(pattern);
		// System.out.println(pageContent);
		while (m.find()) {
			String left = (String) m.group(1);
			String right = (String) m.group(2);
			Relation newRelation = new Relation(left.trim(), right.trim());
			result.add(newRelation);
		}
		return result;
	}
}
