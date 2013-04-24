import jade.core.Agent;

/**
 * This example shows a minimal agent that just prints "Hallo World!" and then
 * terminates.
 * 
 * @author Giovanni Caire - TILAB
 */
public class HelloWorldAgent extends Agent {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	protected void setup() {
		System.out.println("Hello World! My name is " + getLocalName());

		// Make this agent terminate
		doDelete();
	}
}
