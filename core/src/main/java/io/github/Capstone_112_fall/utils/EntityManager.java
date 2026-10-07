package io.github.Capstone_112_fall.utils;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.Array;
import io.github.Capstone_112_fall.components.*;
import io.github.Capstone_112_fall.Main;


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

    public Entity createPlayerEntity(float x, float y) {
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

        StateComponent stateComponent = engine.createComponent(StateComponent.class);
        player.add(stateComponent);

        TextureComponent textureComponent = engine.createComponent(TextureComponent.class);
        textureComponent.zIndex = 10;
        textureComponent.bottomAligned = true;
        player.add(textureComponent);

        player.add(animationFactory.createPlayerAnimations(engine));
        ;

        HealthComponent healthComponent = engine.createComponent(HealthComponent.class);
        healthComponent.maxHp = 100;
        healthComponent.hp = 100;
        player.add(healthComponent);
        body.setUserData(player);
        engine.addEntity(player);
        return player;
    }

    // Written by Claude (Anthropic AI assistant)
    // creates entities from the values read by MapBuilder
    public void createMapEntities(Array<MapEntityData> data) {
        for (MapEntityData d : data) {
            createMapEntity(d);
        }
    }

    public void createMapSideBoundaries(float mapWidth, float mapHeight) {
        float wallThickness = 1f;
        float wallHeight = mapHeight * 2f;
        float wallCenterY = mapHeight / 2f;

        createBoundaryWall(-wallThickness / 2f, wallCenterY, wallThickness, wallHeight);
        createBoundaryWall(mapWidth + wallThickness / 2f, wallCenterY, wallThickness, wallHeight);
    }

    public void createPlayerAttackHitbox(Entity player) {
        Box2DComponent box2D = player.getComponent(Box2DComponent.class);
        StateComponent state = player.getComponent(StateComponent.class);
        PlayerComponent playerComp = player.getComponent(PlayerComponent.class);

        if (box2D == null || box2D.body == null) return;

        // Determine attack direction based on facing direction (-1 for Left, 1 for Right)
        float direction = !playerComp.isFlipped ? 1.0f : -1.0f;

        // Create sensor fixture offset in front of the player
        PolygonShape shape = new PolygonShape();
        shape.setAsBox(0.16f, 0.4f, new Vector2(0.5f * direction, 0f), 0f);

        FixtureDef fDef = new FixtureDef();
        fDef.shape = shape;
        fDef.isSensor = true; // Sensor ensures it detects overlap without physical collision response

        Fixture attackFixture = box2D.body.createFixture(fDef);
        attackFixture.setUserData("player_attack_hitbox");
        shape.dispose();

        // Attach tracking component to the entity to manage lifespan
        AttackHitboxComponent attackComp = engine.createComponent(AttackHitboxComponent.class);
        attackComp.sensorFixture = attackFixture;
        attackComp.duration = 0.4f;
        attackComp.timer = 0f;

        DamageComponent damageComp = engine.createComponent(DamageComponent.class);
        damageComp.damage = 25;

        player.add(attackComp);
        player.add(damageComp);
    }

    public void destroyAttackHitbox(Entity entity) {
        if (entity == null) return;

        Box2DComponent box2D = entity.getComponent(Box2DComponent.class);
        AttackHitboxComponent attackComp = entity.getComponent(AttackHitboxComponent.class);

        // Ensure the entity actually has a Box2D body and an active attack component
        if (attackComp != null) {
            if (box2D != null && box2D.body != null && attackComp.sensorFixture != null) {
                // Safely destroy the sensor fixture attached to the body
                box2D.body.destroyFixture(attackComp.sensorFixture);
                attackComp.sensorFixture = null;
            }

            // Clean up components from the Ashley ECS entity
            entity.remove(AttackHitboxComponent.class);
            entity.remove(DamageComponent.class);
        }
    }

    // rewritten by Gemini
    private Body createPlayer(float x, float y) {
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.DynamicBody;
        bodyDef.position.set(x, y);
        bodyDef.fixedRotation = true; // Prevent player from tipping over

        Body body = world.createBody(bodyDef);

        float radius = 0.3f;

        // --- 1. Upper Body Box (Flat Sides prevent Wall Sticking) ---
        PolygonShape upperBox = new PolygonShape();
        upperBox.setAsBox(radius, 0.2f, new Vector2(0, 0.1f), 0);

        FixtureDef boxDef = new FixtureDef();
        boxDef.shape = upperBox;
        boxDef.friction = 0f; // MUST be 0f to slide smoothly against vertical walls
        boxDef.density = 1.0f;
        boxDef.restitution = 0.0f;

        Fixture boxFixture = body.createFixture(boxDef);
        boxFixture.setUserData("player_body");
        upperBox.dispose();

        // --- 2. Bottom Circle (Smooth Slope & Step Traversal) ---
        // Radius = 0.4m, Center at y = -0.1m -> Covers y = +0.3m down to y = -0.5m (Feet)
        CircleShape bottomCircle = new CircleShape();
        bottomCircle.setRadius(radius);
        bottomCircle.setPosition(new Vector2(0, -0.2f));

        FixtureDef circleDef = new FixtureDef();
        circleDef.shape = bottomCircle;
        circleDef.friction = 0f; // 0f friction on body prevents wall sticking
        circleDef.density = 1.0f;
        circleDef.restitution = 0.0f;

        Fixture circleFixture = body.createFixture(circleDef);
        circleFixture.setUserData("player_slope_fixture");
        bottomCircle.dispose();

        // --- 3. Foot Sensor (Ground / Slope Detection) ---
        // Centered at y = -0.49m (overlaps feet boundary at -0.5m)
        PolygonShape footShape = new PolygonShape();
        footShape.setAsBox(0.15f, 0.02f, new Vector2(0, -0.49f), 0);

        FixtureDef footDef = new FixtureDef();
        footDef.shape = footShape;
        footDef.isSensor = true;

        Fixture footFixture = body.createFixture(footDef);
        footFixture.setUserData("player_foot");
        footShape.dispose();

        // --- 4. Enforce Exact 1.0kg Mass ---
        MassData massData = body.getMassData();
        massData.mass = 1.0f;
        body.setMassData(massData);

        return body;
    }

    // Written by Claude (Anthropic AI assistant)
    private Entity createMapEntity(MapEntityData d) {
        TextureRegion region = atlas.findRegion(d.type);

        if (d.properties.get("decoration", false, Boolean.class)) {
            return createDecorationBlock(d, region);
        }
        if (d.properties.get("slope", false, Boolean.class)) {
            return createSlopeBlock(d, region, false);
        }
        if (d.properties.get("inverted_slope", false, Boolean.class)) {
            return createSlopeBlock(d, region, true);
        }
        if(d.properties.get("sensor", false, Boolean.class)) {
            return createRectangleBlock(d, region, true);
        }

        return createRectangleBlock(d, region, false);
    }

    private Entity createRectangleBlock(MapEntityData data, TextureRegion region, boolean isSensor) {
        Entity block = createBox2DBody(data.x, data.y, data.width, data.height, false, data, isSensor);

        createTransformComponent(block, data.x, data.y, data.rotation);

        if (region != null) {
            createTextureComponent(block, region, 1, data.flipX, data.flipY, data.width, region.getRegionWidth());
        }

        engine.addEntity(block);
        return block;
    }

    private Entity createDecorationBlock(MapEntityData data, TextureRegion region) {
        Entity block = engine.createEntity();

        createTransformComponent(block, data.x, data.y, data.rotation);

        createTextureComponent(block, region, 0, data.flipX, data.flipY, data.width, region.getRegionWidth());

        engine.addEntity(block);
        return block;
    }

    private Entity createSlopeBlock(MapEntityData data, TextureRegion region, boolean inverted) {
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.StaticBody;
        bodyDef.position.set(data.x, data.y);
        Body body = world.createBody(bodyDef);

        // 2. Define Triangle Polygon Shape
        PolygonShape shape = new PolygonShape();
        float halfW = data.width / 2f;
        float halfH = data.height / 2f;

        Vector2[] vertices = new Vector2[3];
        if (inverted && !data.flipX) {
            vertices[0] = new Vector2(-halfW, halfH);   // Top Left
            vertices[1] = new Vector2(halfW, halfH);  // Top Right
            vertices[2] = new Vector2(halfW, -halfH); // Bottom Right
        } else if (inverted) {
            vertices[0] = new Vector2(-halfW, halfH);   // Top Left
            vertices[1] = new Vector2(halfW, halfH);  // Top Right
            vertices[2] = new Vector2(-halfW, -halfH); // Bottom Left
        } else if (data.flipX) {
            vertices[0] = new Vector2(-halfW, -halfH);   // Bottom Left
            vertices[1] = new Vector2(halfW, -halfH);  // Bottom Right
            vertices[2] = new Vector2(halfW, halfH); // Top Right
        } else {
            vertices[0] = new Vector2(-halfW, halfH);   // Top Left
            vertices[1] = new Vector2(-halfW, -halfH);  // Bottom Left
            vertices[2] = new Vector2(halfW, -halfH); // Bottom Right
        }
        shape.set(vertices);

        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = shape;
        fixtureDef.friction = 0.2f;
        fixtureDef.density = 1f;

        Entity entity = engine.createEntity();

        Fixture slopeFixture = body.createFixture(fixtureDef);
        slopeFixture.setUserData("slope");
        shape.dispose();

        Box2DComponent box2DComponent = engine.createComponent(Box2DComponent.class);
        box2DComponent.body = body;

        createTransformComponent(entity, data.x, data.y, data.rotation);

        if (region != null) {
            createTextureComponent(entity, region, 1, data.flipX, data.flipY, data.width, region.getRegionWidth());
        }
        engine.addEntity(entity);
        return entity;
    }

    private void createBoundaryWall(float x, float y, float width, float height) {
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.StaticBody;
        bodyDef.position.set(x, y);
        Body body = world.createBody(bodyDef);

        PolygonShape shape = new PolygonShape();
        shape.setAsBox(width / 2f, height / 2f);
        body.createFixture(shape, 0f);
        shape.dispose();
    }

    private Entity createBox2DBody(float x, float y, float width, float height, boolean isDynamic, MapEntityData data, boolean isSensor) {
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = isDynamic ? BodyDef.BodyType.DynamicBody : BodyDef.BodyType.StaticBody;
        bodyDef.position.set(x, y);
        Body body = world.createBody(bodyDef);

        PolygonShape shape = new PolygonShape();
        shape.setAsBox(width / 2f, height / 2f);

        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = shape;
        if(isSensor) {
            fixtureDef.isSensor = true;
        } else {
            fixtureDef.friction = 0.5f;
            fixtureDef.density = 1f;
        }

        Entity entity = engine.createEntity();

        Fixture fixture = body.createFixture(fixtureDef);
        String property = data.properties.get("property", "", String.class);
        switch (property) {
            case "oneWay": fixture.setUserData("one_way_platform"); break;
            case "spike": fixture.setUserData("spike"); break;
            default: fixture.setUserData("static_block"); break;
        }
        shape.dispose();

        Box2DComponent box2DComponent = engine.createComponent(Box2DComponent.class);
        box2DComponent.body = body;

        return entity;
    }

    private void createTransformComponent(Entity block, float x, float y, float rotation) {
        TransformComponent transformComponent = engine.createComponent(TransformComponent.class);
        transformComponent.x = x;
        transformComponent.y = y;
        transformComponent.rotation = rotation;
        block.add(transformComponent);
    }

    private void createTextureComponent(Entity block, TextureRegion region, int zIndex, boolean flipX, boolean flipY, float width, float regionWidth) {
        TextureComponent textureComponent = engine.createComponent(TextureComponent.class);
        textureComponent.textureRegion = region;
        textureComponent.zIndex = zIndex;
        textureComponent.flipX = flipX;
        textureComponent.flipY = flipY;
        textureComponent.scale = width / (regionWidth / Main.PPM);
        block.add(textureComponent);
    }

}
