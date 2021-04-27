import java.beans.PropertyChangeListener;
/*
 *  EngineerAgent is a engineer Agent with a PropertyChangeListener 
 *  for use in Agent Based Modeling
 *  @author Chad Holmes
 *
 *  For MIT EM.426 Spring 2021 class
 *  
 *  The EngineerAgent class ... is an Engineer Agent
 *  
 */
public class EngineerAgent extends Agent implements PropertyChangeListener {

	/*
	 * Constructors
	 */
	public EngineerAgent() {
		this("Engineer");
	}
	
	public EngineerAgent(String name) {
		this(name, 95);
	}
	
	public EngineerAgent(String name, int efficiency) {
		super(name, efficiency);
		initialize();
	}
	
	/* 
	 * Helper functions
	 */
	private void initialize() {
		// assign supplies to Agent
	  
    	// create skills with random capacity 
		Supply skill_analyze = new Supply("analyze", SupplyType.SKILL2, this.rand.nextInt(100), SupplyQuality.HIGH);
		Supply skill_model = new Supply("model", SupplyType.SKILL3, this.rand.nextInt(100), SupplyQuality.MEDIUM);
		Supply skill_comms = new Supply("communicate", SupplyType.SKILL4, this.rand.nextInt(100), SupplyQuality.MEDIUM);
		
		// add skills to resources list
		this.resources.add(skill_analyze);
		this.resources.add(skill_model);
		this.resources.add(skill_comms);
	}
	
//	@Override
//	public void complete() {
//		
//		boolean flaked = (this.getEfficiency() < rand.nextInt(101));
//		
//		if(!flaked) {
//			this.setCount(this.getCount()+1);
//			if(this.isBusy()) {
//				this.setIncrementalProgress();
//			}
//			else if(this.isMissedEvent()){
//				// missed an event!  handle it now
//				this.doSomething(this.missed_event.get());
//				this.resetMissedEvent();
//			}
//		}
//		else {
//			System.out.println("EngineerAgent "+this.getName()+" chased links on Google...");
//		}
//		System.out.println("..current steps: "+this.getCount());
//	}
}
