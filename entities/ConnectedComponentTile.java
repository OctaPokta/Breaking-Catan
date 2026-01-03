package CatanAnalysis.entities;

public class ConnectedComponentTile 
{
    // attributes
    public int TILE_INDEX;
    public double TILE_VALUE;    
    public int TILE_RESOURCE_TYPE; 
    public double TILE_DICE_NUMBER;

    // constructor
    public ConnectedComponentTile (int TileIndex, double TileValue, int tileResourceType, double tileDiceNumber) {
        this.TILE_INDEX = TileIndex; // graph index in adjecancy matrix
        this.TILE_VALUE = TileValue; // heatmap value
        this.TILE_RESOURCE_TYPE = tileResourceType; // resource type for tile
        this.TILE_DICE_NUMBER = tileDiceNumber; // dice number 

    }

    // get methods:
    public int getTileIndex() {
        return this.TILE_INDEX;
    }

    public void printAllTiles() {
        System.out.println("Tile Index: " + this.TILE_INDEX + " Dice: " + this.TILE_DICE_NUMBER + "ResourceType: " + this.TILE_RESOURCE_TYPE);
    }
}
