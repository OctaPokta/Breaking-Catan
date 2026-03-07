//CatanHandler.java

package CatanAnalysis.handlers;

public interface CatanHandler {

    // analysis
    void runMapAnalysis(); 
    void pickOptTown(); 
    void pickOptRoad(); // WIP

    // console
    void printConsoleResults(int flags); 
    void runInteractiveConsole(); 
}
