package io.github.Capstone_112_fall.utils;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.Array;
import io.github.Capstone_112_fall.components.*;


// creates entities and adds components to them
public class EntityManager {
    private final Engine engine;
    private final AnimationFactory animationFactory;
    private final World world;
    private final TextureAtlas atlas;

    public EntityManager(Engine engine, AnimationFactory animationFactory, World world, TextureAtlas atlas) {
        this.atlas = atlas;
        this.engine = engine;
        this.animationFactory = animationFactory;
        this.world = world;
    }

    public Entity createPlayerEntity(float x, float y){
        Entity player = engine.createEntity();
        Body body = createPlayer(x, y);

        // create and add body component
        Box2DComponent box2DComponent = engine.createComponent(Box2DComponent.class);
        box2DComponent.body = body;
        player.add(box2DComponent);

        // create and add transform component
        TransformComponent transformComponent = engine.createComponent(TransformComponent.class);
        player.add(transformComponent);

        // create and add player component/tag
        // creates a reference to player component
        PlayerComponent playerComponent = engine.createComponent(PlayerComponent.class);
        player.add(playerComponent);

        // create and add grounded component\
        GroundedComponent groundedComponent = engine.createComponent(GroundedComponent.class);
        player.add(groundedComponent);
        body.setUserData(groundedComponent); // attach grounded component to body for collision detection

        StateComponent stateComponent = engine.createComponent(StateComponent.class);
        player.add(stateComponent);

        TextureComponent textureComponent = engine.createComponent(TextureComponent.class);
        textureComponent.zIndex = 10;
        player.add(textureComponent);

        player.add(animationFactory.createPlayerAnimations(engine));;
        engine.addEntity(player);
        return player;
    }

    // Written by Claude (Anthropic AI assistant)
    // creates entities from the values read by MapBuilder
    public void createMapEntities(Array<MapEntityData> data) {
        for(MapEntityData d : data) {
            createMapEntity(d);
        }
    }

    // Written by Claude (Anthropic AI assistant)
    public Entity createMapEntity(MapEntityData d) {
        // atlas region overrides the tileset texture if set
        TextureRegion region = d.regionName != null ? atlas.findRegion(d.regionName) : d.region;

        switch(d.type) {
            // add cases here for special blocks (key, lucky, save, spike...)
            default:
                return createStaticBlock(d.x, d.y, d.width, d.height, region);
        }
    }

    // Modified by Claude (Anthropic AI assistant)
    public Entity createStaticBlock(float x, float y, float width, float height, TextureRegion region) {
        // create box2d body
        BodyDef bDef = new BodyDef();
        bDef.type = BodyDef.BodyType.StaticBody;
        bDef.position.set(x, y);
        Body body = world.createBody(bDef);

        PolygonShape shape = new PolygonShape();
        shape.setAsBox(width / 2f, height / 2f);

        FixtureDef fDef = new FixtureDef();
        fDef.shape = shape;
        fDef.friction = 0.5f;
        body.createFixture(fDef);
        shape.dispose();

        // create entity and add components
        Entity block = engine.createEntity();
        Box2DComponent box2DComponent = engine.createComponent(Box2DComponent.class);
        box2DComponent.body = body;
        block.add(box2DComponent);

        TransformComponent transformComponent = engine.createComponent(TransformComponent.class);
        transformComponent.x = x;
        transformComponent.y = y;
        block.add(transformComponent);

        TextureComponent textureComponent = engine.createComponent(TextureComponent.class);
        textureComponent.textureRegion = region;
        textureComponent.zIndex = 0;
        // scale the texture so it fills the body
        if(region != null) textureComponent.scale = width / (region.getRegionWidth() / 32f);
        block.add(textureComponent);

        engine.addEntity(block);
        return block;
    }

    private Body createPlayer(float x, float y){
        // Definition: Type and position
        BodyDef bDef = new BodyDef();
        bDef.type = BodyDef.BodyType.DynamicBody;
        bDef.position.set(x, y);
        bDef.fixedRotation = true;

        Body playerBody = world.createBody(bDef); // create body in world

        // Create shape for body
        PolygonShape shape = new PolygonShape();
        shape.setAsBox(0.48f, 0.5f);

        // Attach shape and fixtures to body
        FixtureDef fDef = new FixtureDef();
        fDef.shape = shape;
        fDef.friction = 0f;
        fDef.density = 1/0.98f; // Mass = area * density
        fDef.restitution = 0.1f;
        Fixture mainFixture = playerBody.createFixture(fDef);
        mainFixture.setUserData("player_body"); // to identify body later
        shape.dispose();

        // Create shape for foot
        PolygonShape shape2 = new PolygonShape();
        shape2.setAsBox(0.40f, 0.1f, new Vector2(0, -0.45f), 0);

        // Attach foot to body
        FixtureDef fDef2 = new FixtureDef();
        fDef2.shape = shape2;
        fDef2.isSensor = true; // only used to detect collisions
        Fixture footFixture = playerBody.createFixture(fDef2);
        footFixture.setUserData("player_foot"); // to identify foot later
        shape2.dispose();

        return playerBody;
    }
}
