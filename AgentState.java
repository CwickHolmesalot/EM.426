/*
 *  AgentStatus for Agent Based Modeling
 *  @author Chad Holmes
 *  
 *  MIT EM.426 Spring 2021 class
 *  
 *  Enumerated list of States for Supplies to be in:
 * 	ACTIVE        = agent is active and completing tasks
 *  WAITING       = agent is waiting on another agent
 *  COMMUNICATING = agent is collaborating/communicating with another agent
 *  UNAVAILABLE   = agent is temporarily unavailable
 */

public enum AgentState {
	ACTIVE, WAITING, COMMUNICATING, UNAVAILABLE
}
