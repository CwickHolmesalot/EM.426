import java.util.*;
/*
 * SupplyDemandDictionary Class for Agent Based Modeling
 *  @author Chad Holmes
 *
 *  For MIT EM.426 Spring 2021 class
 *  
 *  Conceptually, the SupplyDemandDictionary stores a map of DemandTypes
 *  and a list of SupplyTypes that meet that DemandType
 *  
 *  The intent is to look up a DemandType to determine what eligible 
 *  SupplyTypes can be used to meet that Demand.  This supports the first
 *  step is determining if an Agent is capable of being matched to a Demand
 *  
 */
public class SupplyDemandDictionary {
	
	// only property is a HashTable
	private Hashtable<DemandType, ArrayList<SupplyType>> sdmap;
	
	// Constructor initializes the hashtable
	SupplyDemandDictionary(){
		sdmap = new Hashtable<DemandType, ArrayList<SupplyType>>();
	}

	// Add a DemandType, SupplyType pair
	void addSupplyDemand(DemandType dt, SupplyType st) {
		
		// check if this DemandType already exists in dictionary
		if(sdmap.containsKey(dt)) {
			// add SupplyType to existing list of SupplyTypes tied to DemandType
			sdmap.get(dt).add(st);
		}
		else {
			// DemandType is not in dictionary yet, create a new pair
			ArrayList<SupplyType> alist = new ArrayList<SupplyType>();
			alist.add(st);
			sdmap.put(dt, alist);
		}
	}
	
	// Convenience function for determining if SupplyType matches a DemandType
	boolean isValidMatch(DemandType dt, SupplyType st) {
		
		// if DemandType in dictionary, check it's paired ArrayList of SupplyTypes
		if(sdmap.containsKey(dt)) {
			return sdmap.get(dt).contains(st);
		}
		else {
			// default to not valid if not explicitly defined in dictionary
			return false;
		}
	}

	@Override
	public String toString() {
		String retstr = "SupplyDemandDictionary \n[\n";
		
		Enumeration<DemandType> names = sdmap.keys();
		while (names.hasMoreElements()) {
			DemandType nextdt = names.nextElement();
			retstr += " " + nextdt.toString() + ":";
			
			for (SupplyType st: sdmap.get(nextdt)) {
				retstr += " " + st.toString(); 
			}
			retstr += "\n";
		}
		retstr += "]\n";
		return retstr;
	}	
	
//	public static void main(String args[]) {
//		
//		SupplyDemandDictionary sdd = new SupplyDemandDictionary();
//		
//		// map needs to skills
//		sdd.addSupplyDemand(DemandType.NEED1,SupplyType.SKILL1);
//		sdd.addSupplyDemand(DemandType.NEED2,SupplyType.SKILL2);
//		sdd.addSupplyDemand(DemandType.NEED2,SupplyType.SKILL3);
//		sdd.addSupplyDemand(DemandType.NEED2,SupplyType.SKILL4);
//		sdd.addSupplyDemand(DemandType.NEED3,SupplyType.SKILL3);
//		sdd.addSupplyDemand(DemandType.NEED4,SupplyType.SKILL1);
//		sdd.addSupplyDemand(DemandType.NEED4,SupplyType.SKILL4);
//		
//		System.out.println(sdd.toString());
//	}
}
