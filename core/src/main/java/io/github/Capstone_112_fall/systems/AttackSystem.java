package io.github.Capstone_112_fall.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import io.github.Capstone_112_fall.components.*;
import io.github.Capstone_112_fall.utils.EntityManager;

public class AttackSystem extends IteratingSystem {

    private final ComponentMapper<PlayerComponent> playerMapper = ComponentMapper.getFor(PlayerComponent.class);
    private final ComponentMapper<AttackHitboxComponent> attackMapper = ComponentMapper.getFor(AttackHitboxComponent.class);

    private final EntityManager entityFactory;

    public AttackSystem(EntityManager entityFactory) {
        super(Family.all(PlayerComponent.class, StateComponent.class).get());
        this.entityFactory = entityFactory;
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        PlayerComponent player = playerMapper.get(entity);
        AttackHitboxComponent attack = attackMapper.get(entity);

        // 1. Attack started, but no sensor fixture created yet
        if (player.isAttacking && attack == null) {
//            System.out.println("Creating attack hitbox for entity: " + entity);
            entityFactory.createPlayerAttackHitbox(entity);
            return;
        }

        // 2. Attack fixture is active -> manage timer
        if (attack != null) {
            attack.timer += deltaTime;

            if (attack.timer >= attack.duration) {
                // Remove sensor fixture from Box2D body
                entityFactory.destroyAttackHitbox(entity);

                // Reset attack flags and state
                player.attackRequested = false;
                player.isAttacking = false;
            }
        }
    }
}
