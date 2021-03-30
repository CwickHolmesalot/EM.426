import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Random;
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
		this("Manager",60);
	}
	
	public ManagerAgent(String name, int efficiency) {
		super(name, efficiency);

		this.setProgress(-1);
		this.setCurrentTask(Optional.empty());
		initialize();
	}
	
	/* 
	 * Member variables
	 */

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
	
	@Override
	public void start() {
		
		System.out.println("ManagerAgent::Start "+this.getName());
		
		// set count to 0
		this.setCount(0);
		
		// no event loop here - will respond to time events and choose
		// demands to tackle in response
	}
	
	/*
	 * review demand list for something to do
	 */
	@Override
	public void doSomething(DemandList dl) {
		
		if (!this.isBusy()) {
			for (Demand d : dl.getDemandlist()) {
				// only consider Demands that are unclaimed and not yet complete
				if(d.getState() == DemandState.QUEUED) {
					if(this.demandValid(d, dl.getSupplyDemandDict())) {
						System.out.println("ManagerAgent "+this.getName()+" starting a new task! "+d.toString());
						this.startNewDemand(d);
						break;
					}
					System.out.println("ScienceAgent "+this.getName()+" cannot perform demand: "+d.toString());
				}
			}
		}
		else {
			System.out.println("ManagerAgent "+this.getName()+" is too busy to start a new task");	
		}
	}

	@Override
	public void step() {
		
		boolean flaked = this.getEfficiency()<rand.nextInt(101);
		
		if(!flaked) {
			this.setCount(this.getCount()+1);
			if(this.isBusy()) {
				this.setIncrementalProgress();
			}
		}
		else {
			System.out.println("ManagerAgent "+this.getName()+" stepped out for coffee...");
		}
		System.out.println("..current steps: "+this.getCount());
	}
}
