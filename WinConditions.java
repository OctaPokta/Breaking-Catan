package CatanAnalysis;

import java.util.ArrayList;

public class WinConditions {


  // Calculating important index table
  private static double[][] importantIndex(double[][] winConditionsResources, double[][] resourcesAvailability) {
    // Calculating total resources needed for each win condition on the last column of winConditionsResources & save in new array
    int sum;
    int[] oldTotal = new int[5]; // store old totals (just for reference)
    for (int i = 0; i < 5; i++) {
        sum = 0;
        for (int j = 0; j < 5; j++) {
            sum += winConditionsResources[i][j];
        }
        winConditionsResources[i][5] = sum;
        oldTotal[i] = sum;
    }

    // Calculating the total weighted needs for each win condition (Resource needed / area coverage)
    double weightedNeed;
    double totalResourcesNumber;
    double resourceAreaCoverage;
    double areaProbability;
    double totalWeightedNeed;

    for (int i = 0; i < 5; i++) {
        resourceAreaCoverage = resourcesAvailability[i][0];
        totalWeightedNeed = 0;
        for (int j = 0; j < 5; j++) {

          totalResourcesNumber = winConditionsResources[i][j];
          // calculate weighted need for each resource
          areaProbability = resourceAreaCoverage / 18;
          weightedNeed = totalResourcesNumber / (areaProbability);
          totalWeightedNeed += weightedNeed;
          // add to cell
          winConditionsResources[i][j] = weightedNeed;
        }

        winConditionsResources[i][5] = totalWeightedNeed;
    }


    // Calculate resource important index (resource wighted needs / Total weighted needs), 
    // this gives us the percentage importance of each resource for each win condition
    for (int i = 0; i < 5; i++) {
        for (int j = 0; j < 5; j++) {
            // calculate resource importance index
            winConditionsResources[i][j] = winConditionsResources[i][j] / winConditionsResources[i][5];
        }
    }
    return winConditionsResources;
  }


  // Calculate highest index for each win condition
  private static double[] highestIndexCalculation(double[][] winConditionsResources) {
    double[] highestindex = {0,0,0,0,0};
    for (int i = 0; i < 5; i++) {
        for (int j = 0; j < 5; j++) {
            // load highest index
            if (winConditionsResources[i][j] > highestindex[i]) {
                highestindex[i] = winConditionsResources[i][j];
            }
        }
    }
    return highestindex;
  }


  // Main function to calculate win conditions
  public ArrayList<int[][]> WinConditionsCalculation(double[][] resourcesAvailability, double[][] winConditionsResources) {

    // Calculate important index table
    winConditionsResources = importantIndex(winConditionsResources, resourcesAvailability);
    double[] highestindex = highestIndexCalculation(winConditionsResources);
    // Create relative token score table:
    // given the resource importance index table, it then calclates the relative dice token score for each resource.
    // for less than 7
    int[][] relativeTokenScoreBottom = new int[5][5];
    for (int i = 0; i < 5; i++) {
        for (int j = 0; j < 5; j++) {
            // calculate relative token score
            double resourcePercentage = winConditionsResources[i][j];
            relativeTokenScoreBottom[i][j] = (int)(6 - (highestindex[i]*100 - resourcePercentage*100) / 3); // scale to 1-6
            System.out.print(relativeTokenScoreBottom[i][j] + " ");
        }
        System.out.println();
    }

    System.out.println();
    System.out.println();

    // load relative token score for token greater than 7
    int[][] relativeTokenScoreUpper = new int[5][5];
    for (int i = 0; i < 5; i++) {
        for (int j = 0; j < 5; j++) {
          int oldToken = relativeTokenScoreBottom[i][j];
          relativeTokenScoreUpper[i][j] = 7 + Math.abs(oldToken-7); // Calculate 
          System.out.print(relativeTokenScoreUpper[i][j] + " ");
        }
      System.out.println();
      }


    System.out.println();
    System.out.println();
    
    // Store both relative token scores in a list and return
    ArrayList<int[][]> relativeTokenScores = new ArrayList<>();
    relativeTokenScores.add(relativeTokenScoreUpper);
    relativeTokenScores.add(relativeTokenScoreBottom);

    return relativeTokenScores;
  }
}


// Outputs "Hello World!"