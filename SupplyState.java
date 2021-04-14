/*
 *  SupplyType for Agent Based Modeling
 *  @author Chad Holmes
 *  
 *  MIT EM.426 Spring 2021 class
 *  
 *  Enumerated list of States for Supplies to be in:
 * 	AVAILABLE = supply exists and is ready for use
 *  EXPIRED   = supply is past its expiry and is no longer usable
 *  EXHAUSTED = supply has been fully used up
 *  
 */

public enum SupplyState {
	AVAILABLE, EXPIRED, EXHAUSTED
}
