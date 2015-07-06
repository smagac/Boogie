package github.nhydock.boogie.view;

import github.nhydock.boogie.Boogie;
import github.nhydock.boogie.DownloadUtils;
import github.nhydock.boogie.view.MainScreen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.ai.fsm.DefaultStateMachine;
import com.badlogic.gdx.ai.fsm.StateMachine;
import com.badlogic.gdx.ai.msg.MessageManager;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

/**
 * 
 * @author nhydock
 *
 */
public class MainScreen implements Screen, Telegraph {

	/**
	 * main git repository for downloading the latest version of the managed app
	 */
	String repository;
	
	//key ui elements
	Actor mainButtons;
	SettingsPane settings;
	ScrollPane readmeView;
	Label runPopup;
	Stage ui;
	Image background;
	
	//statemachine managing the current conditions of the ui
	StateMachine<MainScreen> uiMachine;
	
	//label used for indicating download and update progress of the app
	Label updateLabel;
	protected ScrollPane cleanDialog;
	LoadingAnimation loadingAnimation;
	
	@Override
	public void show() {
		final MainScreen scene = this;

		//prepare the ui
		ui = new Stage();
		uiMachine = new DefaultStateMachine<MainScreen>(this);
		final Skin skin = new Skin(Gdx.files.internal("uiskin.json"));
		
		//load values from preferences json
		JsonReader j = new JsonReader();
		JsonValue cfg = j.parse(Gdx.files.internal("boogie.cfg"));
		repository = cfg.getString("url");
		
		//fill the background with a cfg defined background
		background = new Image(
					new Texture(
						Gdx.files.internal(cfg.getString("background")
					)
				));
		//when the image is clicked while the settings menu is open allow to click out
		background.addListener(new InputListener(){
			@Override
			public boolean touchDown(InputEvent evt, float x, float y, int pointer, int button)
			{
				uiMachine.changeState(States.Home);
				return true;
			}
		});
		
		background.setFillParent(true);
		ui.addActor(background);
		
		//make the main buttons
		Table buttonList = new Table();
		buttonList.pad(20);
		
		TextButton play = new TextButton("Play Game", skin, "title");
		play.addListener(new ChangeListener(){
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				MessageManager.getInstance().dispatchMessage(null, scene, StateMessage.PlayGame);
			}		
		});
		buttonList.add(play).expandX().align(Align.right).row();
		
		TextButton settings = new TextButton("Settings", skin, "title");
		settings.addListener(new ChangeListener(){
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				MessageManager.getInstance().dispatchMessage(0, scene, scene, StateMessage.OpenSettings);
			}		
		});
		buttonList.add(settings).expandX().align(Align.right).row();
		
		TextButton update = new TextButton("Update", skin, "title");
		update.addListener(new ChangeListener(){
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				MessageManager.getInstance().dispatchMessage(0, scene, scene, StateMessage.UpdateGame);
			}		
		});
		buttonList.add(update).expandX().align(Align.right).row();
		
		//parse and load readme file if it exists
		FileHandle readmeFile = DownloadUtils.internalToAbsolute(Boogie.GAME_DIR+"/README");
		if (readmeFile.exists())
		{
			TextButton readme = new TextButton("Readme", skin, "title");
			readme.addListener(new ChangeListener(){
				@Override
				public void changed(ChangeEvent event, Actor actor) {
					MessageManager.getInstance().dispatchMessage(0, scene, scene, StateMessage.OpenReadme);
				}		
			});
			buttonList.add(readme).expandX().align(Align.right).row();

			Table text = new Table();
			text.pad(20f);
			text.add(new Label("Readme", skin, "title")).expandX().fillX().align(Align.left).row();
			for (String line : readmeFile.readString().split("(\r\n|\n)"))
			{
				Label l;
				if (line.startsWith("##"))
				{
					l = new Label(line.substring(2), skin, "h3");
				}
				else if (line.startsWith("#"))
				{
					l = new Label(line.substring(1), skin, "title");	
				}
				else if (line.length() == 0)
				{
					l = new Label(" ", skin);
				}
				else
				{
					l = new Label(line, skin);
				}
				l.setWrap(true);
				text.add(l).expandX().fillX().row();
			}
			
			readmeView = new ScrollPane(text, skin, "prompt");
			readmeView.setSize(480, ui.getHeight() - 40);
			readmeView.setPosition(ui.getWidth()/2f - readmeView.getWidth() / 2f, -20 - readmeView.getHeight());
			readmeView.setFadeScrollBars(false);
			ui.addActor(readmeView);
		}
		
		TextButton quit = new TextButton("Quit", skin, "title");
		quit.addListener(new ChangeListener(){
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				MessageManager.getInstance().dispatchMessage(0, scene, scene, StateMessage.ExitGame);
			}		
		});
		buttonList.add(quit).expandX().align(Align.right).row();
		
		buttonList.pack();
		buttonList.setPosition(ui.getWidth()-buttonList.getWidth(), 0);
		ui.addActor(buttonList);
		
		//make the settings pane
		this.settings = new SettingsPane(ui, uiMachine, skin, cfg);
		ui.addActor(this.settings);
		
		//prepare update label
		updateLabel = new Label("", skin, "h3");
		updateLabel.setPosition(10, 10);
		updateLabel.pack();
		updateLabel.setColor(1, 1, 1, 0);
		ui.addActor(updateLabel);
		
		//prepare run popup label
		runPopup = new Label("The Game is Running", skin, "title");
		runPopup.pack();
		runPopup.setPosition(ui.getWidth() / 2f - runPopup.getWidth() / 2f, ui.getHeight() / 2f - runPopup.getHeight() / 2f);
		runPopup.setColor(1,1,1,0);
		runPopup.setTouchable(Touchable.disabled);
		ui.addActor(runPopup);
		
		//prepare clean files dialog
		Table cleanTable = new Table(skin);
		cleanTable.pad(20f);
		cleanTable.add("Are you sure you wish to clean up and redownload all game files?").expandX().align(Align.center).colspan(2).padBottom(15f).row();
		final TextButton confirmButton = new TextButton("Yes I'm sure", skin);
		final TextButton rejectButton = new TextButton("I've changed my mind", skin);
		confirmButton.addListener(new ChangeListener(){

			@Override
			public void changed(ChangeEvent event, Actor actor) {
				if (confirmButton.isChecked())
				{
					MessageManager.getInstance().dispatchMessage(0, scene, scene, StateMessage.Clean);
					confirmButton.setChecked(false);
				}
			}
			
		});
		
		rejectButton.addListener(new ChangeListener(){

			@Override
			public void changed(ChangeEvent event, Actor actor) {
				if (rejectButton.isChecked())
				{
					MessageManager.getInstance().dispatchMessage(0, scene, scene, StateMessage.Home);
					rejectButton.setChecked(false);
				}
			}
			
		});
		
		cleanTable.add(confirmButton).colspan(1).fillX().width(150f);
		cleanTable.add(rejectButton).colspan(1).fillX().width(150f);
		cleanTable.pack();
		cleanDialog = new ScrollPane(cleanTable, skin, "prompt");
		cleanDialog.pack();
		cleanDialog.setPosition(ui.getWidth()/2f - cleanDialog.getWidth()/2f, -cleanDialog.getHeight());
		ui.addActor(cleanDialog);
		
		mainButtons = buttonList;
		mainButtons.setColor(1,1,1,0);
		
		loadingAnimation = new LoadingAnimation(skin, ui);

		//create the ui state machine
		uiMachine.changeState(States.Home);
		
		Gdx.input.setInputProcessor(ui);
		
		MessageManager.getInstance().addListener(this, StateMessage.DownloadCompleted);
		MessageManager.getInstance().addListener(this, StateMessage.DownloadFailed);
		MessageManager.getInstance().addListener(this, StateMessage.DownloadCancelled);
	}

	@Override
	public boolean handleMessage(Telegram arg0) {
		return uiMachine.handleMessage(arg0);
	}

	@Override
	public void render(float delta) {
		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
		
		ui.act(delta);
		uiMachine.update();
		
		ui.draw();
	}

	@Override
	public void resize(int width, int height) {
		ui.getViewport().update(width, height);
	}

	@Override
	public void hide() {
		dispose();
	}

	@Override
	public void pause() {
		
	}

	@Override
	public void resume() {
		
	}

	@Override
	public void dispose() {
		ui.dispose();
	}

}
