package io.github.Capstone_112_fall.utils;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import io.github.Capstone_112_fall.ai_behaviors.SlimeBehavior;
import io.github.Capstone_112_fall.ai_behaviors.SlimeBossBehavior;
import io.github.Capstone_112_fall.components.*;
import io.github.Capstone_112_fall.Main;


// creates entities and adds components to them
public class EntityManager {
    private static final float PLAYER_MASS = 1.0f;

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

    public void createPlayerEntity(float x, float y) {
        Entity player = engine.createEntity();
        Body body = createPlayer(x, y);

        // create and add body component
        createBox2DComponent(player, body);

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


        createHealthComponent(player, 100, 100, 1);

        body.setUserData(player);
        engine.addEntity(player);
    }

    // Written by Claude (Anthropic AI assistant)
    // Updated by Gemini for double parsing
    // creates entities from the values read by MapBuilder
    public void createMapEntities(Array<MapEntityData> data) {
        // Stores created visual Ashley Entities keyed by entityID
        ObjectMap<Integer, Entity> pendingEntities = new ObjectMap<>();
        ObjectMap<Integer, PlayerBarrierComponent> pendingBarriers = new ObjectMap<>();
        ObjectMap<Integer, MapEntityData> hurtboxDataMap = new ObjectMap<>();
        ObjectMap<Integer, MapEntityData> attackboxDataMap = new ObjectMap<>();

        // PASS 1: Instantiate visual/logical entities (No Box2D bodies yet)
        for (MapEntityData d : data) {
            String property = d.properties.get("property", "", String.class);

            switch (property != null ? property : "") {
                case "spawn":
                    createPlayerEntity(d.x, d.y);
                    createSpawnCheckpoint(d.x, d.y);
                    break;

                case "entity_hurtbox":
                    // Defer hitboxes until all parent entities are created
                    hurtboxDataMap.put(d.entityID, d);
                    break;

                case "entity_attackbox":
                    attackboxDataMap.put(d.entityID, d);
                    break;

                case "playerBarrier":
                    Entity barrier = createMapEntity(d);
                    pendingBarriers.put(d.entityID, ComponentMappers.PLAYER_BARRIER.get(barrier));
                    break;

                case "entity":
                    if (d.entityID != 0) {
                        // Create visual Ashley entity and register in map
                        Entity entity = createMapEntity(d);
                        pendingEntities.put(d.entityID, entity);
                        break;
                    }
                    // If entityID == 0, fall through to default behavior

                default:
                    // Standard static blocks, slopes, decorations, etc.
                    createMapEntity(d);
                    break;
            }
        }

        // PASS 2: Parse Hitboxes & Attach Box2D Bodies directly to pending Entities
        for (ObjectMap.Entry<Integer, Entity> entry : pendingEntities) {
            Integer entityID = entry.key;
            Entity parentEntity = entry.value;
            MapEntityData hurtboxData = hurtboxDataMap.get(entityID);
            MapEntityData attackboxData = attackboxDataMap.get(entityID);

            createEnemy(parentEntity, hurtboxData, attackboxData);
        }

        for (ObjectMap.Entry<Integer, PlayerBarrierComponent> entry : pendingBarriers) {
            entry.value.key = pendingEntities.get(entry.value.entityID);
        }
    }

    public void createMapSideBoundaries(float mapWidth, float mapHeight) {
        float wallThickness = 1f;
        float wallHeight = mapHeight * 2f;
        float wallCenterY = mapHeight / 2f;

        createBoundaryWall(-wallThickness / 2f, wallCenterY, wallThickness, wallHeight);
        createBoundaryWall(mapWidth + wallThickness / 2f, wallCenterY, wallThickness, wallHeight);
    }

    public void createPlayerExtension(Entity player, PlayerExtensionComponent.Type type) {
        Box2DComponent box2D = player.getComponent(Box2DComponent.class);
        PlayerComponent playerComp = player.getComponent(PlayerComponent.class);

        if (box2D == null || box2D.body == null || playerComp == null || type == null
            || player.getComponent(PlayerExtensionComponent.class) != null) return;

        float direction = !playerComp.isFlipped ? 1.0f : -1.0f;

        PolygonShape shape = new PolygonShape();
        if(type == PlayerExtensionComponent.Type.ATTACK) {
            shape.setAsBox(0.2f, 0.5f, new Vector2(0.5f * direction, 0.1f), 0f);
        } else {
            shape.setAsBox(0.1f, 0.5f, new Vector2(0.4f * direction, 0.1f), 0f);
        }

        FixtureDef fDef = new FixtureDef();
        fDef.shape = shape;
        fDef.isSensor = true;

        Fixture sensorFixture = box2D.body.createFixture(fDef);
        sensorFixture.setUserData(type == PlayerExtensionComponent.Type.ATTACK
            ? "player_attack_hitbox"
            : "player_parry_hitbox");
        shape.dispose();
        restorePlayerMass(box2D.body);

        PlayerExtensionComponent extension = engine.createComponent(PlayerExtensionComponent.class);
        extension.sensorFixture = sensorFixture;
        extension.type = type;

        if (type == PlayerExtensionComponent.Type.ATTACK) {
            createDamageComponent(player, 20);
            createKnockbackComponent(player, 3, 1);
        } else {
            createKnockbackComponent(player,1, 1);
            createStunComponent(player, 2f);

        }

        player.add(extension);
        box2D.body.setAwake(true);
    }

    public void destroyPlayerExtension(Entity entity) {
        if (entity == null) return;

        Box2DComponent box2D = entity.getComponent(Box2DComponent.class);
        PlayerExtensionComponent extension = entity.getComponent(PlayerExtensionComponent.class);

        if (extension != null) {
            if (box2D != null && box2D.body != null && extension.sensorFixture != null) {
                box2D.body.destroyFixture(extension.sensorFixture);
                extension.sensorFixture = null;
                restorePlayerMass(box2D.body);
            }

            entity.remove(PlayerExtensionComponent.class);
            if (extension.type == PlayerExtensionComponent.Type.ATTACK) {
                entity.remove(DamageComponent.class);
                entity.remove(KnockbackComponent.class);
            } else {
                entity.remove(KnockbackComponent.class);
                entity.remove(StunComponent.class);
            }
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
        PolygonShape footShape = new PolygonShape();
        footShape.setAsBox(0.15f, 0.02f, new Vector2(0, -0.45f), 0);

        FixtureDef footDef = new FixtureDef();
        footDef.shape = footShape;
        footDef.isSensor = true;

        Fixture footFixture = body.createFixture(footDef);
        footFixture.setUserData("player_foot");
        footShape.dispose();

        // --- 4. Enforce Exact 1.0kg Mass ---
        MassData massData = body.getMassData();
        massData.mass = PLAYER_MASS;
        body.setMassData(massData);

        return body;
    }

    private void restorePlayerMass(Body body) {
        MassData massData = body.getMassData();
        massData.mass = PLAYER_MASS;
        body.setMassData(massData);
    }

    // rewritten by Gemini
    private void createEnemy(Entity parentEntity, MapEntityData hurtboxData, MapEntityData attackboxData) {
        // 1. Build Body centered on Hurtbox position
        BodyDef bodyDef = new BodyDef();
        boolean isStatic = hurtboxData.properties.get("static", false, Boolean.class);
        bodyDef.type = isStatic ? BodyDef.BodyType.StaticBody : BodyDef.BodyType.KinematicBody;
        bodyDef.position.set(hurtboxData.x, hurtboxData.y);
        bodyDef.fixedRotation = true;

        Body body = world.createBody(bodyDef);

        // 2. Create Hurtbox Fixture
        PolygonShape hurtboxShape = new PolygonShape();
        hurtboxShape.setAsBox(hurtboxData.width / 2f, hurtboxData.height / 2f);

        FixtureDef hurtboxDef = new FixtureDef();
        hurtboxDef.shape = hurtboxShape;
        hurtboxDef.isSensor = true;

        Fixture hurtbox = body.createFixture(hurtboxDef);
        hurtboxShape.dispose();

        String hurtboxTag = hurtboxData.properties.get("fixtureTag", "enemy_hurtbox", String.class);
        hurtbox.setUserData(hurtboxTag);

        // 3. Create Attackbox Fixture (if provided in Tiled map)
        Fixture attackFixture = null;
        if (attackboxData != null) {
            PolygonShape attackShape = new PolygonShape();

            // Calculate offset relative to parent Body position (hurtbox center)
            float offsetX = attackboxData.x - hurtboxData.x;
            float offsetY = attackboxData.y - hurtboxData.y;

            attackShape.setAsBox(
                attackboxData.width / 2f,
                attackboxData.height / 2f,
                new Vector2(offsetX, offsetY),
                0f);

            FixtureDef attackDef = new FixtureDef();
            attackDef.shape = attackShape;
            attackDef.isSensor = true; // Always sensor for attack checks

            attackFixture = body.createFixture(attackDef);
            attackShape.dispose();

            String attackTag = attackboxData.properties.get("fixtureTag", "enemy_attackbox", String.class);
            attackFixture.setUserData(attackTag);
        }

        // 4. Component Assignments & Two-Way Linking
        body.setUserData(parentEntity);

        createBox2DComponent(parentEntity, body);

        HitboxComponent hitboxComp = engine.createComponent(HitboxComponent.class);
        hitboxComp.body = body;
        hitboxComp.hurtboxFixture = hurtbox;
        hitboxComp.attackboxFixture = attackFixture;
        parentEntity.add(hitboxComp);

        int health = hurtboxData.properties.get("hp", 1, Integer.class);
        float kbRes = hurtboxData.properties.get("kbRes", 1f, Float.class);
        createHealthComponent(parentEntity, health, health, kbRes);

        // Attach DamageComponent if the attackbox has damage properties defined
        if (attackboxData != null) {
            int damage = attackboxData.properties.get("damage", 10, Integer.class);
            createDamageComponent(parentEntity, damage);
            float xVel = attackboxData.properties.get("xKnock", 0f, Float.class);
            float yVel = attackboxData.properties.get("yKnock", 0f, Float.class);
            createKnockbackComponent(parentEntity, xVel, yVel);
        }

        StateComponent stateComp = engine.createComponent(StateComponent.class);
        parentEntity.add(stateComp);

        EnemyComponent enemyComp = engine.createComponent(EnemyComponent.class);
        String entityType = hurtboxData.properties.get("entityType", "", String.class);
        float detectionRange = hurtboxData.properties.get("detectionRange", 0f, Float.class);
        switch(entityType) {
            case "slime": enemyComp.behavior = new SlimeBehavior(); break;
            case "slime_custom":
                float walkSpeed = hurtboxData.properties.get("walkSpeed", 0.8f, float.class);
                float chaseSpeed = hurtboxData.properties.get("chaseSpeed", 1.0f, float.class);
                float offset = hurtboxData.properties.get("offset", 0.1f, float.class);
                enemyComp.behavior = new SlimeBehavior(walkSpeed, chaseSpeed, offset);
            case "slime_boss":
                enemyComp.behavior = new SlimeBossBehavior();
            default: break;
        }
        enemyComp.detectionRange = detectionRange;
        parentEntity.add(enemyComp);

        parentEntity.add(animationFactory.createEnemyAnimations(engine));
    }

    // Written by Claude (Anthropic AI assistant)
    private Entity createMapEntity(MapEntityData d) {
        TextureRegion region = atlas.findRegion(d.type);

        if (d.properties.get("decoration", false, Boolean.class)) {
            return createDecorationBlock(d, region, 0);
        }
        if (d.properties.get("noHitbox", false, Boolean.class)) {
            return createDecorationBlock(d, region, 9);
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
            case "entity": fixture.setUserData("entity"); break;
            case "entityBarrier": fixture.setUserData("entity_barrier"); break;
            case "objective": fixture.setUserData("objective"); break;
            case "playerBarrier": fixture.setUserData("player_barrier");
                PlayerBarrierComponent pb = engine.createComponent(PlayerBarrierComponent.class);
                pb.entityID = data.entityID;
                block.add(pb);
                break;
            case "checkpoint": fixture.setUserData("checkpoint");
                  CheckpointComponent cp = engine.createComponent(CheckpointComponent.class);
                  cp.x = data.x;
                  cp.y = data.y;
                  block.add(cp);
                  break;
            default: fixture.setUserData("static_block"); break;
        }
        shape.dispose();

        createBox2DComponent(block, body);

        createTransformComponent(block, data.x, data.y, data.rotation);

        if (region != null) {
            createTextureComponent(block, region, 1, data.flipX, data.flipY, data.width, region.getRegionWidth());
        }

        body.setUserData(block);
        engine.addEntity(block);
        return block;
    }

    private Entity createDecorationBlock(MapEntityData data, TextureRegion region, int z) {
        Entity block = engine.createEntity();

        createTransformComponent(block, data.x, data.y, data.rotation);

        createTextureComponent(block, region, z, data.flipX, data.flipY, data.width, region.getRegionWidth());

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

    private void createBox2DComponent(Entity entity, Body body) {
        Box2DComponent box2D = engine.createComponent(Box2DComponent.class);
        box2D.body = body;
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

    private void createKnockbackComponent(Entity entity, float x, float y) {
        KnockbackComponent knockbackComponent = engine.createComponent(KnockbackComponent.class);
        knockbackComponent.xVel = x;
        knockbackComponent.yVel = y;
        entity.add(knockbackComponent);
    }

    private void createHealthComponent(Entity entity, int max, int hp, float kbRes){
        HealthComponent health = engine.createComponent(HealthComponent.class);
        health.maxHp = max;
        health.hp = hp;
        health.knockbackResistance = kbRes;
        entity.add(health);
    }

    private void createStunComponent(Entity entity, float stunTime) {
        StunComponent stun = engine.createComponent(StunComponent.class);
        stun.stunTime = stunTime;
        entity.add(stun);
    }

    private void createSpawnCheckpoint(float x, float y) {
        Entity checkpoint = engine.createEntity();
        CheckpointComponent component = engine.createComponent(CheckpointComponent.class);
        component.x = x;
        component.y = y;
        component.order = 0;
        checkpoint.add(component);
        engine.addEntity(checkpoint);
    }

}
