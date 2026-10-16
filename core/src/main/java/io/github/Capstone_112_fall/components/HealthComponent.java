package io.github.Capstone_112_fall.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;

// tags an entity that has health
public class HealthComponent implements Component {
    public int hp;
    public int maxHp;
    public float invulnerabilityTimer;
    public final float invulnerabilityDuration = 0.4f;
    public float hitStunTimer;
    public final float hitStunDuration = 0.2f;
    public float knockbackResistance;
    public boolean isTakingDamage;
    public Entity takingDamageFrom;
    public boolean isTakingKnockback;
    public Entity takingKnockbackFrom;

    public void takeDamage(int damage) {
        if (invulnerabilityTimer <= 0) {
            hp -= damage;
            invulnerabilityTimer = invulnerabilityDuration;
        }
    }

    public void applyKnockback(Entity target, Entity hazard, HealthComponent health,
                               ComponentMapper<KnockbackComponent> kc,
                               ComponentMapper<Box2DComponent> bc) {
        if (hazard == null) return;

        KnockbackComponent knockback = kc.get(hazard);
        Box2DComponent hazardBody = bc.get(hazard);
        Box2DComponent affectedBody = bc.get(target);
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
        health.hitStunTimer = health.hitStunDuration;
    }

}
