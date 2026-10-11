package io.github.Capstone_112_fall.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.physics.box2d.World;
import io.github.Capstone_112_fall.Main;
import io.github.Capstone_112_fall.Screens.MainScreen;
import io.github.Capstone_112_fall.components.Box2DComponent;
import io.github.Capstone_112_fall.components.HealthComponent;
import io.github.Capstone_112_fall.components.PlayerComponent;
import io.github.Capstone_112_fall.components.QueueForDestructionComponent;

public class DeathSystem extends IteratingSystem {
    private final MainScreen game;
    private final ComponentMapper<PlayerComponent> playerComponent = ComponentMapper.getFor(PlayerComponent.class);
    private final ComponentMapper<Box2DComponent> box2DComponent = ComponentMapper.getFor(Box2DComponent.class);
    private final ComponentMapper<QueueForDestructionComponent> qdComp = ComponentMapper.getFor(QueueForDestructionComponent.class);
    private final ComponentMapper<HealthComponent> healthComponent = ComponentMapper.getFor(HealthComponent.class);
    private final World world;
    public DeathSystem(MainScreen game, World world){
        super(Family.all(PlayerComponent.class, Box2DComponent.class, QueueForDestructionComponent.class).get());
        this.game = game;
        this.world = world;
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        PlayerComponent player = playerComponent.get(entity);
        Box2DComponent box2d = box2DComponent.get(entity);
        QueueForDestructionComponent qd = qdComp.get(entity);
        HealthComponent health = healthComponent.get(entity);

        if(qd.destructionTimer > 0) {
            qd.destructionTimer-=deltaTime;
        }

        // 1. Safely dispose of the Box2D Body
        if (box2d != null && box2d.body != null && qd.destructionTimer <= 0) {
            // Destroy the body and all attached fixtures from the Box2D world
            world.destroyBody(box2d.body);
            box2d.body = null;
            game.setScreen(new Main(game));
        }

    }
}
