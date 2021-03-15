import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.UUID;

public class Agent {

	/* 
	 * Default constructor
	 * set default name to ""
	 * set default efficiency to 85 (85%)
	 */
	public Agent() {
		this("", 85);
	}
	
	/*
	 * Full constructor
	 */
	public Agent(String name, int efficiency) {
		super();
		this.setName(name);
		this.setEfficiency(efficiency);
		
		this.rsrcs = new ArrayList<Supply>();
	}

	// member variables
	private String name;
	private int efficiency;
	
	// list of Supply (resources, talent, skills)
	public ArrayList<Supply> rsrcs;
	
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
	
	/* 
	 * Helper Functions
	 */
	// return true if Demand can be met with existing supplies
	boolean demandValid(Demand d, SupplyDemandDictionary sdd) {
		
		// cycle through supplies
		for (Supply s : this.rsrcs){
			
			System.out.println("..checking against Supply: "+s.getName());
			
			// look for a match using SupplyDemandDictionary
			if(sdd.isValidMatch(d.getType(), s.getType())) {
				
				// found match, but is this Demand doable?
				System.out.println("...found a match between "+d.toString()+" and "+s.toString());
				
				// use efficiency*supply amount and compare to demand effort
				if(((double)(this.getEfficiency())/100.)*(double)(s.getAmount()) >= (double)(d.getEffort())) {
					System.out.println("...agent has enough remaining capacity! demand is valid.");
					return true;
				}
				
				System.out.println("...agent does not have enough remaining capacity. demand effort="+
									String.valueOf(d.getEffort())+
									", agent capacity="+String.valueOf(s.getAmount())+
									", agent efficiency="+String.valueOf(this.getEfficiency())+
									", agent ability="+String.valueOf(this.getEfficiency()*s.getAmount()/100));
			}
		}
		
		return false;
	}

	public static void main(String args[]) {
		
		// create dictionary for matching skills with needs
		SupplyDemandDictionary sdd = new SupplyDemandDictionary();
		
		// define what demands and supplies match with each other
		sdd.addSupplyDemand(DemandType.NEED1,SupplyType.SKILL1);
		sdd.addSupplyDemand(DemandType.NEED2,SupplyType.SKILL2);
		sdd.addSupplyDemand(DemandType.NEED2,SupplyType.SKILL3);
		sdd.addSupplyDemand(DemandType.NEED2,SupplyType.SKILL4);
		sdd.addSupplyDemand(DemandType.NEED3,SupplyType.SKILL3);
		sdd.addSupplyDemand(DemandType.NEED4,SupplyType.SKILL1);
		sdd.addSupplyDemand(DemandType.NEED4,SupplyType.SKILL4);
		
		System.out.println(sdd.toString());
		
		// define 3 agents
		Agent geo = new Agent("Geologist",50);
		Agent eng = new Agent("Engineer",75);
		Agent mgr = new Agent("Manager",25);
		
		// give them skills with capacity is in seconds
		Supply skill_interp = new Supply("interp", SupplyType.SKILL1, 10, SupplyQuality.HIGH);
		Supply skill_assess = new Supply("assess", SupplyType.SKILL2, 20, SupplyQuality.MEDIUM);
		Supply skill_lead   = new Supply("lead",   SupplyType.SKILL3, 25, SupplyQuality.MEDIUM);

		// assign skills to agents
		geo.rsrcs.add(skill_interp);
		eng.rsrcs.add(skill_assess);
		mgr.rsrcs.add(skill_lead);
		
		// add them to a list (pooled agent resources)
		ArrayList<Agent> agentList = new ArrayList<Agent>();
		agentList.add(geo);
		agentList.add(eng);
		agentList.add(mgr);
		
		// define some demands
		Demand demand_interp = new Demand("interp", DemandPriority.HIGH, DemandType.NEED1, 8);
		Demand demand_assess = new Demand("assess", DemandPriority.MEDIUMHIGH, DemandType.NEED2, 10);
		Demand demand_lead   = new Demand("lead",   DemandPriority.MEDIUMLOW, DemandType.NEED3, 30);
		
		// add demands to list
		ArrayList<Demand> demandList = new ArrayList<Demand>();
		demandList.add(demand_interp);
		demandList.add(demand_assess);
		demandList.add(demand_lead);
		
		// READY TO ROCK & ROLL!
		
		// TODO: consider Demands that require more than 1 Supply
		// TODO: check for Supply expiration before Demand completion
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
			}
		}
	}
}