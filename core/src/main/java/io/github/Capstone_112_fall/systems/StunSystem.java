package io.github.Capstone_112_fall.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import io.github.Capstone_112_fall.components.ComponentMappers;
import io.github.Capstone_112_fall.components.HealthComponent;
import io.github.Capstone_112_fall.components.StunComponent;

public class StunSystem extends IteratingSystem {

    public StunSystem() {
        super(Family.all(StunComponent.class).get());
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        StunComponent stun = ComponentMappers.STUN.get(entity);

        if(stun.applyStun) {
            Entity stunnedEntity = stun.stunnedEntity;
            HealthComponent health = ComponentMappers.HEALTH.get(stunnedEntity);
            health.hitStunTimer = stun.stunTime;
            stun.applyStun = false;
            stun.stunnedEntity = null;
        }
    }
}
