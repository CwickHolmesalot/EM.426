
import java.io.IOException;

import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.util.converter.NumberStringConverter;

public class InterAxView extends BorderPane {

	    @FXML
	    private TextField globalTimeTB, agentCountTB, collabTB;
	    @FXML
	    private TextField completedTB, committedTB;
	    @FXML
	    private Button goButton;
	    @FXML
	    private ProgressBar progressPB;
	    
	    // global time
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
	        
	        goButton.setOnAction(e-> {
		    	System.out.println("Button Clicked!");
	        	startSim();
	        });
	        
	        simenv.addListener((obs,oldV,newV) -> {
	        	
	        	if(oldV != null) {
	    		    globalTimeTB.textProperty().unbindBidirectional(simenv.get().global_time);
	    		    agentCountTB.textProperty().unbindBidirectional(simenv.get().global_time);
	    		    collabTB.textProperty().unbindBidirectional(simenv.get().n_collaborations);
	    		    completedTB.textProperty().unbindBidirectional(simenv.get().n_completed);
	    		    committedTB.textProperty().unbindBidirectional(simenv.get().n_committed);
	    		    progressPB.progressProperty().unbind();
	        	}
	        	
			    if(newV != null) {
			    	NumberStringConverter cv = new NumberStringConverter();
				    globalTimeTB.textProperty().bindBidirectional(simenv.get().global_time,cv);
				    agentCountTB.textProperty().bindBidirectional(simenv.get().n_agents,cv);
				    collabTB.textProperty().bindBidirectional(simenv.get().n_collaborations,cv);
				    completedTB.textProperty().bindBidirectional(simenv.get().n_completed,cv);
				    committedTB.textProperty().bindBidirectional(simenv.get().n_committed,cv);
	    		    progressPB.progressProperty().bind(simenv.get().getProgress());
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
	}