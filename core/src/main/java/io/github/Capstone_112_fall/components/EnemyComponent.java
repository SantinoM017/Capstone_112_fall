package io.github.Capstone_112_fall.components;

import com.badlogic.ashley.core.Component;
import io.github.Capstone_112_fall.ai_behaviors.EnemyBehavior;

public class EnemyComponent implements Component {
    public enum AIState { PATROL, CHASE, STUNNED }

    public AIState aiState = AIState.PATROL;
    public boolean facingRight = true;
    public EnemyBehavior behavior;

    // Vision / Aggro parameters
    public float detectionRange;
}
