import java.beans.PropertyChangeListener;
/*
 *  ScienceAgent is a scientist Agent with a PropertyChangeListener 
 *  for use in Agent Based Modeling
 *  @author Chad Holmes
 *
 *  For MIT EM.426 Spring 2021 class
 *  
 *  The ScienceAgent class ... is a Scientist Agent
 *  
 */
public class ScienceAgent extends Agent implements PropertyChangeListener {

	/*
	 * Constructors
	 */
	public ScienceAgent() {
		this("Scientist");
	}
	
	public ScienceAgent(String name) {
		this(name, 75);
	}
	
	public ScienceAgent(String name, int efficiency) {
		super(name, efficiency);
		initialize();
	}

	/* 
	 * Helper functions
	 */
	private void initialize() {
		// assign supplies to Agent
	  
    	// create skills with random capacity 
		Supply skill_software = new Supply("software", SupplyType.SKILL1, this.rand.nextInt(100), SupplyQuality.MEDIUM);
		Supply skill_interp   = new Supply("analyze", SupplyType.SKILL2, this.rand.nextInt(100), SupplyQuality.HIGH);
		Supply skill_comms    = new Supply("communicate", SupplyType.SKILL4, this.rand.nextInt(100), SupplyQuality.MEDIUM);
		
		// add skills to resources list
		this.resources.add(skill_software);
		this.resources.add(skill_interp);
		this.resources.add(skill_comms);
	}
}
