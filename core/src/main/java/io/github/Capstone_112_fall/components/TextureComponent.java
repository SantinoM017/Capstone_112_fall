package io.github.Capstone_112_fall.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

// tags an entity with a texture region to be drawn
public class TextureComponent implements Component {
    public TextureRegion textureRegion;
    public TextureRegion sourceRegion;
    public TextureRegion renderRegion;
    public int zIndex = 0;
    public boolean flipX = false;
    public boolean flipY = false;
    public boolean bottomAligned = false;
    // Written by Claude (Anthropic AI assistant)
    public float scale = 0.5f;
}
