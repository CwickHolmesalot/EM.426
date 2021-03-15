import java.util.UUID;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.property.LongProperty;

/*
 * ISupply Interface
 * @author Chadwick Holmes
 *
 */

public interface ISupply {
/* 
 * interface for a Supply class capturing the available resources for use in an Agent Based
 * Modeling scenario. Supply will be depleted to meet Demand. Agents own Supply.
 * 
 */
	// UNIQUE IDENTIFIER ------------------------
	public UUID getId();
	public void setId(UUID id ); 
	
	// NAME -------------------------------------
	public StringProperty nameProperty();
	public String getName();
	public void setName(String name);

	// TYPE -------------------------------------
	public ObjectProperty<SupplyType> typeProperty();
	public SupplyType getType();
	public void setType(SupplyType type);  
	
	// STATE ------------------------------------
	public ObjectProperty<SupplyState> stateProperty();
	public SupplyState getState();
	public void setState(SupplyState state);

	// CAPACITY ---------------------------------
	public IntegerProperty capacityProperty();
	public int getCapacity();
	public void setCapacity(int capacity);  
	
	// QUALITY ----------------------------------
	public ObjectProperty<SupplyQuality> qualityProperty();
	public SupplyQuality getQuality();
	public void setQuality(SupplyQuality quality);  
	
	// EFFICIENCY -------------------------------
	public IntegerProperty efficiencyProperty();
	public int getEfficiency();
	public void setEfficiency(int efficiency);  

	// REPLENISH --------------------------------
	public BooleanProperty replenishProperty();
	public boolean getReplenish();
	public void setReplenish(boolean isReplenishing);
	public boolean isReplenishing();
	
	// (REPLENISH) EVERY ------------------------
	public IntegerProperty everyProperty();
	public int getEvery();
	public void setEvery(int every);
	
	// (REPLENISH) UNTIL-------------------------
	public LongProperty untilProperty();
	public long getUntil();
	public void setUntil(long until);	
	
	// LIFESPAN ---------------------------------
	public IntegerProperty lifespanProperty();
	public int getLifespan();
	public void setLifespan(int lifespan);
}
