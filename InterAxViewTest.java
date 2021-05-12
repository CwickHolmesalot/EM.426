import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class InterAxViewTest extends Application {
	
    public static void main(String[] args) {
    	launch(args);
    }
	    
    @Override
    public void start(Stage pStage) throws Exception {
    	
    	InterAxView iav = new InterAxView();
    	
    	Scene scene = new Scene(iav);
    	pStage.setScene(scene);
    	pStage.show();
    	
    	// set up simulation environment
		SimEnvironment sim = new SimEnvironment();
		iav.simenv.set(sim);
    }
}