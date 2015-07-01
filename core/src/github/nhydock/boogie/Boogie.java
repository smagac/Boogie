package github.nhydock.boogie;

import github.nhydock.boogie.view.MainScreen;

import com.badlogic.gdx.Game;

public class Boogie extends Game {
    public static Game app;
    public static final String GAME_DIR = "bin/game/";
    
	@Override
	public void create () {
	    MainScreen screen = new MainScreen();
        this.setScreen(screen);
        
        app = this;
	}

}
