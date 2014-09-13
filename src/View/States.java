package View;

import java.io.IOException;
import org.eclipse.jgit.lib.ProgressMonitor;

import app.Boogie;
import app.GitUtils;

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
		String format = "Cloning data from %s:%s";
		
		@Override
		public void enter(final MainScreen scene) {
			//make sure git is set up and cloned before allowing the game to be played/launched
			if (!GitUtils.gitExists())
			{
				//make sure all the folders for the downloading exists
				FileHandle gameDir = GitUtils.internalToAbsolute(Boogie.GAME_DIR);
				gameDir.mkdirs();
				
				ProgressMonitor monitor = new ProgressMonitor(){

					@Override
					public void beginTask(String title, int totalWork) { 
					}

					@Override
					public void endTask() { 
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
					}

					@Override
					public boolean isCancelled() { return false; }

					@Override
					public void start(int totalTasks) { }

					@Override
					public void update(int completed) { }
				};
				
				final Thread gitThread = GitUtils.cloneRepo(scene.repository, scene.branch, gameDir, monitor);
				scene.background.addAction(Actions.alpha(.5f, .2f, Interpolation.linear));
				scene.loadingAnimation.showLoading();
				scene.mainButtons.setVisible(false);
				scene.updateLabel.addAction(
					Actions.sequence(
						Actions.alpha(0f, .3f),
						Actions.run(new Runnable(){
							public void run()
							{
								//change the label's text
								scene.updateLabel.setText(String.format(format, scene.repository, scene.branch));
								scene.updateLabel.pack();
							}
						}),
						Actions.moveTo(-scene.updateLabel.getWidth(), scene.updateLabel.getY()),
						Actions.alpha(1f),
						Actions.moveTo(10f, scene.updateLabel.getY(), .3f, Interpolation.circleOut),
						Actions.run(new Runnable(){
							public void run()
							{
								gitThread.start();
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
		public boolean onMessage(MainScreen scene, Telegram t) {
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
		
		static final String format = "Pulling data from %s:%s";
		Thread gitThread;
		
		@Override
		public void enter(final MainScreen scene) { 
			try {
				scene.mainButtons.addAction(Actions.alpha(.4f, .3f));
				scene.ui.getRoot().setTouchable(Touchable.disabled);
			
				FileHandle gameDir = GitUtils.internalToAbsolute(Boogie.GAME_DIR);
				gitThread = GitUtils.pullRepo(gameDir, scene.branch, null);
				
				scene.updateLabel.addAction(
					Actions.sequence(
						Actions.alpha(0f, .3f),
						Actions.run(new Runnable(){
							public void run()
							{
								scene.loadingAnimation.showLoading();
								//change the label's text
								scene.updateLabel.setText(String.format(format, scene.repository, scene.branch));
								scene.updateLabel.pack();
							}
						}),
						Actions.moveTo(-scene.updateLabel.getWidth(), scene.updateLabel.getY()),
						Actions.alpha(1f),
						Actions.moveTo(10f, scene.updateLabel.getY(), .3f, Interpolation.circleOut),
						Actions.run(new Runnable(){
							public void run()
							{
								Gdx.app.log("Git", "Start pulling");
								gitThread.start();
							}
						})
					)	
				);
			} catch (IOException e) {
				e.printStackTrace();
				scene.updateLabel.addAction(
					Actions.sequence(
						Actions.alpha(0f, .3f),
						Actions.run(new Runnable(){
							public void run()
							{
								//change the label's text
								scene.updateLabel.setText("Git directory could not be found");
								scene.updateLabel.pack();
							}
						}),
						Actions.moveTo(-scene.updateLabel.getWidth(), scene.updateLabel.getY()),
						Actions.alpha(1f),
						Actions.moveTo(10f, scene.updateLabel.getY(), .3f, Interpolation.circleOut),
						Actions.delay(2f),
						Actions.alpha(0f, .3f)
					)	
				);
				scene.uiMachine.changeState(States.Home);
			}
		}

		@Override
		public void exit(final MainScreen scene) { 
			scene.mainButtons.addAction(Actions.alpha(1f, .3f));
			scene.ui.getRoot().setTouchable(Touchable.childrenOnly);
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
		}
		
		public void update(final MainScreen scene) {
			if (gitThread != null)
			{
				if (!gitThread.isAlive() && gitThread.getState() != Thread.State.NEW)
				{
					scene.uiMachine.changeState(Home); 
					gitThread = null;
				}
			}
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
						pb.directory(GitUtils.internalToAbsolute(Boogie.GAME_DIR).file());
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
		
		Thread deleteThread;
		
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
				deleteThread = GitUtils.clean();
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
								deleteThread.start();;
							}
						})
					)
				);
				scene.ui.getRoot().setTouchable(Touchable.disabled);
			}
			
			return false;
		}
		
		@Override
		public void update(MainScreen scene)
		{
			if (deleteThread != null)
			{
				if (!deleteThread.isAlive() && deleteThread.getState() != Thread.State.NEW)
				{
					scene.runPopup.addAction(
						Actions.sequence(
							Actions.alpha(1f),
							Actions.delay(.5f),
							Actions.alpha(0f, .4f),
							Actions.delay(1f),
							Actions.run(new Runnable(){
								@Override
								public void run() {
									Boogie.app.setScreen(new MainScreen());
								}
							})
						)
					);
					
				}	
			}
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