package CatanAnalysis.testing;

import CatanAnalysis.CatanAnalysisEngine;
import CatanAnalysis.entities.ConnectedComponentTile;
import java.util.ArrayList;

public class Test {

    private final int NUM_OF_TILES_ON_MAP = 19; // standard Catan map has 19 tiles
    private final String [] WIN_COND_NAMES = CatanAnalysisEngine.WIN_COND_NAMES;
    private final double[][] WIN_COND_PER_TILE_SCORES; 
    private final ArrayList<ArrayList<ConnectedComponentTile>>[] mapGroupData; // from Map class, contains the connected components sorted by tile scores for each strategy

    // constructor
    public Test(double[][] winCondPerTileScores, ArrayList<ArrayList<ConnectedComponentTile>>[] mapGroupData) {
        this.WIN_COND_PER_TILE_SCORES = winCondPerTileScores;
        this.mapGroupData = mapGroupData;
    }

    // show heatmaps cluster properties based on strategy, per each tile
    public void printHeatmapClusterProprity() {

        // --- Part A: Visual Heatmap Debugging ---
        for (int i = 0; i < mapGroupData.length; i++) {
            int lastColumnIndex = WIN_COND_PER_TILE_SCORES[i].length - 1;
            int originalID = (int) WIN_COND_PER_TILE_SCORES[i][lastColumnIndex];
            String wcName = (originalID < WIN_COND_NAMES.length) ? WIN_COND_NAMES[originalID] : "Unknown";

            System.out.println("\n################################################################");
            System.out.println("STRATEGY " + (i + 1) + ": " + wcName.toUpperCase());
            System.out.println("################################################################");

            double currentPrintedScore = -1.0; 
            int clusterCount = 1;

            for (ArrayList<ConnectedComponentTile> group : this.mapGroupData[i]) 
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
    }

    // print Python data block for heatmap visualization
    public void printPythondataBlockForHeatmap() {

        // This prints a matrix where EACH ROW represents ONE CONNECTED GROUP
        System.out.println("\n\n=== COPY THE TEXT BELOW INTO PYTHON 'data_str' VARIABLE ===");
            
        for (int i = 0; i < mapGroupData.length; i++) {
            int lastColumnIndex = WIN_COND_PER_TILE_SCORES[i].length - 1;
            int originalID = (int) WIN_COND_PER_TILE_SCORES[i][lastColumnIndex];

            for (ArrayList<ConnectedComponentTile> group : this.mapGroupData[i]) {
                if (group.isEmpty()) continue;

                // Create a row of zeros
                double[] rowForPython = new double[this.NUM_OF_TILES_ON_MAP];
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
        return switch (resourceID) {
            case 0 -> "brick";
            case 1 -> "ore";
            case 2 -> "Sheep";
            case 3 -> "Wheat";
            case 4 -> "wood";
            case 5 -> "Desert";
            default -> "Res" + resourceID;
        };
    }
    
}
