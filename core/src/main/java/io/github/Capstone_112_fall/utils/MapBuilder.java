package io.github.Capstone_112_fall.utils;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.objects.TiledMapTileMapObject;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;

// Code in this file was written by Claude (Anthropic AI assistant)
// utility class that reads from tiled maps and turns tiles and objects into MapEntityData
public class MapBuilder {
    private static final String DEFAULT_TYPE = "block";

    // reads every layer in the map
    public static Array<MapEntityData> parse(TiledMap map, float ppm) {
        Array<MapEntityData> out = new Array<>();
        if(map == null) return out;
        for(MapLayer layer : map.getLayers()) {
            parseLayer(layer, ppm, out);
        }
        return out;
    }

    private static void parseLayer(MapLayer layer, float ppm, Array<MapEntityData> out) {
        if(layer == null) return;
        if(layer instanceof TiledMapTileLayer) {
            parseTileLayer((TiledMapTileLayer) layer, ppm, out);
        } else {
            parseObjectLayer(layer, ppm, out);
        }
    }

    private static void parseTileLayer(TiledMapTileLayer layer, float ppm, Array<MapEntityData> out) {
        float tileWidth = layer.getTileWidth();
        float tileHeight = layer.getTileHeight();
        float widthMeters = tileWidth / ppm;
        float heightMeters = tileHeight / ppm;

        // row 0 is the bottom of the map in libgdx
        for(int row = 0; row < layer.getHeight(); row++) {
            for(int col = 0; col < layer.getWidth(); col++) {
                TiledMapTileLayer.Cell cell = layer.getCell(col, row);
                if(cell == null || cell.getTile() == null) continue;
                TiledMapTile tile = cell.getTile();

                float centerX = ((col + 0.5f) * tileWidth + layer.getOffsetX()) / ppm;
                float centerY = ((row + 0.5f) * tileHeight - layer.getOffsetY()) / ppm;

                MapProperties props = new MapProperties();
                props.putAll(tile.getProperties());

                out.add(new MapEntityData(getType(props), centerX, centerY, widthMeters, heightMeters,
                    0f, cell.getFlipHorizontally(), cell.getFlipVertically(), props));
            }
        }

        // entities draw these tiles now, so the map renderer should skip this layer
        layer.setVisible(false);
    }

    private static void parseObjectLayer(MapLayer layer, float ppm, Array<MapEntityData> out) {
        // iterate through every object
        for(MapObject object : layer.getObjects()) {
            if(object instanceof TiledMapTileMapObject) {
                // objects placed with the tile tool
                TiledMapTileMapObject tileObject = (TiledMapTileMapObject) object;
                TiledMapTile tile = tileObject.getTile();
                TextureRegion region = tile.getTextureRegion();

                // object properties override tile properties
                MapProperties props = new MapProperties();
                props.putAll(tile.getProperties());
                props.putAll(object.getProperties());

                // x and y are the bottom left corner
                float width = props.get("width", (float) region.getRegionWidth(), Float.class);
                float height = props.get("height", (float) region.getRegionHeight(), Float.class);
                float centerX = (tileObject.getX() + width / 2f) / ppm;
                float centerY = (tileObject.getY() + height / 2f) / ppm;

                out.add(new MapEntityData(getType(props), centerX, centerY, width / ppm, height / ppm,
                    -tileObject.getRotation(), tileObject.isFlipHorizontally(), tileObject.isFlipVertically(), props));
            } else if(object instanceof RectangleMapObject) {
                Rectangle rect = ((RectangleMapObject) object).getRectangle();
                MapProperties props = object.getProperties();

                float centerX = (rect.x + rect.width / 2f) / ppm;
                float centerY = (rect.y + rect.height / 2f) / ppm;

                out.add(new MapEntityData(getType(props), centerX, centerY, rect.width / ppm, rect.height / ppm,
                    0f, false, false, props));
            }
        }
    }

    private static String getType(MapProperties props) {
        return props.get("type", DEFAULT_TYPE, String.class);
    }
}
