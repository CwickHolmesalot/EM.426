
import java.io.IOException;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Side;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.BorderPane;
import javafx.util.StringConverter;
import javafx.util.converter.NumberStringConverter;

public class InterAxView extends BorderPane {

	    @FXML
	    private TextField globalTimeTB, collabTB, completedTB, committedTB;
	    @FXML
	    private TextField agentCountTB, ncyclesTB, learnrateTB, newtaskTB;
	    @FXML
	    private Button goButton;
	    @FXML
	    private ProgressBar progressPB;
	    @FXML
	    private LineChart<Number, Number> burndownLC, interaxLC;
	    @FXML
	    private NumberAxis inter_xAxis, inter_yAxis, burn_xAxis, burn_yAxis;
	     
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
	    	FXMLLoader loader = new FXMLLoader(getClass().getResource("InterAxView.fxml"));
	        loader.setRoot(this);
	        loader.setController(this);
	        
	        try {
	        	loader.load();
	        } 
	        catch (IOException e1) {
	        	e1.printStackTrace();
	        }
	        
	        simenv = new SimpleObjectProperty<SimEnvironment>();
	        
//	        Pattern doublePattern = Pattern.compile("-?(([1-9][0-9]*)|0)?(\\.[0-9]*)?");
//	        final double MIN_LR = 0.5;
//	        final double MAX_LR = 1.0;
//	        UnaryOperator<TextFormatter.Change> LRfilter = change -> {
//	            if (change.isAdded()) {
//	            	if(!doublePattern.matcher(change.getControlNewText()).matches()){
//	            		return null;
//	            	}
//	            	else if(!((Double.parseDouble(change.getControlNewText()) >= MIN_LR) && 
//	            			  (Double.parseDouble(change.getControlNewText()) <= MAX_LR))) {
//                        return null;
//                    }
//                }
//
//	            return change;
//	        };
//	        StringConverter<Double> LRconverter = new StringConverter<Double>(){
//		        @Override
//		        public Double fromString(String s) {
//		            if (s.isEmpty() || "-".equals(s) || ".".equals(s) || "-.".equals(s)) {
//		                return 0.0 ;
//		            } else {
//		                return Double.valueOf(s);
//		            }
//		        }
//		        @Override
//		        public String toString(Double d) {
//		            return d.toString();
//		        }
//	        };
//	        TextFormatter<Double> LRFormatter = new TextFormatter<>(LRconverter, 0.0, LRfilter);
//	        learnrateTB.setTextFormatter(LRFormatter);
	        
	        final Integer MIN_LR = 0;
	        final Integer MAX_LR = 100;
	        UnaryOperator<TextFormatter.Change> LRfilter = change -> {
	            if (change.isDeleted()) {
	                return change;
	            }
	            
	            // How would the text look like after the change?
	            String txt = change.getControlNewText();
	            
	            // Try parsing and check if the result is in range
	            try {
	                int n = Integer.parseInt(txt);
	                return MIN_LR <= n && n <= MAX_LR ? change : null;
	            }
	            catch (NumberFormatException e) {
	                return null;
	            }
	        };
	        learnrateTB.setTextFormatter(new TextFormatter<>(LRfilter));
	        
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
	        newtaskTB.setTextFormatter(new TextFormatter<>(PNTfilter));
	        
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

	        goButton.setOnAction(e-> {
	        	// lock in sim controls after starting
		    	agentCountTB.setEditable(false);
		    	ncyclesTB.setEditable(false);
		    	learnrateTB.setEditable(false);
		    	newtaskTB.setEditable(false);
		    	agentCountTB.setDisable(true);
		    	ncyclesTB.setDisable(true);
		    	learnrateTB.setDisable(true);
		    	newtaskTB.setDisable(true);
		    	
		    	// run simulation
	        	startSim();
	        });
	        
	        simenv.addListener((obs,oldV,newV) -> {
	        	
	        	if(oldV != null) {
	    		    agentCountTB.textProperty().unbindBidirectional(simenv.get().global_time);
			    	ncyclesTB.textProperty().unbindBidirectional(simenv.get().n_cycles);
			    	learnrateTB.textProperty().unbindBidirectional(simenv.get().interax_lr);
			    	newtaskTB.textProperty().unbindBidirectional(simenv.get().prob_new_demand);
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
			    	learnrateTB.textProperty().bindBidirectional(simenv.get().interax_lr,cv);
			    	newtaskTB.textProperty().bindBidirectional(simenv.get().prob_new_demand,cv);
				    globalTimeTB.textProperty().bindBidirectional(simenv.get().global_time,cv);
				    collabTB.textProperty().bindBidirectional(simenv.get().n_collaborations,cv);
				    completedTB.textProperty().bindBidirectional(simenv.get().n_completed,cv);
				    committedTB.textProperty().bindBidirectional(simenv.get().n_committed,cv);
	    		    progressPB.progressProperty().bind(simenv.get().getProgress());
	    		    
	    		    completedList.bind(simenv.get().completeTimeseriesProperty());
	    		    committedList.bind(simenv.get().commitTimeseriesProperty());
	    		    collabList.bind(simenv.get().collabTimeseriesProperty());
			    }
			    
		        //burndownLC.visibleProperty().bind(simenv.get().is_finished);
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
	}