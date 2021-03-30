import java.util.*;

import javafx.util.Pair;
/*
 * SupplyDemandDictionary Class for Agent Based Modeling
 *  @author Chad Holmes
 *
 *  For MIT EM.426 Spring 2021 class
 *  
 *  Conceptually, the SupplyDemandDictionary stores a map of DemandTypes
 *  and a list of SupplyTypes that collectively meet that DemandType
 *  
 *  The intent is to look up a DemandType to determine what SupplyTypes \
 *  are necessary to meet that Demand.  This supports the first step in
 *  determining if an Agent is capable of being matched to a Demand
 *  
 */
public class SupplyDemandDictionary {
	
	// only property is a HashTable
	//private Hashtable<DemandType, ArrayList<SupplyType>> sdmap;
	private Hashtable<DemandType, ArrayList<Pair<SupplyType, SupplyQuality>>> sdmap;
	
	/*
	 *  Constructor
	 */
	SupplyDemandDictionary(){
		sdmap = new Hashtable<DemandType, ArrayList<Pair<SupplyType,SupplyQuality>>>();
	}

	// Add a DemandType, SupplyType pair
	void addSupplyDemand(DemandType dt, SupplyType st, SupplyQuality sq) {
		
		// check if this DemandType already exists in dictionary
		if(sdmap.containsKey(dt)) {
			// add SupplyType to existing list of SupplyTypes tied to DemandType
			sdmap.get(dt).add(new Pair<SupplyType,SupplyQuality>(st,sq));
		}
		else {
			// DemandType is not in dictionary yet, create a new pair
			ArrayList<Pair<SupplyType,SupplyQuality>> alist = new ArrayList<Pair<SupplyType,SupplyQuality>>();
			alist.add(new Pair<SupplyType,SupplyQuality>(st,sq));
			sdmap.put(dt, alist);
		}
	}
	
	// Convenience function for determining if a Supply matches a DemandType
	boolean isValidMatch(DemandType dt, Supply s) {
		return isValidMatch(dt, s.getType(), s.getQuality());
	}
	
	// Convenience function for determining if a SupplyType of a certain SupplyQuality matches a DemandType
	boolean isValidMatch(DemandType dt, SupplyType st, SupplyQuality sq) {
		// if DemandType in dictionary, check it's paired ArrayList of SupplyType
		if(sdmap.containsKey(dt)) {
			for (Pair<SupplyType,SupplyQuality> p : sdmap.get(dt)) {
				
				// look for match based on SupplyType and Quality
				// specifically, sq must be greater or equality to quality in map
				if(p.getKey()==st && (sq.ordinal() >= p.getValue().ordinal())) {
					return true;
				}
			}
		}
		return false;
	}
	
	// Convenience function for determining if a list of SupplyTypes matches a DemandType
	boolean isValidMatch(DemandType dt, ArrayList<Supply> sts) {
		
		// if DemandType in dictionary, check it's paired ArrayList of SupplyType
		if(sdmap.containsKey(dt)) {
			
			// pull required list of SupplyTypes
			ArrayList<Pair<SupplyType,SupplyQuality>> req_sts = sdmap.get(dt);
			
			// cycle through required SupplyTypes
			boolean allfound = true;
			for (Pair<SupplyType,SupplyQuality> req : req_sts) {
			
				// cycle through supplies to look for a match
				boolean found = false;
				for (Supply chk : sts) {
					
					// is there a match with the current required SupplyType and SupplyQuality?
					if(chk.getType() == req.getKey() && 
                       chk.getQuality().ordinal() >= req.getValue().ordinal() ) {
						// found a match!
						found=true;
						break;
					}
				}
				
				// track all found
				allfound = allfound & found;
				
				// short circuit if one not found
				if(!allfound)
					break;
			}
			
			return allfound;
		}
		else {
			// default to not valid if not explicitly defined in dictionary
			System.err.println("DemandType not found in SupplyDemandDisctionary: "+dt.toString());
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
			
			for (Pair <SupplyType,SupplyQuality> pr: sdmap.get(nextdt)) {
				retstr += " " + pr.getKey().toString() + 
						  " (" + pr.getValue().toString() + ")"; 
			}
			retstr += "\n";
		}
		retstr += "]\n";
		return retstr;
	}	
}
