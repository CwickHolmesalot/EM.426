import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import javafx.beans.property.SimpleIntegerProperty;
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

	public static final int SYNC_BACKLOG_EVERY = 50;    // how often to re-check demand list
	public static final int INCOMPLETE_TASK_PENALTY = 8; // 1 work day (in hours)
	public static final int AGENT_COLLAB_GAP_THRESHOLD = 5; // amount of time difference allowed without rollback
	
	public static enum TASK_RESULTS { FAIL, SUCCESS, SKIP_COMPLETE, SKIP_INCOMPLETE, SKIP_TOOEARLY};
	
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
		this.setWaitTime(0);
		this.setCurrenttask(Optional.empty());
		this.setMaxWaitCycles(5);
		this.setState(AgentState.UNAVAILABLE);
		this.rand = new Random();
		rand.setSeed(43);
		
		this.resources = new ArrayList<Supply>();

		this.committed_tasks = new ArrayList<Demand>();
		this.completed_tasks = new ArrayList<Demand>();
		this.abandoned_tasks = new ArrayList<Demand>();
		this.backlog_tasks = new ArrayList<Demand>();
		this.ledger = new Hashtable<UUID, LedgerEntry>();
		this.interactions = new Hashtable<String, Integer>();
		this.cycles_since_sync = 0;
		this.setSupplyDemandDictionary(Optional.empty());
		this.backup = Optional.empty();
		this.setInteraxLearningRate(0.85);
		
		// PropertyChangeListener set-up
		this.support = new PropertyChangeSupport(this);
 	}

	// member variables meant to be exposed for UI purposes
	private final SimpleIntegerProperty max_wait_cycles = new SimpleIntegerProperty();
	
	// member variables
	protected UUID id;        // universal unique id
	protected String name;    // agent name
	protected int efficiency; // agent efficiency
	
	protected AgentState state; // agent state

	// list of Supply (resources, talent, skills)
	protected ArrayList<Supply> resources;
	
	// track considered demands
	private int cycles_since_sync;
	
	// track urgent requests
	private int n_urgent;
	private Optional<Demand> backup; // placeholder if interrupted mid-wait

	// task lists
	protected ArrayList<Demand> committed_tasks; // officially completed and committed
	protected ArrayList<Demand> completed_tasks; // completed but not committed
	protected ArrayList<Demand> abandoned_tasks; // failed to complete (no collaboration)
	protected ArrayList<Demand> backlog_tasks;   // tasks not yet performed
	
	// look-up table for task completion times
	protected Hashtable<UUID,LedgerEntry> ledger; 
	protected Optional<Demand> current_task;      // current active task
	
	// look-up table for agent interactions
	//protected Hashtable<UUID,Integer> interactions; 
	protected Hashtable<String,Integer> interactions; 
		
	// current "time" according to the agent
	protected int agentTime;
	
	// cumulative time spent waiting
	protected int waitTime;
	
	// tracks steps/cycles not in Active state
	protected int stepsSinceLastActive;

	// used for random number generation
	protected Random rand;

	// used for messaging with DemandList
	protected PropertyChangeSupport support;
	
	// use for lookups
	protected Optional<SupplyDemandDictionary> sd_dict;
	
	// factor for reducing collaboration time
	protected double interax_learning_rate;

	/*
	 * Agent "interface" functions to be overridden in child class
	 */
	public void start(DemandList dl){
		
		// add tasks to backlog
		refreshBacklog(dl, false); //true);
		
		// child Agent class functionality can override this function
		this.setState(AgentState.ACTIVE);
		System.out.println("Agent::Start "+this.getName());
	}
	
	public void refreshBacklog(DemandList dl) {
		refreshBacklog(dl, false);
	}
	public void refreshBacklog(DemandList dl, boolean verbose) {
		for (Demand d: dl.getDemandlist()) {
			DemandState s = d.getState();
			if(s == DemandState.COMPLETE) {
				// skip completed demands
				continue;
			}
			else if(s == DemandState.INCOMPLETE && 
					this.abandoned_tasks.contains(d)) {
				// skip demands that *i* abandoned earlier
				continue;
			}
			else if(this.backlog_tasks.contains(d)) {
				// already on my list!
				continue;
			}
			// check demand match
			if(this.isDemandAchieveableANY(d, false)) {
				this.addToBacklog(d);
				
				if(verbose)
					System.out.println(this.getName()+" added "+d.getName()+" to backlog");
			}
		}
	}

	public void doSomething(PropertyChangeEvent evt) {
		doSomething(evt, false);
	}
	public void doSomething(PropertyChangeEvent evt, boolean verbose) {
		
		// child Agent class functionality can override this function
		if(evt.getPropertyName()=="newdemand") {
			
			//System.out.println(this.getName()+": alerted that a new demand has been issued...");
			Demand d = (Demand)(evt.getNewValue());
			
			// only consider Demands that are not active, partial (handled by collaboration), or complete
			if(d.getState() == DemandState.QUEUED) {
				
				// is this a collaboration demand I created 
				if (!((d.getType()==DemandType.COLLABORATE) && (d.creator.get() == this))) {
					// !(this.alreadyConsidered(d))) {
					
					// check demand match
 					if(this.isDemandAchieveableANY(d, false)) {
						this.addToBacklog(d);
						
						if(verbose)
							System.out.println(this.getName()+" added demand to backlog: "+d.toString());
						
						// keep track of urgent requests
						if(d.getPriority() == DemandPriority.URGENT) {
							this.numUrgentPlusOne();
						}
					}
 					else {
 						// quietly fall through
 						//System.out.println(this.getName()+" cannot perform demand: "+d.toString());
 					}
				}
				else if(this.getState() != AgentState.WAITING) {
					// forgot I was waiting for help!  
					this.setState(AgentState.WAITING);
					this.setCurrenttask(d.ancillaryDemand);
				}
			}
		}
		else if (evt.getPropertyName()=="commituntil") {
			// commit any completed tasks up until time provided
			this._updateTaskLists((Integer)evt.getNewValue());
		}
		else if (evt.getPropertyName()=="finishtask") {
			// if Agent is in ACTIVE state, complete a task
			if(this.getState() == AgentState.ACTIVE) {
				this.cycles_since_sync++;
				this.completeNextTask();
				
				if(this.cycles_since_sync >= Agent.SYNC_BACKLOG_EVERY) {
					// flag that it's time for me to refresh my backlog
					this.support.firePropertyChange("refresh_backlog",0,this);
				}
			}
			// manage WAITING when an urgent request has been seen
			else if(this.getState() == AgentState.WAITING && this.getNumUrgent() > 0) {
				// handle urgent tasks
				this.cycles_since_sync++;
				this.backup = this.getCurrenttask();
				
				if(this.completeNextTask(true)) {
					// handled urgent request
					this.numUrgentMinusOne();
				}
				else {
					System.err.println(this.getName()+" unable to pivot to collaborate. Continuing to wait...");
				}

				// restore current task
    			this.setCurrenttask(backup);
			}
			else {
				this.updateStepsSinceLastActive();
				if(this.getStepsSinceLastActive() >= this.getMaxWaitCycles().get()) {
					System.err.println("-E "+this.getName()+" found no collaborators. "
							+ "Marking demand incomplete and moving on.");
					
					// give up on Waiting and leave Demand unfinished for now
					this.releaseWait();
				}
			}
		}
		else {
			System.err.println(this.getName()+" does not know how to manage event "+evt.getPropertyName());
		}
	}
	
	// track interactions with a collaborating Agent
	protected void updateInteractions(Agent collaborator) {
		//int prevval = this.interactions.getOrDefault(collaborator.getId(), 0);
		//this.interactions.put(collaborator.getId(),prevval+1);
		int prevval = this.interactions.getOrDefault(collaborator.getName(), 0);
		this.interactions.put(collaborator.getName(),prevval+1);
	}
	
	// manage timeout on waiting for collaboration
	protected void releaseWait() {
		
		Demand d = this.getCurrenttask().get();
		
		// leave Demand as incomplete
		d.setIncomplete();
		
		// log shared Demand and Collaboration Demand
		this.backlog_tasks.remove(d);
		this.abandoned_tasks.add(d);
		
		// move local time forward from collaboration
		this.updateAgentTime(Agent.INCOMPLETE_TASK_PENALTY*this.getMaxWaitCycles().get());
		this.updateWaitTime(Agent.INCOMPLETE_TASK_PENALTY*this.getMaxWaitCycles().get());
		
		// reset Agent
		this.setCurrenttask(Optional.empty());
		this.setState(AgentState.ACTIVE);
	}
	
	protected void finishCollaboration(Agent collaborator, Demand d, int collab_effort) {
		
		// mark a collaboration
		this.updateInteractions(collaborator);
		
		// mark Demand as complete
		d.ancillaryDemand.get().setComplete();
		d.setComplete();
		
		// log shared Demand and Collaboration Demand
		this.backlog_tasks.remove(d.ancillaryDemand.get());
		this.completed_tasks.add(d.ancillaryDemand.get());
		this.completed_tasks.add(d);
		
		// add collaboration to ledger
		this.ledger.put(d.getId(), 
				new LedgerEntry(this.getAgentTime(),
								collab_effort,
								new ArrayList<SupplyImage>()));
		
		// was there excess wait time?
		int wait = 0;
		try {
			// wait time is non-negative
			wait = Math.max(0, collaborator.getTimeStartedOn(d.ancillaryDemand.get())-this.getAgentTime());
		}
		catch (Exception e) {
			e.printStackTrace();
			wait = 0;
		}
		
		// move local time forward from collaboration
		this.updateAgentTime(collab_effort+wait);
		this.updateWaitTime(wait);

		// reset Agent
		this.setCurrenttask(Optional.empty());
		this.setState(AgentState.ACTIVE);
		collaborator.setState(AgentState.ACTIVE);
	}
	
	// commit any completed tasks that were completed before commit time
	protected void _updateTaskLists(int committime) {
		
		// cycle through completed tasks
	    Iterator<Demand> itr = completed_tasks.iterator();
        while (itr.hasNext()) {
        	Demand d = (Demand)itr.next();
        	
        	// if tasks were completed before new global time
        	// mark as officially committed
			if(ledger.get(d.getId()).getTimeAtFinish() <= committime) {
				// move demand to committed_tasks
				committed_tasks.add(d);
				// remove from completed_tasks
				itr.remove();
			}
        }
	}

	protected void _rollBack() {
		// roll back last completed Demand
		_rollBack(this.completed_tasks.get(this.completed_tasks.size()-1),true);
	}
	protected void _rollBack(Demand d) {
		// roll back Demand, assume completed already
		_rollBack(d, true);
	}
	protected void _rollBack(Demand d, boolean isCompleted) {
		
		// roll back progress to just before completing Demand d
		// NOTE: assumes rollback Demand is last demand completed!
		if(isCompleted) {
			if(this.completed_tasks.contains(d)) {
				// remove from completed task list
				this.completed_tasks.remove(d);
			}
			else {
				// false start - nothing to roll-back
				return;
			}
		}
		
		// roll-back Supply usage
		LedgerEntry p = ledger.get(d.getId());
		for (SupplyImage si : p.getSi()) {

			// find matching Supply
			for (Supply s : this.resources) {
				if(s.getId() == si.id) {
					
					// reset "image" of Supply
					si.ReimageSupply(s);
					break;
				}
			}
		}
		
		if(isCompleted) {
			// reset local time
			this.setAgentTime(p.getTimeAtStart());
			
			// reset demand to not completed
			d.setActive();
			
			// add back to backlog
			this.addToBacklog(d);
		}
		else if(this.getState() == AgentState.WAITING){
			// reset local time
			this.setAgentTime(p.getTimeAtStart());
		}
		
		// remove from ledger
		ledger.remove(d.getId());
	}
	
	protected void _partialRollBack(int timecap) {
		
		/* 
		 * Only comes up when Agent is asked to rollback 
		 * 1) performs rollback of Demand
		 * 2) marks Demand as REPLACED to keep others from
		 * collaborating on it. 
		 * 3) Clones demand as a new demand with different UID
		 * 4) Completes demand with a cap on effort
		 */
		
		Demand lastdemand;
		
		// which demand?  try backup first
		if(!this.backup.isEmpty()) {
			lastdemand = this.backup.get();
		}
		// grab last completed demand instead
		else if(!this.completed_tasks.isEmpty()) {
			lastdemand = this.completed_tasks.get(this.completed_tasks.size()-1);
		}
		else {
			// don't know what demand to rollback!
			return;
		}

		// 1) full rollback of demand
		_rollBack(lastdemand, lastdemand.getState()==DemandState.COMPLETE);
		
		// 2) update demand state to warn others
		lastdemand.setState(DemandState.REPLACED);
		
		// 3) make a clone for use now
		Demand newdemand = lastdemand.clone();
		newdemand.setState(DemandState.QUEUED); // wards against being screened out by completeTask
		System.out.println("+D cloning demand after rollback: "+newdemand.toString());
		
		// add clone to backlog
		this.backlog_tasks.add(newdemand);
		
		// 4) perform work on clone with limited time
		this.partialCompleteTask(newdemand,timecap);
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
		
		ArrayList<Pair<SupplyType,SupplyQuality>> reqsupp;
		if(d.getType()==DemandType.COLLABORATE) {
			reqsupp = this.getSupplyDemandDictionary().get().getRequiredSupplies(d.ancillaryDemand.get().getType());
		}
		else {
			reqsupp = this.getSupplyDemandDictionary().get().getRequiredSupplies(d.getType());
		}
		
		boolean matchedone = false;
		
		if(reqsupp != null) {
			// consider required supplies
			for (Pair<SupplyType,SupplyQuality> p : reqsupp) {
				
				// has this supply been handled already (e.g. COLLABORATION)
				if (d.getPartial().containsKey(p.getKey())) {
					continue;
				}
	
				for (Supply s: this.resources){ // match to supplies on-hand
					
					if (s.getType() == p.getKey()) { // type matches
						
						if(s.getQuality().ordinal() >= p.getValue().ordinal()){ // quality is good enough
							
							if (s.isUsable()) {// non-expired and replenishment checked
								
								// deep check considers if enough supply is present
								if (checkamt == true) {
								
									if(s.getAmount() >= d.getEffort()) {
										// found a match
										matchedone = true;
										break;
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
				}
				
				if(matchedone) break;
			}
		}
		
		return matchedone;
	}
	
	/*
	 * single function to check for all supply/demand matches
	 * or check if demand can be accomplished immediately
	 */
	public boolean isDemandAchieveableALL(Demand d, boolean checkamt) {
		
		// get list of supplies required
		if(this.sd_dict.isEmpty())
			return false;
		
		ArrayList<Pair<SupplyType,SupplyQuality>> reqsupp;
		if(d.getType()==DemandType.COLLABORATE) {
			reqsupp = this.getSupplyDemandDictionary().get().getRequiredSupplies(d.ancillaryDemand.get().getType());
		}
		else {
			reqsupp = this.getSupplyDemandDictionary().get().getRequiredSupplies(d.getType());
		}
		
		// consider required supplies
		boolean matchedall = true;
		for (Pair<SupplyType,SupplyQuality> p : reqsupp) {
			
			// has this supply been handled already (e.g. COLLABORATION)
			if (d.getPartial().containsKey(p.getKey())) {
				continue;
			}
			
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
		ArrayList<Pair<SupplyType,SupplyQuality>> reqsupp;
		
		if(d.getType()==DemandType.COLLABORATE) {
			this.setState(AgentState.COMMUNICATING);
			reqsupp = this.getSupplyDemandDictionary().get().getRequiredSupplies(d.ancillaryDemand.get().getType());
		}
		else {
			reqsupp = this.getSupplyDemandDictionary().get().getRequiredSupplies(d.getType());
		}
		
		boolean success = true;
		int work_already_done;
		// consider all required supplies
		for (Pair<SupplyType,SupplyQuality> p : reqsupp) {
			
			boolean nomatch = true;
			
			// how much has already been done
			work_already_done = d.getPartial().getOrDefault(p.getKey(), 0);
			
			// cycle through all of this agent's supplies
			for (Supply s: this.resources){
							
				// match on type and quality
				if ((s.getType() == p.getKey()) && 
					(s.getQuality().ordinal() >= p.getValue().ordinal())) {
					
					// found case where random efficiency issues resulted in too much effort!
					if(s.getAmount() < (effort-work_already_done)) {
						
						// try achievable with a replenishment?
						if(s.getCapacity() < (effort-work_already_done)) {
							// failed to complete this part of a task
							System.out.println("too much effort to complete task ("+s.getAmount()+","+(effort-work_already_done)+")");
							success = false;
							break;							
						}
						else {
							// try to replenish and check again
							s.replenishSupply();
							effort += s.getReplenishTime();
							if(s.getAmount() < (effort-work_already_done)) {
								// failed to complete this part of a task
								System.out.println("too much effort to complete task ("+s.getAmount()+","+(effort-work_already_done)+")");
								success = false;
								break;
							}
						}
						
					}
					else {
						nomatch = false; 
					}
					
					// add a record of current Supply state (for potential roll-back)
					snapshot.add(new SupplyImage(s));
					
					// expend actual effort (which is >= amount in Demand d)
					s.reduceAmount((effort-work_already_done));
					d.getPartial().put(p.getKey(),effort); // if value present, replaces value
					break;
				}
			}
			
			// short-circuit if already failed
			if (!success) { 
				break;
			}
			
			// not all parts of demand are met
			if(nomatch && !(d.getType()==DemandType.COLLABORATE)) {
				d.setPartial();
			}
		}
		
		// return actual effort expended
		return success;
	}
	
	protected boolean completeNextTask() {
		return completeNextTask(false);
	}
	
	// use backlog to select next task
	protected boolean completeNextTask(boolean urgent_only) {
		
		TASK_RESULTS retval = TASK_RESULTS.SUCCESS;
		
		if(!this.backlog_tasks.isEmpty()) {
			
			// TODO: consider alternate selection criteria on backlog
			
			// sort demands by priority
	    	Collections.sort(this.backlog_tasks, new Comparator<Demand>() {
	    		public int compare(Demand d1, Demand d2) {
	    			int result = d1.getPriority().compareTo(d2.getPriority());
	    			return -result; // return reverse order (high priority first)
	    		}
	    	});

	    	// complete one task if possible
	    	int dindex = 0;
	    	while(true) {
	    		
	    		if(this.backlog_tasks.size()>dindex) {
	    			// only allow work to be done on urgent tasks
	    			if(urgent_only && this.backlog_tasks.get(dindex).getPriority() != DemandPriority.URGENT) {
	    				break;
	    			}
	    			this.setCurrenttask(Optional.of(this.backlog_tasks.get(dindex)));
	    			
	    			if(this.getState()==AgentState.WAITING && 
	    			   this.getCurrenttask().get().creator.get().getAgentTime()<this.getAgentTime()) {
	    				
	    				System.out.println(this.getName()+" rolling back unfinished Demand to collaborate");
	    				this._partialRollBack(this.getCurrenttask().get().creator.get().getAgentTime());
	    				
	    				this.backup = Optional.empty();
	    				this.setState(AgentState.ACTIVE);
	    			}
	    		}
	    		try {
	    			retval = this.completeTask();
	    		}
	    		catch(Exception e){
	    			e.printStackTrace();
	    			retval = TASK_RESULTS.FAIL;
	    		}
	    		
	    		// exit loop if completed a demand or no demands left
	    		if(retval == TASK_RESULTS.SUCCESS || 
	    		   retval == TASK_RESULTS.FAIL || 
	    		   this.backlog_tasks.isEmpty()) {
	 	    		break;
 	    		}
	    		if(retval == TASK_RESULTS.SKIP_TOOEARLY) {
	    			// don't want to toss out demand, it's just too early for this
	    			// agent to help, so look for the next one to work on
	    			System.out.println(this.getName()+" is too far behind. skipping collaboration for now...");
    				
	    			dindex++;
	    		}
	    	}
		}
		
		return retval==TASK_RESULTS.SUCCESS;
	}
	
	// look up demand in ledger and return effort expended on it
	public int getEffortSpentOn(Demand d) {
		if(ledger.containsKey(d.getId())){
			return this.ledger.get(d.getId()).getEffort();
		}
		else {
			return 0;
		}
	}
	
	// look up demand in ledger and return when it was started
	public int getTimeStartedOn(Demand d) throws Exception {
		if(ledger.containsKey(d.getId())){
			return this.ledger.get(d.getId()).getTimeAtStart();
		}
		throw new Exception("Demand not in ledger");
	}
	
	// start handling a Demand
	// return value 0 = failed,
	//              1 = success, 
	//              2 = skipped - demand complete
    //              3 = skipped - not ready for the demand 
	protected TASK_RESULTS completeTask() throws Exception {
		
		// fail fast
		if(this.getCurrenttask().isEmpty()) {
			return TASK_RESULTS.FAIL;
		}
		
		Demand d = this.getCurrenttask().get();
		
		// only pick up Demands that aren't in progress already
		if(d.getState() == DemandState.QUEUED || d.getState() == DemandState.INCOMPLETE) {
			d.setActive(); // mark task as started by an agent
			
			//this.setCurrenttask(Optional.of(d));
			System.out.println(this.getName()+" has started working on "+d.toString());
		
			if(d.getType() == DemandType.COLLABORATE) {
				// -------- COLLABORATION ------------
				
				// requesting Agent gave up and marked Demand as incomplete
				if(d.ancillaryDemand.get().getState() == DemandState.INCOMPLETE ||
				   d.ancillaryDemand.get().getState() == DemandState.REPLACED) {
					System.out.println("...demand already marked "+
							d.ancillaryDemand.get().getState().toString()+
							" Moving on.");
					
					// set collaboration demand as complete to avoid others working on it
					d.setComplete();
					
					// remove from backlog
					this.backlog_tasks.remove(d);
					this.setCurrenttask(Optional.empty());
					return TASK_RESULTS.SKIP_INCOMPLETE;
				}
				
				// Agent local time behind requesting Agent, too early to engage
				if(d.creator.get().getTimeStartedOn(d.ancillaryDemand.get())>this.getAgentTime()) {
					
					// remove for now, will re-add on next DemandList sync
					//this.backlog_tasks.remove(d); 
					this.setCurrenttask(Optional.empty());
					return TASK_RESULTS.SKIP_TOOEARLY;
				}
				
				// Agent too far ahead, needs to roll back (potentially multiple...)
				while(d.creator.get().getAgentTime() < this.getAgentTime()) {
					
					// if last completed task was a collaboration, no rollback
					// NOTE: this means waiting agent will just need to wait
					
					// CANNOT roll back collaborations!
					if(!this.completed_tasks.isEmpty() &&
					   this.completed_tasks.get(this.completed_tasks.size()-1).getType()==DemandType.COLLABORATE) {
						break;
					}
					else {
						
						int tdiff = this.getAgentTime()-d.creator.get().getAgentTime();
						if((tdiff > Agent.AGENT_COLLAB_GAP_THRESHOLD) && (this.completed_tasks.size()>0)) {
							this._rollBack();
						}
						else {
							break;
						}
					}
				}

				// **** KEY ELEMENT OF MODEL ****
				// MORE INTERACTIONS == BETTER AT TASKS
				
				// scale task effort on interactions
				int collab_effort;
				if(this.interactions.contains(d.creator.get().getName())) {
					int factor = this.interactions.getOrDefault(d.creator.get().getName(),0);
					collab_effort = (int)Math.ceil(d.getEffort()*
							Math.exp(-factor*this.interax_learning_rate));
				}
				else {
					collab_effort = d.getEffort();
				}
				
				// complete task
				ArrayList<SupplyImage> snapshot = new ArrayList<SupplyImage>();
				
				// update resources based on demand
				if(this._expendEffort(d, d.ancillaryDemand.get().getEffort(), snapshot)) {	
						
					// move demand to cleared list
					this.backlog_tasks.remove(d);
					this.completed_tasks.add(d.ancillaryDemand.get());
					this.completed_tasks.add(d);
					
					// update local time and add to ledger
					this.ledger.put(d.ancillaryDemand.get().getId(),
							new LedgerEntry(this.getAgentTime(),
											d.ancillaryDemand.get().getEffort(),
											snapshot));
					this.updateAgentTime(d.ancillaryDemand.get().getEffort());

					// add demand and collaboration to ledger
					this.ledger.put(d.getId(),
							new LedgerEntry(this.getAgentTime(),
									collab_effort,
									new ArrayList<SupplyImage>()));							
					this.updateAgentTime(collab_effort);
					
					this.setCurrenttask(Optional.empty());
				}

				// update list for myself and collaborator
				this.updateInteractions(d.creator.get());
				d.creator.get().finishCollaboration(this,d,collab_effort);
				
				System.out.println("...time needed to complete task: "+d.ancillaryDemand.get().getEffort());
				System.out.println("...time needed to collaborate: "+collab_effort);
			}
			else {
	
				// -------- NOT COLLABORATION ----------
				
				// RANDOMNESS/UNCERTAINTY
				// leverage efficiency to represent un-productive time
				int totaleffort = 0;
				for (int i = 0; i < d.getEffort(); totaleffort++, i++) {
					if (this.getEfficiency() < rand.nextInt(101)) totaleffort++; // add extra effort randomly 
				}
				
				ArrayList<SupplyImage> snapshot = new ArrayList<SupplyImage>();
				
				// update resources based on demand
				if(this._expendEffort(d, totaleffort, snapshot)) {	
					if(d.getState()==DemandState.PARTIAL) {
						// fire signal for collaboration
						this.setState(AgentState.WAITING);
						
						System.out.println("...agent is waiting for help from others");
						this.support.firePropertyChange("collaborate",d,this); // announce need for help
					}
					else {
						// consider Demand completed
						d.setComplete();
						
						// move demand to cleared list
						this.backlog_tasks.remove(d);
						this.completed_tasks.add(d);
						this.setCurrenttask(Optional.empty());
					}

					// make note of work completed (even if only PARTIAL and waiting...)
					this.ledger.put(d.getId(),
							new LedgerEntry(this.getAgentTime(), totaleffort, snapshot));
					this.updateAgentTime(totaleffort);
				}
				else {
					System.err.println("...failed to complete task");
					
					// can't complete task
					// treat like completed, then instantly roll-back
					
					this.backlog_tasks.remove(d);
					this.ledger.put(d.getId(),
							new LedgerEntry(this.getAgentTime(), totaleffort, snapshot));
					this.setCurrenttask(Optional.empty());
					
					// fix any changes to Supplies during failed attempt
					this._rollBack(d,false);
					
					return TASK_RESULTS.FAIL;
				}
	
				System.out.println("...time needed to complete task: "+totaleffort);
			}
		}	
		else {
			// task has already been completed, remove from backlog
			this.backlog_tasks.remove(d);
			this.setCurrenttask(Optional.empty());
			//System.out.println("Demand state is "+d.getState());
			return TASK_RESULTS.SKIP_COMPLETE;
		}
		return TASK_RESULTS.SUCCESS;
	}
	
	// task completion when effort is hard-limited (e.g. to allow for collaboration)
	protected TASK_RESULTS partialCompleteTask(Demand d, int totaleffort) {
		
		// only pick up Demands that aren't in progress already
		if(d.getState() == DemandState.QUEUED || d.getState() == DemandState.INCOMPLETE) {
			d.setActive(); // mark task as started by an agent

			System.out.println(this.getName()+" has started working on "+d.toString());
			
			ArrayList<SupplyImage> snapshot = new ArrayList<SupplyImage>();
			
			// update resources based on demand
			if(this._expendEffort(d, totaleffort, snapshot)) {	
				if(d.getState()==DemandState.PARTIAL) {
					// fire signal for collaboration
					this.setState(AgentState.WAITING);
					
					System.out.println("...agent is waiting for help from others");
					this.support.firePropertyChange("collaborate",d,this); // announce need for help
				}
				else {
					// consider Demand completed
					d.setComplete();
					
					// move demand to cleared list
					this.backlog_tasks.remove(d);
					this.completed_tasks.add(d);
					this.setCurrenttask(Optional.empty());
				}

				// make note of work completed (even if only PARTIAL and waiting...)
				this.ledger.put(d.getId(),
						new LedgerEntry(this.getAgentTime(),totaleffort,snapshot));
				this.updateAgentTime(totaleffort);
			}
			else {
				System.err.println("...failed to complete task");
				
				// can't complete task
				// treat like completed, then instantly roll-back
				
				this.backlog_tasks.remove(d);
				this.ledger.put(d.getId(),
						new LedgerEntry(this.getAgentTime(),totaleffort,snapshot));
				this.setCurrenttask(Optional.empty());
				
				// fix any changes to Supplies during failed attempt
				this._rollBack(d,false);
				
				return TASK_RESULTS.FAIL;
			}

			System.out.println("...time needed to complete task: "+totaleffort);
		}
		else {
			// task has already been completed, remove from backlog
			this.backlog_tasks.remove(d);
			this.setCurrenttask(Optional.empty());
			//System.out.println("Demand state is "+d.getState());
			return TASK_RESULTS.SKIP_COMPLETE;
		}
		return TASK_RESULTS.SUCCESS;
	}
	
	public int getCollaborationCount() {
		int count = 0;
		//Iterator<Entry<String, Integer>> it = this.interactions.entrySet().iterator();
	    for (int v : interactions.values() ) {
	        count += v;
	    }
	    
	    return count;
	}
	
	public String toProgressString() {
		
		String progressString = "Name: "+this.getName()+"\n";
		progressString += "Status: "+this.getState().name()+"\n";
		progressString += "Local Time: "+this.getAgentTime()+"\n";
		progressString += "Wait Time: "+this.getWaitTime()+"\n";
		progressString += "Backlog: "+this.backlog_tasks.size()+" demands\n";
		progressString += "Completed: "+this.completed_tasks.size()+" demands\n";
		progressString += "Committed: "+this.committed_tasks.size()+" demands\n";
		progressString += "Abandoned: "+this.abandoned_tasks.size()+" demands\n";
		progressString += "Collaborations: "+this.getCollaborationCount()+"\n";
		progressString += "List of Interactions: \n";

		//Iterator<Entry<UUID, Integer>> it = this.interactions.entrySet().iterator();
		Iterator<Entry<String, Integer>> it = this.interactions.entrySet().iterator();
	    while (it.hasNext()) {
	    	Entry<String, Integer> nextentry = it.next();
	    	progressString += ("("+this.getName()+","+nextentry.getKey()+") x "+nextentry.getValue()+"\n");
	    }
		// * total collaborations
		return progressString;
	}
	
	// print interactions to file
	public void reportInteractions(FileWriter intWriter) {
		//Iterator<Entry<UUID, Integer>> it = this.interactions.entrySet().iterator();
		Iterator<Entry<String, Integer>> it = this.interactions.entrySet().iterator();
	    while (it.hasNext()) {
	    	try {
		        // progressString += ("("+this.getId()+","+it.next().getKey()+")\n"); //pair.getValue());
	    		Entry<String, Integer> nextentry = it.next();
		    	intWriter.write(this.getName()+","+nextentry.getKey()+","+nextentry.getValue()+"\n");
	    	}
	    	catch (IOException e) {
				// fail gracefully
				System.err.println("Failed while writing progress report to file.");
				e.printStackTrace();
				break;
	    	}
	    }
	}
	
	/*
	 * Getters and Setters
	 */
	public String getName() { return name; }
	public void setName(String name) {
		this.name = name;
	}

	public int getEfficiency() { return efficiency; }
	public void setEfficiency(int efficiency) {
		this.efficiency = efficiency;
	}
	
	public UUID getId() { return id; }
	public void setId(UUID id) {
		this.id = id;
	}

	public ArrayList<Supply> getResources() { return resources; }
	public void setResources(ArrayList<Supply> rsrcs) {
		resources.clear();
		resources.addAll(rsrcs);
	}
	
	public ArrayList<Demand> getCompletedtasks() { return completed_tasks; }
	public ArrayList<Demand> getCommittedtasks() { return committed_tasks; }
	
	public int getAgentTime() { return agentTime; }
	public void setAgentTime(int agentTime) { 
		this.agentTime = agentTime; 
	}
	public void updateAgentTime(int updatetime) {
		this.agentTime += updatetime;
	}
	
	public Optional<Demand> getCurrenttask() { return current_task; }
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

	public double getInteraxLearningRate() {
		return interax_learning_rate;
	}
	
	public void setInteraxLearningRate(double interax_learning_rate) {
		this.interax_learning_rate = interax_learning_rate;
	}
	
	public AgentState getState() { return state; }
	public void setState(AgentState state) { this.state = state; }
	
	public int getWaitTime() { return waitTime; }
	public void setWaitTime(int waitTime) { this.waitTime = waitTime; }
	
	public void updateWaitTime(int waitTime) {
		this.setWaitTime(waitTime+this.getWaitTime());
	}
	
	public int getStepsSinceLastActive() { return stepsSinceLastActive; }
	public void setStepsSinceLastActive(int stepsSinceLastActive) {
		this.stepsSinceLastActive = stepsSinceLastActive;
	}
	public void updateStepsSinceLastActive() {
		this.stepsSinceLastActive += 1;
	}
	
	public SimpleIntegerProperty getMaxWaitCycles() { return max_wait_cycles; }
	public void setMaxWaitCycles(int max_wait) {
		this.max_wait_cycles.set(max_wait);
	}
	
	public int getNumUrgent() {
		return n_urgent;
	}
	public void setNumUrgent(int n_urgent) {
		this.n_urgent = n_urgent;
	}
	public void numUrgentPlusOne(){
		this.n_urgent += 1;
	}
	public void numUrgentMinusOne(){
		this.n_urgent -= 1;
	}
}