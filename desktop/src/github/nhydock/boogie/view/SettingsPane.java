package github.nhydock.boogie.view;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.fsm.StateMachine;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.JsonValue;


/**
 * Side pane that has a list of all settings and parse out
 * cli arguements from them
 * @author nhydock
 *
 */
public class SettingsPane extends Table {

	Skin skin;
	JsonValue cfg;
	Stage ui;
	StateMachine<MainScreen> uiMachine;
	
	public SettingsPane(Stage ui, StateMachine<MainScreen> uiMachine, Skin skin, JsonValue cfg)
	{
		super();
		this.ui = ui;
		this.skin = skin;
		this.cfg = cfg;
		this.uiMachine = uiMachine;
		
		setWidth(320);
		setHeight(ui.getHeight());
		setPosition(ui.getWidth(), 0);
		genElements(cfg);
		
		ui.addActor(this);
		
	}
	
	/**
	 * Parses settings from a json cfg file and inserts them into the ui
	 * @param json
	 */
	private void genElements(JsonValue json)
	{
		Table elements = new Table(skin);
		elements.pad(20);
		elements.padRight(30);
		elements.top();
		elements.add(new Label("Settings", skin, "title")).colspan(1).expandX().fillX().align(Align.left);
		final TextButton cleanButton = new TextButton("Clean Files", skin);
		cleanButton.pad(4, 15, 4, 15);
		cleanButton.addListener(new ChangeListener(){

			@Override
			public void changed(ChangeEvent event, Actor actor) {
				if (cleanButton.isChecked())
				{
					uiMachine.changeState(States.Clean);
					cleanButton.setChecked(false);
				}
			}
			
		});
		elements.add(cleanButton).colspan(1).expandX().align(Align.right);
		elements.row();
		for (JsonValue setting : json.get("settings"))
		{
			Actor element = null;
			String type = setting.getString("type");
			if (type.equals("select"))
			{
				SelectBox<String> select = new SelectBox<String>(skin);
				select.setItems(setting.get("options").asStringArray());
				select.setSelectedIndex(setting.get("default").asInt());
				
				element = select;
			}
			else if (type.equals("boolean") || type.equals("bool"))
			{
				CheckBox check = new CheckBox(" ", skin);
				check.setChecked(setting.get("default").asBoolean());
				element = check;
			}
			else if (type.equals("float"))
			{
				float[] options = setting.get("options").asFloatArray();
				float min = options[0];
				float max = options[1];
				float step = options[2];
				Slider slide = new Slider(min, max, step, false, skin);
				slide.setValue(setting.getFloat("default"));
				element = slide;
			}
			else if (type.equals("divider"))
			{
				element = new Label(setting.getString("title", " "), skin, "title");
				elements.add(element).expandX().align(Align.left).padBottom(10f).padTop(24f).row();
				continue;
			}
			else if (type.equals("color"))
			{
				Gdx.app.log("Settings", "Color has not yet been implemented");
				//TODO make color selectors
				continue;
			}
			//improper formatted type
			else
			{
				continue;
			}
			
			element.setName(setting.name());
			Label label = new Label(setting.getString("title"), skin);
			elements.add(label).colspan(1).expandX().align(Align.left);
			if (type.equals("boolean") || type.equals("bool"))
			{
				elements.add(element).colspan(1).expandX().align(Align.right);
			}
			else
			{
				elements.add(element).colspan(1).expandX().fillX().align(Align.right);
			}
			elements.row().padTop(6f);
		}
		
		ScrollPane pane = new ScrollPane(elements, skin);
		pane.setFillParent(true);
		pane.setFadeScrollBars(false);
		pane.setScrollingDisabled(true, false);
		add(pane).expand().fill();
	}
	
	@SuppressWarnings("unchecked")
	public String getValuesArg()
	{
		String argFormat = " -%s ";
		String args = "";
		for (JsonValue setting : cfg.get("settings"))
		{
			Actor a = ui.getRoot().findActor(setting.name());
			String type = setting.getString("type");
			if (type.equals("select"))
			{
				SelectBox<String> element = (SelectBox<String>)a;
				args += String.format(argFormat, setting.name());
				args += element.getSelected();
			}
			else if (type.equals("boolean") || type.equals("bool"))
			{
				CheckBox element = (CheckBox)a;
				if (element.isChecked())
				{
					args += String.format(argFormat, setting.name());
				}
			}
			else if (type.equals("float"))
			{
				Slider element = (Slider)a;
				args += String.format(argFormat, setting.name());
				args += element.getValue();
			}
			else
			{
				continue;
			}
		}
		
		return args;
	}
}
