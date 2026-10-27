package io.github.Capstone_112_fall.Screens;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import io.github.Capstone_112_fall.Main;
import io.github.Capstone_112_fall.utils.GameAssets;
import io.github.Capstone_112_fall.utils.MusicManager;

public class MainScreen extends Game {
    private GameAssets assets;

    @Override
    public void create() {
        assets = new GameAssets();
        setScreen(new GameLoadingScreen(this));
    }

    public GameAssets getAssets() {
        return assets;
    }

    public Skin getSkin() {
        return assets.getSkin();
    }

    public void skipMenuMusic() {
        MusicManager musicManager = assets.getMusicManager();
        String nextTrack = GameAssets.MUSIC_MAIN_MENU.equals(musicManager.getCurrentPath())
            ? GameAssets.MUSIC_MAIN_MENU_ALTERNATE
            : GameAssets.MUSIC_MAIN_MENU;
        musicManager.playMusic(nextTrack);
    }

    public String getMenuMusicTitle() {
        String currentPath = assets.getMusicManager().getCurrentPath();
        if (GameAssets.MUSIC_MAIN_MENU.equals(currentPath)) {
            return "Main Menu 1";
        }
        if (GameAssets.MUSIC_MAIN_MENU_ALTERNATE.equals(currentPath)) {
            return "Main Menu 2";
        }
        return "No music";
    }

    @Override
    public void setScreen(Screen screen) {
        Screen previousScreen = getScreen();

        if (assets != null) {
            MusicManager music = assets.getMusicManager();

            if (screen instanceof Main) {
                music.playMusic(GameAssets.MUSIC_LEVEL_1);
            }
            else if (screen instanceof MainMenuScreen || screen instanceof LevelSelectScreen) {
                // Only start/change menu music if coming from outside the menu system (e.g., Main game screen or startup)
                boolean comingFromMenu = previousScreen instanceof MainMenuScreen
                    || previousScreen instanceof LevelSelectScreen;

                if (!comingFromMenu) {
                    playRandomMenuMusic(music);
                }
            }
            else {
                music.stopMusic();
            }
        }

        super.setScreen(screen);
    }

    private void playRandomMenuMusic(MusicManager music) {
        String track = MathUtils.randomBoolean()
            ? GameAssets.MUSIC_MAIN_MENU
            : GameAssets.MUSIC_MAIN_MENU_ALTERNATE;
        music.playMusic(track);
    }

    @Override
    public void render() {
        super.render();
    }

    @Override
    public void dispose() {
        if(screen != null) screen.dispose();
        if(assets != null) {
            assets.getMusicManager().stopMusic();
            assets.dispose();
        }
    }
}
