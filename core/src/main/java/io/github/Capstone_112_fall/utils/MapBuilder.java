package io.github.Capstone_112_fall.utils;

import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.physics.box2d.*;

// utility class that reads from tiled maps and turns into objects
public class MapBuilder {
    public static void buildShapes(MapLayer layer, float ppm, EntityManager entityManager, String defaultRegionName){
        if(layer == null) return;
        // iterate through every object
        for(MapObject object : layer.getObjects()){
            // check for rectangle objects
            Rectangle rect = ((RectangleMapObject)object).getRectangle();

            float centerX = (rect.x + rect.width / 2f) / ppm;
            float centerY = (rect.y + rect.height / 2f) / ppm;
            float widthMeters = rect.width / ppm;
            float heightMeters = rect.height / ppm;

            String regionName = object.getProperties().get("texture", defaultRegionName, String.class);

            entityManager.createStaticBlock(centerX, centerY, widthMeters, heightMeters, regionName);
        }
    }
}

