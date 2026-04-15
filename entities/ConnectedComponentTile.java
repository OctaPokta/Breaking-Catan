package CatanAnalysis.entities;

import java.util.ArrayList;

// this class is used to represent the connected component of a tile, it stores the tile data and the list of all the towns that are connected to it, this is used to rate the town locations based on the hexagons that touches it and the importance of the resources that those hexagons produce.
public class ConnectedComponentTile 
{
    // attributes
    public int TILE_INDEX; // graph index in adjecancy matrix
    public double TILE_VALUE; // heatmap value
    public int TILE_RESOURCE_TYPE; // resource type for tile
    public double TILE_DICE_NUMBER; // dice number 
    public ArrayList<RoadConnectionGraph> connectedTowns; // list of all the connected towns of class "TownsAndHexagons"

    // constructor
    public ConnectedComponentTile (int TileIndex, double TileValue, int tileResourceType, double tileDiceNumber) {
        this.TILE_INDEX = TileIndex;
        this.TILE_VALUE = TileValue;
        this.TILE_RESOURCE_TYPE = tileResourceType;
        this.TILE_DICE_NUMBER = tileDiceNumber;
        this.connectedTowns = new ArrayList<>();
    }

    // get methods:
    public int getTileIndex() {
        return this.TILE_INDEX;
    }

    public void printAllTiles() {
        System.out.println("Tile Index: " + this.TILE_INDEX + " Dice: " + this.TILE_DICE_NUMBER + "ResourceType: " + this.TILE_RESOURCE_TYPE);
    }
}
