
import java.io.IOException;
import java.util.List;
import java.util.function.UnaryOperator;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Side;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.BorderPane;
import javafx.util.converter.NumberStringConverter;

public class InterAxView extends BorderPane {

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

    // simulator
    public SimpleObjectProperty<SimEnvironment> simenv;
        
    public InterAxView() {
    	
    	// add scene code here
    	FXMLLoader loader;
		loader = new FXMLLoader(getClass().getResource("InterAxView.fxml"));
        loader.setRoot(this);
        loader.setController(this);
        
        try {
        	loader.load();
        } 
        catch (IOException e1) {
        	e1.printStackTrace();
        }
        
    	//this.burndownLC.setAnimated(false);
    	//this.interaxLC.setAnimated(false);
    	
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
					else if(c.wasRemoved()) {
						// if any removed, consider all removed
						completedSeries.getData().clear();
						break;
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
					else if(c.wasRemoved()) {
						// if any removed, consider all removed
						committedSeries.getData().clear();
						break;
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
							//addDataToCollabChart(d);
							collabSeries.getData().add(d);
						}
					}
					else if(c.wasRemoved()) {
						// if any removed, consider all removed
						//removeAllFromCollabChart();
						collabSeries.getData().clear();
						break;
					}
				}
			}
		};

		completedList.addListener(completeListener);
		committedList.addListener(commitListener);
		collabList.addListener(collabListener);
		
		completedSeries = new XYChart.Series<Number,Number>();
		committedSeries = new XYChart.Series<Number,Number>();
		collabSeries    = new XYChart.Series<Number,Number>();
		
		completedSeries.setName("Completed");
		committedSeries.setName("Committed");
		collabSeries.setName("Collaborations");		

		// start at 0
		completedSeries.getData().add(new Data<Number,Number>(0,0));
		committedSeries.getData().add(new Data<Number,Number>(0,0));
		collabSeries.getData().add(new Data<Number,Number>(0,0));
		
		burndownLC.getData().add(completedSeries);
		burndownLC.getData().add(committedSeries);
		interaxLC.getData().add(collabSeries);
		
		burndownLC.setLegendVisible(true);
		burndownLC.setLegendSide(Side.BOTTOM);
		interaxLC.setLegendVisible(true);
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
		    }
        });        
    }
    
    public void addDataToCollabChart(Data<Number,Number> d) {
    	var newd = new Data<Number,Number>();
    	newd.setXValue(d.getXValue());
    	newd.setYValue(d.getYValue());
    	newd.setExtraValue(d);
    	this.collabSeries.getData().add(newd);
    }
    
    public void removeAllFromCollabChart() {
    	this.interaxLC.getData().remove(collabSeries);
    	this.collabSeries.getData().clear();
		this.collabSeries.getData().add(new Data<Number,Number>(0,0));
    	this.interaxLC.getData().add(collabSeries);
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
}