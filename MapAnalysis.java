package CatanAnalysis;

import java.util.ArrayList;
import java.util.Arrays;

// Given a Catan game map, this class handle the connection between the map to the win conditions analysis
public class MapAnalysis 
{
      int NUM_OF_WIN_CONDITIONS; // example number of win conditions  
      private final int NUM_OF_TILES_ON_MAP = 19; // standard catan has 19 tiles including desert
      Tile[] mapTiles = new Tile[NUM_OF_TILES_ON_MAP];
      ArrayList<int[][]> winConditionRelativeTokenScore;

      // win condition names
      private String [] winConditionNames = {
            "4 Cities + LR",
            "2 Cities + 1 Settlement + LR & LA (Extra Brick & Wood)",
            "3 cities + 2 settlements + LR",
            "2 Cities + 3 Settlements + LR",
            "1 City + 3 Settlements + LR & LA"
      };

      // constructor 
      public MapAnalysis(int[] mapResources, int[] mapDiceNumbers, ArrayList<int[][]> winConditionRelativeTokenScore, int NUM_OF_WIN_CONDITIONS) {
            
            this.NUM_OF_WIN_CONDITIONS = NUM_OF_WIN_CONDITIONS;
            
            // initialize map tiles based on input resource and dice number arrays
            for (int i = 0; i < NUM_OF_TILES_ON_MAP; i++) {
                  this.mapTiles[i] = new Tile(); // create new Tile object
                  this.mapTiles[i].resourceType = mapResources[i];
                  this.mapTiles[i].diceNumber = mapDiceNumbers[i];
                  System.out.println("Tile " + i + ": Resource Type = " + this.mapTiles[i].resourceType + ", Dice Number = " + this.mapTiles[i].diceNumber);
            }

            this.winConditionRelativeTokenScore = winConditionRelativeTokenScore; // store the relative token score tables

            /* Given the win conditions relative token score tables & tile resource and dice number information, this analyse the most suitable win condtiion for the current map.
            I.e. returns which win condition is the most suitable one to any "tiles area" on the map including the resource and dice number.*/
            WinConditionTileScores();

            /* After running the most suitable win condition algorithm. we then want to loop each win scenerio in a sorted way
            and rank the biggest "Strongly Connected Component" on the graph with the best resources and dice numbers matching what we wanted.
            at the end, we'll output the most suitable areas for the start settelments for each of the win scenarios. */
           StronglyConnectedComponentsSorting();


            // .................
           /*
            Then, we want to create a Bipartite Graph between all avaliable 54 settlement locations and the 18 tiles
            The goal is to then find the best matching pairs between the best win scenerios and for each of them to the areas we just found.
            This way, we can find the best settlement locations that closely match the win condition scener
            */

            // Create a Bipartite Graph between all avaliable 54 settlement locations and the 18 tiles
            int[][] BipartiteGraph = new int[54][18];
      }


      // calculates the optimal tiles (based on dice num & resource type) for each win condition based on the map graph connection
      private void WinConditionTileScores() 
      {     
            /* 
            the algorithm works like this:

            Given a matrix (num of win conditions) x (18) -18 tiles on the map & winconditions dice numbers matrix
            Initialize all values to 0

            for each win condition scenerio:
                  for each tile on the map:
                        if the resource on the tile & dice number matches EXACTLY the win condition relative to the score table (look up the score using resource type and dice number):
                              then, add +1 to the score of that win condition for that tile area.
                        if the dice number in the according resource in the score table has an error range of 1:
                              then, add +0.5 to the score of that win condition for that tile area.
                        if the resource on the tile & dice number has error range of 2:
                              then, add +0.25 to the score of that win condition for that tile area
                        if the resource on the tile & dice number has error range of 3:
                              then, add +0.1 to the score of that win condition for that tile area.

                  After going through all matrix entries, we then sort the win conditions (i .e. rows) based on their total score across all 18 tiles.
                  The win condition with the highest score is considered the most suitable for the current given map configuration. */
            
            // set a new array, already initialized to zeros
            double[][] winConditionPerTileScores = new double[NUM_OF_WIN_CONDITIONS][NUM_OF_TILES_ON_MAP+1]; // rows: win conditions, columns: tiles on map

            // for each win condition scenerio
            for (int i = 0; i < NUM_OF_WIN_CONDITIONS; i++) {

                  winConditionPerTileScores[i][NUM_OF_TILES_ON_MAP] = i; // store win condition index

                  // for each tile on the map
                  for (int j = 0; j < NUM_OF_TILES_ON_MAP; j++) {

                        // get tile resource type and dice number
                        int tileResourceType = this.mapTiles[j].resourceType;
                        int tileDiceNumber = this.mapTiles[j].diceNumber;

                        if (tileResourceType == 5) {
                              continue; // skip desert tiles
                        }

                        // get win condition relative token score for that resource type and dice number
                        int relativeTokenScoreBottom = winConditionRelativeTokenScore.get(0)[i][tileResourceType]; // first table is for <7
                        int relativeTokenScoreUpper = winConditionRelativeTokenScore.get(1)[i][tileResourceType]; // second table is for >7
                        
                        // calculate distance between tile dice number and both relative token scores
                        int distBottom = Math.abs(tileDiceNumber - relativeTokenScoreBottom);
                        int distUpper = Math.abs(tileDiceNumber - relativeTokenScoreUpper);

                        int bestDist = Math.min(distBottom, distUpper); // get best distance

                        winConditionPerTileScores[i][j] += getClosenessScore(bestDist);
                  }
            }

            // sort win conditions based on their total score across all tiles, without losing type of win condition
            winConditionPerTileScores = sortWinConditionsByScore(winConditionPerTileScores);

            // print matrix with
            System.out.println("Win Condition Per Tile Scores Matrix:");
            for (int i = 0; i < NUM_OF_WIN_CONDITIONS; i++) {
                  for (int j = 0; j <= NUM_OF_TILES_ON_MAP; j++) {
                        System.out.print(winConditionPerTileScores[i][j] + " ");
                  }
                  System.out.println();
            }
      }


      /*
      for each win scenerio, we want to create a set (List) of different strongly connected components and rank them from the biggest to smallest.
            then, we output the 2d array consisting of the same ordered win condition rank from before, but a set of ranked strongly connected components for each win condition.
            each strongly connected component is represented as a list of tile indices. the edges are already represented in the adjencency matrix.
            we use DFS to find all strongly connected components on the graph for each win condition.
      */
      void StronglyConnectedComponentsSorting() {
            
            Tile graph = new Tile(); // create Tile object to access adjencency matrix
            int[][] adjencencyMatrix = graph.adjencencyMatrix; // get adjencency matrix

      }
















      // other helper methods

      // helper method to get closeness score based on dice difference
      private double getClosenessScore(int ddifference) {
          return switch (ddifference) {
              case 0 -> 1.0;
              case 1 -> 0.75;
              case 2 -> 0.5;
              case 3 -> 0.25;
              default -> 0.0;
          };
      }


      // sorts win conditions based on their total score across all tiles
      private double[][] sortWinConditionsByScore(double[][] winConditionPerTileScores) {
            // sort win conditions based on their total score across all tiles
            Arrays.sort(winConditionPerTileScores, (row1, row2) -> Double.compare(sum(row2), sum(row1)));

            return winConditionPerTileScores;
      }


      // Helper method to calculate the sum of a double array
      private double sum(double[] array) {
            double total = 0.0;
            for (double value : array) {
                  total += value;
            }
            return total;
      }

    
   
}

