import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class InterAxViewTest extends Application {
	
    public static void main(String[] args) {
    	launch(args);
    }
	    
    @Override
    public void start(Stage pStage) throws Exception {
    	
    	InterAxView iav = new InterAxView();
    	
    	//String currentDir = new java.io.File(".").getCanonicalPath();
    	//System.out.println("Current dir:"+currentDir);
    	
    	Scene scene = new Scene(iav);
    	pStage.setScene(scene);
    	pStage.getIcons().add(new Image(getClass().getResourceAsStream("icon_16.png")));
    	pStage.getIcons().add(new Image(getClass().getResourceAsStream("icon_32.png")));
    	pStage.getIcons().add(new Image(getClass().getResourceAsStream("icon_64.png")));
    	pStage.getIcons().add(new Image(getClass().getResourceAsStream("icon_128.png")));
    	pStage.getIcons().add(new Image(getClass().getResourceAsStream("icon_256.png")));
    	pStage.getIcons().add(new Image(getClass().getResourceAsStream("icon_512.png")));
    	pStage.show();
    	
    	// set up simulation environment
		SimEnvironment sim = new SimEnvironment();
		iav.simenv.set(sim);
    }
}