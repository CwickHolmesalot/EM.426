import java.util.ArrayList;

/*
 *  LedgerEntry Class for Agent Based Modeling
 *  @author Chad Holmes
 *  
 *  Used for capturing a record of Demand completion
 *  
 */
public class LedgerEntry {
	int timeAtStart;
	int timeAtFinish;
	ArrayList<SupplyImage> si;
	
	public LedgerEntry(int timeAtStart, int effort, ArrayList<SupplyImage> si) {
		super();
		this.timeAtStart = timeAtStart;
		this.timeAtFinish = timeAtStart+effort;
		this.si = si;
	}
	
	public int getEffort() {
		return timeAtFinish-timeAtStart;
	}
	
	public int getTimeAtStart() {
		return timeAtStart;
	}
	public void setTimeAtStart(int timeAtStart) {
		this.timeAtStart = timeAtStart;
	}
	public int getTimeAtFinish() {
		return timeAtFinish;
	}
	public void setTimeAtFinish(int timeAtFinish) {
		this.timeAtFinish = timeAtFinish;
	}
	public ArrayList<SupplyImage> getSi() {
		return si;
	}
	public void setSi(ArrayList<SupplyImage> si) {
		this.si = si;
	}
}
