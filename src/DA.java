import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import jade.wrapper.ControllerException;

import java.util.ArrayList;

public class DA extends Agent {
	private static final long serialVersionUID = 1L;

	private ArrayList<String> relations = new ArrayList<String>();
	private AID searchAgent = new AID();

	protected void setup() {
		// Get the relations as a start-up argument
		Object[] args = getArguments();
		if (args != null) {
			for (Object o : args) {
				relations.add(o.toString());
			}
		}
		System.out.println("DIPRE Agent " + getLocalName()
				+ " is ready with relations:");
		for (String s : relations) {
			System.out.println(s);
		}

		// Search with the DF for the name of the SA
		DFAgentDescription dfd = new DFAgentDescription();
		ServiceDescription sd = new ServiceDescription();
		sd.setType("SA");
		try {
			sd.setName(getContainerController().getContainerName());
		} catch (ControllerException e) {
			System.err.println(getLocalName()
					+ " cannot get container name. Reason: " + e.getMessage());
			doDelete();
		}
		dfd.addServices(sd);
		try {
			while (true) {
				System.out.println(getLocalName()
						+ " waiting for an SA registering with the DF");
				SearchConstraints c = new SearchConstraints();
				c.setMaxDepth(new Long(3));
				DFAgentDescription[] result = DFService.search(this, dfd, c);
				if ((result != null) && (result.length > 0)) {
					dfd = result[0];
					searchAgent = dfd.getName();
					break;
				}
				Thread.sleep(10000);
			}
		} catch (Exception fe) {
			fe.printStackTrace();
			System.err.println(getLocalName()
					+ " search with DF is not succeeded because of "
					+ fe.getMessage());
			doDelete();
		}
		System.out.println(getLocalName() + " found "
				+ searchAgent.getLocalName() + " Search Agent.");

		addBehaviour(new CommunicationBehaviour());

		addBehaviour(new TickerBehaviour(this, 10000) {
			private static final long serialVersionUID = 1L;

			protected void onTick() {
				System.out.println(getLocalName() + " still alive.");
			}
		});
	}

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

		private int step = 0;

		public void action() {
			switch (step) {
			case 0:
				// Send the keywords to Search Agent
				ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
				request.addReceiver(searchAgent);
				request.setContent(nextRequest());
				myAgent.send(request);
				step = 1;
				break;
			case 1:
				// Receive results from Search Agent
				MessageTemplate mt = MessageTemplate
						.MatchPerformative(ACLMessage.INFORM);
				ACLMessage msg = myAgent.receive(mt);
				if (msg != null) {
					// INFORM message received. Process it
					try {
						@SuppressWarnings("unchecked")
						ArrayList<String> links = (ArrayList<String>) msg
								.getContentObject();
						processLinks(links);
					} catch (UnreadableException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					step = 0;
				} else {
					block();
				}
				break;
			}
		}
	} // End of inner class CommunicationBehaviour

	protected String nextRequest() {
		// TODO: implement choosing next relation to query Search Agent
		if (relations.isEmpty())
			return "relation";
		else
			return relations.get(0);
	}

	protected void processLinks(ArrayList<String> links) {
		// TODO: implement link processing
		System.out.println(getLocalName() + " received links:");
		for (String l : links) {
			System.out.println(l);
		}
	}

}
