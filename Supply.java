import java.util.*;
//import java.util.logging.Logger;

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
	
//	static public final int DAYVAL = 60*60*24;
//	static public final int HOURVAL = 60*60;
//	static public final int WEEKVAL = 60*60*24*7;
//	static public final int YEARVAL = 60*60*24*365;
//	static public final int MONTHVAL = 60*60*24*365/12; // average over a year
	
	static private final SupplyQuality DEFAULT_QUALITY = SupplyQuality.MEDIUM;
	static private final int DEFAULT_CAPACITY = 9999;
	static private final int DEFAULT_EFFICIENCY = 85;
	static private final int DEFAULT_LIFESPAN = 9999;
	static private final int DEFAULT_EVERY = 1;
	static private final int DEFAULT_UNTIL = 9999;
	
	static private final int DEFAULT_REPLENISH_TIME = 8; // assume full working day to "refresh"
	
	/* 
	 * Supply constructors
	 */
	
	/*
	 * DEFAULT constructor: 
	 */
	public Supply() {
		this(SupplyType.SKILL1, DEFAULT_CAPACITY);
	}
	
	/* 
	 * BASIC constructor: only requires a type, effort, start, and finish
	 * UID is randomly assigned
	 * identifier set to empty string
	 * type set to input type value
	 * capacity set to input capacity value
	 * replenish set to true
	 * quality, efficiency, lifespan, every, until set to defaults
	 */
	public Supply(SupplyType type, int capacity) {
		this(UUID.randomUUID(), "", type, capacity, 
				Supply.DEFAULT_QUALITY,
				Supply.DEFAULT_EFFICIENCY, 
				Supply.DEFAULT_LIFESPAN, 
				true, 
				Supply.DEFAULT_EVERY, 
				Supply.DEFAULT_UNTIL);
	}
	
	/* 
	 * BASIC constructor: only requires a type, effort, start, and finish
	 * UID is randomly assigned
	 * name set to input string value
	 * type set to input type value
	 * capacity set to input capacity value
	 * quality set to input quality value
	 * efficiency set to input efficiency value
	 * replenish set to true
	 * efficiency, lifespan, every, until set to defaults
	 */
	public Supply(String name, SupplyType type, int capacity, SupplyQuality quality) {
		this(UUID.randomUUID(), name, type, capacity, quality, 
				Supply.DEFAULT_EFFICIENCY, 
				Supply.DEFAULT_LIFESPAN, 
				true, 
				Supply.DEFAULT_EVERY, 
				Supply.DEFAULT_UNTIL);
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
		this.setLearningthreshold(300);
		
		// set learning counter to 0
		this.resetLearning();

		this.setState(SupplyState.AVAILABLE);
		this.setLastreplenish(-1);
		this.resetAmount();
	}

	/* 
	 * Member Variables
	 */
	private UUID id; // universal unique id

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
	
	// learning rate
	private int learningthreshold;
	private int learningcounter;

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
	public void reduceAmount(int byamount) { 
		int remainder = byamount-this.getAmount();
		if(remainder<=0) {
			this.setAmount(-remainder);
		}
		else {
			while(remainder > 0) {
				if(this.getState() != SupplyState.AVAILABLE) {
					this.setAmount(0);
					break;
				}
				else if(this.isReplenishing()) {
					this.replenishSupply();
					remainder = remainder-this.getAmount();
				}
			}
			this.setAmount(-remainder);
		}
		this.learn(byamount-Math.max(remainder, 0));
	}
	public void increaseAmount(int byamount) { 
		this.setState(SupplyState.AVAILABLE);		
		int addition = this.getAmount()+byamount;
		while(addition > this.getCapacity()) {
			addition -= this.getCapacity();
			this.setReplenish(true);
		}
		this.setAmount(addition);
	}
	
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
	
	// LEARNINGTHRESHOLD ------------------------
	public int getLearningthreshold() {return learningthreshold;}
	public void setLearningthreshold(int learningthreshold) {this.learningthreshold = learningthreshold;}
	public void resetLearning() {this.learningcounter = 0;}

	// LEARNINGCOUNTER --------------------------
	public int getLearningcounter() {return learningcounter;}
	public void setLearningcounter(int learningcounter) {this.learningcounter = learningcounter;}
	
	public int getReplenishTime() {return Supply.DEFAULT_REPLENISH_TIME;}
	
	// class logger
	//private static final Logger logger = Logger.getLogger(Demand.class.getName());

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
		
			// TODO: manage expiration and Supply "time" better
			// short circuit for now
			this.resetAmount();
			
//			// replenish up to current timestamp
//			while((this.getLastreplenish()+this.getEvery()) < (System.currentTimeMillis()/1000)) {
//				
//				// update replenishment timestamp
//				this.setLastreplenish( this.getLastreplenish() + this.getEvery());
//				
//				// set amount to capacity
//				this.resetAmount();
//			}
		}
		
		// update state to exhausted if unable to replenish
		if(this.getAmount() == 0) {
			this.setState(SupplyState.EXHAUSTED);
		}
	}
	
	// update state based on expiry
	public void checkExpiry() {
		
		// TODO: manage Supply "time" better
		
//		if (this.getExpirytime() <= (System.currentTimeMillis() / 1000)) {
//			this.setState(SupplyState.EXPIRED);
//		}
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
	
	// update SupplyQuality based on experience
	private void learn(int experience) {
		if(this.getLearningthreshold() < (this.learningcounter + experience)) {

			System.out.println("+S "+this.getName()+" increased in quality from learning");

			if((this.getQuality().ordinal()+1) < SupplyQuality.values().length) {
				this.setQuality(SupplyQuality.values()[this.getQuality().ordinal()+1]);
				this.resetLearning();
			}
			else {
				// already at highest quality of skill!  nothing to do here.
			}
		}
		else {
			this.learningcounter += experience;
		}
	}
	
	// print all components of supply
	public String toStringFull() {
		String retstr = "Supply \n[\n id=" + id + ",\n name=" + name + ",\n type=" + type + ",\n capacity=" + capacity
				+ ",\n quality=" + quality + ",\n efficiency=" + efficiency + ",\n replenish=" + replenish + ",\n every="
				+ every + ",\n until=" + until + ",\n lifespan=" + lifespan + ",\n starttime=" + starttime + ",\n expirytime="
				+ expirytime + ",\n lastreplenish=" + lastreplenish + "\n]";
		
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
