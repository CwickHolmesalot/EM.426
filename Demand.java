import java.time.*; 
import java.util.*;
//import java.util.logging.Logger;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/*
 *  Demand Class for Agent Based Modeling
 *  @author Chad Holmes
 *  
 *  Based on Demand class created by Bryan Moser and shared with 
 *  the MIT EM.426 Spring 2021 class
 *  
 *  Conceptually, the Demand is viewed as a need with key attributes:
 *  1. priority
 *  2. effort (time-based)
 *  3. type (grouped into a handful of buckets)
 *  4. status (at the least granular: is it complete?)
 *  
 *  Demand can have an owner. This would be the stakeholder for that Demand.
 *  Present implementation captures the owner with a String.  
 *  TODO: Future versions can use a class representing the Owner.
 *  
 *  Demands are currently free-floating.  
 *  TODO: Define a demand "hopper" or centralized demand collection that
 *  distributes demands to those who can meet them based on heuristics
 *  
 */
public class Demand implements IDemand {
	private static final double COLLAB_FACTOR = 0.1;
	private static final int DEFAULT_EFFORT = 30;
	private static final int DEFAULT_START = 0;
	private static final int DEFAULT_STOP = 12*3600;
	private static final boolean DEFAULT_RECUR = false;
	private static final int DEFAULT_EVERY = -1;
	private static final int DEFAULT_UNTIL = -1;
	
	/* 
	 * Collaboration Demand factory
	 */
	public static Demand createCollaborationDemand(Demand d, Agent a) {
		Demand collab = new Demand(UUID.randomUUID(),
								  d.getName()+"_Collab",
								  DemandPriority.URGENT,
								  DemandType.COLLABORATE,
								  DemandState.DEFINED,
								  (int)(d.getEffort()*COLLAB_FACTOR)+1, // always >0
								  DEFAULT_START,
								  DEFAULT_STOP,
								  DEFAULT_RECUR, 
								  DEFAULT_EVERY,
								  DEFAULT_UNTIL);
		collab.setAncillaryDemand(d);
		collab.setCreator(a);
		collab.setPartial(d.getPartial());
		return collab;
	}
	
	/* 
	 * Demand Constructors
	 */
	
	/*
	 * DEFAULT constructor:
	 * type set to Make
	 * effort, perceived_effort set to 30 seconds
	 * start is right away (0)
	 * stop is in 12 hours (12*3600)
	 * all other values use Basic constructor defaults 
	 */
	public Demand() {
		this(DemandType.NEED1, 
			DEFAULT_EFFORT,
			DEFAULT_START,
			DEFAULT_STOP);
	}
	
	/* 
	 * BASIC constructor: only requires a type, effort, start, and finish
	 * UID is randomly assigned
	 * identifier set to empty string
	 * priority is set to MEDIUM
	 * type set to input type value
	 * state set to DEFINED
	 * effort set to input effort value
	 * start set to input start value
	 * stop set to input stop value
	 * repeats set to false
	 * every, until set to -1
	 */
	public Demand(DemandType type, int effort, int start, int stop) {
		this(UUID.randomUUID(),
			 "", 
			 DemandPriority.MEDIUM, 
			 type, 
			 DemandState.DEFINED,
			 effort, 
			 start, 
			 stop, 
			 DEFAULT_RECUR,
			 DEFAULT_UNTIL,
			 DEFAULT_EVERY);
	}
	
	/* 
	 * BASIC constructor: only requires a type, effort, start, and finish
	 * UID is randomly assigned
	 * name set to input name value
	 * priority set to input priority value
	 * type set to input type value
	 * state set to DEFINED
	 * effort set to input effort value
	 * start is right away (0)
	 * stop is in 1 hour (3600)
	 * repeats set to false
	 * every, until set to -1
	 */
	public Demand(String name, DemandPriority priority, DemandType type, int effort) {
		this(UUID.randomUUID(), 
			name, 
			priority,
			type,
			DemandState.DEFINED,
			effort,
			DEFAULT_START,
			DEFAULT_STOP,
			DEFAULT_RECUR,
			DEFAULT_UNTIL,
			DEFAULT_EVERY);
	}
	
	/*
	 * Full constructor
	 */
	public Demand(UUID id, String name, DemandPriority priority, DemandType type, DemandState state,
			int effort, int start, int stop, boolean recur, int every, int until) {
		super();
		this.setId(id);
		this.setName(name);
		this.setPriority(priority);
		this.setType(type);
		this.setState(state);
		this.setEffort(effort);
		this.setStart(start);
		this.setStop(stop);
		this.setRecur(recur);
		this.setEvery(every);
		this.setUntil(until);
		
		//this.startdate = startdate;
		//this.completedate = completedate;
		
		this.ancillaryDemand = Optional.empty();
		this.creator = Optional.empty();
		this.partial = new Hashtable<SupplyType,Integer>();
	}

	/* 
	 * Member Variables
	 */
	public UUID id; // universal unique id
	
	// human-readable identifier
	private final StringProperty name = new SimpleStringProperty();
	
	// sets a priority level to the demand
	private final ObjectProperty<DemandPriority> priority = new SimpleObjectProperty<DemandPriority>();
	
	// assigns a "type" to demand based on DemandType enumerated list
	private final ObjectProperty<DemandType> type = new SimpleObjectProperty<DemandType>();
	
	// current status of demand based on enum DemandState
	private final ObjectProperty<DemandState> state = new SimpleObjectProperty<DemandState>(); 
	
	// demand time requirements (in hours)  
	private final IntegerProperty effort = new SimpleIntegerProperty();  
	
	// variables to manage recurring demands
	private final IntegerProperty start = new SimpleIntegerProperty();
	private final IntegerProperty stop  = new SimpleIntegerProperty();
	private final BooleanProperty recur = new SimpleBooleanProperty();
	private final IntegerProperty every = new SimpleIntegerProperty();
	private final IntegerProperty until = new SimpleIntegerProperty();
	
	// key milestone time stamps
	private LocalDate startdate;
	private LocalDate completedate;
	
	// associated demand
	public Optional<Demand> ancillaryDemand;
	
	// capture agent who created demand (if any)
	public Optional<Agent> creator;
	
	// manage partial completeness of Demand 
	public Hashtable<SupplyType,Integer> partial;
	
	/* 
	 * Getters and Setters	
	 */	
	// ID -------------------------------------
	public UUID getId() { return id; }
	public void setId(UUID id) { this.id = id; }
	
	// NAME -----------------------------------
	public StringProperty nameProperty() { return name; }
	public String getName() { return name.get(); }
	public void setName(String name) { this.name.set(name); }
	
	// PRIORITY -------------------------------
	public ObjectProperty<DemandPriority> priorityProperty() { return priority; }
	public DemandPriority getPriority() { return priority.get(); }
	public void setPriority(DemandPriority priority) { this.priority.set(priority); }
	
	// TYPE -----------------------------------
	public ObjectProperty<DemandType> typeProperty() { return type; }	
	public DemandType getType() { return type.get(); }
	public void setType(DemandType type) { this.type.set(type); }
	
	// STATE ----------------------------------
	public ObjectProperty<DemandState> stateProperty() { return state; }
	public DemandState getState() { return state.get(); }
	public void setState(DemandState state) {
		this.state.set(state);
		
		// update dates at milestones
		if(state == DemandState.COMPLETE) {
			this.setCompletedate(LocalDate.now());
		}
		else if(state == DemandState.ACTIVE) { 
			this.setStartdate(LocalDate.now());
		}
	}

	// EFFORT ---------------------------------
	public IntegerProperty effortProperty() { return effort; }
	public int getEffort() {return effort.get(); }
	public void setEffort(int effort) { this.effort.set(effort); }
	
	// START ----------------------------------
	public IntegerProperty startProperty() { return start; }
	public int getStart() { return start.get(); }
	public void setStart(int start) { this.start.set(start); }
	
	// STOP -----------------------------------
	public IntegerProperty stopProperty()  { return stop; }
	public int getStop() { return stop.get(); }
	public void setStop(int stop) { this.stop.set(stop); }
	
	// RECUR ----------------------------------
	public BooleanProperty recurProperty() { return recur; }
	public boolean getRecur() { return recur.get(); }
	public void setRecur(boolean isRecurring) { this.recur.set(isRecurring); }
	public boolean isRecur() { return recur.get(); }
		
	// EVERY ----------------------------------
	public IntegerProperty everyProperty() { return every; }
	public int getEvery() { return every.get(); }
	public void setEvery(int every) { this.every.set(every); }

	// UNTIL ----------------------------------
	public IntegerProperty untilProperty() { return until; }
	public int getUntil() { return until.get(); }
	public void setUntil(int until) { this.until.set(until); }
	
	// STARTDATE ------------------------------
	public LocalDate getStartdate() { return startdate; }
	public void setStartdate(LocalDate startdate) { this.startdate = startdate; }
	
	// ENDDATE --------------------------------
	public LocalDate getCompletedate() { return completedate; }
	public void setCompletedate(LocalDate completedate) { this.completedate = completedate; }
	
	
	// ANCILLARYDEMAND ------------------------
	public void setAncillaryDemand(Demand d) { this.ancillaryDemand = Optional.of(d); }
	
	// CREATOR --------------------------------
	public void setCreator(Agent a) { this.creator = Optional.of(a); }
	
	// PARTIAL --------------------------------
	public Hashtable<SupplyType, Integer> getPartial() { return partial; }
	public void setPartial(Hashtable<SupplyType, Integer> partial) { this.partial = partial; }
	
	// class logger
	//private static final Logger logger = Logger.getLogger(Demand.class.getName());
		
	/*
	 *  Convenience Functions
	 */
	public double getEffortHrs() { 
		// return effort in hours
		return this.effort.get()/(60*60); 
	}
	public void setEffortHrs(double _effort) { 
		// set effort in hours as seconds
		this.effort.set((int)(_effort*60*60)); 
	}
	
	// convenience functions for setting state
	public void setQueued() {
		this.setState(DemandState.QUEUED);
	}
	public void setActive() {
		this.setState(DemandState.ACTIVE);
	}
	public void setComplete() {
		this.setState(DemandState.COMPLETE);
	}	
	public void setPartial() {
		this.setState(DemandState.PARTIAL);
	}
	public void setIncomplete() {
		this.setState(DemandState.INCOMPLETE);
	}
	
	// convenience functions for checking demand state
	public boolean isActive() {
		// return(now  >= start &&   now <=  stop);
		return this.getState() == DemandState.ACTIVE;
	}
	public boolean isComplete() {
		return this.getState() == DemandState.COMPLETE;
	}
	
	// make demand a recurring demand
	public void makeRecurring(int every, int until) {
		this.setRecur(true);
		this.setEvery(every);
		this.setUntil(until);
	}
	
	// stop recurrence of demand
	public void stopRecurring() {
		this.setRecur(false);
	}
	
	// print all components of demand
	public String toStringFull() { 
		 return "Demand [\n id=" + id +
				 ", \n name=" + name + ", \n priority=" + priority + ", \n type=" + type +
				 ", \n state=" + state + ", \n effort=" + effort + ", \n start=" + start +
				 ", \n stop=" + stop + ", \n recur=" + recur + ", \n every=" + every +
				 ", \n until=" + until + ", \n startdate=" + startdate + ", \n completedate="
				 + completedate + "\n]\n"; }
	 
	@Override
	public String toString() {
		return "Demand [name=" + name.get() + ", priority=" + priority.get() + ", type=" + type.get() + ", effort=" + effort.get() + "]";
	}	

	/*
	 * public static void main(String args[]) {
	 * 
	 * // create fully-specified demand Demand full_demand = new
	 * Demand(UUID.randomUUID(), "My Demand", DemandPriority.HIGH, DemandType.NEED3,
	 * DemandState.DEFINED, 60*60*30, 3,7,true,3,55); full_demand.setActive();
	 * 
	 * // create default demand Demand default_demand = new Demand();
	 * 
	 * // create basic demand Demand basic_demand = new Demand(DemandType.NEED1,
	 * 60*60*72, 10, 18);
	 * 
	 * full_demand.setComplete();
	 * 
	 * // print demands to stdout //System.out.println(default_demand);
	 * //System.out.println(basic_demand); //System.out.println(full_demand);
	 * 
	 * // make "hopper" for all demands ArrayList<Demand> demandHopper = new
	 * ArrayList<Demand>(); demandHopper.add(default_demand);
	 * demandHopper.add(basic_demand); demandHopper.add(full_demand);
	 * 
	 * // print the Demands in the list demandHopper.forEach(System.out::println);
	 * 
	 * // determine total effort int totalHours = 0; for (Demand d : demandHopper)
	 * totalHours += d.getEffortHrs(); System.out.println("total effort (hours): " +
	 * totalHours); }
	 */
}