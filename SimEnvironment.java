import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Optional;
import java.util.Random;
import java.lang.Integer;

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
	
	// to manage requests for collaboration
	private ArrayList<PropertyChangeEvent> collab_requests;
	
	// global (committed) time
	private int global_time;
	
	// helper function to ensure underutilized agents float to top
	void sortAgents() {
		
        // sort Agents by local time
    	Collections.sort(this.agent_list, new Comparator<Agent>() {
    		public int compare(Agent a1, Agent a2) {
    			if(a1.getAgentTime() < a2.getAgentTime()) {
    				return -1;
    			}
    			if(a1.getAgentTime() == a2.getAgentTime()) {
    				return 0;
    			}
    			return 1;
    		}
    	});
	}
	
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
		
		// no requests for collaboration yet
		collab_requests = new ArrayList<PropertyChangeEvent>();
		
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
		 * COLLABORATION = collaboration, only created by other agents
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
	public void createDemand(boolean alert_agents) {
		// RANDOMNESS/UNCERTAINTY
		
		// number of demand types
		int dtype = rand.nextInt(DemandType.values().length);
		
		// don't allow collaboration demands to be created here
		while (dtype == DemandType.COLLABORATE.ordinal())
			dtype = rand.nextInt(DemandType.values().length);
		
		int dprior = rand.nextInt(DemandPriority.values().length);
		
		Demand newdemand = new Demand("Demand"+String.valueOf(demand_list.getDemandCount()),
				DemandPriority.values()[dprior], 
				DemandType.values()[dtype], 
				rand.nextInt(50));
		
		System.out.println("+D Simulation created new Demand:"+newdemand.toString());
		
		// add demand to demand_list (which triggers an event)
		demand_list.newDemand(newdemand, alert_agents);
	}
	
	// randomly choose whether or not to generate a new Demand
	public void createRandomDemand(int pcntchance) {
		
		if(!this.collab_requests.isEmpty()) {
			// generate a new Demand
			demand_list.newDemand(Demand.createCollaborationDemand((Demand)(collab_requests.get(0).getOldValue()),
																   (Agent)(collab_requests.get(0).getNewValue())));
			
			// reset collab_request
			collab_requests.remove(0);
		}
		
		// RANDOMNESS/UNCERTAINTY
		if(rand.nextInt(100)<pcntchance) {
			createDemand(true);
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
			
			// have the agent pre-populate backlog from initial demands
			a.start(demand_list);
		}
	}
	
	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if(evt.getPropertyName() == "collaborate") {
			// agent needs help to complete demand
			// store collaboration request (captures who sent it)
			this.collab_requests.add(evt);
		}
		else {
			System.err.println("SimEnvironment does not know how to manage event "+evt.getPropertyName());
		}
 	}

	// Time management signals
    public void syncGlobalTime() {

    	// tell agents to finish up their tasks
        support.firePropertyChange("finishtask", 0, 1);
    	
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

		/****** CREATE SOME DEMANDS ******/
		for (int nd = 0; nd < 10; nd++) {
			sim.createDemand(false);
		}
		
		/****** DEFINE AGENTS ******/
		sim.createAgents(2,2,1);
		
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
