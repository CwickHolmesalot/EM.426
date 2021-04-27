
/*
 *  DemandType for Agent Based Modeling
 *  @author Chad Holmes
 *  
 *  MIT EM.426 Spring 2021 class
 *  
 *  Generic enumerated list of for Demand states
 */
public enum DemandState {
	DEFINED, QUEUED, ACTIVE, PARTIAL, COMPLETE
	// DEFINED = demand is defined, but no interaction
	// QUEUED  = demand is available for selection by an agent
	// ACTIVE  = demand is being worked on by agent(s)
	// PARTIAL = some parts of demand are complete
	// COMPLETE = demand has been met, no further action
}
