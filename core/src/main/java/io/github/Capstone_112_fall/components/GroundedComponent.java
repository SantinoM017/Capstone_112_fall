package io.github.Capstone_112_fall.components;

import com.badlogic.ashley.core.Component;

// tag if entity is grounded and how many ground contacts to avoid falling through blocks
public class GroundedComponent implements Component {
    public boolean isGrounded = false;
    public int groundContacts = 0;
}
