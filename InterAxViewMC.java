
import java.io.IOException;
import java.util.List;
import java.util.function.UnaryOperator;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Side;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.BorderPane;
import javafx.util.Pair;
import javafx.util.converter.NumberStringConverter;

public class InterAxViewMC extends BorderPane {
    @FXML
    private TextField globalTimeTB, collabTB, completedTB, committedTB;
    @FXML
    private TextField agentCountTB, ncyclesTB, taskpoolTB, pnewtaskTB;
    @FXML
    private Button goButton;
    @FXML
    private ProgressBar progressPB;
    @FXML
    private LineChart<Number, Number> burndownLC, interaxLC;
    @FXML
    private ScatterChart<Number, Number> tradespaceSC;
    //@FXML
    //private NumberAxis inter_xAxis, inter_yAxis, burn_xAxis, burn_yAxis;
    
    private SimpleListProperty<Data<Number,Number>> completedList;
    private ListChangeListener<Data<Number,Number>> completeListener;
    private XYChart.Series<Number, Number> completedSeries;
    
    private SimpleListProperty<Data<Number,Number>> committedList;
    private ListChangeListener<Data<Number,Number>> commitListener;
    private XYChart.Series<Number, Number> committedSeries;
    
    private SimpleListProperty<Data<Number,Number>> collabList;
    private ListChangeListener<Data<Number,Number>> collabListener;
    private XYChart.Series<Number, Number> collabSeries;

//    private SimpleBooleanProperty simFinished;
//    private SimpleIntegerProperty simLastCollab;
//    private SimpleIntegerProperty simLastCommit;
    private SimpleListProperty<Pair<Number,Number>> ensembleList;
    private ListChangeListener<Pair<Number, Number>> ensembleListener;
    private XYChart.Series<Number, Number> ensembleSeries;
    
    // simulator
    public SimpleObjectProperty<SimEnvironment> simenv;
        
    public InterAxViewMC() {
    	
    	// add scene code here
    	FXMLLoader loader;
		loader = new FXMLLoader(getClass().getResource("InterAxViewXtra.fxml"));
        loader.setRoot(this);
        loader.setController(this);
        
        try {
        	loader.load();
        } 
        catch (IOException e1) {
        	e1.printStackTrace();
        }
        
        simenv = new SimpleObjectProperty<SimEnvironment>();

        final double MIN_PNT = 0;
        final double MAX_PNT = 100;
        UnaryOperator<TextFormatter.Change> PNTfilter = change -> {
            if (change.isDeleted()) {
                return change;
            }

            // How would the text look like after the change?
            String txt = change.getControlNewText();
            
            // Try parsing and check if the result is in range
            try {
                int n = Integer.parseInt(txt);
                return MIN_PNT <= n && n <= MAX_PNT ? change : null;
            }
            catch (NumberFormatException e) {
                return null;
            }
        };
        pnewtaskTB.setTextFormatter(new TextFormatter<>(PNTfilter));
        
        completedList = new SimpleListProperty<Data<Number,Number>>(FXCollections.observableArrayList());
        committedList = new SimpleListProperty<Data<Number,Number>>(FXCollections.observableArrayList());
        collabList    = new SimpleListProperty<Data<Number,Number>>(FXCollections.observableArrayList());
        ensembleList  = new SimpleListProperty<Pair<Number,Number>>(FXCollections.observableArrayList());
        
        completeListener = new ListChangeListener<Data<Number,Number>>() {
			@Override
			public void onChanged(javafx.collections.ListChangeListener.Change<? extends Data<Number,Number>>c) {
				while (c.next()) {
					if(c.wasAdded()) {
						List<? extends Data<Number,Number>> addedSubList = c.getAddedSubList();
						for(Data<Number,Number> d: addedSubList) {
							completedSeries.getData().add(d);
						}
					}
				}
			}
		};
		
        commitListener = new ListChangeListener<Data<Number,Number>>() {
			@Override
			public void onChanged(javafx.collections.ListChangeListener.Change<? extends Data<Number,Number>>c) {
				while (c.next()) {
					if(c.wasAdded()) {
						List<? extends Data<Number,Number>> addedSubList = c.getAddedSubList();
						for(Data<Number,Number> d: addedSubList) {
							committedSeries.getData().add(d);
						}
					}
				}
			}
		};
		
        collabListener = new ListChangeListener<Data<Number,Number>>() {
			@Override
			public void onChanged(javafx.collections.ListChangeListener.Change<? extends Data<Number,Number>>c) {
				while (c.next()) {
					if(c.wasAdded()) {
						List<? extends Data<Number,Number>> addedSubList = c.getAddedSubList();
						for(Data<Number,Number> d: addedSubList) {
							collabSeries.getData().add(d);
						}
					}
				}
			}
		};
		
        ensembleListener = new ListChangeListener<Pair<Number,Number>>() {
			@Override
			public void onChanged(javafx.collections.ListChangeListener.Change<? extends Pair<Number,Number>>c) {
				while (c.next()) {
					if(c.wasAdded()) {
						List<? extends Pair<Number,Number>> addedSubList = c.getAddedSubList();
						for(Pair<Number,Number> p: addedSubList) {
							addRxToChart(p);
						}
					}
				}
			}
		};

//		simFinished = new SimpleBooleanProperty();
//		simFinished.addListener(new ChangeListener<>(){
//			@Override
//			public void changed(ObservableValue<? extends Boolean> observable,
//					Boolean oldValue, 
//					Boolean newValue) {
//				if(newValue == true) {
//					System.out.println("*** Simulation has finished ***");			
//					// add last data point to ensembleSeries
//			    	addRxToChart();
//					// run sim again
//					if(rx_cnt++ < n_realizations) {
//						this.reset();
//						this.startSim();
//					}
//				}
//			}
//		});
			

//		simLastCollab = new SimpleIntegerProperty();
//		simLastCommit = new SimpleIntegerProperty();
	    
		completedList.addListener(completeListener);
		committedList.addListener(commitListener);
		collabList.addListener(collabListener);
		ensembleList.addListener(ensembleListener);
		
		completedSeries = new XYChart.Series<Number,Number>();
		committedSeries = new XYChart.Series<Number,Number>();
		collabSeries    = new XYChart.Series<Number,Number>();
		ensembleSeries = new XYChart.Series<Number,Number>();
		
		completedSeries.setName("Completed");
		committedSeries.setName("Committed");
		collabSeries.setName("Collaborations");
		ensembleSeries.setName("Monte Carlo");	

		// start at 0
		completedSeries.getData().add(new Data<Number,Number>(0,0));
		committedSeries.getData().add(new Data<Number,Number>(0,0));
		collabSeries.getData().add(new Data<Number,Number>(0,0));
		
		burndownLC.getData().add(completedSeries);
		burndownLC.getData().add(committedSeries);
		interaxLC.getData().add(collabSeries);
		tradespaceSC.getData().add(ensembleSeries);
		
		
		burndownLC.setLegendVisible(true);
		burndownLC.setLegendSide(Side.BOTTOM);

        goButton.setOnAction(e-> {
        	// lock in sim controls after starting
	    	agentCountTB.setEditable(false);
	    	ncyclesTB.setEditable(false);
	    	taskpoolTB.setEditable(false);
	    	pnewtaskTB.setEditable(false);
	    	agentCountTB.setDisable(true);
	    	ncyclesTB.setDisable(true);
	    	taskpoolTB.setDisable(true);
	    	pnewtaskTB.setDisable(true);
	    	
	    	goButton.setDisable(true);
	    	
	    	// run simulation
        	startSim();
        });
        
        simenv.addListener((obs,oldV,newV) -> {
        	
        	if(oldV != null) {
    		    agentCountTB.textProperty().unbindBidirectional(simenv.get().global_time);
		    	ncyclesTB.textProperty().unbindBidirectional(simenv.get().n_cycles);
		    	taskpoolTB.textProperty().unbindBidirectional(simenv.get().n_init_demands);
		    	pnewtaskTB.textProperty().unbindBidirectional(simenv.get().prob_new_demand);
    		    globalTimeTB.textProperty().unbindBidirectional(simenv.get().global_time);
    		    collabTB.textProperty().unbindBidirectional(simenv.get().n_collaborations);
    		    completedTB.textProperty().unbindBidirectional(simenv.get().n_completed);
    		    committedTB.textProperty().unbindBidirectional(simenv.get().n_committed);
    		    progressPB.progressProperty().unbind();
    		    
    		    completedList.unbind();
    		    committedList.unbind();
    		    collabList.unbind();
    		    ensembleList.unbind();
    		    
//		    	simFinished.unbind();
//		    	simLastCollab.unbind();
//		    	simLastCommit.unbind();
    		}
        	
		    if(newV != null) {
		    	NumberStringConverter cv = new NumberStringConverter();
			    agentCountTB.textProperty().bindBidirectional(simenv.get().n_agents,cv);
		    	ncyclesTB.textProperty().bindBidirectional(simenv.get().n_cycles,cv);
		    	taskpoolTB.textProperty().bindBidirectional(simenv.get().n_init_demands,cv);
		    	pnewtaskTB.textProperty().bindBidirectional(simenv.get().prob_new_demand,cv);
			    globalTimeTB.textProperty().bindBidirectional(simenv.get().global_time,cv);
			    collabTB.textProperty().bindBidirectional(simenv.get().n_collaborations,cv);
			    completedTB.textProperty().bindBidirectional(simenv.get().n_completed,cv);
			    committedTB.textProperty().bindBidirectional(simenv.get().n_committed,cv);
    		    progressPB.progressProperty().bind(simenv.get().getProgress());
    		    
    		    completedList.bind(simenv.get().completeTimeseriesProperty());
    		    committedList.bind(simenv.get().commitTimeseriesProperty());
    		    collabList.bind(simenv.get().collabTimeseriesProperty());
    		    ensembleList.bind(simenv.get().mcRxTimeseriesProperty());
    		    
//		    	simFinished.bind(simenv.get().finishedProperty());
//		    	simLastCollab.bind(simenv.get().numCollaborationsProperty());
//		    	simLastCommit.bind(simenv.get().numCommittedProperty());
		    }
        });        
    }
    
    public void startSim() {
    	
    	Runnable task = new Runnable() {
    		public void run() {
	    			if(simenv.isNotNull().get())
	    				simenv.get().start();
    		}	
    	};
    	
    	// run the task in a background thread
    	Thread backgroundThread = new Thread(task);
    	
    	// terminate the running thread if the application exits
    	backgroundThread.setDaemon(true);
    	
    	// start the thread
    	backgroundThread.start();
    }
   
    
    public void addRxToChart(Pair<Number,Number> pr) {
    	var d = new XYChart.Data<Number,Number>();
    	d.setXValue(pr.getKey());
    	d.setYValue(pr.getValue());
    	d.setExtraValue(pr);
    	ensembleSeries.getData().add(d);
		//ensembleSeries.getNode().setStyle("-fx-stroke: #ff880099");
    }
        
//	public final SimpleBooleanProperty simFinishedProperty() {
//		return this.simFinished;
//	}
//
//	public final boolean isSimFinished() {
//		return this.simFinishedProperty().get();
//	}
//	
//	public final void setSimFinished(final boolean finished) {
//		this.simFinishedProperty().set(finished);
//	}

//	public final SimpleIntegerProperty simLastCollabProperty() {
//		return this.simLastCollab;
//	}
//
//	public final int getSimLastCollab() {
//		return this.simLastCollabProperty().get();
//	}
//	
//	public final void setSimLastCollab(final int simLastCollab) {
//		this.simLastCollabProperty().set(simLastCollab);
//	}
	
//	public final SimpleIntegerProperty simLastCommitProperty() {
//		return this.simLastCommit;
//	}
//	
//	public final int getSimLastCommit() {
//		return this.simLastCommitProperty().get();
//	}
//
//	public final void setSimLastCommit(final int simLastCommit) {
//		this.simLastCommitProperty().set(simLastCommit);
//	}
}