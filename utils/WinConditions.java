package CatanAnalysis.utils;

import java.util.ArrayList;

public class WinConditions {

  public ArrayList<int[][]> relativeTokenScoresResults;
  private final int[][] relativeTokenScoreBottom; // to store relative token scores for dice numbers less than 7
  private final int[][] relativeTokenScoreUpper; // to store relative token scores for dice numbers greater than 7
  private final int[] oldTotal; // to store old totals for reference
  private final int TOTAL_WIN_CONDITION_NUM;
  private final int TOTAL_RESOURCES = 5; // total number of resources

  // constructor
  public WinConditions(int totalWinConditionNum) {
    this.oldTotal = new int[this.TOTAL_RESOURCES];
    this.TOTAL_WIN_CONDITION_NUM = totalWinConditionNum;
    this.relativeTokenScoreBottom = new int[this.TOTAL_WIN_CONDITION_NUM][this.TOTAL_RESOURCES];
    this.relativeTokenScoreUpper = new int[this.TOTAL_WIN_CONDITION_NUM][this.TOTAL_RESOURCES];
  }
  

  // Calculating important index table for each win condition
  public double[][] importantIndex(double[][] winConditionsResources, double[][] resourcesAvailability) {
    // Calculating total resources needed for each win condition on the last column of winConditionsResources & save in new array
    //
    // BUG FIX: this outer loop iterates over WIN CONDITIONS (rows of winConditionsResources),
    // so it must be bounded by TOTAL_WIN_CONDITION_NUM, not TOTAL_RESOURCES (the resource-COLUMN
    // count). The two constants happened to both equal 5 with the original 5-row strategy table,
    // which silently masked this for every run of the algorithm to date - any row beyond index 4
    // was never processed. Same fix applied to the two loops below.
    int sum;
    for (int i = 0; i < this.TOTAL_WIN_CONDITION_NUM; i++) {
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

    for (int i = 0; i < this.TOTAL_WIN_CONDITION_NUM; i++) {
        totalWeightedNeed = 0;
        for (int j = 0; j < this.TOTAL_RESOURCES; j++) {

          // BUG FIX: area coverage must be looked up per RESOURCE (column j - brick/ore/sheep/
          // wheat/wood), not per win-condition row (i). The original code fetched
          // resourcesAvailability[i][0] once per row and applied that single (wrong) resource's
          // coverage to all 5 resource columns of that row.
          resourceAreaCoverage = resourcesAvailability[j][0];
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
    for (int i = 0; i < this.TOTAL_WIN_CONDITION_NUM; i++) {
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
    
    for (int i = 0; i < this.TOTAL_WIN_CONDITION_NUM; i++) {
        for (int j = 0; j < this.TOTAL_RESOURCES; j++) {
            // calculate relative token score
            double resourcePercentage = winConditionsResources[i][j];
            this.relativeTokenScoreBottom[i][j] = (int)(6 - (highestindex[i]*100 - resourcePercentage*100) / 3); // scale to 1-6
        }
    }

    // load relative token score for token greater than 7
    for (int i = 0; i < this.TOTAL_WIN_CONDITION_NUM; i++) {
        for (int j = 0; j < this.TOTAL_RESOURCES; j++) {
          int oldToken = this.relativeTokenScoreBottom[i][j];
          this.relativeTokenScoreUpper[i][j] = 7 + Math.abs(oldToken-7); // Calculate 
        }
    }
    
    // Store both relative token scores in a list and return
    ArrayList<int[][]> relativeTokenScores = new ArrayList<>();
    relativeTokenScores.add(this.relativeTokenScoreUpper);
    relativeTokenScores.add(this.relativeTokenScoreBottom);

    this.relativeTokenScoresResults = relativeTokenScores;
  }


  // DEBUG

  // print lower dice relative token score table
  public void printRelativeTokenScoreBottom() {
    System.out.println("Relative Token Score for dice numbers less than 7:");
    for (int i = 0; i < this.TOTAL_WIN_CONDITION_NUM; i++) {
        for (int j = 0; j < this.TOTAL_RESOURCES; j++) {
            System.out.print(this.relativeTokenScoreBottom[i][j] + " ");
        }
        System.out.println();
    }
  }


  // print upper dice relative token score table
  public void printRelativeTokenScoreTop() {
    System.out.println("Relative Token Score for dice numbers greater than 7:");
    for (int i = 0; i < this.TOTAL_WIN_CONDITION_NUM; i++) {
        for (int j = 0; j < this.TOTAL_RESOURCES; j++) {
            System.out.print(this.relativeTokenScoreUpper[i][j] + " ");
        }
        System.out.println();
    }
  }


}

