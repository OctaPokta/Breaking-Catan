package CatanAnalysis.utils;

import java.util.ArrayList;

public class WinConditions {

  public ArrayList<int[][]> relativeTokenScoresResults;
  private int[] oldTotal; // to store old totals for reference
  private final int TOTAL_WIN_CONDITION_NUM;
  private final int TOTAL_RESOURCES = 5; // total number of resources

  // constructor
  public WinConditions(int totalWinConditionNum) {
    this.oldTotal = new int[this.TOTAL_RESOURCES];
    this.TOTAL_WIN_CONDITION_NUM = totalWinConditionNum;
  }


  // Calculating important index table for each win condition
  public double[][] importantIndex(double[][] winConditionsResources, double[][] resourcesAvailability) {
    // Calculating total resources needed for each win condition on the last column of winConditionsResources & save in new array
    int sum;
    for (int i = 0; i < this.TOTAL_RESOURCES; i++) {
        sum = 0;
        for (int j = 0; j < this.TOTAL_RESOURCES; j++) {
            sum += winConditionsResources[i][j];
        }
        winConditionsResources[i][this.TOTAL_RESOURCES] = sum;
        this.oldTotal[i] = sum; // store old total
    }

    // Calculating the total weighted needs for each win condition (Resource needed / area coverage)
    double weightedNeed;
    double totalResourcesNumber;
    double resourceAreaCoverage;
    double areaProbability;
    double totalWeightedNeed;

    for (int i = 0; i < this.TOTAL_RESOURCES; i++) {
        resourceAreaCoverage = resourcesAvailability[i][0];
        totalWeightedNeed = 0;
        for (int j = 0; j < this.TOTAL_RESOURCES; j++) {

          totalResourcesNumber = winConditionsResources[i][j];
          // calculate weighted need for each resource
          areaProbability = resourceAreaCoverage / 18;
          weightedNeed = totalResourcesNumber / (areaProbability);
          totalWeightedNeed += weightedNeed;
          // add to cell
          winConditionsResources[i][j] = weightedNeed;
        }

        winConditionsResources[i][this.TOTAL_RESOURCES] = totalWeightedNeed;
    }


    // Calculate resource important index (resource wighted needs / Total weighted needs), 
    // this gives us the percentage importance of each resource for each win condition
    for (int i = 0; i < this.TOTAL_RESOURCES; i++) {
        for (int j = 0; j < this.TOTAL_RESOURCES; j++) {
            // calculate resource importance index
            winConditionsResources[i][j] = winConditionsResources[i][j] / winConditionsResources[i][this.TOTAL_RESOURCES];
        }
    }
    return winConditionsResources;
  }


  // Calculate highest index for each win condition
  private double[] highestIndexCalculation(double[][] winConditionsResources) {
    double[] highestindex = {0,0,0,0,0};
    // loop through each win condition
    for (int i = 0; i < this.TOTAL_WIN_CONDITION_NUM; i++) {

      // loop through each resource
      for (int j = 0; j < this.TOTAL_RESOURCES; j++) {
          // load highest index
          if (winConditionsResources[i][j] > highestindex[i]) {
              highestindex[i] = winConditionsResources[i][j];
          }
      }
    }
    return highestindex;
  }


  // Main function to calculate win conditions
  public void winConditionsCalculation(double[][] resourcesAvailability, double[][] winConditionsResources) {

    // Calculate important index table
    winConditionsResources = importantIndex(winConditionsResources, resourcesAvailability);
    double[] highestindex = highestIndexCalculation(winConditionsResources);
    // Create relative token score table:
    // given the resource importance index table, it then calclates the relative dice token score for each resource.
    // for less than 7
    int[][] relativeTokenScoreBottom = new int[this.TOTAL_WIN_CONDITION_NUM][this.TOTAL_RESOURCES];
    for (int i = 0; i < this.TOTAL_WIN_CONDITION_NUM; i++) {
        for (int j = 0; j < this.TOTAL_RESOURCES; j++) {
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
    int[][] relativeTokenScoreUpper = new int[this.TOTAL_WIN_CONDITION_NUM][this.TOTAL_RESOURCES];
    for (int i = 0; i < this.TOTAL_WIN_CONDITION_NUM; i++) {
        for (int j = 0; j < this.TOTAL_RESOURCES; j++) {
          int oldToken = relativeTokenScoreBottom[i][j];
          relativeTokenScoreUpper[i][j] = 7 + Math.abs(oldToken-7); // Calculate 
          System.out.print(relativeTokenScoreUpper[i][j] + " ");
        }
      System.out.println();
      }


    System.out.println();
    
    // Store both relative token scores in a list and return
    ArrayList<int[][]> relativeTokenScores = new ArrayList<>();
    relativeTokenScores.add(relativeTokenScoreUpper);
    relativeTokenScores.add(relativeTokenScoreBottom);

    this.relativeTokenScoresResults = relativeTokenScores;
  }

}

