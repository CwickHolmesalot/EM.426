import java.util.UUID;

/*
 *  Supply Image Class
 *  @author Chad Holmes
 *  
 *  Based on Supply class concept outlined by Bryan Moser 
 *  MIT EM.426 Spring 2021 class
 *  
 *  Conceptually, the Supply Image class captures a snapshot of 
 *  the state of a Supply object for archival and for resetting
 *  Supply, e.g., in the event of an agent roll-back.
 *  
 */

public class SupplyImage {
	UUID id;
	SupplyState state;
	int amount;
	boolean replenish;
	int every;
	long until;
	long lastreplenish;
	int lifespan;
	int learningcounter;
	
	// cature image of Supply
	public SupplyImage(Supply s) {
		this.id = s.getId();
		this.state = s.getState();
		this.amount = s.getAmount();
		this.replenish = s.getReplenish();
		this.every = s.getEvery();
		this.until = s.getUntil();
		this.lastreplenish = s.getLastreplenish();
		this.lifespan = s.getLifespan();
		this.learningcounter = s.getLearningcounter();
	}
	
	// replace Supply internal state with image
	public void ReimageSupply(Supply s) {
		if(s.getId() == this.id) {
			s.setState(state);
			s.setAmount(amount);
			s.setReplenish(replenish);
			s.setEvery(every);
			s.setUntil(until);
			s.setLastreplenish(lastreplenish);
			s.setLifespan(lifespan);
			s.setLearningcounter(learningcounter);
		}
	}
}
