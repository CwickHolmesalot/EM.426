import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
/*
 * Agent base class for Agent Based Modeling
 *  @author Chad Holmes
 *
 *  For MIT EM.426 Spring 2021 class
 *  
 *  The Agent class is used as a base class that can be extended for
 *  specific agent definitions. An agent is defined by only a few characteristics:
 *  1. a UUID
 *  2. a name
 *  3. an efficiency (i.e. how effective is the agent at completing work)
 *  4. a list of "supplies," i.e., a set of skills 
 *  
 */
public class Agent implements PropertyChangeListener {

	/* 
	 * Default constructor
	 * set UUID to random id
	 * set default name to ""
	 * set default efficiency to 85 (85%)
	 */
	public Agent() {
		this(UUID.randomUUID(),"", 85);
	}
	public Agent(String name, int efficiency) {
		this(UUID.randomUUID(),name,efficiency);
	}
	
	/*
	 * Full constructor
	 */
	public Agent(UUID id, String name, int efficiency) {
		super();
		this.setId(id);
		this.setName(name);
		this.setEfficiency(efficiency);
		this.setBusy(false);
		
		this.rand = new Random();
		rand.setSeed(43);
		
		this.resources = new ArrayList<Supply>();
	}

	// member variables
	protected UUID id;        // universal unique id
	protected String name;    // agent name
	protected int efficiency; // agent efficiency	
	protected boolean busy;   // is agent occupied?
	
	// keep track of event time since started
	protected int count;

	// manage tasks assigned to agent
	protected Optional<Demand> current_task;
	protected int progress;
	
	// used for random number generation
	protected Random rand;
		
	// list of Supply (resources, talent, skills)
	public ArrayList<Supply> resources;
	
	/*
	 * Agent "interface" functions to be overridden in child class
	 */
	public void start(){
		// to be implemented by child classes
	}
	public void step() {
		// to be implemented by child classes
	}
	public void doSomething(DemandList d) {
		// needs to be overloaded by child classes
	}
	
	/*
	 * PropertyChangeListener interface functions
	 */
	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if(evt.getPropertyName()=="Step") {
			System.out.println("Agent "+this.getName()+": notices time has passed...");
			this.step();
		}
		else if(evt.getPropertyName()=="newdemand") {
			System.out.println("Agent "+this.getName()+": notices a demand has been added to the demand list...");
			this.doSomething((DemandList)(evt.getNewValue()));
		}
	}
	
	/* 
	 * Convenience Functions
	 */
	// return true if Demand can be met with existing Supplies and efficiency
	// i.e., answer the question, "Can I do this?"
	//
	// NOTE: this is a more specific validation than the isDemandValid function in 
	// the SupplyDemandDictionary class
	//
	// ASSUMPTION: demand effort is equal across all supply needs
	public boolean demandValid(Demand d, SupplyDemandDictionary sdd) {
		
		// cursory check: do I have the supplies to meet this demand?
		// TODO: if not, can I put a call out for another Agent who can help?
		if(!sdd.isValidMatch(d.getType(), this.getResources())){
			return false;
		}
		// cycle through supplies
		for (Supply s : this.resources){
			
			System.out.println("..checking against Supply: "+s.getName());
			
			if(sdd.isValidMatch(d.getType(), s)){
			
				System.out.println("...checking against SupplyType "+s.getType().toString());
				
				// use supply amount and compare to demand effort
				if(s.getAmount() >= d.getEffort()) {
					System.out.println("...agent has enough remaining capacity!");
					
					// check if available or expired...
					if(s.isUsable()) {
						System.out.println("...and supply is usable (not expired)!");
						return true;
					}
					else {
						System.out.println("...BUT supply is not usable.");
					}
				}
				else {
				System.out.println("...BUT agent does not have enough remaining capacity. demand effort="+
									String.valueOf(d.getEffort())+
									", agent capacity="+String.valueOf(s.getAmount()));
				}
			}
		}
			
		return false;
	}
	
	// start handling a Demand
	protected void startNewDemand(Demand d) {
		this.setBusy(true);
		this.setProgress(0);
		
		d.setActive(); // mark task as started by an agent
		this.setCurrentTask(Optional.of(d));

		System.out.println("Agent "+this.getName()+" has started task "+d.toString());
	}
	
	// make progress on Demand
	protected void setIncrementalProgress() {
		// update progress
		this.setProgress(this.getProgress()+1);

		// check progress against demand
		if (!this.getCurrentTask().isEmpty()) {
			if(this.getProgress() >= this.getCurrentTask().get().getEffort()) {
				// demand is complete!
				this.finishDemand();
			}
		}
	}
	
	// wrap up a Demand
	protected void finishDemand() {
		// access Demand
		Demand d = this.getCurrentTask().get();
		d.setComplete();
		System.out.println("Agent "+this.getName()+" has completed task "+d.toString());
		
		// clear out variables
		this.setCurrentTask(Optional.empty());
		this.setProgress(-1);
		this.setBusy(false);
	}
	
	/*
	 * Getters and Setters
	 */
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getEfficiency() {
		return efficiency;
	}

	public void setEfficiency(int efficiency) {
		this.efficiency = efficiency;
	}
	
	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public ArrayList<Supply> getResources() {
		return resources;
	}

	public void setResources(ArrayList<Supply> rsrcs) {
		resources.clear();
		resources.addAll(rsrcs);
	}
	
	public boolean isBusy() {
		return busy;
	}
	
	public void setBusy(boolean busy) {
		this.busy = busy;
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
	
	/*
	 * public static void main(String args[]) {
	 * 
	 * // create dictionary for matching skills with needs SupplyDemandDictionary
	 * sdd = new SupplyDemandDictionary();
	 * 
	 * // define what demands and supplies match with each other
	 * sdd.addSupplyDemand(DemandType.NEED1,SupplyType.SKILL1);
	 * sdd.addSupplyDemand(DemandType.NEED2,SupplyType.SKILL2);
	 * sdd.addSupplyDemand(DemandType.NEED2,SupplyType.SKILL3);
	 * sdd.addSupplyDemand(DemandType.NEED2,SupplyType.SKILL4);
	 * sdd.addSupplyDemand(DemandType.NEED3,SupplyType.SKILL3);
	 * sdd.addSupplyDemand(DemandType.NEED4,SupplyType.SKILL1);
	 * sdd.addSupplyDemand(DemandType.NEED4,SupplyType.SKILL4);
	 * 
	 * System.out.println(sdd.toString());
	 * 
	 * // define 3 agents Agent geo = new Agent("Geologist",50); Agent eng = new
	 * Agent("Engineer",75); Agent mgr = new Agent("Manager",25);
	 * 
	 * // give them skills with capacity is in seconds Supply skill_interp = new
	 * Supply("interp", SupplyType.SKILL1, 10, SupplyQuality.HIGH); Supply
	 * skill_assess = new Supply("assess", SupplyType.SKILL2, 20,
	 * SupplyQuality.MEDIUM); Supply skill_lead = new Supply("lead",
	 * SupplyType.SKILL3, 25, SupplyQuality.MEDIUM);
	 * 
	 * // assign skills to agents geo.rsrcs.add(skill_interp);
	 * eng.rsrcs.add(skill_assess); mgr.rsrcs.add(skill_lead);
	 * 
	 * // add them to a list (pooled agent resources) ArrayList<Agent> agentList =
	 * new ArrayList<Agent>(); agentList.add(geo); agentList.add(eng);
	 * agentList.add(mgr);
	 * 
	 * // define some demands Demand demand_interp = new Demand("interp",
	 * DemandPriority.HIGH, DemandType.NEED1, 8); Demand demand_assess = new
	 * Demand("assess", DemandPriority.MEDIUMHIGH, DemandType.NEED2, 10); Demand
	 * demand_lead = new Demand("lead", DemandPriority.MEDIUMLOW, DemandType.NEED3,
	 * 30);
	 * 
	 * // add demands to list ArrayList<Demand> demandList = new
	 * ArrayList<Demand>(); demandList.add(demand_interp);
	 * demandList.add(demand_assess); demandList.add(demand_lead);
	 * 
	 * // READY TO ROCK & ROLL!
	 * 
	 * // TODO: consider Demands that require more than 1 Supply // TODO: add logic
	 * around subordinate Demand checks when determining Agent match
	 * 
	 * // sort Demands by priority Collections.sort(demandList, new
	 * Comparator<Demand>() { public int compare(Demand d1, Demand d2) { int result
	 * = d1.getPriority().compareTo(d2.getPriority()); return -result; // return
	 * reverse order (high priority first) } });
	 * 
	 * // cycle through demand list and find agents to handle them for (Demand d:
	 * demandList) { System.out.println("Found a new Demand: "+d.toString());
	 * 
	 * boolean matched = false; // look for a matching agent for (Agent a:
	 * agentList) {
	 * System.out.println(".looking for match with Agent: "+a.getName());
	 * 
	 * if(a.demandValid(d, sdd)) { String thumb = "\uD83D\uDC4D";
	 * System.out.println(thumb+" Found a match! Agent being assigned Demand: "+
	 * d.toString()+"\n");
	 * 
	 * // TODO: update Agent state, somewhere update Demand matched = true; break; }
	 * } if(matched == false) {
	 * System.out.println("X no agent is able to handle Demand: "+d.toString()+"\n")
	 * ; } } }
	 */
}