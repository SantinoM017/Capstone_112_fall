package io.github.Capstone_112_fall.utils;

import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.physics.box2d.*;

public class MapBuilder {
    public static void buildShapes(MapLayer layer, float ppm, World world){
        if(layer == null) return;
        // iterate through every object
        for(MapObject object : layer.getObjects()){
            // check for rectangle objects
            if(object instanceof RectangleMapObject){
                Rectangle r = ((RectangleMapObject) object).getRectangle();

                BodyDef bdef = new BodyDef();
                bdef.type = BodyDef.BodyType.StaticBody;

                // convert from pixels to meters
                float centerX = (r.x + r.width / 2f) / ppm;
                float centerY = (r.y + r.height / 2f) / ppm;
                bdef.position.set(centerX, centerY);

                Body body = world.createBody(bdef);

                PolygonShape shape = new PolygonShape();
                // convert from pixels to meters
                shape.setAsBox((r.width / 2f) / ppm, (r.height / 2f) / ppm);

                FixtureDef fdef = new FixtureDef();
                fdef.shape = shape;
                fdef.friction = 0.5f;
                body.createFixture(fdef);

                shape.dispose();
            }
        }
    }
}
