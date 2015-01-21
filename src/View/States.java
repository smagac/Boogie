package View;

import java.io.IOException;

import app.Boogie;
import app.DownloadUtils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.fsm.State;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;

/**
 * Defined states for the UI's interactions
 * @author nhydock
 *
 */
enum States implements State<MainScreen>
{
	Home(){
		//format used for cloning
		String format = "Cloning data from %s";
		
		@Override
		public void enter(final MainScreen scene) {
			//make sure the game is set up and cloned before allowing the game to be played/launched
			if (!DownloadUtils.gameExists())
			{
				//make sure all the folders for the downloading exists
				final FileHandle gameDir = DownloadUtils.internalToAbsolute(Boogie.GAME_DIR);
				gameDir.mkdirs();
				
				scene.background.addAction(Actions.alpha(.5f, .2f, Interpolation.linear));
				scene.background.setTouchable(Touchable.disabled);
				scene.loadingAnimation.showLoading();
				scene.mainButtons.setVisible(false);
				scene.updateLabel.addAction(
					Actions.sequence(
						Actions.alpha(0f, .3f),
						Actions.run(new Runnable(){
							public void run()
							{
								//change the label's text
								scene.updateLabel.setText(String.format(format, scene.repository));
								scene.updateLabel.pack();
							}
						}),
						Actions.moveTo(-scene.updateLabel.getWidth(), scene.updateLabel.getY()),
						Actions.alpha(1f),
						Actions.moveTo(10f, scene.updateLabel.getY(), .3f, Interpolation.circleOut),
						Actions.run(new Runnable(){
							public void run()
							{
								DownloadUtils.cloneRepo(scene.repository, gameDir);
							}
						})
					)	
				);
			}
			else
			{
				scene.background.addAction(Actions.alpha(1f, .2f, Interpolation.linear));
				scene.mainButtons.addAction(Actions.alpha(1f, .4f));
			}
		}
		
		@Override
		public void exit(MainScreen scene)
		{
			scene.background.addAction(Actions.alpha(.5f, .2f, Interpolation.linear));
		}
		
		@Override
		public boolean onMessage(final MainScreen scene, Telegram t) {
			if (t.message == StateMessage.PlayGame)
			{
				scene.uiMachine.changeState(PlayGame);
			}
			else if (t.message == StateMessage.OpenSettings)
			{
				scene.uiMachine.changeState(Settings);
			}
			else if (t.message == StateMessage.UpdateGame)
			{
				scene.uiMachine.changeState(Update);
			}
			else if (t.message == StateMessage.OpenReadme)
			{
				scene.uiMachine.changeState(ReadMe);
			}
			else if (t.message == StateMessage.ExitGame)
			{
				Gdx.app.exit();
			}
			else if (t.message == StateMessage.DownloadCompleted) {
				scene.updateLabel.addAction(
					Actions.sequence(
						Actions.alpha(0f, .3f),
						Actions.run(new Runnable(){
							public void run()
							{
								scene.loadingAnimation.hideLoading();
								//change the label's text
								scene.updateLabel.setText("Cloning Complete, enjoy the game");
								scene.updateLabel.pack();
								DownloadUtils.writePerm();
							}
						}),
						Actions.moveTo(-scene.updateLabel.getWidth(), scene.updateLabel.getY()),
						Actions.alpha(1f),
						Actions.moveTo(10f, scene.updateLabel.getY(), .3f, Interpolation.circleOut),
						Actions.delay(3f),
						Actions.alpha(0f, .3f),
						Actions.delay(1f),
						Actions.run(new Runnable(){
							public void run(){
								Boogie.app.setScreen(new MainScreen());
							}
						})
					)
				);
				return true;
			}
			else if (t.message == StateMessage.DownloadFailed) {
				scene.updateLabel.addAction(
					Actions.sequence(
						Actions.alpha(0f, .3f),
						Actions.run(new Runnable(){
							public void run()
							{
								scene.loadingAnimation.hideLoading();
								//change the label's text
								scene.updateLabel.setText("Could not establish a connection/failed to connect to remote repository.");
								scene.updateLabel.pack();
							}
						}),
						Actions.moveTo(-scene.updateLabel.getWidth(), scene.updateLabel.getY()),
						Actions.alpha(1f),
						Actions.moveTo(10f, scene.updateLabel.getY(), .3f, Interpolation.circleOut),
						Actions.delay(3f),
						Actions.alpha(0f, .3f),
						Actions.delay(1f),
						Actions.run(new Runnable(){
							public void run(){
								Gdx.app.exit();
							}
						})
					)
				);
				return true;
			}
			return false;
		}
	},
	Settings(){
		@Override
		public void enter(final MainScreen scene) {
			scene.settings.clearActions();
			scene.settings.addAction(
				Actions.sequence(
					Actions.moveTo(scene.ui.getWidth(), 0),
					Actions.moveTo(scene.ui.getWidth() - scene.settings.getWidth(), 0, .5f, Interpolation.circleOut)
				)
			);
			scene.mainButtons.setTouchable(Touchable.disabled);
		}

		@Override
		public void exit(final MainScreen scene) {
			scene.settings.clearActions();
			scene.settings.addAction(
				Actions.sequence(
					Actions.moveTo(scene.ui.getWidth() - scene.settings.getWidth(), 0),
					Actions.moveTo(scene.ui.getWidth(), 0, .2f, Interpolation.circleIn)
				)
			);
			scene.mainButtons.setTouchable(Touchable.childrenOnly);
		}
	},
	ReadMe(){
		public void enter(MainScreen scene) { 
			scene.readmeView.addAction(
				Actions.sequence(
					Actions.moveTo(scene.readmeView.getX(), -20-scene.readmeView.getHeight()),
					Actions.moveTo(scene.readmeView.getX(), 20, .5f, Interpolation.circleOut)
				)
			);
			scene.mainButtons.addAction(Actions.alpha(0f, .3f));
			scene.mainButtons.setTouchable(Touchable.disabled);
		}

		@Override
		public void exit(MainScreen scene) { 
			scene.readmeView.addAction(
				Actions.sequence(
					Actions.moveTo(scene.readmeView.getX(), 20),
					Actions.moveTo(scene.readmeView.getX(), -20-scene.readmeView.getHeight(), .5f, Interpolation.circleIn)
				)
			);
			scene.mainButtons.addAction(Actions.alpha(1f, .3f));
			scene.mainButtons.setTouchable(Touchable.enabled);
		}
		
	},
	Update(){
		
		static final String format = "Pulling data from %s";
		
		@Override
		public void enter(final MainScreen scene) { 
			scene.mainButtons.addAction(Actions.alpha(.4f, .3f));
			scene.ui.getRoot().setTouchable(Touchable.disabled);
		
			final FileHandle gameDir = DownloadUtils.internalToAbsolute(Boogie.GAME_DIR);
			
			scene.updateLabel.addAction(
				Actions.sequence(
					Actions.alpha(0f, .3f),
					Actions.run(new Runnable(){
						public void run()
						{
							scene.loadingAnimation.showLoading();
							//change the label's text
							scene.updateLabel.setText(String.format(format, scene.repository));
							scene.updateLabel.pack();
						}
					}),
					Actions.moveTo(-scene.updateLabel.getWidth(), scene.updateLabel.getY()),
					Actions.alpha(1f),
					Actions.moveTo(10f, scene.updateLabel.getY(), .3f, Interpolation.circleOut),
					Actions.run(new Runnable(){
						public void run()
						{
							DownloadUtils.pullRepo(scene.repository, gameDir, false);
						}
					})
				)	
			);
		}

		@Override
		public void exit(final MainScreen scene) { 
			scene.mainButtons.addAction(Actions.alpha(1f, .3f));
			scene.ui.getRoot().setTouchable(Touchable.childrenOnly);
			
		}
		
		public boolean onMessage(final MainScreen scene, Telegram t) {
			if (t.message == StateMessage.DownloadCompleted) {
				DownloadUtils.writePerm();
				scene.uiMachine.changeState(Home);
				scene.updateLabel.addAction(
					Actions.sequence(
						Actions.alpha(0f, .3f),
						Actions.run(new Runnable(){
							public void run()
							{
								scene.loadingAnimation.hideLoading();
								//change the label's text
								scene.updateLabel.setText("Game Files are up to date!");
								scene.updateLabel.pack();
							}
						}),
						Actions.moveTo(-scene.updateLabel.getWidth(), scene.updateLabel.getY()),
						Actions.alpha(1f),
						Actions.moveTo(10f, scene.updateLabel.getY(), .3f, Interpolation.circleOut),
						Actions.delay(3f),
						Actions.alpha(0f, .3f)
					)
				);
				return true;
			} else if (t.message == StateMessage.DownloadFailed) {
				scene.uiMachine.changeState(Home);
				scene.updateLabel.addAction(
					Actions.sequence(
						Actions.alpha(0f, .3f),
						Actions.run(new Runnable(){
							public void run()
							{
								scene.loadingAnimation.hideLoading();
								//change the label's text
								scene.updateLabel.setText("Could not properly connect to server to update files.");
								scene.updateLabel.pack();
							}
						}),
						Actions.moveTo(-scene.updateLabel.getWidth(), scene.updateLabel.getY()),
						Actions.alpha(1f),
						Actions.moveTo(10f, scene.updateLabel.getY(), .3f, Interpolation.circleOut),
						Actions.delay(3f),
						Actions.alpha(0f, .3f)
					)
				);
			}
			return false;
		}
	},
	//fork a game process with all settings appended to the execute statement as cmd line args
	PlayGame()
	{
		Thread watchThread;
		
		@Override
		public void enter(final MainScreen scene) { 
			final Runnable watch = new Runnable(){
				public void run(){
					Process game;
					try {
						String cmd = scene.cmd + scene.settings.getValuesArg();
						
						ProcessBuilder pb = new ProcessBuilder(cmd.split(" "));
						pb.directory(DownloadUtils.internalToAbsolute(Boogie.GAME_DIR).file());
						game = pb.start();
						game.waitFor();
						
					} catch (IOException e) {
						e.printStackTrace();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					game = null;
				}
			};
			
			scene.mainButtons.addAction(Actions.alpha(0f, .2f));
			scene.runPopup.addAction(
				Actions.sequence(
					Actions.alpha(0f),
					Actions.alpha(1f, .4f),
					Actions.delay(1f),
					Actions.run(new Runnable(){
						public void run(){
							watchThread = new Thread(watch);
							watchThread.start();
						}
					})
				)
			);
			scene.ui.getRoot().setTouchable(Touchable.disabled);
		}
		@Override
		public void exit(MainScreen scene) { 
			scene.runPopup.addAction(
				Actions.sequence(
					Actions.alpha(1f),
					Actions.alpha(0f, .4f)
				)
			);
			scene.ui.getRoot().setTouchable(Touchable.enabled);
		}
		
		@Override
		public void update(MainScreen scene) {
			if (watchThread != null)
			{
				if (!watchThread.isAlive() && watchThread.getState() != Thread.State.NEW)
				{
					scene.uiMachine.changeState(Home);
					watchThread = null;
				}	
			}
		}
	},
	Clean(){
		
		public void enter(MainScreen scene) { 
			scene.cleanDialog.addAction(
				Actions.sequence(
					Actions.moveTo(scene.cleanDialog.getX(), -scene.cleanDialog.getHeight()),
					Actions.moveTo(scene.cleanDialog.getX(), scene.ui.getHeight() / 2f - scene.cleanDialog.getHeight()/2f, .5f, Interpolation.circleOut)
				)
			);
			scene.background.setTouchable(Touchable.disabled);
			scene.mainButtons.addAction(Actions.alpha(0f, .3f));
			scene.mainButtons.setTouchable(Touchable.disabled);
		}

		@Override
		public void exit(MainScreen scene) { 
			scene.cleanDialog.addAction(
				Actions.sequence(
					Actions.moveTo(scene.cleanDialog.getX(), scene.ui.getHeight() / 2f  - scene.cleanDialog.getHeight()/2f),
					Actions.moveTo(scene.cleanDialog.getX(), -scene.cleanDialog.getHeight(), .5f, Interpolation.circleIn)
				)
			);
			scene.background.setTouchable(Touchable.enabled);
			scene.mainButtons.addAction(Actions.alpha(1f, .3f));
			scene.mainButtons.setTouchable(Touchable.enabled);
		}
		
		@Override
		public boolean onMessage(MainScreen scene, Telegram telegram) {
			if (telegram.message == StateMessage.Home)
			{
				scene.uiMachine.changeState(Home);
			}
			else if (telegram.message == StateMessage.Clean)
			{
				
				scene.cleanDialog.addAction(
					Actions.sequence(
						Actions.moveTo(scene.cleanDialog.getX(), scene.ui.getHeight() / 2f  - scene.cleanDialog.getHeight()/2f),
						Actions.moveTo(scene.cleanDialog.getX(), -scene.cleanDialog.getHeight(), .5f, Interpolation.circleIn)
					)
				);
				scene.runPopup.setText("Cleaning game directory");
				scene.runPopup.pack();
				scene.runPopup.addAction(
					Actions.sequence(
						Actions.alpha(0f),
						Actions.delay(.5f),
						Actions.alpha(1f, .4f),
						Actions.delay(1f),
						Actions.run(new Runnable(){
							public void run(){
								DownloadUtils.clean();				
								Boogie.app.setScreen(new MainScreen());
							}
						})
					)
				);
				scene.ui.getRoot().setTouchable(Touchable.disabled);
			}
			
			return false;
		}
	};

	@Override
	public void enter(MainScreen arg0) { }

	@Override
	public void exit(MainScreen arg0) { }
	
	@Override
	public boolean onMessage(MainScreen arg0, Telegram arg1) {
		return false;
	}

	@Override
	public void update(MainScreen arg0) { }
}