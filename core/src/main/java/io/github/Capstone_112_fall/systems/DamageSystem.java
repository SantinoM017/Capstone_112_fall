package io.github.Capstone_112_fall.systems;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import io.github.Capstone_112_fall.components.ComponentMappers;
import io.github.Capstone_112_fall.components.DamageComponent;
import io.github.Capstone_112_fall.components.HealthComponent;
import io.github.Capstone_112_fall.utils.GameAssets;
import io.github.Capstone_112_fall.utils.SoundManager;

public class DamageSystem extends IteratingSystem {
    private final SoundManager soundManager;

    public DamageSystem(SoundManager soundManager) {
        super(Family.all(DamageComponent.class).get());
        this.soundManager = soundManager;
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        DamageComponent damage = ComponentMappers.DAMAGE.get(entity);
        Entity affectedEntity = damage.dealingDamageTo;

        if(damage.applyDamage) {
            HealthComponent affectedHp = ComponentMappers.HEALTH.get(affectedEntity);
            if (affectedHp != null && damage.takeDamage(damage.damage, affectedHp)
                && ComponentMappers.PLAYER.get(affectedEntity) != null) {
                soundManager.playSound(GameAssets.SFX_HURT);
            }
        }

    }
}
