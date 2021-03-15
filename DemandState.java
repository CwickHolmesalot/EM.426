public enum DemandState {
	DEFINED, QUEUED, ACTIVE, COMPLETE
	// DEFINED = demand is defined, but no interaction
	// QUEUED = demand is being assigned to an agent
	// ACTIVE = demand is being worked on by agent(s)
	// COMPLETE = demand has been met, no further action
}
