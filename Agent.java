import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import javafx.util.Pair;
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
		this.setAgentTime(0);
		this.setHorizonTime(50);
		this.setCurrenttask(Optional.empty());
		this.rand = new Random();
		rand.setSeed(43);
		
		this.resources = new ArrayList<Supply>();
		
		this.seen_tasks = new ArrayList<UUID>();
		this.committed_tasks = new ArrayList<Demand>();
		this.completed_tasks = new ArrayList<Demand>();
		this.ledger = new Hashtable<UUID, Pair<Integer,ArrayList<SupplyImage>>>();
		this.backlog_tasks = new ArrayList<Demand>();
		
		this.setSupplyDemandDictionary(Optional.empty());
		
		// PropertyChangeListener set-up
		this.support = new PropertyChangeSupport(this);
 	}

	// member variables
	protected UUID id;        // universal unique id
	protected String name;    // agent name
	protected int efficiency; // agent efficiency
	
	protected AgentState state; // agent state

	// list of Supply (resources, talent, skills)
	protected ArrayList<Supply> resources;
	
	// track considered demands
	protected ArrayList<UUID> seen_tasks;
	
	// task lists
	protected ArrayList<Demand> committed_tasks; // officially completed and committed
	protected ArrayList<Demand> completed_tasks; // completed but not committed
	protected ArrayList<Demand> backlog_tasks;   // tasks not yet performed
	
	// look-up table for task completion times
	protected Hashtable<UUID,Pair<Integer,ArrayList<SupplyImage>>> ledger; 
	protected Optional<Demand> current_task;      // current active task
		
	// current "time" according to the agent
	protected int agentTime;
	
	// time tolerance for "complete ahead" in one go
	// trades off with risk of roll-backs
	protected int horizonTime;
	
	// image of the Supplies before completing last task
	//protected ArrayList<SupplyImage> supplyimgs;
	
	public int getHorizonTime() {
		return horizonTime;
	}
	
	public void setHorizonTime(int horizonTime) {
		this.horizonTime = horizonTime;
	}

	// used for random number generation
	protected Random rand;

	// used for messaging with DemandList
	protected PropertyChangeSupport support;
	
	// use for lookups
	protected Optional<SupplyDemandDictionary> sd_dict;

	/*
	 * Agent "interface" functions to be overridden in child class
	 */
	public void start(){
		// child Agent class functionality can override this function
		System.out.println("Agent::Start "+this.getName());
	}
	
	public void doSomething(PropertyChangeEvent evt) {

		// child Agent class functionality can override this function
		if(evt.getPropertyName()=="newdemand") {
			//System.out.println(this.getName()+": alerted that a new demand has been issued...");

			Demand d = (Demand)(evt.getNewValue());
			
			// only consider Demands that are not active or complete
			if(d.getState() == DemandState.QUEUED) {
				
				// have I already considered this demand?
				if (!this.alreadyConsidered(d)) {
					
					// check demand match
 					if(this.isDemandAchieveableANY(d, false)) {
						this.addToBacklog(d);
						System.out.println(this.getName()+" added demand to backlog");
					}
 					else {
 						System.out.println(this.getName()+" cannot perform demand: "+d.toString());
 					}
 					
 					// mark as seen
 					this.seen_tasks.add(d.getId());
				}
			}
		}
		else if (evt.getPropertyName() == "collaborate") {
			
			//TODO check if I'm a worthy collaborator
			
			//TODO if so, do I need to rollback?  
			
			//TODO after rollback, proceed to executing collaboration Demand
			
		}
		else if (evt.getPropertyName()=="commituntil") {
			// commit any completed tasks up until timestamp provided
			this._updateTaskLists((Integer)evt.getNewValue());
		}
		else {
			System.err.println(this.getName()+" does not know how to manage event "+evt.getPropertyName());
		}
	}
	
	protected void _updateTaskLists(int committime) {
		
		// cycle through completed tasks
	    Iterator<Demand> itr = completed_tasks.iterator();
        while (itr.hasNext()) {
        	Demand d = (Demand)itr.next();
        	
        	// if tasks were completed before new global time
        	// mark as officially committed
			if(ledger.get(d.getId()).getKey() <= committime) {
				// move demand to committed_tasks
				committed_tasks.add(d);
				// remove from completed_tasks
				itr.remove();
			}
        }
	}
	
	protected void _rollBack(Demand d) {
		// roll back progress to just before completing Demand d
	
		// NOTE: assumes rollback Demand is last demand completed!
		// TODO: MANAGE ROLL-BACK OF MULTIPLE TASKS
		
		if(this.completed_tasks.contains(d)) {
			
			// remove from completed task list
			this.completed_tasks.remove(d);
			
			// roll back Supply impacts
			Pair<Integer,ArrayList<SupplyImage>> p = ledger.get(d.getId());
			for (SupplyImage si : p.getValue()) {
				
				// find matching Supply
				for (Supply s : this.resources) {
					if(s.getId() == si.id) {
						
						// reset "image" of Supply
						si.ReimageSupply(s);
					}
				}
			}
			
			// reset local time by adding back time expended
			this.setAgentTime(this.getAgentTime()-p.getKey());
			
			// remove from ledger
			ledger.remove(d.getId());
			
			// add back to backlog
			this.addToBacklog(d);
		}
	}

	/*
	 * PropertyChangeListener interface functions
	 */
	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		this.doSomething(evt);
	}
	
	/* 
	 * Convenience Functions
	 */
	public boolean alreadyConsidered(Demand d) {
		return this.seen_tasks.contains(d.getId());
	}
	
	public void addToBacklog(Demand d) {
		this.backlog_tasks.add(d);
	}
	
	public void addPropertyChangeListener(PropertyChangeListener pcl) {
		this.support.addPropertyChangeListener(pcl);
	}
	
	/*
	 * single function to check for any supply/demand matches
	 * or check if demand can be accomplished immediately
	 * 
	 */
	public boolean isDemandAchieveableANY(Demand d, boolean checkamt) {
		
		// get list of supplies required
		if(this.sd_dict.isEmpty())
			return false;
		ArrayList<Pair<SupplyType,SupplyQuality>> reqsupp = this.getSupplyDemandDictionary().get().getRequiredSupplies(d.getType());

		boolean matchedone = false;
		// consider required supplies
		for (Pair<SupplyType,SupplyQuality> p : reqsupp) {

			for (Supply s: this.resources){ // match to supplies on-hand
				
				if (s.getType() == p.getKey()) { // type matches
					
					if(s.getQuality().ordinal() >= p.getValue().ordinal()){ // quality is good enough

						// deep check considers if enough supply is present
						if (checkamt == true) {
							
							if (s.isUsable()) {// non-expired and replenishment checked
							
								if(s.getAmount() >= d.getEffort()) {
									// found a match
									matchedone = true;
									break;
								}
							}
						}
						// shallow check only cares if matching supplies are present
						else {
							// found a match
							matchedone = true;
							break;
						}
					}
				}
			}
			
			if(matchedone) break;
		}
		
		return matchedone;
	}
	
	/*
	 * single function to check for all supply/demand matches
	 * or check if demand can be accomplished immediately
	 * 
	 */
	public boolean isDemandAchieveableALL(Demand d, boolean checkamt) {
		
		// get list of supplies required
		if(this.sd_dict.isEmpty())
			return false;
		ArrayList<Pair<SupplyType,SupplyQuality>> reqsupp = this.getSupplyDemandDictionary().get().getRequiredSupplies(d.getType());
		
		// consider required supplies
		boolean matchedall = true;
		for (Pair<SupplyType,SupplyQuality> p : reqsupp) {

			boolean matchedone = false;
			for (Supply s: this.resources){ // match to supplies on-hand
				
				if (s.getType() == p.getKey()) { // type matches
				
					if(s.getQuality().ordinal() >= p.getValue().ordinal()){ // quality is good enough

						// deep check considers if enough supply is present
						if (checkamt == true) {
							
							if (s.isUsable()) {// non-expired and replenishment checked
							
								if(s.getAmount() >= d.getEffort()) {
									// found a match
									matchedone = true;
									break;
								}
							}
						}
						// shallow check only cares if matching supplies are present
						else {
							// found a match
							matchedone = true;
							break;
						}
					}
				}
			}
			
			matchedall &= matchedone;	
			if (!matchedall) {
				break;	
			}
		}
		
		return matchedall;
	}
	
	/*
	 * Update amount of remaining supply after completing Demand
	 * 
	 */
	protected boolean _expendEffort(Demand d, int effort, ArrayList<SupplyImage> snapshot) {
		
		// get list of supplies required
		ArrayList<Pair<SupplyType,SupplyQuality>> reqsupp = 
				this.getSupplyDemandDictionary().get().getRequiredSupplies(d.getType());
		
		boolean success = true;
		
		// consider all required supplies
		for (Pair<SupplyType,SupplyQuality> p : reqsupp) {

			// cycle through all of this agent's supplies
			for (Supply s: this.resources){
				
				// match on type and quality
				if ((s.getType() == p.getKey()) && (s.getQuality().ordinal() >= p.getValue().ordinal())) {
					
					// found case where random efficiency issues resulted in too much effort!
					if(s.getAmount() < effort) {
						success = false;
					}
					
					// add a record of current Supply state (for potential roll-back)
					snapshot.add(new SupplyImage(s));
					
					// expend actual effort (which is >= amount in Demand d)
					s.reduceAmount(effort);
					break;
				}
			}
		}	
		
		// return actual effort expended
		return success;
	}
	
	// use backlog to select next task
	protected boolean completeNextTask() {
		if(!this.backlog_tasks.isEmpty()) {
			
			// TODO: use alternate selection criteria on backlog
			
	        // sort demands by priority
	    	Collections.sort(this.backlog_tasks, new Comparator<Demand>() {
	    		public int compare(Demand d1, Demand d2) {
	    			int result = d1.getPriority().compareTo(d2.getPriority());
	    			return -result; // return reverse order (high priority first)
	    		}
	    	});
			
			this.setCurrenttask(Optional.of(this.backlog_tasks.get(0)));
			return this.completeTask();
		}
		return true;
	}
	
	// start handling a Demand
	protected boolean completeTask() {
		
		// fail fast
		if(this.getCurrenttask().isEmpty()) {
			return false;
		}
		
		Demand d = this.getCurrenttask().get();
		d.setActive(); // mark task as started by an agent
		
		this.setCurrenttask(Optional.of(d));
		System.out.println("Agent "+this.getName()+" has started task "+d.toString());
		
		// RANDOMNESS/UNCERTAINTY
		// leverage efficiency to represent un-productive time
		int totaleffort = 0;
		for (int i = 0; i < d.getEffort(); totaleffort++, i++) {
			if (this.getEfficiency() < rand.nextInt(101)) totaleffort++; // add extra effort randomly 
		}
		
		ArrayList<SupplyImage> snapshot = new ArrayList<SupplyImage>();
		
		// update resources based on demand
		if(this._expendEffort(d, totaleffort,snapshot)) {		
			// move demand to cleared list
			this.backlog_tasks.remove(d);
			this.completed_tasks.add(d);
			this.ledger.put(d.getId(),new Pair<Integer,ArrayList<SupplyImage>>(totaleffort,snapshot));
			this.setCurrenttask(Optional.empty());
		}
		else {
			System.out.println("...failed to complete task");
			return false;
		}

		System.out.println("...time needed to complete task: "+totaleffort);
		return true;
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
	
	public ArrayList<Demand> getCompletedtasks() {
		return completed_tasks;
	}
	
	public int getAgentTime() {
		return agentTime;
	}
	
	public void setAgentTime(int agentTime) {
		this.agentTime = agentTime;
	}
	
	public Optional<Demand> getCurrenttask() {
		return current_task;
	}
	
	public void setCurrenttask(Optional<Demand> current_task) {
		this.current_task = current_task;
	}
	
	public Optional<SupplyDemandDictionary> getSupplyDemandDictionary() {
		return sd_dict;
	}
	
	public void setSupplyDemandDictionary(SupplyDemandDictionary sdd) {
		this.sd_dict= Optional.of(sdd);
	}
	
	public void setSupplyDemandDictionary(Optional<SupplyDemandDictionary> sd_dict) {
		this.sd_dict = sd_dict;
	}
	
	public AgentState getState() {
		return state;
	}
	
	public void setState(AgentState state) {
		this.state = state;
	}
}