import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;

public class TestAgent extends Agent implements PropertyChangeListener {

	public TestAgent() {
		this("MyName",85);
	}
	
	public TestAgent(String name, int efficiency) {
		this(name, efficiency, 999, 5);
	}
	
	public TestAgent(String name, int efficiency, int id, int count) {
		super(name, efficiency);
		this.setCount(count);
		this.setComplete(false);
	}

	private int id;
	private int count;
	private boolean complete;
	
	// Event dispatcher (event here is time)
	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if(evt.getPropertyName()=="Step") {
			System.out.println("Agent "+this.getName()+": notices time has passed...");
			this.step();
		}
	}
	
	/*
	 * start is the main Agent loop where the 
	 * Agent can poll for new demands to meet
	 */
	public void start(DemandList demandlist) {
		
		System.out.println("TestAgent::Start "+this.getName());
		
		// main Agent loop
		//while(!complete) {
			// wait for signal
			
			// signal caught! 
			//demandlist..visit(this); // Visitor pattern
		//}
	}
	
	/*
	 * doSomething is the generic Visitor command
	 * calls Visitor-specific functions
	 */
	public void doSomething(ArrayList<Demand> dlist) {
		// deal with Demands here
		System.out.println("TestAgent::doSomething "+this.getName());
		this.step();
	}
	
	public void step() {

		System.out.println("TestAgent::step "+this.getName());
		if(!this.isComplete()) {
			this.setCount(this.getCount()-1);
			if(this.getCount() <= 0) {
				this.setComplete(true);
			}
		}
		else {
			System.out.println("xxx TestAgent has expired");
		}

		System.out.println("..remaining steps: "+this.getCount());
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public boolean isComplete() {
		return complete;
	}

	public void setComplete(boolean complete) {
		this.complete = complete;
	}
	
//	public static void main(String args[]) {
//		// define 3 agents
//		TestAgent geo = new TestAgent("Geologist",50);
//		TestAgent eng = new TestAgent("Engineer",75);
//		TestAgent mgr = new TestAgent("Manager",25);
//		
//		// give them skills with capacity is in seconds
//		Supply skill_interp = new Supply("interp", SupplyType.SKILL1, 10, SupplyQuality.HIGH);
//		Supply skill_assess = new Supply("assess", SupplyType.SKILL2, 20, SupplyQuality.MEDIUM);
//		Supply skill_lead   = new Supply("lead",   SupplyType.SKILL3, 25, SupplyQuality.MEDIUM);
//		
//		// assign skills to agents
//		geo.rsrcs.add(skill_interp);
//		eng.rsrcs.add(skill_assess);
//		mgr.rsrcs.add(skill_lead);
//		
//		// add them to a list (pooled agent resources)
//		ArrayList<Agent> agentList = new ArrayList<Agent>();
//		agentList.add(geo);
//		agentList.add(eng);
//		agentList.add(mgr);
//		
//		for (Agent a: agentList) {
//			System.out.println(a.toString());
//		}
//	}
}
