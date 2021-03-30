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
 *  3. connect Agents to a DemandList
 *  4. trigger the passage of time
 *  5. generate new Demands over time (randomly)
 *  6. add new demands to a DemandList
 *  ... the magic of Agents handling Demands happens on its own 
 */

public class SimEnvironment {

	/* 
	 * Member variables for simulation environment
	 */
	private DemandList demand_list;         // list of demands
	private Random rand;
	
	// keep track of Agents here so they don't get garbage collected
	private ArrayList<Agent> agent_list;
	
	/* 
	 * Constructors
	 */
	public SimEnvironment() {
		demand_list = new DemandList();
		agent_list = new ArrayList<Agent>();
		rand = new Random();
		rand.setSeed(43);
		
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
	}
	
	// generate demands for demand_list
	public void createDemand() {
		
		// randomly create a demand
		
		// number of demand types
		int ndt = DemandType.values().length;
		int dtype = rand.nextInt(ndt);
		
		int ndp = DemandPriority.values().length;
		int dprior = rand.nextInt(ndp);
		
		Demand newdemand = new Demand("Demand"+String.valueOf(demand_list.getDemandCount()),
				DemandPriority.values()[dprior], 
				DemandType.values()[dtype], 
				rand.nextInt(50));
		
		System.out.println("\n+D Simulation created new Demand:"+newdemand.toString()+"\n");
		
		// add demand to demand_list (which triggers an event)
		demand_list.newDemand(newdemand);
	}
	
	public void createAgents(int numAgents) {

		//TODO: implement createAgents with ability to create numAgents agents
		// using random number generator to determine which kind
		
		//Random rand = new Random();
		
		agent_list.add(new EngineerAgent());
		agent_list.add(new ScienceAgent());
		agent_list.add(new ManagerAgent());
		
		for (Agent a : agent_list) {
			
			// connect agent with demand list
			demand_list.addPropertyChangeListener(a);
			
			// turn agent loose
			a.start();
		}		
	}
	
	public static void main(String args[]) {
				
		/****** CREATE ENVIRONMENT ******/
		SimEnvironment sim = new SimEnvironment();

		/****** DEFINE AGENTS ******/
		sim.createAgents(3);		

		/*
		 * Start simulation
		 */
		while(sim.demand_list.getElapsedtime() < 300) {
			
			sim.demand_list.nextTimeStep();

			/****** GENERATE DEMANDS ******/
			if(sim.demand_list.getElapsedtime() % 25 == 1) {
				sim.createDemand();
			}
			
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
		}
	}
}
