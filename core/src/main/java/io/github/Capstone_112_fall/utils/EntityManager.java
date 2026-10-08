package io.github.Capstone_112_fall.utils;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
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
        Box2DComponent box2dComp = engine.createComponent(Box2DComponent.class);
        box2dComp.body = body;
        player.add(box2dComp);

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


        createHealthComponent(player, 100, 100);

        body.setUserData(player);
        engine.addEntity(player);
        return player;
    }

    // Written by Claude (Anthropic AI assistant)
    // Updated by Gemini for double parsing
    // creates entities from the values read by MapBuilder
    public void createMapEntities(Array<MapEntityData> data) {
        // Stores created visual Ashley Entities keyed by entityID
        ObjectMap<String, Entity> pendingEntities = new ObjectMap<>();
        Array<MapEntityData> deferredHitboxes = new Array<>();

        // PASS 1: Instantiate visual/logical entities (No Box2D bodies yet)
        for (MapEntityData d : data) {
            String property = d.properties.get("property", "", String.class);

            if ("entity_hitbox".equals(property)) {
                // Defer hitboxes until all parent entities are created
                deferredHitboxes.add(d);
            } else if (!d.entityID.isEmpty() && "entity".equals(property)) {
                // Create visual Ashley entity and register in map
                Entity entity = createMapEntity(d);
                pendingEntities.put(d.entityID, entity);
            } else {
                // Standard static blocks, slopes, decorations, etc.
                createMapEntity(d);
            }
        }

        // PASS 2: Parse Hitboxes & Attach Box2D Bodies directly to pending Entities
        for (MapEntityData d : deferredHitboxes) {
            if (d.entityID.isEmpty()) continue;

            Entity parentEntity = pendingEntities.get(d.entityID);
            if (parentEntity == null) continue;

            // Build the Box2D Body (KinematicBody or DynamicBody for moving entities)
            BodyDef bodyDef = new BodyDef();
            boolean isDynamic = d.properties.get("dynamic", false, Boolean.class);
            bodyDef.type = isDynamic ? BodyDef.BodyType.DynamicBody : BodyDef.BodyType.KinematicBody;
            bodyDef.position.set(d.x, d.y);
            bodyDef.fixedRotation = true;

            Body body = world.createBody(bodyDef);

            PolygonShape shape = new PolygonShape();
            shape.setAsBox(d.width / 2f, d.height / 2f);

            FixtureDef fixtureDef = new FixtureDef();
            fixtureDef.shape = shape;
            fixtureDef.isSensor = d.properties.get("sensor", true, Boolean.class);

            Fixture hurtbox = body.createFixture(fixtureDef);
            shape.dispose();

            // Label fixture for ContactListener collision checks
            String fixtureTag = d.properties.get("fixtureTag", "enemy_hurtbox", String.class);
            hurtbox.setUserData(fixtureTag);

            // --- CRITICAL TWO-WAY LINKING ---
            // 1. Point Box2D Body back to Ashley Entity
            body.setUserData(parentEntity);

            // 2. Attach Box2DComponent to the existing Ashley Entity
            Box2DComponent box2dComp = engine.createComponent(Box2DComponent.class);
            box2dComp.body = body;
            parentEntity.add(box2dComp);

            // 3. Attach HitboxComponent to track fixtures on Ashley Entity
            HitboxComponent hitboxComp = engine.createComponent(HitboxComponent.class);
            hitboxComp.body = body;
            hitboxComp.hurtboxFixture = hurtbox;
            parentEntity.add(hitboxComp);
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

        createDamageComponent(player,20);

        player.add(attackComp);
        box2D.body.setAwake(true);
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

        float radius = 0.25f;

        // --- 1. Upper Body Box (Flat Sides prevent Wall Sticking) ---
        PolygonShape upperBox = new PolygonShape();
        upperBox.setAsBox(radius/1.5f, 0.2f, new Vector2(0, 0.1f), 0);

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
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.StaticBody;
        bodyDef.position.set(data.x, data.y);
        Body body = world.createBody(bodyDef);

        PolygonShape shape = new PolygonShape();
        shape.setAsBox(data.width / 2f, data.height / 2f);

        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = shape;
        if(isSensor) {
            fixtureDef.isSensor = true;
        } else {
            fixtureDef.friction = 0.5f;
            fixtureDef.density = 1f;
        }
        Fixture fixture = body.createFixture(fixtureDef);

        Entity block = engine.createEntity();

        String property = data.properties.get("property", "", String.class);
        switch (property) {
            case "oneWay": fixture.setUserData("one_way_platform"); break;
            case "spike": fixture.setUserData("spike");
                int damage = data.properties.get("damage", 0, Integer.class);
                createDamageComponent(block, damage); break;
            case "entity": fixture.setUserData("entity");
                int health = data.properties.get("health", 0, Integer.class);
                createHealthComponent(block, health, health);
            default: fixture.setUserData("static_block"); break;
        }
        shape.dispose();

        createBox2DComponent(block);

        createTransformComponent(block, data.x, data.y, data.rotation);

        if (region != null) {
            createTextureComponent(block, region, 1, data.flipX, data.flipY, data.width, region.getRegionWidth());
        }

        body.setUserData(block);
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

    private void createBox2DComponent(Entity entity) {
        Box2DComponent box2D = engine.createComponent(Box2DComponent.class);
        entity.add(box2D);
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

    private void createDamageComponent(Entity entity, int damage) {
        DamageComponent damageComponent = engine.createComponent(DamageComponent.class);
        damageComponent.damage = damage;
        entity.add(damageComponent);
    }

    private void createHealthComponent(Entity entity, int max, int hp){
        HealthComponent health = engine.createComponent(HealthComponent.class);
        health.maxHp = max;
        health.hp = hp;
        entity.add(health);
    }

}
