package CatanAnalysis;

import java.util.ArrayList;

public class Test {

    
    // =========================================================================
      //  VISUALIZATION: Print Debugging & Python Data
      // =========================================================================
      public void printAnalysisAndPythonData(ArrayList<ArrayList<ConnectedComponentTile>>[] sortedComponents, double[][] winConditionPerTileScores, String[] winConditionNames, int NUM_OF_TILES_ON_MAP) 
      {
        // --- Part A: Visual Heatmap Debugging ---
        for (int i = 0; i < sortedComponents.length; i++) {
            int lastColumnIndex = winConditionPerTileScores[i].length - 1;
            int originalID = (int) winConditionPerTileScores[i][lastColumnIndex];
            String wcName = (originalID < winConditionNames.length) ? winConditionNames[originalID] : "Unknown";

            System.out.println("\n################################################################");
            System.out.println("STRATEGY " + (i + 1) + ": " + wcName.toUpperCase());
            System.out.println("################################################################");

            double currentPrintedScore = -1.0; 
            int clusterCount = 1;

            for (ArrayList<ConnectedComponentTile> group : sortedComponents[i]) 
                {
                if (group.isEmpty()) continue;

                double groupScore = group.get(0).TILE_VALUE;
                if (groupScore != currentPrintedScore) {
                        System.out.println("\n  === " + getIntensityLabel(groupScore) + " (Value: " + groupScore + ") ===");
                        currentPrintedScore = groupScore;
                }

                System.out.print("    Cluster #" + clusterCount + ": ");
                for (int k = 0; k < group.size(); k++) {
                        ConnectedComponentTile tile = group.get(k);
                        String rName = getResourceName(tile.TILE_RESOURCE_TYPE);
                        int dNum = (int) tile.TILE_DICE_NUMBER;
                        System.out.print("[" + rName + " " + dNum + " (id:" + tile.TILE_INDEX + ")]");
                        if (k < group.size() - 1) System.out.print(" -- ");
                }
                System.out.println();
                clusterCount++;
            }
        }


        // --- Part B: Python Data Block Generation ---
        // This prints a matrix where EACH ROW represents ONE CONNECTED GROUP
        System.out.println("\n\n=== COPY THE TEXT BELOW INTO PYTHON 'data_str' VARIABLE ===");
            
        for (int i = 0; i < sortedComponents.length; i++) {
            int lastColumnIndex = winConditionPerTileScores[i].length - 1;
            int originalID = (int) winConditionPerTileScores[i][lastColumnIndex];

            for (ArrayList<ConnectedComponentTile> group : sortedComponents[i]) {
                if (group.isEmpty()) continue;

                // Create a row of zeros
                double[] rowForPython = new double[NUM_OF_TILES_ON_MAP];
                // Fill in values for tiles in this group
                for (ConnectedComponentTile tile : group) {
                        rowForPython[tile.TILE_INDEX] = tile.TILE_VALUE;
                }

                // Print formatted row
                StringBuilder sb = new StringBuilder();
                for (double val : rowForPython) {
                        sb.append(String.format("%.2f ", val));
                }
                sb.append(originalID); // Append Strategy ID
                System.out.println(sb.toString());
            }
        }
        System.out.println("===========================================================\n");
      }



      // info
      private String getIntensityLabel(double score) {
            if (score >= 1.0) return "HIGHEST PRIORITY (Dark Red)";
            if (score >= 0.75) return "HIGH PRIORITY (Red)";
            if (score >= 0.50) return "MEDIUM PRIORITY (Orange)";
            return "LOW PRIORITY (Yellow)";
      }

      // info
      private String getResourceName(int resourceID) {
            switch (resourceID) {
                case 0: return "brick";
                case 1: return "ore";
                case 2: return "Sheep";
                case 3: return "Wheat";
                case 4: return "wood";
                case 5: return "Desert";
                default: return "Res" + resourceID;
            }
      }
    
}
