import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Random;
/*
 * ScienceAgent is a scientist Agent with a PropertyChangeListener 
 * for use in Agent Based Modeling
 *  @author Chad Holmes
 *
 *  For MIT EM.426 Spring 2021 class
 *  
 *  The ScienceAgent class ...
 *  
 */
public class ScienceAgent extends Agent implements PropertyChangeListener {

	/*
	 * Constructors
	 */
	public ScienceAgent() {
		this("Scientist",85);
	}
	
	public ScienceAgent(String name, int efficiency) {
		super(name, efficiency);
		
		this.setProgress(-1);
		this.setCurrentTask(Optional.empty());
		initialize();
	}
	
	/* 
	 * Member variables
	 */
	// keep track of event time since started
	private int count;
	
	// manage tasks assigned to agent
	private Optional<Demand> current_task;
	private int progress;

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
	
	private void startNewDemand(Demand d) {
		this.setBusy(true);
		this.setCount(0);
		this.setCurrentTask(Optional.of(d));
	}

	@Override
	public void start() {
		
		System.out.println("ScienceAgent::Start "+this.getName());
		
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
				if(this.demandValid(d, dl.getSupplyDemandDict())) {
					System.out.println("ScienceAgent "+this.getName()+" starting a new task! "+d.toString());
					this.startNewDemand(d);
					break;
				}
			}
		}
		else {
			System.out.println("ScienceAgent "+this.getName()+" is too busy to start a new task");	
		}
	}
	@Override
	public void step() {
		this.setCount(this.getCount()+1);
		System.out.println("..current steps: "+this.getCount());
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public Optional<Demand> getCurrentTask() {
		return current_task;
	}

	public void setCurrentTask(Optional<Demand> task) {
		this.current_task = task;
	}
	
	public int getProgress() {
		return progress;
	}

	public void setProgress(int progress) {
		this.progress = progress;
	}
}
