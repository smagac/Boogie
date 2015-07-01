package github.nhydock.boogie.desktop;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;

import github.nhydock.boogie.Boogie;

public class DesktopLauncher {
	public static void main (String[] arg) {
	    LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
        config.backgroundFPS = 60;
        config.foregroundFPS = 60;
        config.width = 800;
        config.height = 480;
        //config.resizable = false;
        config.title = "Boogie";
        config.resizable = false;
        
        System.setProperty("org.lwjgl.opengl.Window.undecorated", "true");
        Boogie app = new Boogie();
        new LwjglApplication(app, config);
	}
}
