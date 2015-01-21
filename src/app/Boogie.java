package app;
import View.MainScreen;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;

public class Boogie {

	public static Game app;
	public static final String GAME_DIR = "bin/game/";
	
	/**
	 * Launches Boogie
	 * @param args
	 *   Boogie has no args, but requires a boogie.cfg in its operating directory
	 */
	public static void main(String... args)
	{
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
		config.backgroundFPS = 60;
		config.foregroundFPS = 60;
		config.width = 800;
		config.height = 480;
		//config.resizable = false;
		config.title = "Boogie";
		config.resizable = false;
		
		System.setProperty("org.lwjgl.opengl.Window.undecorated", "true");
		app = new App();
		new LwjglApplication(app, config);
	}

	/**
	 * Wraps a game with the main scene, for simplicity's sake
	 */
	private static class App extends Game {
		@Override
		public void create() {
			MainScreen screen = new MainScreen();
			this.setScreen(screen);
		}
	}
}
