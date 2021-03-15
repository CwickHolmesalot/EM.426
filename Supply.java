import java.util.*;
import java.util.logging.Logger;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.SimpleLongProperty;

/*
 *  Supply Class for Agent Based Modeling
 *  @author Chad Holmes
 *  
 *  Based on Supply class concept outlined by Bryan Moser 
 *  MIT EM.426 Spring 2021 class
 *  
 *  Conceptually, the Supply is viewed as a skill or ability of an agent,
 *  with the following properties:
 *  1. capacity
 *  2. time sensitivity
 *  3. quality
 *  4. efficiency
 *  5. potentially secondary needs (demands)
 *  
 */

public class Supply implements ISupply {
	
	static public final int DAYVAL = 60*60*24;
	static public final int HOURVAL = 60*60;
	static public final int WEEKVAL = 60*60*24*7;
	static public final int YEARVAL = 60*60*24*365;
	static public final int MONTHVAL = 60*60*24*365/12; // average over a year
	
	/* 
	 * Supply constructors
	 */
	
	/*
	 * DEFAULT constructor:
	 * type set to RAW_MATERIALS
	 * capacity (in time units) set to 100 seconds
	 * all other values use Basic constructor defaults 
	 */
	public Supply() {
		this(SupplyType.SKILL1, 100);
	}
	
	/* 
	 * BASIC constructor: only requires a type, effort, start, and finish
	 * UID is randomly assigned
	 * identifier set to empty string
	 * type set to input type value
	 * capacity set to input capacity value
	 * quality set to MEDIUM
	 * efficiency set to 85 (%)
	 * lifespan set to 9999 seconds
	 * replenish set to false
	 * every nominally set to 1
	 * until set to timestamp 0L
	 */
	public Supply(SupplyType type, int capacity) {
		this(UUID.randomUUID(), "", type, capacity, SupplyQuality.MEDIUM, 85, 9999, false, 1, 0L);
	}
	
	/* 
	 * BASIC constructor: only requires a type, effort, start, and finish
	 * UID is randomly assigned
	 * name set to input string value
	 * type set to input type value
	 * capacity set to input capacity value
	 * quality set to input quality value
	 * efficiency set to input efficiency value
	 * lifespan set to 9999 seconds
	 * replenish set to false
	 * every nominally set to 1
	 * until set to timestamp 0L
	 */
	public Supply(String name, SupplyType type, int capacity, SupplyQuality quality) {
		this(UUID.randomUUID(), name, type, capacity, quality, 100, 9999, false, 1, 0L);
	}	

	
	/*
	 * Full constructor
	 */	
	public Supply(UUID id, String name, SupplyType type, int capacity, SupplyQuality quality,
			int efficiency, int lifespan, boolean isReplenishing, int every, long until) {
		super();
		this.setId(id);
		this.setName(name);
		this.setType(type);
		this.setCapacity(capacity);
		this.setQuality(quality);
		this.setEfficiency(efficiency);
		this.setLifespan(lifespan);
		this.setReplenish(isReplenishing);
		this.setEvery(every);
		this.setUntil(until);

		this.setState(SupplyState.AVAILABLE);
		this.setLastreplenish(-1);
		this.resetAmount();
	}

	/* 
	 * Member Variables
	 */
	public UUID id; // universal unique id

	// human-readable identifier
	private final StringProperty name = new SimpleStringProperty();

	// assigns a "type" to demand based on SupplyType enumerated list
	private final ObjectProperty<SupplyType> type = new SimpleObjectProperty<SupplyType>();

	// current status of demand based on enum SupplyState
	private final ObjectProperty<SupplyState> state = new SimpleObjectProperty<SupplyState>(); 
	
	// supply capacity (in units)  
	private final IntegerProperty capacity = new SimpleIntegerProperty();

	// current supply amount (in units)
	private final IntegerProperty amount = new SimpleIntegerProperty();
	
	// current status of demand based on enum SupplyState
	private final ObjectProperty<SupplyQuality> quality = new SimpleObjectProperty<SupplyQuality>(); 

	// supply efficiency (decimal*100)  
	private final IntegerProperty efficiency = new SimpleIntegerProperty();

	// variables to manage supply replenishment
	private final BooleanProperty replenish = new SimpleBooleanProperty();
	private final IntegerProperty every     = new SimpleIntegerProperty();
	private final LongProperty until        = new SimpleLongProperty();
	
	// supply lifespan before expiry
	private final IntegerProperty lifespan = new SimpleIntegerProperty();
	
	// key milestone time stamps
	private long starttime;
	private long expirytime;
	private long lastreplenish;
	
	// Demands associated with this supply
	private ArrayList<Demand> associatedDemandsList = new ArrayList<Demand>();
	
	/* 
	 * Getters and Setters	
	 */	
	// UNIQUE IDENTIFIER ----------------------
	public UUID getId() { return id; }
	public void setId(UUID id) { this.id = id; }
	
	// NAME -----------------------------------
	public StringProperty nameProperty() { return name; }
	public String getName() { return name.get(); }
	public void setName(String name) { this.name.set(name); }
	
	// TYPE -----------------------------------
	public ObjectProperty<SupplyType> typeProperty() { return type; }	
	public SupplyType getType() { return type.get(); }
	public void setType(SupplyType type) { this.type.set(type); }	
	
	// STATE ----------------------------------
	public ObjectProperty<SupplyState> stateProperty() { return state; }
	public SupplyState getState() { return state.get(); }
	public void setState(SupplyState state) {
		this.state.set(state);

		if(state == SupplyState.AVAILABLE) {
			this.setStarttime(System.currentTimeMillis() / 1000);
			this.setExpirytime(this.getStarttime() + this.getLifespan());
		}
	}
	
	// CAPACITY ---------------------------------
	public IntegerProperty capacityProperty() { return capacity; }
	public int getCapacity() { return capacity.get(); }
	public void setCapacity(int capacity) { this.capacity.set(capacity); }
	
	// AMOUNT -----------------------------------
	public IntegerProperty amountProperty() { return amount; }
	public int getAmount() { return amount.get(); }
	public void setAmount(int amount) { this.amount.set(amount); }
	public void resetAmount() { this.amount.set(this.getCapacity()); }
	
	// QUALITY ----------------------------------
	public ObjectProperty<SupplyQuality> qualityProperty() { return quality; }
	public SupplyQuality getQuality() { return quality.get(); }
	public void setQuality(SupplyQuality quality) { this.quality.set(quality); }   
	
	// EFFICIENCY -------------------------------
	public IntegerProperty efficiencyProperty() { return efficiency; }
	public int getEfficiency() { return this.efficiency.get(); }
	public void setEfficiency(int efficiency) { this.efficiency.set(efficiency); }
	
	// LIFESPAN -------------------------------
	public IntegerProperty lifespanProperty() { return lifespan; }
	public int getLifespan() { return this.lifespan.get(); }
	public void setLifespan(int lifespan) { this.lifespan.set(lifespan); }
	
	// REPLENISH --------------------------------
	public BooleanProperty replenishProperty() { return replenish; }
	public boolean getReplenish() { return this.replenish.get(); }
	public void setReplenish(boolean isReplenishing) { this.replenish.set(isReplenishing); }
	public boolean isReplenishing() { return this.replenish.get(); }
	
	// EVERY ------------------------------------
	public IntegerProperty everyProperty() { return every; } 
	public int getEvery() { return this.every.get(); }
	public void setEvery(int every) { this.every.set(every); }
	
	// UNTIL ------------------------------------
	public LongProperty untilProperty() { return until; }
	public long getUntil() { return until.get(); }
	public void setUntil(long until) { this.until.set(until); }
	
	// LASTREPLENISH ----------------------------
	public long getLastreplenish() { return lastreplenish; }
	public void setLastreplenish(long lastreplenish) { this.lastreplenish = lastreplenish; }
	
	// STARTTIME --------------------------------
	public long getStarttime() { return starttime; }
	public void setStarttime(long start) { this.starttime = start; }

	// EXPIRYDATE -------------------------------
	public long getExpirytime() { return expirytime; }
	public void setExpirytime( long expiry ) { this.expirytime = expiry; }
	
	// class logger
	private static final Logger logger = Logger.getLogger(Demand.class.getName());

	/*
	 *  Convenience Functions
	 */	
	// make supply replenishing
	public void makeReplenishing(int every, int until) {
		this.setReplenish(true);
		this.setEvery(every);
		this.setUntil(until);
	}
	
	// stop replenishment of supply
	public void stopReplenishing() {
		this.setReplenish(false);
	}
	
	// replenish amount
	public void replenishSupply() {
		
		// can we replenish?
		if(this.getReplenish()) {
		
			// replenish up to current timestamp
			while((this.getLastreplenish()+this.getEvery()) < (System.currentTimeMillis()/1000)) {
				
				// update replenishment timestamp
				this.setLastreplenish( this.getLastreplenish() + this.getEvery());
				
				// set amount to capacity
				this.resetAmount();
			}
		}
		
		// update state to exhausted if unable to replenish
		if(this.getAmount() == 0) {
			this.setState(SupplyState.EXHAUSTED);
		}
	}
	
	// update state based on expiry
	public void checkExpiry() {
		if (this.getExpirytime() <= (System.currentTimeMillis() / 1000)) {
			this.setState(SupplyState.EXPIRED);
		}
	}
	
	// check whether Supply is valid
	public boolean isUsable() {
		
		boolean retval = true;
		
		// check expiry
		this.checkExpiry();

		if (this.getState() == SupplyState.EXPIRED)
			retval = false;
		else {	
			// try to replenish if possible
			this.replenishSupply();
	
			// check if enough supply available for use
			retval = (this.getState() == SupplyState.AVAILABLE);	
		}
		
		return retval;
	}
	
	// manage Demands tied to Supply
	public boolean anyAssociatedDemands() {
		return this.associatedDemandsList.size() > 0;
	}
	public ArrayList<Demand> getAssociatedDemands(){
		return this.associatedDemandsList;
	}
	public void addAssociatedDemand(Demand d) {
		this.associatedDemandsList.add(d);
	}
	
	// print all components of supply
	public String toStringFull() {
		String retstr = "Supply \n[\n id=" + id + ",\n name=" + name + ",\n type=" + type + ",\n capacity=" + capacity
				+ ",\n quality=" + quality + ",\n efficiency=" + efficiency + ",\n replenish=" + replenish + ",\n every="
				+ every + ",\n until=" + until + ",\n lifespan=" + lifespan + ",\n starttime=" + starttime + ",\n expirytime="
				+ expirytime + ",\n lastreplenish=" + lastreplenish + "\n]";
		
		if (this.associatedDemandsList.size() > 0) {
			retstr += "\nAssociated Demands:\n";
			for (Demand d : this.associatedDemandsList) {
				retstr += d.toString();
			}
		}
		return retstr;
	}
	
	@Override
	public String toString() {
		return "Supply [name=" + name.get() + ", type=" + type.get() + ", amount=" + amount.get() + "]";
	}
	
//	public static void main(String args[]) {
//		
//		// create default supply
//		Supply defaultSupply = new Supply();
//		
//		// create basic supply
//		Supply basicSupply = new Supply(SupplyType.SKILL3, 10);
//		
//		// create fully-defined supply
//		Supply fullSupply = new Supply(UUID.randomUUID(),"My Skill",
//									   SupplyType.SKILL4, 50,
//									   SupplyQuality.MEDIUMHIGH,
//									   95, Supply.YEARVAL,
//									   true, Supply.DAYVAL,
//									   System.currentTimeMillis()/1000 + Supply.MONTHVAL);
//		
//		// add demands to Supply
//		Demand default_demand = new Demand();
//		Demand basic_demand = new Demand(DemandType.NEED4, 60*60*72, 10, 18);		
//		fullSupply.addAssociatedDemand(default_demand);
//		fullSupply.addAssociatedDemand(basic_demand);
//		
//		// make "hopper" for all demands
//		ArrayList<Supply> resourceList = new ArrayList<Supply>();
//		resourceList.add(defaultSupply);
//		resourceList.add(basicSupply);
//		resourceList.add(fullSupply);
//	
//		// print the Supplies in the list
//		resourceList.forEach(System.out::println);
//	}
}
