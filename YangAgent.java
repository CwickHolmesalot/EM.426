import java.beans.PropertyChangeListener;
/*
 *  YangAgent is an Agent with a PropertyChangeListener 
 *  for use in Agent Based Modeling
 *  @author Chad Holmes
 *
 *  For MIT EM.426 Spring 2021 class
 *  
 *  The YangAgent class is part of a pair of Agents
 *  YangAgent can only do SKILL2
 *  YangAgent has peak efficiency and limitless capacity to do this
 *  single skill.
 *  
 */
public class YangAgent extends Agent implements PropertyChangeListener {

	/*
	 * Constructors
	 */
	public YangAgent() {
		this("YangAgent");
	}
	
	public YangAgent(String name) {
		this(name, 100);
	}
	
	public YangAgent(String name, int efficiency) {
		super(name, efficiency);
		initialize();
	}
	
	/* 
	 * Helper functions
	 */
	private void initialize() {
		// assign supplies to Agent
	  
    	// create skills with random capacity 
		Supply skill_analyze = new Supply("analyze", SupplyType.SKILL2, 999, SupplyQuality.HIGH);
		
		// add skills to resources list
		this.resources.add(skill_analyze);
	}
}
