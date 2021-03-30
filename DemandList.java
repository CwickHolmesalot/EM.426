import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class DemandList {

	private ArrayList<Demand> demand_list;
	private PropertyChangeSupport support;
	private SupplyDemandDictionary sd_dict; // map between demands and supplies

	private int unfinished_demands;
	private int finished_demands;
	private int elapsedtime;
	
	/* 
	 * Constructors
	 */
	public DemandList() {
		demand_list = new ArrayList<Demand>();
		support = new PropertyChangeSupport(this);
		sd_dict = new SupplyDemandDictionary();
		
		this.setFinished_demands(0);
		this.setUnfinished_demands(0);
	}

	/*
	 * Convenience Functions
	 */
	public void addDemand(Demand d) {
		getDemandlist().add(d);
	}

	public void incrementTime() { 
		this.setElapsedtime(this.getElapsedtime()+1);
	}
	
	public int getDemandCount() {
		return this.getDemandlist().size();
	}
	
	public void addSupplyDemandPair(DemandType dt, SupplyType st) {
		sd_dict.addSupplyDemand(dt, st);
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
	
	public int getUnfinished_demands() {
		return unfinished_demands;
	}

	public void setUnfinished_demands(int unfinished_demands) {
		this.unfinished_demands = unfinished_demands;
	}

	public int getFinished_demands() {
		return finished_demands;
	}

	public void setFinished_demands(int finished_demands) {
		this.finished_demands = finished_demands;
	}

	public int getElapsedtime() {
		return elapsedtime;
	}

	public void setElapsedtime(int elapsedtime) {
		this.elapsedtime = elapsedtime;
	}
	
	public SupplyDemandDictionary getSupplyDemandDict() {
		return sd_dict;
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
     * Events
     */
    public void newDemand(Demand d) {
    	System.out.println("***DemandList triggering a new demand notification***");
    	
    	// add demand and keep sorted by priority
        this.demand_list.add(d);
    	Collections.sort(this.demand_list, new Comparator<Demand>() {
    		public int compare(Demand d1, Demand d2) {
    			int result = d1.getPriority().compareTo(d2.getPriority());
    			return -result; // return reverse order (high priority first)
    		}
    	});
        support.firePropertyChange("newdemand", null, this);

    }  
    public void nextTimeStep() {
    	System.out.println("***DemandList triggering another step in time***");
    	support.firePropertyChange("Step",this.getElapsedtime(),this.getElapsedtime()+1);
    	this.incrementTime();
    }
	
	/*
	// READY TO ROCK & ROLL!
	
	// TODO: consider Demands that require more than 1 Supply
	// TODO: add logic around subordinate Demand checks when determining Agent match
	
	// sort Demands by priority
	Collections.sort(demandList, new Comparator<Demand>() {
		public int compare(Demand d1, Demand d2) {
			int result = d1.getPriority().compareTo(d2.getPriority());
			return -result; // return reverse order (high priority first)
		}
	});
	
	// cycle through demand list and find agents to handle them
	for (Demand d: demandList) {
		System.out.println("Found a new Demand: "+d.toString());
		
		boolean matched = false;
		// look for a matching agent
		for (Agent a: agentList) {
			System.out.println(".looking for match with Agent: "+a.getName());
			
			if(a.demandValid(d, sdd)) {
		        String thumb = "\uD83D\uDC4D";
		        System.out.println(thumb+" Found a match! Agent being assigned Demand: "+
		        					d.toString()+"\n");
		        
		        // TODO: update Agent state, somewhere update Demand
				matched = true;
				break;
			}
		}
		if(matched == false) {
			System.out.println("X no agent is able to handle Demand: "+d.toString()+"\n");
		}*/
}
