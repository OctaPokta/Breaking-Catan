package CatanAnalysis;

public class ConnectedComponentTile 
{
    // attributes
    public int TILE_INDEX;
    public double TILE_VALUE;    
    public int TILE_RESOURCE_TYPE; 
    public double TILE_DICE_NUMBER;

    // constructor
    public ConnectedComponentTile (int TileIndex, double TileValue, int tileResourceType, double tileDiceNumber) {
        TILE_INDEX = TileIndex; // graph index in adjecancy matrix
        TILE_VALUE = TileValue; // heatmap value
        TILE_RESOURCE_TYPE = tileResourceType; // resource type for tile
        TILE_DICE_NUMBER = tileDiceNumber; // dice number 

    }
}
