//CatanHandler.java

package CatanAnalysis.handlers;

public interface CatanHandler {

    
    
    void runMapAnalysis(); // method to start the map analysis, to be implemented by the main engine
    void pickOptTown(); // method to start the optimal town location analysis, to be implemented by the main engine
    void pickOptRoad(); // method to start the optimal road placement analysis, to be implemented by the main engine
    void runConsoleResults(int flags); // method to print results for verification, to be implemented by the main engine
    void runInteractiveConsole(); // method to run interactive console for user to select debug options, to be implemented by the main engine
}
