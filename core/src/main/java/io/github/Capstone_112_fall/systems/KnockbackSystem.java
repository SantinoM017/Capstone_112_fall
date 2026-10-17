package io.github.Capstone_112_fall.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import io.github.Capstone_112_fall.components.Box2DComponent;
import io.github.Capstone_112_fall.components.ComponentMappers;
import io.github.Capstone_112_fall.components.HealthComponent;
import io.github.Capstone_112_fall.components.KnockbackComponent;

public class KnockbackSystem extends IteratingSystem {

    public KnockbackSystem() {
        super(Family.all(KnockbackComponent.class).get());
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        KnockbackComponent kb = ComponentMappers.KNOCKBACK.get(entity);

        if(kb.applyKnockback) {
            HealthComponent enemyHealth = ComponentMappers.HEALTH.get(kb.dealingKnockbackTo);
            applyKnockback(kb.dealingKnockbackTo, entity, enemyHealth);
        }

    }

    private void applyKnockback(Entity target, Entity hazard, HealthComponent health) {
        if (hazard == null) return;
        if(health.invulnerabilityTimer > 0f) return;

        KnockbackComponent knockback = ComponentMappers.KNOCKBACK.get(hazard);
        Box2DComponent hazardBody = ComponentMappers.BOX_2D.get(hazard);
        Box2DComponent affectedBody = ComponentMappers.BOX_2D.get(target);
        if (knockback == null || hazardBody == null || hazardBody.body == null
            || affectedBody == null || affectedBody.body == null) return;

        Body body = affectedBody.body;
        if (body.getType() == BodyDef.BodyType.KinematicBody) {
            affectedBody.temporarilyDynamicFromKinematic = true;
            affectedBody.previousDynamicX = body.getPosition().x;
            affectedBody.previousDynamicY = body.getPosition().y;
            affectedBody.hasPreviousDynamicY = true;
            body.setType(BodyDef.BodyType.DynamicBody);
        }

        float direction = (body.getPosition().x >= hazardBody.body.getPosition().x) ? 1.0f : -1.0f;
        float impulseX = knockback.xVel * direction * health.knockbackResistance;
        float impulseY = knockback.yVel * health.knockbackResistance;
        body.setLinearVelocity(0, 0);
        body.applyLinearImpulse(new Vector2(impulseX, impulseY), body.getWorldCenter(), true);
        if(health.hitStunTimer > 0f) return;
        health.hitStunTimer = health.hitStunDuration;
    }
}
