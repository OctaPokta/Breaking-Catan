//catanAnalysisEngine.java

package CatanAnalysis;

import CatanAnalysis.handlers.CatanHandler;
import CatanAnalysis.utils.Map;
import CatanAnalysis.utils.Towns;
import CatanAnalysis.utils.WinConditions;

// Main class to run the Catan Analysis
public class CatanAnalysisEngine implements CatanHandler {

  // 0 = brick, 1 = ore, 2 = sheep, 3 = wheat, 4 = wood, 5 = desert 
  private final int[] mapResources;
  private final int[] mapDiceNumbers;

  // attributes
  private double topWinStrategy; 
  private final double[][] resourcesAvailability;
  private final double[][] winConditionsResources;
  private final int NUM_OF_WIN_CONDITIONS;
  WinConditions winConditions; // to store win conditions analysis results
  Map catanMap; // to store map analysis results
  Towns catanTowns; // to store town analysis results

  // constructor
  public CatanAnalysisEngine(double[][] resourcesAvailability, double[][] winConditionsResources, int[] mapResources, int[] mapDiceNumbers) {

    // user input
    this.resourcesAvailability = resourcesAvailability;
    this.winConditionsResources = winConditionsResources;
    this.NUM_OF_WIN_CONDITIONS = winConditionsResources.length;
    this.mapResources = mapResources;
    this.mapDiceNumbers = mapDiceNumbers;
  }

  
  @Override
  public void runMapAnalysis() {
    calculateOptHexData(); 
  }
 
  @Override
  public void pickOptTown() {
    // Safety Check: Ensure map and town data exists before analyzing towns
    if (this.catanMap == null) {
      calculateOptHexData();
    }
    runTownOptLocationAnalysis();
    printResultsForVerification(); // test purposes
    pickBestTown();
  }

  
  public void startRoadEngine() {
    // WIP...
  }

  // used to calculate opt hexs data on the board with a given map status
  private void calculateOptHexData() {
    runWinConditionsAnalysis();
    analyseHexagonsData();
  }

  // used to calculate the statistical importance of each resource for each win condition, and then calculate the relative token score for each resource based on the importance index.
  private void runWinConditionsAnalysis() {
    this.winConditions = new WinConditions(this.NUM_OF_WIN_CONDITIONS);
    this.winConditions.winConditionsCalculation(this.resourcesAvailability, this.winConditionsResources);
  }

  // used to calculate the optimal data for each hexagon on the board, such as the optimal resource and optimal dice number, based on the win conditions analysis and the map status.
  private void analyseHexagonsData() {
    this.catanMap = new Map(this.mapResources, this.mapDiceNumbers, winConditions.relativeTokenScoresResults, this.NUM_OF_WIN_CONDITIONS);
    this.catanMap.HexagonAnalyse(); // perform hexagon analysis
    this.topWinStrategy = this.catanMap.topWinCondition; 
  }

  // used to determine the optimal town locations for all towns on the map, based on the hexagon analysis and the map status, it rates each town location based on the hexagons that touches it and the importance of the resources that those hexagons produce.
  private void runTownOptLocationAnalysis() {
    this.catanTowns = new Towns(this.catanMap, this.resourcesAvailability, this.winConditionsResources, (int)this.topWinStrategy);
    catanTowns.connectHexToTowns(); // connect each town to the hexagon that touches it
    catanTowns.connectTownsToHexagons(); // connect each hexagon to the towns that touches it
    catanTowns.setTownHexagonCounts(); // for each town, set the number of hexagon that touches it
    catanTowns.rateTownLocation(); // rate each town location based on the hexagons that touches it
  }

  // used to find the current best town location best on the existing data
  private void pickBestTown() {
    int BestTownIndex = catanTowns.getBestTownIndex(); // get the best town index based on the optimal location values
    System.out.println("Best Town Index: " + BestTownIndex);
  }

  //test method to print results for verification
  private void printResultsForVerification() {
    // print results for verification
    catanTowns.printTownHex();
    catanTowns.printFinalOptLocation(3);
    //catanTowns.printTiles();
  }
  



 
}