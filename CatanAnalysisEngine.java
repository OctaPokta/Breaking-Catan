//catanAnalysisEngine.java

package CatanAnalysis;

import CatanAnalysis.handlers.CatanHandler;
import CatanAnalysis.handlers.ConsoleColors;
import CatanAnalysis.testing.Test;
import CatanAnalysis.utils.Map;
import CatanAnalysis.utils.Towns;
import CatanAnalysis.utils.WinConditions;
import java.util.Scanner;

// Engine class to run the Catan Analysis
public class CatanAnalysisEngine implements CatanHandler {

    

  /*
  * Resources Avalibality:
  * brick | ore | sheep | wheat | wood | desert
  * j corrosponds to: Number of Tiles | Area Coverage %
  * i corrosponds to: Resource index
  */
  private final double[][] resourcesAvailability = {
    { 3, 15.8 }, // brick
    { 3, 15.8 }, // ore
    { 4, 21.1 }, // sheep
    { 4, 21.1 }, // wheat
    { 4, 21.1 }, // wood
    { 1, 5.3 } // desert
  };

  public static final String [] WIN_COND_NAMES = Main.WIN_COND_NAMES;

  // 0 = brick, 1 = ore, 2 = sheep, 3 = wheat, 4 = wood, 5 = desert 
  private final int[] mapResources;
  private final int[] mapDiceNumbers;

  // attributes
  private double topWinStrategy; 
  private final double[][] winConditionsResources;
  private final int NUM_OF_WIN_CONDITIONS;
  WinConditions winConditions; // to store win conditions analysis results
  Map catanMap; // to store map analysis results
  Towns catanTowns; // to store town analysis results

  // constructor
  public CatanAnalysisEngine(double[][] winConditionsResources, int[] mapResources, int[] mapDiceNumbers) {

    // user input
    this.winConditionsResources = winConditionsResources;
    this.NUM_OF_WIN_CONDITIONS = winConditionsResources.length;
    this.mapResources = mapResources;
    this.mapDiceNumbers = mapDiceNumbers;
  }

  // INTERFACE METHODS

  @Override
  public void runInteractiveConsole() {
    try (Scanner scanner = new Scanner(System.in)) {
      java.util.Map<Integer, Integer> optionMap = new java.util.HashMap<>();

      optionMap.put(1, 1);   // option 1 -> flag 1
      optionMap.put(2, 2);   // option 2 -> flag 2
      optionMap.put(3, 4);   // option 3 -> flag 4
      optionMap.put(4, 8);   // option 4 -> flag 8
      optionMap.put(5, 16);  // option 5 -> flag 16 
      optionMap.put(6, 32); // option 6 -> flag 32 
      optionMap.put(7, 64);   
      optionMap.put(8, 128);   
      optionMap.put(0, 0); // option 0 -> flag 0 (select all)
      
      int selectionFlags = 0;
      boolean readyToRun = false;

      System.out.println("\n--- Debug Menu ---");
      System.out.println("Select options to add to the queue:");
      System.out.println("1: Win Conditions upper dice & lower dice table\n2: Win Conditions table per strategies\n3: Showing heatmaps cluster proprity based on strategy, per each tile\n4: Debugging used for Python visualization");
      System.out.println("5: Win Conditions (WIP...)\n6: Connection between each town to its adjusted hex\n7: Final Optimal Location Values for Each Town\n8: Connection between indices in ConnectedComponentTile class and Tile Class");
      System.out.println("Commands: 'r' to reset | [Empty Enter] to Run | 'q' to quit | '0' to select all");

      while (!readyToRun) {
        System.out.print("Current Selection [" + (selectionFlags + 1) + "] > ");
        String input = scanner.nextLine().trim().toLowerCase();

        if (input.isEmpty()) {
          if (selectionFlags == 0 && !readyToRun) {
            System.out.println("Nothing selected. Add options before running.");
            continue;
          }
          readyToRun = true;
        } else if (input.equals("r")) {
          selectionFlags = 0;
          System.out.println("Selection cleared.");
        } else if (input.equals("q")) {
          System.out.println("Exiting...");
          return;
        } else if (input.equals("0")) {
          selectionFlags = 1 | 2 | 4 | 8 | 16 | 32 | 64 | 128;
          System.out.println("All options selected.");
        } else {
          selectionFlags = parseSelectionInput(input, selectionFlags, optionMap);
        }
      }
      System.out.println("");
      runConsoleResults(selectionFlags);
    }
  }

  // helper method to parse user input for debug options and update flags accordingly
  private int parseSelectionInput(String input, int currentFlags, java.util.Map<Integer, Integer> optionMap) {
    String[] parts = input.split("\\s+");
    int flags = currentFlags;
    
    for (String part : parts) {
      try {
        int val = Integer.parseInt(part);
        if (optionMap.containsKey(val)) {
          flags |= optionMap.get(val);
        } else {
          System.out.println("Error: [" + val + "] is not a valid option.");
        }
      } catch (NumberFormatException e) {
        System.out.println("Error: '" + part + "' is not a number.");
      }
    }
    return flags;
  }

  @Override
  public void runMapAnalysis() {
    calculateOptHexData(); 
  }
 
  @Override
  public void pickOptTown() {
    if (this.catanMap == null) {
      calculateOptHexData(); // note: this is a safety check to ensure that the hex data is calculated before picking optimal town, but ideally this should be structured better to avoid such dependencies.
    }
    runTownOptLocationAnalysis();
    pickBestTown();
  }

  @Override
  public void pickOptRoad() {
    // WIP...
  }

  // MAIN METHODS

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


  // DEBUG METHODS

  //test method to print results for verification
  @Override
  public void runConsoleResults(int flags) {

    final int SELECT_ALL = 0; // special flag to select all options
    final int WIN_COND_UPP_DICE_LOW_DICE = 1; // 1
    final int WIN_COND_STRATEGY_TABLE = 1 << 1; // 2
    final int HEATMAP_CLUSTER_PROP = 1 << 2; // 4
    final int PYTHON_DEBUG = 1 << 3; // 8  
    final int WIN_COND = 1 << 4; // 16
    final int TOWN_HEX = 1 << 5; // 32
    final int FINAL_OPT = 1 << 6; // 64
    final int TILES_DEBUG = 1 << 7; // 128 

    Test dataPerTile = new Test(this.catanMap.winConditionPerTileScores, this.catanMap.mapGroupData);

    // if SELECT_ALL is selected, set flags to include all options
    if (flags == SELECT_ALL) {
      flags = WIN_COND_UPP_DICE_LOW_DICE | WIN_COND_STRATEGY_TABLE | HEATMAP_CLUSTER_PROP | PYTHON_DEBUG | WIN_COND | TOWN_HEX | FINAL_OPT | TILES_DEBUG;
    }
    // win condition upper dice and lower dice tables [Flag = 1]
    if ((flags & WIN_COND_UPP_DICE_LOW_DICE) != 0) { 
      System.out.println(ConsoleColors.BLUE_BOLD + "---[Win Conditions upper dice & lower dice table]---" + ConsoleColors.RESET);
      System.out.println();
      this.winConditions.printRelativeTokenScoreTop();
      System.out.println();
      this.winConditions.printRelativeTokenScoreBottom();
      System.out.println();
    }
    // win condition table per strategy [Flag = 2]
    if ((flags & WIN_COND_STRATEGY_TABLE) != 0) { 
      System.out.println(ConsoleColors.BLUE_BOLD + "---[Win Conditions table per strategies]---" + ConsoleColors.RESET);
      System.out.println();
      this.catanMap.printStrategiesTable();
      System.out.println();
    }
    // heatmap cluster proprity based on strategy, per each tile [Flag = 3]
    if ((flags & HEATMAP_CLUSTER_PROP) != 0) { 
      System.out.println(ConsoleColors.BLUE_BOLD + "---[Showing heatmaps cluster proprity based on strategy, per each tile]---" + ConsoleColors.RESET);
      System.out.println();
      dataPerTile.printHeatmapClusterProprity();
      System.out.println();
    }
    // debugging used for Python visualization [Flag = 4]
    if ((flags & PYTHON_DEBUG) != 0) { 
      System.out.println(ConsoleColors.BLUE_BOLD + "---[Debugging used for Python visualization]---" + ConsoleColors.RESET);
      System.out.println();
      dataPerTile.printPythondataBlockForHeatmap();
      System.out.println();
    }
    // win conditions analysis results [Flag = 5]
    if ((flags & WIN_COND) != 0) { 
      System.out.println(ConsoleColors.BLUE_BOLD + "---[Win Conditions Analysis Results]---" + ConsoleColors.RESET);
      System.out.println();
      System.out.println("WIP...");
      System.out.println();
    }
    // connection between each town and its adjusted hex [Flag = 6]
    if ((flags & TOWN_HEX) != 0) { 
      System.out.println(ConsoleColors.BLUE_BOLD + "---[Connection between each town and its adjusted hex]---" + ConsoleColors.RESET);
      System.out.println();
      catanTowns.printTownHex();
      System.out.println();
    }
    // final optimal location values for each town [Flag = 7]
    if ((flags & FINAL_OPT) != 0) { 
      System.out.println(ConsoleColors.BLUE_BOLD + "---[Final Optimal Location Values for Each Town]---" + ConsoleColors.RESET);
      System.out.println();
      catanTowns.printFinalOptLocation(3);
      System.out.println();
    }
    // debugging the connection between indices in ConnectedComponentTile class and Tile Class [Flag = 8]
    if ((flags & TILES_DEBUG) != 0) { 
      System.out.println(ConsoleColors.BLUE_BOLD + "---[Debugging the connection between indices in ConnectedComponentTile class and Tile Class]---" + ConsoleColors.RESET);
      System.out.println();
      catanTowns.printTiles();
      System.out.println();
    }
    
  }
  



 
}