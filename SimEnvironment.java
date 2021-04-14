import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Random;

/*
 *  SimEnvironment for Agent Based Modeling
 *  @author Chad Holmes
 *  
 *  MIT EM.426 Spring 2021 class
 *  
 *  The SimEnvironment class creates the simulation environment for
 *  Agent Based Modeling. The primary purpose of SimEnvironment includes:
 *  1. build initial list of Demands
 *  2. create Agents for the simulation
 *  3. generate new Demands over time (randomly)
 *  4. add new demands to a DemandList
 *  5. keep track of global time as the minimum local agent time
 *  ... the magic of Agents handling Demands happens on its own 
 */

public class SimEnvironment implements PropertyChangeListener {

	/* 
	 * Member variables for simulation environment
	 */
	private DemandList demand_list; // list of demands
	private Random rand;
	
	// keep track of Agents here so they don't get garbage collected
	private ArrayList<Agent> agent_list;
	private PropertyChangeSupport support;
	
	// global (committed) time
	private int global_time;
	
	/* 
	 * Constructors
	 */
	public SimEnvironment() {
		
		// list of all demands in simulation
		demand_list = new DemandList();
		
		// list of all agents in simulation
		agent_list = new ArrayList<Agent>();
		
		// random number generator
		rand = new Random();
		rand.setSeed(43);
		
		// PropertyChangeListener set-up
		support = new PropertyChangeSupport(this);
		
		// initialize environment
		initialize();
	}
	
	/*
	 * Helper Functions
	 */
	private void initialize() {
	
		// create dictionary for matching skills with needs
		
		/* For this example, skills assignments:
		 * SKILL1 = develop
		 * SKILL2 = analyze
		 * SKILL3 = model
		 * SKILL4 = communicate
		 * SKILL5 = manage
		 */
		
		/* For this example, needs assignments:
		 * NEED1 = software development
		 * NEED2 = data analysis
		 * NEED3 = model building
		 * NEED4 = communication
		 * NEED5 = project management
		 */
		
		// define what demands and supplies match with each other
		demand_list.addSupplyDemandPair(DemandType.NEED1,SupplyType.SKILL1);
		demand_list.addSupplyDemandPair(DemandType.NEED2,SupplyType.SKILL2);
		demand_list.addSupplyDemandPair(DemandType.NEED3,SupplyType.SKILL2);
		demand_list.addSupplyDemandPair(DemandType.NEED3,SupplyType.SKILL3);
		demand_list.addSupplyDemandPair(DemandType.NEED4,SupplyType.SKILL4);
		demand_list.addSupplyDemandPair(DemandType.NEED5,SupplyType.SKILL4);
		demand_list.addSupplyDemandPair(DemandType.NEED5,SupplyType.SKILL5);

		// view mapping
		demand_list.printSupplyDemandMap();
		
		// set global time
		this.setGlobaltime(0);
	}
	
	// Getters and Setters
	public int getGlobaltime() {
		return global_time;
	}

	public void setGlobaltime(int globaltime) {
		this.global_time = globaltime;
	}
	
	// generate demands for demand_list
	public void createDemand() {
		// RANDOMNESS/UNCERTAINTY
		
		// number of demand types
		int dtype = rand.nextInt(DemandType.values().length);
		int dprior = rand.nextInt(DemandPriority.values().length);
		
		Demand newdemand = new Demand("Demand"+String.valueOf(demand_list.getDemandCount()),
				DemandPriority.values()[dprior], 
				DemandType.values()[dtype], 
				rand.nextInt(50));
		
		System.out.println("\n+D Simulation created new Demand:"+newdemand.toString()+"\n");
		
		// add demand to demand_list (which triggers an event)
		demand_list.newDemand(newdemand);
	}
	
	// randomly choose whether or not to generate a new Demand
	public void createRandomDemand(int pcntchance) {
		
		// RANDOMNESS/UNCERTAINTY
		if(rand.nextInt(100)<pcntchance) {
			createDemand();
		}
	}
	
	public void createAgents(int nEng, int nSci, int nMgr) {

		for ( int e = 0; e < nEng; e++)
			agent_list.add(new EngineerAgent("Engineer"+(e+1)));
		
		for ( int s = 0; s < nSci; s++)
			agent_list.add(new ScienceAgent("Scientist"+(s+1)));
		
		for ( int m = 0; m < nMgr; m++)
			agent_list.add(new ManagerAgent("Manager"+(m+1)));
		
		for (Agent a : agent_list) {
			
			// give agent the supply-demand map
			a.setSupplyDemandDictionary(demand_list.getSupplyDemandDict());
			
			// randomly assign different horizon times to agents
			a.setHorizonTime(rand.nextInt(100));
			
			// bind agents to property change supports
			demand_list.addPropertyChangeListener(a);
			support.addPropertyChangeListener(a);
			a.addPropertyChangeListener(this);
			
			// turn agent loose
			a.start();
		}
	}
	
	@Override
	public void propertyChange(PropertyChangeEvent evt) {
//		if(evt.getPropertyName() == "agenttimecheck") {
//			
//
//		}
//		else {
			System.err.println("SimEnvironment does not know how to manage event "+evt.getPropertyName());
//		}
 	}

	// Time management signals
    public void syncGlobalTime() {

    	int  min_time = Integer.MAX_VALUE;
    	
    	// check all agents to identify min local time
		for (Agent a : this.agent_list) {
			if(a.getAgentTime() < min_time) {
				min_time = a.getAgentTime();
			}
		}
    	
    	// update all agents to solidify completed tasks
        support.firePropertyChange("commituntil", null, min_time);
        
        // update global time stamp
		this.setGlobaltime(min_time);
    }
	
	public static void main(String args[]) {
				
		/****** CREATE ENVIRONMENT ******/
		SimEnvironment sim = new SimEnvironment();

		/****** DEFINE AGENTS ******/
		sim.createAgents(5,5,2);		

		/*
		 * Start simulation
		 */
		for (int i = 0; i < 300; i++) {			
			
			/****** GENERATE DEMANDS ******/
			sim.createRandomDemand(50);
			
			try
			{
				// pause before next time step
			    Thread.sleep(1000);
			}
			catch(InterruptedException ex)
			{
				// kill loop if an interrupt is captured
			    Thread.currentThread().interrupt();
			    break;
			}
			
			// update simulation global time
	        sim.syncGlobalTime();
		}
	}
}
