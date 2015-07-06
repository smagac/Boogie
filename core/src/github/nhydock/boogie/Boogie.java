package github.nhydock.boogie;

import github.nhydock.boogie.view.MainScreen;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Screen;

public class Boogie extends Game {
    public static Game app;
    public static final String GAME_DIR = "bin/game/";
    
    Screen screen;
    Screen queue;
    
	@Override
	public void create () {
	    MainScreen screen = new MainScreen();
        this.setScreen(screen);
        
        app = this;
	}
	
	@Override
	public void render() {
	    super.render();
	    
	    if (this.queue != null) {
	        super.setScreen(screen);
	        this.screen = this.queue;
	        this.queue = null;
	    }
	}
	
	@Override
	public void setScreen(Screen screen) {
	    if (this.screen == null) {
	        super.setScreen(screen);
	        this.screen = screen;
	    } else {
	        this.queue = screen;
	    }
	}

}
