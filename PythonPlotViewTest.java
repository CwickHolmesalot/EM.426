import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class PythonPlotViewTest extends Application {
	
    public static void main(String[] args) {
    	launch(args);
    }
	    
    @Override
    public void start(Stage pStage) throws Exception {
    	
    	PythonPlotView ppv = new PythonPlotView();
    	
    	Scene scene = new Scene(ppv);
    	pStage.setScene(scene);
    	pStage.show();
    }
}