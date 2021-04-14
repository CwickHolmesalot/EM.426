import java.util.UUID;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.StringProperty;

/*
 * IDemand Interface
 * @author Bryan Moser
 * 
 */

public interface IDemand { 
	// A property, from JavaFX, allows the attributes to be observable (and bindable)
	// if only working with a text UI, these attributes could be their plain type (e.g. int, String)
	// BELOW is a BUNCH of boilerplate code for getting, setting, and accessing properties
	// Yes, it is verbose, one of the challenges of Java. In most cases can be automatically generated.
	// While it would be MUCH simpler to just have a public property which is then .get() and .set(x),
	// it is best practice and convention in Java to encapsulate internal variables and to conform to
	// convention and backward compatibility by have the get and set calls.  
	// Take a look at the alternative code which is much shorter,
	// but which would lead to less flexibility and compatibility in later development
	
	public UUID getId();
	public void setId(UUID id ); 
	
	// NAME -------------------------------------
	public StringProperty nameProperty();
	public String getName();
	public void setName(String name);
	
	// PRIORITY ---------------------------------
	public ObjectProperty<DemandPriority> priorityProperty();
	public DemandPriority getPriority();
	public void setPriority(DemandPriority priority); 
	
	// TYPE -------------------------------------
	public ObjectProperty<DemandType> typeProperty();
	public DemandType getType();
	public void setType(DemandType type);  
	
	// STATE ------------------------------------
	public ObjectProperty<DemandState> stateProperty();
	public DemandState getState();
	public void setState(DemandState state);
	
	// EFFORT -----------------------------------
	public IntegerProperty effortProperty();
	public int getEffort();
	public void setEffort(int nominalEffort_seconds);  
	
	/*  
	 * Returns the effort of this demand in hours (convenience method) 
	 */ 
	public double getEffortHrs();
	public void setEffortHrs(double _effort); 
	
	// START ------------------------------------
	public IntegerProperty startProperty();
	public int getStart();
	public void setStart(int start);
	
	// STOP -------------------------------------
	public IntegerProperty stopProperty();
	public int getStop();
	public void setStop(int stop);
	
	// RECUR ------------------------------------
	public BooleanProperty recurProperty();
	public boolean getRecur();
	public void setRecur(boolean isRecurring);
	public boolean isRecur();
	
	// EVERY ------------------------------------
	public IntegerProperty everyProperty();
	public int getEvery();
	public void setEvery(int every);
	
	// UNTIL ------------------------------------
	public IntegerProperty untilProperty();
	public int getUntil();
	public void setUntil(int until);
}