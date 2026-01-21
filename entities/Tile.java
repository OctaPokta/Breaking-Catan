package CatanAnalysis.entities;

public class Tile {
    
    // attributes
    public int RESOURCE_TYPE; // 0 = brick, 1 = ore, 2 = sheep, 3 = wheat, 4 = wood, 5 = desert 
    public int DICE_NUMBER; // dice number on map
    public int TILE_INDEX; // tile index

    // create adjencency matrix for map graph. 0 = no adjencency, 1 = adjencency
    // for 18 tiles, the numbers are counted from top left to bottom right, left to right, top to bottom.
    // the rotation of the board does not matter for this analysis
    public int[][] adjencencyMatrix = {
    {0,1,0,1,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0}, // 0
    {1,0,1,0,1,1,0,0,0,0,0,0,0,0,0,0,0,0,0}, // 1
    {0,1,0,0,0,1,1,0,0,0,0,0,0,0,0,0,0,0,0}, // 2
    {1,0,0,0,1,0,0,1,1,0,0,0,0,0,0,0,0,0,0}, // 3
    {1,1,0,1,0,1,0,0,1,1,0,0,0,0,0,0,0,0,0}, // 4
    {0,1,1,0,1,0,1,0,0,1,1,0,0,0,0,0,0,0,0}, // 5
    {0,0,1,0,0,1,0,0,0,0,1,1,0,0,0,0,0,0,0}, // 6
    {0,0,0,1,0,0,0,0,1,0,0,0,1,0,0,0,0,0,0}, // 7
    {0,0,0,1,1,0,0,1,0,1,0,0,1,1,0,0,0,0,0}, // 8
    {0,0,0,0,1,1,0,0,1,0,1,0,0,1,1,0,0,0,0}, // 9
    {0,0,0,0,0,1,1,0,0,1,0,1,0,0,1,1,0,0,0}, // 10
    {0,0,0,0,0,0,1,0,0,0,1,0,0,0,0,1,0,0,0}, // 11
    {0,0,0,0,0,0,0,1,1,0,0,0,0,1,0,0,1,0,0}, // 12
    {0,0,0,0,0,0,0,0,1,1,0,0,1,0,1,0,1,1,0}, // 13
    {0,0,0,0,0,0,0,0,0,1,1,0,0,1,0,1,0,1,1}, // 14
    {0,0,0,0,0,0,0,0,0,0,1,1,0,0,1,0,0,0,1}, // 15
    {0,0,0,0,0,0,0,0,0,0,0,0,1,1,0,0,0,1,0}, // 16
    {0,0,0,0,0,0,0,0,0,0,0,0,0,1,1,0,1,0,1}, // 17
    {0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,1,0,1,0}}; // 18    

    // get functions:
    public int getResourceType() {
      return this.RESOURCE_TYPE;
    }

    public int getDiceNumber() {
      return this.DICE_NUMBER;
    }

    public int getTileIndex() {
      return this.TILE_INDEX;
    }
}
