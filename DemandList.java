import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
//import java.util.Collections;
//import java.util.Comparator;
import java.util.Iterator;

public class DemandList implements Iterable<Demand> {

	private ArrayList<Demand> demand_list;
	private PropertyChangeSupport support;
	private SupplyDemandDictionary sd_dict; // map between demands and supplies

	/* 
	 * Constructors
	 */
	public DemandList() {
		demand_list = new ArrayList<Demand>();
		support = new PropertyChangeSupport(this);
		sd_dict = new SupplyDemandDictionary();
	}
	
	/*
	 * Convenience Functions
	 */
	public void addDemand(Demand d) {
		getDemandlist().add(d);
	}
	
	public void addIfUnseen(Demand d) {
		if(!getDemandlist().contains(d)) {
			addDemand(d);
		}
	}
	
	public void reset() {
		this.getDemandlist().clear();
		this.setSupport(new PropertyChangeSupport(this));
	}

	public int getDemandCount() {
		return this.getDemandlist().size();
	}

	public void addSupplyDemandPair(DemandType dt, SupplyType st) {
		addSupplyDemandPair(dt, st, SupplyQuality.LOW);
	}
	
	public void addSupplyDemandPair(DemandType dt, SupplyType st, SupplyQuality sq) {
		sd_dict.addSupplyDemand(dt, st, sq);
	}
	
	public void printSupplyDemandMap() {
		System.out.println(sd_dict.toString());
	}
	
	/*
	 * Getters and Setters
	 */
	public ArrayList<Demand> getDemandlist() {
		return demand_list;
	}

	public void setDemandlist(ArrayList<Demand> demand_list) {
		this.demand_list = demand_list;
	}
	
	public SupplyDemandDictionary getSupplyDemandDict() {
		return sd_dict;
	}

	public PropertyChangeSupport getSupport() {
		return support;
	}

	public void setSupport(PropertyChangeSupport support) {
		this.support = support;
	}
	
    /* 
     * PropertyChangeListener Functions
     */
    public void addPropertyChangeListener(PropertyChangeListener pcl) {
        support.addPropertyChangeListener(pcl);
    }

    public void removePropertyChangeListener(PropertyChangeListener pcl) {
        support.removePropertyChangeListener(pcl);
    }
	
    /* 
     * implement Iterable
     */
    public Iterator<Demand> iterator() {
    	return this.demand_list.iterator();
    }

    /*
     * Events
     */
    public void newDemand(Demand d) {
    	newDemand(d,true);
    }
    public void newDemand(Demand d, boolean alert_agents) {
    	
    	// add demand to the demand list
    	d.setQueued();
        this.demand_list.add(d);
    	
    	// signal the demand list has changed
        if(alert_agents) {
        	System.out.println("***DemandList triggering a new demand notification***");
        	support.firePropertyChange("newdemand", this, d);
        }
    }
}
