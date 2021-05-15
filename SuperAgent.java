import java.beans.PropertyChangeListener;
/*
 *  SuperAgent is an Agent with a PropertyChangeListener 
 *  for use in Agent Based Modeling
 *  @author Chad Holmes
 *
 *  For MIT EM.426 Spring 2021 class
 *  
 *  The SuperAgent class ... is an Super Agent!
 *  All skills, full efficiency, highest capacity. He/She can do anything!
 *  
 */
public class SuperAgent extends Agent implements PropertyChangeListener {

	/*
	 * Constructors
	 */
	public SuperAgent() {
		this("SuperAgent");
	}
	
	public SuperAgent(String name) {
		this(name, 100);
	}
	
	public SuperAgent(String name, int efficiency) {
		super(name, efficiency);
		initialize();
	}
	
	/* 
	 * Helper functions
	 */
	private void initialize() {
		// assign supplies to Agent
	  
    	// create skills with random capacity 
		Supply skill_develop = new Supply("develop", SupplyType.SKILL1, 999, SupplyQuality.HIGH);
		Supply skill_analyze = new Supply("analyze", SupplyType.SKILL2, 999, SupplyQuality.HIGH);
		Supply skill_model = new Supply("model", SupplyType.SKILL3, 999, SupplyQuality.HIGH);
		Supply skill_comms = new Supply("communicate", SupplyType.SKILL4, 999, SupplyQuality.HIGH);
		Supply skill_mng = new Supply("manage", SupplyType.SKILL5, 999, SupplyQuality.HIGH);
		
		// add skills to resources list
		this.resources.add(skill_develop);
		this.resources.add(skill_analyze);
		this.resources.add(skill_model);
		this.resources.add(skill_comms);
		this.resources.add(skill_mng);
	}
}
