import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

/*
 * View Agent
 */
public class VA extends Agent {
	private static final long serialVersionUID = 1L;

	private VAGui myGui;

	protected void setup() {
		// Create and show the GUI
		myGui = new VAGui(this);
		myGui.showGui();

		// Add the behaviour receiving messages from DIPRE Agents
		addBehaviour(new ReceiveMessagesBehaviour());
	}

	protected void takeDown() {
		// Close the GUI
		myGui.dispose();
		// Printout a dismissal message
		System.out.println("View Agent " + getLocalName() + " terminating.");
	}

	/**
	 * This is invoked by the GUI
	 */
	public void getRelations(final String name, final boolean isGUID) {
		addBehaviour(new OneShotBehaviour() {
			private static final long serialVersionUID = 1L;

			public void action() {
				// Send the REQUEST to DIPRE Agent
				ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
				request.addReceiver(new AID(name, isGUID));
				request.setContent("Give me your relations.");
				VA.this.send(request);
			}
		});
	}

	/**
	 * Inner class ReceiveMessagesBehaviour
	 */
	private class ReceiveMessagesBehaviour extends CyclicBehaviour {
		private static final long serialVersionUID = 1L;

		public void action() {
			MessageTemplate mt = MessageTemplate
					.MatchPerformative(ACLMessage.INFORM);
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {
				// INFORM Message received. Process it
				myGui.appendRelations(msg.getContent());
			} else {
				block();
			}
		}
	} // End of inner class ReceiveMessagesBehaviour

}
