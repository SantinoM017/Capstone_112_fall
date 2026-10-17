package io.github.Capstone_112_fall.systems;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import io.github.Capstone_112_fall.components.ComponentMappers;
import io.github.Capstone_112_fall.components.DamageComponent;
import io.github.Capstone_112_fall.components.HealthComponent;

public class DamageSystem extends IteratingSystem {

    public DamageSystem() {
        super(Family.all(DamageComponent.class).get());
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        DamageComponent damage = ComponentMappers.DAMAGE.get(entity);
        Entity affectedEntity = damage.dealingDamageTo;

        if(damage.applyDamage) {
            HealthComponent affectedHp = ComponentMappers.HEALTH.get(affectedEntity);
            damage.takeDamage(damage.damage, affectedHp);
        }

    }
}
