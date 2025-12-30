package CatanAnalysis;

import CatanAnalysis.utils.MapAnalysis;
import CatanAnalysis.utils.WinConditions;
import java.util.ArrayList;


public class Main {

  public static void main(String[] args) {

    /* Resources Avalibality:
      brick | ore | sheep | wheat | wood | desert
      j corrosponds to: Number of Tiles | Area Coverage %
      i corrosponds to: Resource index
    */
    double[][] resourcesAvailability = {
        {3, 15.8}, // brick
        {3, 15.8}, // ore
        {4, 21.1}, // sheep
        {4, 21.1}, // wheat
        {4, 21.1}, // wood
        {1, 5.3} // desert
    };

    /* win conditions table:
      each row determines the resource importance for a specific win condition, the last column is the total sum (later used for weighted need calculation)
      j corrosponds to: Brick | Ore | Sheep | Wheat | Wood | Total
      i corrosponds to: Condition index
    */
    double[][] winConditionsResources = {
        {11, 12, 4, 12, 11, 0}, // 4 Cities + LR
        {9, 11, 8, 12, 9, 0}, // 2 cities + 1 Settlement + LR & LA (Extra Brick & Wood)
        {14, 9, 5, 11, 14, 0}, // 3 cities + 2 settlements + LR
        {13, 11, 10, 14, 13, 0}, // 2 Cities + 3 Settlements + LR 
        {14, 8, 10, 12, 14, 0} // 1 City + 3 Settlements + LR & LA
    };

    int NUM_OF_WIN_CONDITIONS = winConditionsResources.length;

    // Run Win Conditions Analysis
    WinConditions winConditions = new WinConditions();
    ArrayList<int[][]> relativeTokenScoresResults = winConditions.WinConditionsCalculation(resourcesAvailability, winConditionsResources);
    
    // Run map analysis here:
    // ask user to enter resources & dice number for each index (top left to bottom right)
      // 0 = brick, 1 = ore, 2 = sheep, 3 = wheat, 4 = wood, 5 = desert 
      int[] mapResources = {2,4,2,0,0,3,4,3,1,5,2,1,4,2,1,3,4,0,3}; // example input
      int[] mapDiceNumbers = {5,11,12,9,8,4,5,8,3,0,10,9,3,6,4,10,11,2,6}; // example input

    // Create Map Analysis Object
    MapAnalysis mapAnalysis = new MapAnalysis(mapResources, mapDiceNumbers, relativeTokenScoresResults, NUM_OF_WIN_CONDITIONS);

    // Connect map graph


  }
}