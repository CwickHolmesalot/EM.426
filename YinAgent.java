import java.beans.PropertyChangeListener;
/*
 *  YinAgent is an Agent with a PropertyChangeListener 
 *  for use in Agent Based Modeling
 *  @author Chad Holmes
 *
 *  For MIT EM.426 Spring 2021 class
 *  
 *  The YinAgent class is part of a pair of Agents
 *  YinAgent can only do SKILL3
 *  YinAgent has peak efficiency and limitless capacity to do this
 *  single skill.
 *  
 */
public class YinAgent extends Agent implements PropertyChangeListener {

	/*
	 * Constructors
	 */
	public YinAgent() {
		this("YinAgent");
	}
	
	public YinAgent(String name) {
		this(name, 100);
	}
	
	public YinAgent(String name, int efficiency) {
		super(name, efficiency);
		initialize();
	}
	
	/* 
	 * Helper functions
	 */
	private void initialize() {
		// assign supplies to Agent
	  
    	// create skills with random capacity 
		Supply skill_model = new Supply("model", SupplyType.SKILL3, 999, SupplyQuality.HIGH);
		
		// add skills to resources list
		this.resources.add(skill_model);
	}
}
