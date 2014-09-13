package View;

import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

public class LoadingAnimation {

	Image spin;
	
	public LoadingAnimation(Skin skin, Stage stage)
	{
		spin = new Image(skin.getDrawable("loading"));
		spin.setPosition(stage.getWidth()/2f-spin.getWidth()/2f, stage.getHeight()/2f-spin.getHeight()/2f);
		spin.setOrigin(spin.getWidth()/2f, spin.getHeight()/2f);
		spin.setColor(1, 1, 1, 0f);
		stage.addActor(spin);
	}
	
	public void showLoading()
	{
		spin.clearActions();
		spin.addAction(
			Actions.sequence(
				Actions.rotateTo(0f),
				Actions.parallel(
					Actions.alpha(1f, .5f), 
					Actions.repeat(-1, 
						Actions.rotateBy(360f, .9f, Interpolation.linear)
					)
				)
			)
		);
	}
	
	public void hideLoading()
	{
		spin.clearActions();
		spin.addAction(Actions.alpha(0f,.5f));
	}
}
