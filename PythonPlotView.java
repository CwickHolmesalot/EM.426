
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;

public class PythonPlotView extends BorderPane {

    @FXML
    private Button pyButton;
    
    PythonPlotView(){

    	// add scene code here
    	FXMLLoader loader = new FXMLLoader(getClass().getResource("CreatePythonPlot.fxml"));
        loader.setRoot(this);
        loader.setController(this);
        
        try {
        	loader.load();
        } 
        catch (IOException e1) {
        	e1.printStackTrace();
        }
        
        pyButton.setOnAction(e-> {
        	try {
	        	//String command = "cmd /c start cmd /k python ";
        		//String PyPath = "C:\\Users\\chadw\\AppData\\Local\\Microsoft\\WindowsApps\\";
        		//String ScriptPath = "C:\\Users\\chadw\\eclipse-workspace\\EM426-ABM\\bin\\";
        		final List<String> commands = new ArrayList<String>();
        		commands.add("C:\\Users\\chadw\\Anaconda3\\python");
        		commands.add("/c");
        		commands.add("start");
        		commands.add("C:\\Users\\chadw\\eclipse-workspace\\EM426-ABM\\bin\\InteractionsPlot.py");
        		//ProcessBuilder pb = new ProcessBuilder(PyPath+"python", ScriptPath+"InteractionsPlot.py");
        		ProcessBuilder pb = new ProcessBuilder(commands);
        		pb.start(); 
	        	//Runtime.getRuntime().exec(PyPath+"\\python bin\\InteractionsPlot.py");
        	}
        	catch (Exception pyfail) {
        		pyfail.printStackTrace();
        	}
        });
        
    }
}