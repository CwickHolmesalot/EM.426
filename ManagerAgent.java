import java.beans.PropertyChangeListener;
/*
 *  ManagerAgent is a manager Agent with a PropertyChangeListener 
 *  for use in Agent Based Modeling
 *  @author Chad Holmes
 *
 *  For MIT EM.426 Spring 2021 class
 *  
 *  The ManagerAgent class ... is a Manager Agent
 *  
 */

public class ManagerAgent extends Agent implements PropertyChangeListener {

	/*
	 * Constructors
	 */
	public ManagerAgent() {
		this("Manager");
	}
	
	public ManagerAgent(String name) {
		this(name, 60);
	}
	
	public ManagerAgent(String name, int efficiency) {
		super(name, efficiency);
		initialize();
	}
	
	/* 
	 * Helper functions
	 */
	private void initialize() {
		// assign supplies to Agent
	  
    	// create skills with random capacity 
		Supply skill_comms = new Supply("communicate", SupplyType.SKILL4, rand.nextInt(100), SupplyQuality.MEDIUM);
		Supply skill_manage = new Supply("manage", SupplyType.SKILL5, rand.nextInt(100), SupplyQuality.HIGH);
		
		// add skills to resources list
		this.resources.add(skill_comms);
		this.resources.add(skill_manage);
	}
}
