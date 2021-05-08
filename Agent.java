import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
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

	public static final double INTERACTION_LEARNING_RATE = 0.85;
	public static final int SYNCH_BACKLOG_EVERY = 50;    // how often to re-check demand list
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
		this.rand = new Random();
		rand.setSeed(43);
		
		this.resources = new ArrayList<Supply>();
		
		this.seen_tasks = new HashSet<UUID>();
		this.committed_tasks = new ArrayList<Demand>();
		this.completed_tasks = new ArrayList<Demand>();
		this.abandoned_tasks = new ArrayList<Demand>();
		this.backlog_tasks = new ArrayList<Demand>();
		this.ledger = new Hashtable<UUID, Pair<Integer,ArrayList<SupplyImage>>>();
		this.interactions = new Hashtable<UUID, Integer>();
		this.cycles_since_synch = 0;
		this.setSupplyDemandDictionary(Optional.empty());
		
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
	protected HashSet<UUID> seen_tasks;
	private int cycles_since_synch;
	
	// task lists
	protected ArrayList<Demand> committed_tasks; // officially completed and committed
	protected ArrayList<Demand> completed_tasks; // completed but not committed
	protected ArrayList<Demand> abandoned_tasks; // failed to complete (no collaboration)
	protected ArrayList<Demand> backlog_tasks;   // tasks not yet performed
	
	// look-up table for task completion times
	protected Hashtable<UUID,Pair<Integer,ArrayList<SupplyImage>>> ledger; 
	protected Optional<Demand> current_task;      // current active task
	
	// look-up table for agent interactions
	protected Hashtable<UUID,Integer> interactions; 
		
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

	/*
	 * Agent "interface" functions to be overridden in child class
	 */
	public void start(DemandList dl){
		
		// add tasks to backlog
		refreshBacklog(dl);
		
		// child Agent class functionality can override this function
		this.setState(AgentState.ACTIVE);
		System.out.println("Agent::Start "+this.getName());
	}
	
	public void refreshBacklog(DemandList dl) {
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
				System.out.println(this.getName()+" added "+d.getName()+" to backlog");
			}
			
			// mark as seen
			this.seen_tasks.add(d.getId());
		}
	}
	
	public void doSomething(PropertyChangeEvent evt) {
		
		// child Agent class functionality can override this function
		if(evt.getPropertyName()=="newdemand") {
			
			//System.out.println(this.getName()+": alerted that a new demand has been issued...");
			Demand d = (Demand)(evt.getNewValue());
			
			// only consider Demands that are not active, partial (handled by collaboration), or complete
			if(d.getState() == DemandState.QUEUED) {
				
				// is this a collaboration demand I created OR 
				// have I already considered this demand?
				if (!((d.getType()==DemandType.COLLABORATE) && (d.creator.get() == this))) {
					// !(this.alreadyConsidered(d))) {
					
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
		else if (evt.getPropertyName()=="commituntil") {
			// commit any completed tasks up until time provided
			this._updateTaskLists((Integer)evt.getNewValue());
		}
		else if (evt.getPropertyName()=="finishtask") {
			// if Agent is in ACTIVE state, complete a task
			if(this.getState() == AgentState.ACTIVE) {
				this.cycles_since_synch++;
				this.completeNextTask();
				
				if(this.cycles_since_synch >= Agent.SYNCH_BACKLOG_EVERY) {
					// flag that it's time for me to refresh my backlog
					this.support.firePropertyChange("refresh_backlog",0,this);
				}
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
		int prevval = this.interactions.getOrDefault(collaborator.getId(), 0);
		this.interactions.put(collaborator.getId(),prevval+1);
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
				new Pair<Integer,ArrayList<SupplyImage>>(this.getAgentTime(),
						new ArrayList<SupplyImage>()));
		
		// was there excess wait time?
		int wait = (collaborator.getAgentTime()-collab_effort)-this.getAgentTime();
		if(wait<0) {
			System.err.println("ERROR: " + this.getName() + " has a wait value of "+wait);
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
			if(ledger.get(d.getId()).getKey() <= committime) {
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
		Pair<Integer,ArrayList<SupplyImage>> p = ledger.get(d.getId());
		for (SupplyImage si : p.getValue()) {

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
			// reset local time by adding back time expended
			this.updateAgentTime(-p.getKey());
			
			// reset demand to not completed
			d.setActive();
			
			// add back to backlog
			this.addToBacklog(d);
		}
		
		// remove from ledger
		ledger.remove(d.getId());
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
		
		// consider all required supplies
		for (Pair<SupplyType,SupplyQuality> p : reqsupp) {

			// has this supply been handled already (e.g. COLLABORATION)
			if (d.getPartial().containsKey(p.getKey())) {
				// ASSUMPTION: anything on this list is 100% complete
				continue;
			}

			boolean nomatch = true;
		
			// cycle through all of this agent's supplies
			for (Supply s: this.resources){
							
				// match on type and quality
				if ((s.getType() == p.getKey()) && (s.getQuality().ordinal() >= p.getValue().ordinal())) {
					
					// found case where random efficiency issues resulted in too much effort!
					if(s.getAmount() < effort) {
						
						// try achievable with a replenishment?
						if(s.getCapacity() < effort) {
							// failed to complete this part of a task
							System.out.println("too much effort to complete task ("+s.getAmount()+","+effort+")");
							success = false;
							break;							
						}
						else {
							// try to replenish and check again
							s.replenishSupply();
							effort += s.getReplenishTime();
							if(s.getAmount() < effort) {
								// failed to complete this part of a task
								System.out.println("too much effort to complete task ("+s.getAmount()+","+effort+")");
								success = false;
								break;
							}
						}
						
					}
					else {
						nomatch= false; 
					}
					
					// add a record of current Supply state (for potential roll-back)
					snapshot.add(new SupplyImage(s));
					
					// expend actual effort (which is >= amount in Demand d)
					s.reduceAmount(effort);
					d.getPartial().put(p.getKey(),effort); 
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
	
	// use backlog to select next task
	protected boolean completeNextTask() {
		
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
	    			this.setCurrenttask(Optional.of(this.backlog_tasks.get(dindex)));
	    		}	    		
	    		retval = this.completeTask();
	    		
	    		// exit loop if completed a demand or no demands left
	    		if(retval == TASK_RESULTS.SUCCESS || 
	    		   retval == TASK_RESULTS.FAIL || 
	    		   this.backlog_tasks.isEmpty()) {
	 	    		break;
 	    		}
	    		if(retval == TASK_RESULTS.SKIP_TOOEARLY) {
	    			// don't want to toss out demand, it's just too early for this
	    			// agent to help, so look for the next one to work on
	    			dindex++;
	    		}
	    	}
		}
		
		return retval==TASK_RESULTS.SUCCESS;
	}
	
	// start handling a Demand
	// return value 0 = failed,
	//              1 = success, 
	//              2 = skipped - demand complete
    //              3 = skipped - not ready for the demand 
	protected TASK_RESULTS completeTask() {
		
		// fail fast
		if(this.getCurrenttask().isEmpty()) {
			return TASK_RESULTS.FAIL;
		}
		
		Demand d = this.getCurrenttask().get();
		
		// only pick up Demands that aren't in progress already
		if(d.getState() == DemandState.QUEUED || d.getState() == DemandState.INCOMPLETE) {
			d.setActive(); // mark task as started by an agent
			
			//this.setCurrenttask(Optional.of(d));
			System.out.println("Agent "+this.getName()+" has started task "+d.toString());
		
			if(d.getType() == DemandType.COLLABORATE) {
				// -------- COLLABORATION ------------
				
				// requesting Agent gave up and marked Demand as incomplete
				if(d.ancillaryDemand.get().getState() == DemandState.INCOMPLETE) {
					System.out.println("...demand already marked INCOMPLETE. Moving on.");
					
					// set collaboration demand as complete to avoid others working on it
					d.setComplete();
					
					// remove from backlog
					this.backlog_tasks.remove(d);
					this.setCurrenttask(Optional.empty());
					return TASK_RESULTS.SKIP_INCOMPLETE;
				}
				
				// Agent local time behind requesting Agent, too early to engage
				if(d.creator.get().getAgentTime()>this.getAgentTime()) {
					
					// remove for now, will re-add on next DemandList synch
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
				// MORE INTERACTIONS == LESS TIME TO COMPLETE
				
				// scale task effort on interactions
				int collab_effort;
				if(this.interactions.contains(d.creator.get().getId())) {
					int factor = this.interactions.get(d.creator.get().getId());
					collab_effort = (int)Math.ceil(d.getEffort()*
							Math.exp(-factor*Agent.INTERACTION_LEARNING_RATE));
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
							new Pair<Integer,ArrayList<SupplyImage>>(this.getAgentTime(), snapshot));
					this.updateAgentTime(d.ancillaryDemand.get().getEffort());

					// add demand and collaboration to ledger
					this.ledger.put(d.getId(),
							new Pair<Integer,ArrayList<SupplyImage>>(this.getAgentTime(), 
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
							new Pair<Integer,ArrayList<SupplyImage>>(this.getAgentTime(),snapshot));
					this.updateAgentTime(totaleffort);
				}
				else {
					System.err.println("...failed to complete task");
					
					// can't complete task
					// treat like completed, then instantly roll-back
					
					this.backlog_tasks.remove(d);
					this.ledger.put(d.getId(),
							new Pair<Integer,ArrayList<SupplyImage>>(totaleffort,snapshot));
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

	public String toProgressString() {
		
		String progressString = "Name: "+this.getName()+"\n";
		progressString += "Status: "+this.getState().name()+"\n";
		progressString += "Local Time: "+this.getAgentTime()+"\n";
		progressString += "Wait Time: "+this.getWaitTime()+"\n";
		progressString += "Backlog: "+this.backlog_tasks.size()+" demands\n";
		progressString += "Completed: "+this.completed_tasks.size()+" demands\n";
		progressString += "Committed: "+this.committed_tasks.size()+" demands\n";
		progressString += "Abandoned: "+this.abandoned_tasks.size()+" demands\n";
		progressString += "Collaborations: "+this.interactions.keySet().size()+"\n";
		progressString += "List of Interactions: \n";
		
		Iterator<Entry<UUID, Integer>> it = this.interactions.entrySet().iterator();
	    while (it.hasNext()) {
	        progressString += ("("+this.getId()+","+it.next().getKey()+")\n"); //pair.getValue());
	    }
		// * total collaborations
		return progressString;
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
}