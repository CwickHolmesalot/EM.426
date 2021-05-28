import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.lang.Math;
import java.util.Random;
import java.util.UUID;

import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.XYChart.Data;

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

	/****** CONTROLS ******/
	public SimpleIntegerProperty n_sci_agents;
	public SimpleIntegerProperty n_eng_agents;
	public SimpleIntegerProperty n_mgr_agents;
	public SimpleDoubleProperty  progress;
	public SimpleIntegerProperty n_cycles;
	public SimpleBooleanProperty finished;
	public SimpleIntegerProperty interax_lr;
	public SimpleIntegerProperty prob_new_demand;
	public SimpleIntegerProperty n_init_demands;

	// allow monte carlo
	public ArrayList<ArrayList<Integer>> mc_rx_timeseries;
	private final int MC_NUM_RUNS = 1;
	
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
	private ArrayList<PropertyChangeEvent> resync_requests;
	
	// properties for JavaFX interface
	public SimpleIntegerProperty global_time;
	public SimpleIntegerProperty n_agents;
	public SimpleIntegerProperty n_collaborations;
	public SimpleIntegerProperty n_completed;
	public SimpleIntegerProperty n_committed;
	
	// time series for tracking progress
	public SimpleListProperty<Data<Number,Number>> complete_timeseries;
	public SimpleListProperty<Data<Number,Number>> commit_timeseries;
	public SimpleListProperty<Data<Number,Number>> collab_timeseries;
	
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
	
		mc_rx_timeseries = new ArrayList<ArrayList<Integer>>();
		
		// random number generator
		rand = new Random();
		rand.setSeed(43);
		
		// simulation parameters
		n_agents = new SimpleIntegerProperty();         // number of agents in simulation
		n_cycles = new SimpleIntegerProperty();         // number of cycles to simulate
		n_init_demands = new SimpleIntegerProperty();   // number of Demands to create for initial pool
		prob_new_demand = new SimpleIntegerProperty();  // probability a demand is generated per cycle
		interax_lr = new SimpleIntegerProperty();       // learning rate from collaborations
		
		progress = new SimpleDoubleProperty();          // simulation progress
		finished = new SimpleBooleanProperty();         // has simulation completed?
		global_time = new SimpleIntegerProperty();      // simulation global time
		n_collaborations = new SimpleIntegerProperty(); // number of agent collaborations
		n_completed = new SimpleIntegerProperty();      // number of demand completions
		n_committed = new SimpleIntegerProperty();      // number of demand commits
		n_sci_agents = new SimpleIntegerProperty();     // number of ScientistAgents in the sim
		n_eng_agents = new SimpleIntegerProperty();     // number of EngineerAgents in the sim
		n_mgr_agents = new SimpleIntegerProperty();     // number of ManagerAgents in the sim
		
		agent_list = new ArrayList<Agent>();
		collab_requests = new ArrayList<PropertyChangeEvent>();
		resync_requests = new ArrayList<PropertyChangeEvent>();
		
		complete_timeseries = new SimpleListProperty<Data<Number,Number>>(FXCollections.observableArrayList());
		commit_timeseries   = new SimpleListProperty<Data<Number,Number>>(FXCollections.observableArrayList());
		collab_timeseries   = new SimpleListProperty<Data<Number,Number>>(FXCollections.observableArrayList());
		
		this.setNumAgents(5);
		this.setNumCycles(200);
		this.setNumInitDemands(20);
		this.setProbNewDemand(0);

		// list of all demands in simulation
		demand_list = new DemandList();
		
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
		demand_list.addSupplyDemandPair(DemandType.NEED2,SupplyType.SKILL1);
		demand_list.addSupplyDemandPair(DemandType.NEED2,SupplyType.SKILL2);
		demand_list.addSupplyDemandPair(DemandType.NEED2,SupplyType.SKILL4);
		demand_list.addSupplyDemandPair(DemandType.NEED3,SupplyType.SKILL2);
		demand_list.addSupplyDemandPair(DemandType.NEED3,SupplyType.SKILL3);
		demand_list.addSupplyDemandPair(DemandType.NEED4,SupplyType.SKILL4);
		demand_list.addSupplyDemandPair(DemandType.NEED5,SupplyType.SKILL4);
		demand_list.addSupplyDemandPair(DemandType.NEED5,SupplyType.SKILL5);

		// view mapping
		demand_list.printSupplyDemandMap();

		// initialize environment
		initialize();
	}

	/*
	 * Helper Functions
	 */
	private void initialize() {

		// clear any existing demands
		demand_list.reset();
		
		// list of all agents in simulation
		agent_list.clear();
		
		// PropertyChangeListener set-up
		support = new PropertyChangeSupport(this);
		
		// no requests for collaboration yet
		collab_requests.clear();
		
		// no resync requests yet
		resync_requests.clear();
		
		// set default values
		this.setProgress(0.0);
		this.setFinished(false);
		this.setGlobalTime(0);
		this.setNumCollaborations(0);
		this.setNumCompleted(0);
		this.setNumCommitted(0);
		
		// clear timeseries
		complete_timeseries.clear();
		commit_timeseries.clear();		
		collab_timeseries.clear();
	}

	// generate demands for demand_list
	public void createDemand(boolean alert_agents) {
		// RANDOMNESS/UNCERTAINTY
		
		// number of demand types
		int dtype = rand.nextInt(DemandType.values().length);
		
		// don't allow collaboration demands to be created here
		if(this.getNumAgents()==2) {
			// force collaboration on NEED3 for 2 agent stress test
			dtype = DemandType.NEED3.ordinal();
		}
		else {
			while (dtype == DemandType.COLLABORATE.ordinal())
				dtype = rand.nextInt(DemandType.values().length);
		}
		
		int dprior = rand.nextInt(DemandPriority.values().length);
		// don't allow urgent demands to be created here (save those for collaboration)
		while (dprior == DemandPriority.URGENT.ordinal())
			dprior = rand.nextInt(DemandPriority.values().length);
		
		Demand newdemand = new Demand("Demand"+String.valueOf(demand_list.getDemandCount()),
				DemandPriority.values()[dprior], 
				DemandType.values()[dtype], 
				rand.nextInt(25)+1);  // demand effort between 1 and 25
		
		System.out.println("+D Simulation created new demand: "+newdemand.toString());
		
		// add demand to demand_list (which triggers an event)
		demand_list.newDemand(newdemand, alert_agents);
	}
	
	// randomly choose whether or not to generate a new Demand
	public void createRandomDemand() {
		
		// RANDOMNESS/UNCERTAINTY
		if(rand.nextInt(100)<this.getProbNewDemand()) {
			createDemand(true);
		}
	}
	
	// agent factory function
	public void createAgents() {
		
		if(agent_list.size()>0) {
			// clear old agents
			agent_list.clear();
		}
		
		// validation test with 1 agent
		if(!(this.getNumAgents()>1)) {
			// validation test with 1 agent
			Agent supAgent = new SuperAgent("Super1");
			agent_list.add(supAgent);
		}
		else if(!(this.getNumAgents()>2)) {
			// validation test with 2 agents
			Agent yinAgent = new YinAgent("AgentA");
			Agent yangAgent = new YangAgent("AgentB");
			agent_list.add(yinAgent);
			agent_list.add(yangAgent);
		}
		else {
			for ( int e = 0; e < this.getNumEngAgents().get(); e++) {
				Agent engAgent = new EngineerAgent("Engineer"+(e+1));
				
				// make engineers impatient for waiting
				engAgent.setMaxWaitCycles(4);
				
				agent_list.add(engAgent);
			}
		
			for ( int s = 0; s < this.getNumSciAgents().get(); s++) {
				Agent sciAgent = new ScienceAgent("Scientist"+(s+1));
				
				// make scientists tolerant of waiting
				sciAgent.setMaxWaitCycles(5);
				
				agent_list.add(sciAgent);
			}
			
			for ( int m = 0; m < this.getNumMgrAgents().get(); m++) {
				Agent mgrAgent = new ManagerAgent("Manager"+(m+1));
				
				// make managers mildly tolerant of waiting
				mgrAgent.setMaxWaitCycles(3);
				
				agent_list.add(mgrAgent);
			}
		}
		
		FileWriter agentWriter = null;
		try {
			agentWriter = new FileWriter("agent_report_"+
										 this.getNumAgents()+"_"+
										 this.getNumCycles()+"_"+
										 this.getNumInitDemands()+"_"+
										 this.getProbNewDemand()+".txt");
		}
		catch (Exception e) {
			e.printStackTrace();
		}
			
		for (Agent a : agent_list) {
			
			// update interaction rate
			a.setInteraxLearningRate(this.getInteraxLR()/100.0);
			
			// give agent the supply-demand map
			a.setSupplyDemandDictionary(demand_list.getSupplyDemandDict());
			
			// bind agents to property change supports
			demand_list.addPropertyChangeListener(a);
			support.addPropertyChangeListener(a);
			a.addPropertyChangeListener(this);
			
			// have the agent pre-populate backlog from initial demands
			a.start(demand_list);
			
			// log this agent exists
			if(agentWriter != null) {
				try {
					agentWriter.write(a.getName()+"\n");
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		
		if(agentWriter != null) {
			try {
				agentWriter.close();
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}	
	}
	
	// helper function to get all unique ids
	void _addDemandsToSet(HashSet<UUID> hs,ArrayList<Demand> al) {
		for (Demand d : al) {
			hs.add(d.getId());
		}
	}
	
	// print progress update to console
	public void progressReport(boolean finalreport) {
		
		try{
			FileWriter interaxWriter = null;
			ArrayList<Integer> collabList = null;
			if(finalreport) {
				interaxWriter = new FileWriter("interaction_report_"+
												  this.getNumAgents()+"_"+
												  this.getNumCycles()+"_"+
												  this.getNumInitDemands()+"_"+
												  this.getProbNewDemand()+".txt");
				collabList = new ArrayList<Integer>();
				System.out.println("------Final Progress Report-------");
			}
			else {
				System.out.println("------Progress Report-------");
			}
			System.out.println("Simulation Time: "+this.getGlobalTime().get()+"\n\n");
			
			int col=0, comp=0, comm=0;

			HashSet<UUID> unique_comp = new HashSet<UUID>();
			HashSet<UUID> unique_comm = new HashSet<UUID>();
			
			for (Agent a : agent_list) {
				col += a.getCollaborationCount();
				this._addDemandsToSet(unique_comp,a.getCompletedtasks());
				this._addDemandsToSet(unique_comm,a.getCommittedtasks());
				if(finalreport) {
					collabList.add(a.getCollaborationCount());
					a.reportInteractions(interaxWriter);				
				}
				System.out.println(a.toProgressString()+"\n");
			}
			
			if(finalreport) {
				this.mc_rx_timeseries.add(collabList);
				interaxWriter.close();
			}
			System.out.println("--------End Report---------");
			
			comp = unique_comp.size();
			comm = unique_comm.size();
			
			// collaborations will be noted 2x
			n_collaborations.set(col/2);
			
			// includes collaboration demands and regular ones
			n_completed.set(comp);
			
			// includes collaboration demands and regular ones
			n_committed.set(comm);
			
			// update timeseries
			int index = collab_timeseries.size()-1;
			if((index<0) || 
			   (collab_timeseries.get().get(index).getXValue().intValue() != this.getGlobalTime().get())) {
				commit_timeseries.get().add(new Data<Number,Number>(this.getGlobalTime().get(),comm));
				complete_timeseries.get().add(new Data<Number,Number>(this.getGlobalTime().get(),comp));
				collab_timeseries.get().add(new Data<Number,Number>(this.getGlobalTime().get(),(col/2)));
			}
			
//			if(finalreport) {
//				this.mc_rx_timeseries.add(new Pair<Number,Number>(comm,col/2));
//			}
		}
		catch (IOException e) {
			// fail gracefully
			System.err.println("Failed while writing progress report to file.");
			e.printStackTrace();
		}
	}
	
	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if(evt.getPropertyName() == "collaborate") {
			// agent needs help to complete demand
			// store collaboration request (captures who sent it)
			this.collab_requests.add(evt);
		}
		else if(evt.getPropertyName() == "refresh_backlog") {
			this.resync_requests.add(evt);
		}
		else {
			System.err.println("SimEnvironment does not know how to manage event "+evt.getPropertyName());
		}
 	}
	
	// manage various requests from the agents
	public void manageRequests() {
		while(!this.collab_requests.isEmpty()) {
			
			// check if calling demand is a new/cloned demand
			demand_list.addIfUnseen((Demand)collab_requests.get(0).getOldValue());
			
			// generate a new Demand
			demand_list.newDemand(Demand.createCollaborationDemand((Demand)(collab_requests.get(0).getOldValue()),
																   (Agent)(collab_requests.get(0).getNewValue())));
			
			// reset collab_request
			collab_requests.remove(0);
		}
		
		// manage resync requests for demandlist
		while(!this.resync_requests.isEmpty()) {
			Agent a = (Agent)(resync_requests.get(0).getNewValue());
			a.refreshBacklog(this.demand_list);
			resync_requests.remove(0);
		}
	}

	// Time management signals
    public void syncGlobalTime() {
    	
    	// tell agents to finish up their tasks
        support.firePropertyChange("finishtask", 0, 1);
    	
    	int  min_time = Integer.MAX_VALUE;
    	
    	// order agents by utilization (local time)
    	this.sortAgents();
   	
    	// check all agents to identify min local time
		for (Agent a : this.agent_list) {
			if(a.getAgentTime() < min_time) {
				min_time = a.getAgentTime();
			}
			
			// remove and add again to change agent order in support
	    	support.removePropertyChangeListener(a);
			support.addPropertyChangeListener(a);
		}
    	
    	// update all agents to solidify completed tasks
        support.firePropertyChange("commituntil", null, min_time);
        
        // update global time stamp
		this.setGlobalTime(min_time);
    }
    
	// Time management signals
    public void finalizeGlobalTime() {
    	
    	// collect all local times and sort in ascending order
    	ArrayList<Integer> agent_times = new ArrayList<Integer>();
    	for (Agent a : this.agent_list) {
    		agent_times.add(a.getAgentTime());
    	}
    	Collections.sort(agent_times);
    	
    	// pop minimum (should be current global time)
    	agent_times.remove(0);
    	
    	// cycle through remaining times and loop until 
    	// all completed demands are also committed
    	for (Integer gt : agent_times) {
    		support.firePropertyChange("commituntil", null, gt);
    		this.setGlobalTime(gt);
    	}
    	this.progressReport(true);
    }
    
	public void start() {

		/*
		 * Start simulation
		 */

		int sim_cnt = 0;
		while(true) {
			
			/****** CREATE SOME DEMANDS ******/
			for (int nd = 0; nd < this.getNumInitDemands(); nd++) {
				this.createDemand(false);
			}
	
			/****** DEFINE AGENTS ******/
			// establish a 2:2:1 ratio between agents
			this.setNumSciAgents(Math.max(1,(int)(Math.floor(this.getNumAgents()*0.4))));
			this.setNumEngAgents(Math.max(1,(int)(Math.floor(this.getNumAgents()*0.4))));
			this.setNumMgrAgents(Math.max(1,(int)(Math.floor(this.getNumAgents()*0.2))));
			
			this.createAgents();
			
			System.out.println("Max number of cycles:" +this.getNumCycles());
			
			int prevgt = this.getGlobalTime().get(); 
			for (int i = 0; i < this.getNumCycles(); i++) {						
				
				/****** GENERATE DEMANDS ******/
				this.createRandomDemand();
				
				// manage agent requests
				this.manageRequests();
				
				// update simulation global time
		        this.syncGlobalTime();
		        
		        // report out progress
		        if(i%2 == 0) {
		        	
					this.setProgress((i+1)/(double)this.getNumCycles());
					System.out.println("PROGRESS: "+this.progress.get());
					
					if(this.getGlobalTime().get() > prevgt) {
						// report out on progress
						this.progressReport(false);
						prevgt = this.getGlobalTime().get();
					}
					
					// pause so the UI can catch up
		        	try { 
		        		Thread.sleep(200);
	        		}
		        	catch(InterruptedException ex){ 
		        		// do nothing
					}
		        }
			}
			
			this.finalizeGlobalTime();
			
			// mark complete
			this.setProgress(1.0);
			this.setFinished(true);
			
			// reinitialize for next sim
			if(++sim_cnt >= this.getNumMCRuns()) {
				break;
			}
			else {
				
				// pause so the UI can catch up
	        	try { 
	        		Thread.sleep(1000);
        		}
	        	catch(InterruptedException ex){ 
	        		// do nothing
				}
	        	
				initialize();
			}
		}
		
		// write final monte carlo results to file
		FileWriter mcWriter = null;
		try {
			mcWriter = new FileWriter("montecarlo_report_"+
										 this.getNumAgents()+"_"+
										 this.getNumCycles()+"_"+
										 this.getNumInitDemands()+"_"+
										 this.getProbNewDemand()+".txt");
			
			Iterator<ArrayList<Integer>> iter = this.getMcRxTimeseries().iterator();
			while (iter.hasNext()) {
				ArrayList<Integer> al = iter.next();
				for (Integer i : al) {
					mcWriter.write(i+",");
				}
				mcWriter.write("\n");
			}
			mcWriter.close();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	// Getters and Setters
	public SimpleIntegerProperty getGlobalTime() {	return global_time;	}
	public void setGlobalTime(int globaltime) {
		this.global_time.set(globaltime);
	}

	public SimpleIntegerProperty getNumSciAgents() { return n_sci_agents; }
	public void setNumSciAgents(int n_agents) {
		this.n_sci_agents.set(n_agents);
	}

	public SimpleIntegerProperty getNumEngAgents() {
		return n_eng_agents;
	}

	public void setNumEngAgents(int n_agents) {
		this.n_eng_agents.set(n_agents);;
	}

	public SimpleIntegerProperty getNumMgrAgents() {
		return n_mgr_agents;
	}

	public void setNumMgrAgents(int n_agents) {
		this.n_mgr_agents.set(n_agents);
	}

	public SimpleDoubleProperty getProgress() {
		return progress;
	}

	public void setProgress(double p) {
		this.progress.set(p);
	}

	public final SimpleListProperty<Data<Number, Number>> completeTimeseriesProperty() {
		return this.complete_timeseries;
	}

	public final ObservableList<Data<Number, Number>> getCompleteTimeseries() {
		return this.completeTimeseriesProperty().get();
	}

	public final void setCompleteTimeseries(final ObservableList<Data<Number, Number>> complete_timeseries) {
		this.completeTimeseriesProperty().set(complete_timeseries);
	}

	public final SimpleListProperty<Data<Number, Number>> commitTimeseriesProperty() {
		return this.commit_timeseries;
	}

	public final ObservableList<Data<Number, Number>> getCommitTimeseries() {
		return this.commitTimeseriesProperty().get();
	}

	public final void setCommitTimeseries(final ObservableList<Data<Number, Number>> commit_timeseries) {
		this.commitTimeseriesProperty().set(commit_timeseries);
	}

	public final SimpleListProperty<Data<Number, Number>> collabTimeseriesProperty() {
		return this.collab_timeseries;
	}

	public final ObservableList<Data<Number, Number>> getCollabTimeseries() {
		return this.collabTimeseriesProperty().get();
	}

	public final void setCollabTimeseries(final ObservableList<Data<Number, Number>> collab_timeseries) {
		this.collabTimeseriesProperty().set(collab_timeseries);
	}

	public final SimpleIntegerProperty interaxLRProperty() {
		return this.interax_lr;
	}
	
	public final int getInteraxLR() {
		return this.interaxLRProperty().get();
	}
	
	public final void setInteraxLR(final int interax_lr) {
		this.interaxLRProperty().set(interax_lr);
	}

	public final SimpleIntegerProperty probNewDemandProperty() {
		return this.prob_new_demand;
	}

	public final int getProbNewDemand() {
		return this.probNewDemandProperty().get();
	}

	public final void setProbNewDemand(final int prob) {
		this.probNewDemandProperty().set(prob);
	}

	public final SimpleIntegerProperty numInitDemandsProperty() {
		return this.n_init_demands;
	}

	public final int getNumInitDemands() {
		return this.numInitDemandsProperty().get();
	}

	public final void setNumInitDemands(final int n_init_demands) {
		this.numInitDemandsProperty().set(n_init_demands);
	}

	public final SimpleIntegerProperty numCyclesProperty() {
		return this.n_cycles;
	}
	
	public final int getNumCycles() {
		return this.numCyclesProperty().get();
	}
	
	public final void setNumCycles(final int n_cycles) {
		this.numCyclesProperty().set(n_cycles);
	}

	public final SimpleIntegerProperty numAgentsProperty() {
		return this.n_agents;
	}
	
	public final int getNumAgents() {
		return this.numAgentsProperty().get();
	}
	
	public final void setNumAgents(final int n_agents) {
		this.numAgentsProperty().set(n_agents);
	}

	public final SimpleBooleanProperty finishedProperty() {
		return this.finished;
	}

	public final boolean isFinished() {
		return this.finishedProperty().get();
	}
	
	public final void setFinished(final boolean fin) {
		this.finishedProperty().set(fin);
	}

	public final SimpleIntegerProperty numCollaborationsProperty() {
		return this.n_collaborations;
	}

	public final int getNumCollaborations() {
		return this.numCollaborationsProperty().get();
	}

	public final void setNumCollaborations(final int n_collab) {
		this.numCollaborationsProperty().set(n_collab);
	}

	public final SimpleIntegerProperty numCompletedProperty() {
		return this.n_completed;
	}
	
	public final int getNumCompleted() {
		return this.numCompletedProperty().get();
	}
	
	public final void setNumCompleted(final int n_comp) {
		this.numCompletedProperty().set(n_comp);
	}
	
	public final SimpleIntegerProperty numCommittedProperty() {
		return this.n_committed;
	}
	
	public final int getNumCommitted() {
		return this.numCommittedProperty().get();
	}

	public final void setNumCommitted(final int n_commit) {
		this.numCommittedProperty().set(n_commit);
	}
	
	public int getNumMCRuns() {
		return MC_NUM_RUNS;
	}

	public ArrayList<ArrayList<Integer>> getMcRxTimeseries() {
		return mc_rx_timeseries;
	}

	public void setMcRxTimeseries(ArrayList<ArrayList<Integer>> timeseries) {
		this.mc_rx_timeseries = timeseries;
	}

//	public static void main(String args[]) {
//				
//		/****** CREATE ENVIRONMENT ******/
//		SimEnvironment sim = new SimEnvironment();
//
//		/****** CREATE SOME DEMANDS ******/
//		for (int nd = 0; nd < 10; nd++) {
//			sim.createDemand(false);
//		}
//		
//		/****** DEFINE AGENTS ******/
//		sim.createAgents(1,1,1);
//		
//		sim.start();
//	}
}
