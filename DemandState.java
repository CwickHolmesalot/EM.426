
/*
 *  DemandType for Agent Based Modeling
 *  @author Chad Holmes
 *  
 *  MIT EM.426 Spring 2021 class
 *  
 *  Generic enumerated list of for Demand states
 */
public enum DemandState {
	DEFINED, QUEUED, ACTIVE, PARTIAL, INCOMPLETE, COMPLETE, REPLACED
	// DEFINED = demand is defined, but no interaction
	// QUEUED  = demand is available for selection by an agent
	// ACTIVE  = demand is being worked on by agent(s)
	// PARTIAL = some parts of demand are complete
	// INCOMPLETE = demand never completed (e.g., infeasible with agent mix)
	// COMPLETE = demand has been met, no further action
	// REPLACED = demand is deprecated, a new version exists
}
