package com.cpioli.headabovewater.screens;

import static com.badlogic.gdx.scenes.scene2d.actions.Actions.delay;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.sequence;
import java.util.ArrayList;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.cpioli.headabovewater.Assets;
import com.cpioli.headabovewater.HeadAboveWater02;
import com.cpioli.headabovewater.ui.Swimmer;
import com.cpioli.headabovewater.ui.Border;
import com.cpioli.headabovewater.ui.OxygenMeter;
import com.cpioli.headabovewater.ui.ProgressBar;
import com.cpioli.headabovewater.ui.StaminaMeter;
import com.cpioli.headabovewater.ui.Timer;
import com.cpioli.headabovewater.utils.GameOverObserver;

/*
 * 
 * 1 meter = 32 pixels!!!!
 * 
 */

class RiverbedTile {
	float x, y, width, height;
	
	RiverbedTile(float x, float y, float width, float height) {
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
	}
}

public class GameScreen implements Screen, InputProcessor, GameOverObserver {

	HeadAboveWater02 game;
	Stage stage;
	OrthographicCamera camera;
	Texture playerTexture;
	Texture sky;
	SpriteBatch batch;
	ShapeRenderer renderer;
	Swimmer swimmer;
	boolean goingRight, goingLeft;
	Viewport viewport;
	Border testBorder;
	public boolean gameOver, paused;
	Color translucentBlue;
	
	Overlay gameOverOverlay;
	Overlay pauseOverlay;
	Overlay winOverlay;
	
	OxygenMeter oxygenMeter;
	StaminaMeter staminaMeter;
	ProgressBar progressBar;
	Timer timer;
	Box2DDebugRenderer b2dDebugRenderer = new Box2DDebugRenderer();
	
	ArrayList<RiverbedTile> riverbed;
	float lastRiverbedTileLoc; //the location where the last Riverbed Physics tile ended
	Color riverbedColor = new Color(0.38039f, 0.23921f, 0.10980f, 1.0f);
	private int gameState;
	//97, 61, 28
	
	public static final int GAME_INTRO = 0;
	public static final int GAME_PLAY = 1;
	public static final int GAME_FINISHED_SUCCESS = 2;
	public static final int GAME_PAUSED = 3;
	public static final int GAME_DYING = 4;
	
	boolean canPause;
	
	
	private World world;
	private Box2DDebugRenderer debugRenderer;
	private BodyDef swimmerBodyDef;
	private Body swimmerBody;
	private Fixture swimmerFixture;
	
	public GameScreen(HeadAboveWater02 game) {
		this.game = game;
	}
	
	@Override
	public void show() {
		gameOver = false;

		viewport = new ScreenViewport();
		stage = new Stage(viewport);
		stage.getViewport().setScreenSize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		playerTexture = Assets.playerTexture;
		sky = Assets.sky;
		camera = new OrthographicCamera(30, 20); //in meters
		camera.position.set(0, 0, 0);

		batch = new SpriteBatch(50);
		renderer = new ShapeRenderer(60);
		translucentBlue = new Color(85.0f/255.0f, 138.0f/255.0f, 230.0f/255.0f, 0.5f);
		
		oxygenMeter = new OxygenMeter(renderer, 25.0f, 580.0f);
		staminaMeter = new StaminaMeter(renderer, 25.0f, 605.0f);
		progressBar = new ProgressBar(0.0f, 0.0f, 0.0f, renderer);
		timer = new Timer(800.0f, 590.0f);
		createOverlays();
		riverbed = new ArrayList<RiverbedTile>();
		lastRiverbedTileLoc = -15.0f;		
		
		debugRenderer = new Box2DDebugRenderer();

		createPhysicsWorld(0.0f, 0.0f);
		swimmer.registerObserver(this);
		final GameScreen screen = this;
		Gdx.input.setInputProcessor(this);
		stage.addActor(oxygenMeter);
		stage.addActor(staminaMeter);
		stage.addActor(progressBar);
		stage.addActor(timer);
		stage.addListener(new InputListener() {
			public boolean keyUp(InputEvent event, int keycode) {
				if (keycode == Keys.P) {
					if (screen.gameState == GameScreen.GAME_PAUSED) {
						screen.resume();
					}
				}
				return false;
			}
		});
		stage.setDebugAll(true);

		canPause = false;
		gameState = GAME_INTRO;
	}
	
	
	
	@Override
	public void render(float delta) {
		// TODO Auto-generated method stub
		
		if(gameState == GAME_INTRO) {
			//do an animation thing here
			//send a text flying that says start or something
			gameState = GAME_PLAY;
			canPause = true;
		}
		if(gameState == GAME_PLAY) {
			world.step(1/60f, 8, 3);
			swimmer.update(delta, goingLeft, goingRight); //this is more of a bounding box updater now
			swimmer.setPosition(swimmerBody.getPosition().x, swimmerBody.getPosition().y);
			//System.out.println("SwimmerBody locPos: " + swimmerBody.localPoint2);
			camera.update();
		}
		Gdx.gl.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		
		
		
		batch.setProjectionMatrix(camera.combined);
		batch.begin();
		batch.draw(sky, -15.0f, -96.840f, 148.5625f, 110.906f);
		batch.draw(sky, 133.5625f, -96.840f, 148.5625f, 110.906f);
		batch.draw(swimmer.playerTexture, swimmerBody.getPosition().x - .75f, swimmerBody.getPosition().y - .75f, 1.5f, 1.5f);
		//batch.draw(playerTexture, 15.625f, 0.0f);
		//batch.draw(swimmer.playerTexture, 500.0f, 0);
		batch.end();

		renderer.setProjectionMatrix(camera.combined);
		renderer.begin(ShapeType.Filled);
		renderer.setColor(riverbedColor);
		for(int i = 0; i < riverbed.size(); i++) {
			renderer.rect(riverbed.get(i).x, riverbed.get(i).y, riverbed.get(i).width, riverbed.get(i).height);
		}
		renderer.end();
		
		//this is the water object. It is 10m tall
		Gdx.gl.glEnable(GL20.GL_BLEND);
		renderer.setProjectionMatrix(camera.combined);
		renderer.begin(ShapeType.Filled);
			renderer.setColor(translucentBlue);
			renderer.rect(-2.0f, -30.0f, 300.0f, 30.0f);
		renderer.end();
		Gdx.gl.glDisable(GL20.GL_BLEND);
		
		renderer.setProjectionMatrix(viewport.getCamera().combined); //seems like this also affects stage.draw() since it uses ShapeRenderers...
		renderer.begin(ShapeType.Line);
			renderer.setColor(Color.BLACK);
			//renderer.rect(240.0f, 160.0f, 480.0f, 320.0f); //these are not WORLD coordinates, remember! They're pixel coordinates
			//renderer.rect(304.0f, 160.0f, 372.0f, 320.0f);
			//renderer.rect(384.0f, 160.0f, 192.0f, 320.0f);
		renderer.end();
		//debugRenderer.render(world, camera.combined);
		
		stage.act(Math.min(Gdx.graphics.getDeltaTime(), 1 / 30f));
		stage.draw();
		//System.out.println("Camera position: " + camera.position);
		//System.out.println("JavaHeap size: " + (Gdx.app.getJavaHeap() / 1000000L) + " MB");
		//System.out.println("Native Heap size: " + (Gdx.app.getNativeHeap() / 1000000L) + " MB");
	}

	@Override
	public void resize(int width, int height) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void hide() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void pause() {
		// TODO Auto-generated method stub
		swimmer.pause();
		paused = true;
		gameState = GAME_PAUSED;
		pauseOverlay.setVisible(true);
		Gdx.input.setInputProcessor(stage);
		timer.pause();
	}

	@Override
	public void resume() {
		// TODO Auto-generated method stub
		swimmer.resume(goingLeft, goingRight);
		paused = false;
		gameState = GAME_PLAY;
		pauseOverlay.setVisible(false);
		Gdx.input.setInputProcessor(this);
		timer.resume();
	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub
		stage.dispose();
		swimmer.dispose();
	}

	//UPDATES FROM THE SWIMMER INDICATING NEW CHANGES TO THE GAME STATE OCCUR HERE
	@Override
	public void update(int incomingGameState) {
		// TODO Auto-generated method stub
		if(incomingGameState == GameScreen.GAME_DYING) {
			gameOver = true;
			swimmer.pause();
			timer.pause();
			System.out.println("GAME OVER!");
			
			Assets.aboveSurfaceAmbience.setVolume(0.2f);
			Assets.belowSurfaceAmbience.setVolume(0.2f);
			Assets.drown.play();
			stage.addAction(sequence(delay(1.5f), new Action() {
				public boolean act(float delta) {
					Stage stage = this.getActor().getStage();
					Array<Actor> array = stage.getActors();
					Overlay overlay = null;
					for(int i = 0; i < array.size; i++) {
						if(array.get(i).getName().equals("GameOverOverlay")) {
							overlay = (Overlay)array.get(i);
							break;
						}
					}
					overlay.setVisible(true);
					return true;
				}
			}));						
						
			Gdx.input.setInputProcessor(stage);
		} else if (incomingGameState == GameScreen.GAME_FINISHED_SUCCESS) {
			//CELEBRATE VICTORY!
			swimmer.pause();
			timer.pause();
			((Label)winOverlay.findActor("Completion Time")).setText("Final Time: " + timer.getTime().getText());
			winOverlay.setVisible(true);
			gameState = GameScreen.GAME_FINISHED_SUCCESS;
			Gdx.input.setInputProcessor(stage);
		}
		//gameOverOverlay.setVisible(true);
	}

	public void reset() {
		System.out.println("RESTART! RESTART!");
		swimmer.restart();
		oxygenMeter.restart();
		staminaMeter.restart();
		timer.restart();
		Gdx.input.setInputProcessor(this);
		gameOverOverlay.setVisible(false);
		pauseOverlay.setVisible(false);
		winOverlay.setVisible(false);
		gameOver = false;
		goingRight = false;
		goingLeft = false;
		//camera.lookAt(0.0f, 0.0f, 0.0f);
		paused = false;
		this.gameState = GameScreen.GAME_PLAY;
		//Assets.aboveSurfaceAmbience.stop();
		//Assets.belowSurfaceAmbience.stop();

	}
	

	

	/*
	 * THIS IS A GAME OVER UPDATER
	 * @see com.cpioli.headabovewater.utils.GameOverObserver#update()
	 */

	public void createOverlays() {
		pauseOverlay = new Overlay(250.0f, 200.0f, 460.0f, 250.0f, "Paused", Color.BLACK, renderer);
		final GameScreen screen = this;
		/*ImageButton pauseMainMenuButton = Assets.bigPauseMainMenuButton;
		pauseMainMenuButton.setPosition(100.0f, 10.0f);
		pauseMainMenuButton.addListener(new ClickListener() {
			public void clicked(InputEvent event, float x, float y) {
				System.out.println("MainMenu button clicked at location " + x + ", " + y);
				screen.pauseOverlay.sendAway();
				screen.game.setScreen(screen.game.mainMenuScreen);
				screen.paused = false;
			}
		});*/
		
		ImageButton resumeButtonPAUSE = Assets.bigResumeButton;
		resumeButtonPAUSE.setPosition(100.0f, 80.0f);
		resumeButtonPAUSE.addListener(new ClickListener() {
			public void clicked(InputEvent event, float x, float y) {
				System.out.println("Resume Button clicked at location " + x + ", " + y);
				screen.resume();
			}
		});
		resumeButtonPAUSE.setPosition(350.0f, 210.0f);
		
		ImageButton restartButtonPAUSE = Assets.bigRestartButtonPAUSE;
		restartButtonPAUSE.setPosition(350.0f, 280.0f);
		restartButtonPAUSE.addListener(new ClickListener() {
			public void clicked(InputEvent event, float x, float y) {
				screen.reset();
			}
		});
		
		//pauseOverlay.addActor(pauseMainMenuButton);
		pauseOverlay.addActor(resumeButtonPAUSE);
		pauseOverlay.addActor(restartButtonPAUSE);
		
		gameOverOverlay = new Overlay(250.0f, 200.0f, 460.0f, 250.0f, "Game Over", Color.BLACK, renderer);
		/*ImageButton gameOverMainMenuButton = Assets.bigGameOverMainMenuButton;
		gameOverMainMenuButton.addListener(new ClickListener() {
			public void clicked(InputEvent event, float x, float y) {
				screen.gameOverOverlay.sendAway();
				screen.game.setScreen(screen.game.mainMenuScreen);
				screen.reset();
			}
		});
		gameOverOverlay.addActor(gameOverMainMenuButton);*/

		ImageButton restartButtonDIED = Assets.bigRestartButtonDIED;
		restartButtonDIED.addListener(new ClickListener() {
			public void clicked(InputEvent event, float x, float y) {
				//screen.gameOverOverlay.sendAway();
				/*Assets.aboveSurfaceAmbience.setVolume(0.2f);
				Assets.belowSurfaceAmbience.setVolume(0.2f);
				Assets.swimmerVO_04.play();
				delay(1.311f, new Action() {

					@Override
					public boolean act(float arg0) {
						Assets.aboveSurfaceAmbience.setVolume(1.0f);
						Assets.belowSurfaceAmbience.setVolume(1.0f);
						return false;
					}
					
				});*/
				screen.reset();
			}
		});
		restartButtonDIED.setPosition(350.0f, 250.0f);
		//gameOverMainMenuButton.setPosition(100.0f, 80.0f);
		//gameOverOverlay.addActor(gameOverMainMenuButton);
		gameOverOverlay.addActor(restartButtonDIED);
		gameOverOverlay.setName("GameOverOverlay");
		
		
		winOverlay = new Overlay(250.0f, 200.0f, 460.0f, 250.0f, "You Win!", Color.BLACK, renderer);
		
		ImageButton restartButtonWIN = Assets.bigRestartButtonWIN;
		restartButtonWIN.setPosition(350.0f, 250.0f);
		restartButtonWIN.addListener(new ClickListener() {
			public void clicked(InputEvent event, float x, float y) {
				screen.reset();
			}
		});
		Label completionTime = Assets.bigFinalTime;
		completionTime.setText("Final Time: " + timer.getTime().getText());
		completionTime.setPosition(290.0f,300.0f);
		completionTime.setName("Completion Time");
		winOverlay.addActor(completionTime);
		winOverlay.addActor(restartButtonWIN);
		
		stage.addActor(gameOverOverlay);
		stage.addActor(pauseOverlay);
		stage.addActor(winOverlay);
		gameOverOverlay.setVisible(false);
		pauseOverlay.setVisible(false);
		winOverlay.setVisible(false);
	}
	
	/**************
	 * ************
	 * INPUT PROCESSOR METHODS
	 **************
	 **************
	 */
	
	@Override
	public boolean keyDown(int keycode) {
			
		if(keycode == Keys.D) {
			goingRight = true;
			//swimmerBody.setLinearVelocity(2.0f, swimmerBody.getLinearVelocity().y);
		}
		if(keycode == Keys.A) {
			goingLeft = true;
			//swimmerBody.setLinearVelocity(-2.0f, swimmerBody.getLinearVelocity().y);
		}
		
		if(keycode == Keys.CONTROL_LEFT || keycode == Keys.CONTROL_RIGHT) {
			if(gameState == GAME_PLAY)
				swimmer.performStroke();
		}
		return false;
	}

	@Override
	public boolean keyUp(int keycode) {
		// TODO Auto-generated method stub
		

		if(keycode == Keys.D) {
			goingRight = false;
			swimmerBody.setLinearVelocity(0.0f, swimmerBody.getLinearVelocity().y);
		}

		if(keycode == Keys.A) {
			goingLeft = false;
			swimmerBody.setLinearVelocity(0.0f, swimmerBody.getLinearVelocity().y);
		}

		//cheat code for infinite stamina. V is for victory
		if(keycode == Keys.V){
			//toggle infinite Stamina
			if(swimmer.getMaxStrokesInFullBar() == 18.0f) {
				swimmer.setMaxStrokesInFullBar(5000.0f);
			} else if (swimmer.getMaxStrokesInFullBar() == 5000.0f) {
				swimmer.setMaxStrokesInFullBar(18.0f);
			}
		}

		//cheat code for infinite oxygen. B is for breath
		if(keycode == Keys.B){
			if(swimmer.getO2LossDuration() == 20.0f) {
				swimmer.setO2LossDuration(5000.0f);
			} else if (swimmer.getO2LossDuration() == 5000.0f) {
				swimmer.setO2LossDuration(20.0f);
			}
		}
		
		if(keycode == Keys.ESCAPE && canPause) {
			if(gameState != GAME_PAUSED) {
				this.pause();
			} else {
				System.out.println("I can RESUME!!!");
				this.resume();
			}
		}
		return false;
	}

	@Override
	public boolean keyTyped(char character) {
		// TODO Auto-generated method stub

		return false;
	}

	@Override
	public boolean touchDown(int screenX, int screenY, int pointer, int button) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean touchUp(int screenX, int screenY, int pointer, int button) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean touchDragged(int screenX, int screenY, int pointer) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean mouseMoved(int screenX, int screenY) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean scrolled(int amount) {
		// TODO Auto-generated method stub
		return false;
	}

	public void createPhysicsWorld(float startX, float startY) {
		world = new World(new Vector2(0, -2.6f), true);
		
		createRiverbedTile(13.0f, -1.0f);
		
		createRiverbedTile(10.0f, 7.5f); //1
		createRiverbedTile(10.0f, 10.0f);
		createRiverbedTile(12.5f, 7.5f);
		createRiverbedTile(12.5f, 10.0f);
		createRiverbedTile(5.0f, 5.0f); //50
		
		createRiverbedTile(7.5f, 12.5f); //6
		createRiverbedTile(7.5f, 10.0f);
		createRiverbedTile(12.5f, 7.5f);
		createRiverbedTile(5.0f, 10.0f);
		createRiverbedTile(7.5f, 12.5f); //90
		
		createRiverbedTile(10.0f, 10.0f); //11
		createRiverbedTile(7.5f, 7.5f);
		createRiverbedTile(15.0f, 12.5f);
		createRiverbedTile(5.0f, 5.0f);
		createRiverbedTile(15.0f, 12.5f);//142.5
		
		createRiverbedTile(5.0f, 7.5f);  //16
		createRiverbedTile(10.0f, 10.0f);
		createRiverbedTile(7.5f, 12.5f);
		createRiverbedTile(7.5f, 10.0f);
		createRiverbedTile(5.0f, 5.0f); //177.5
		
		createRiverbedTile(5.0f, 12.5f); //21
		createRiverbedTile(5.0f, 15.0f);
		createRiverbedTile(5.0f, 7.5f);
		createRiverbedTile(5.0f, 15.0f);
		createRiverbedTile(5.0f, 12.5f); //202.5
		
		createRiverbedTile(2.5f, 10.0f); //26
		createRiverbedTile(10.0f, 7.5f);
		createRiverbedTile(5.0f, 5.0f);
		createRiverbedTile(7.5f, 15.0f);
		createRiverbedTile(2.5f, 10.0f); //230.0
		
		createRiverbedTile(7.5f, 7.5f); //31
		createRiverbedTile(7.5f, 15.0f);
		createRiverbedTile(7.0f, 12.5f);
		
		createRiverbedTile(13.0f, 0.0f);
		
		/*createRiverbedTile(5.0f, 12.5f); //36
		createRiverbedTile(7.5f, 7.5f);
		createRiverbedTile(5.0f, 15.0f);*/
		
		
		
		//Sky vertices are for a physics box to keep the Swimmer on top of the water surface
		float[] skyVertices = { -16.0f, 1.0f,
								248.0f, 1.0f,
								248.0f, 1.75f,
								-16.0f, 1.75f
								};
		
		PolygonShape skyShape = new PolygonShape();
		skyShape.set(skyVertices);
		BodyDef skyBodyDef = new BodyDef();
		skyBodyDef.type = BodyType.StaticBody;
		final Body skyBody = world.createBody(skyBodyDef);
		
		FixtureDef skyFixtureDef = new FixtureDef();
		skyFixtureDef.shape = skyShape;
		skyBody.createFixture(skyFixtureDef);
		skyShape.dispose();
		
		swimmerBodyDef = new BodyDef();
		swimmerBodyDef.type = BodyType.DynamicBody;
		swimmerBodyDef.position.x = startX;
		swimmerBodyDef.position.y = startY;
		
		PolygonShape swimmerPoly = new PolygonShape();
		swimmerPoly.setAsBox(0.75f, 0.75f); //because the swimmer is drawn at 150% of 1 meter, we want the swimmer polygonShape to be set to 0.75m
		
		swimmerBody = world.createBody(swimmerBodyDef);
		swimmerFixture = swimmerBody.createFixture(swimmerPoly, 1.5f);
		swimmerFixture.setFriction(0.0f);
		
		swimmer = new Swimmer(camera, oxygenMeter, staminaMeter, progressBar, swimmerBody);
		swimmerBody.setUserData(swimmer);
		swimmerFixture.setUserData(swimmer);
		swimmerPoly.dispose();
		
		world.setContactListener(new ContactListener() {
			
			@Override
			public void beginContact(Contact contact) {
				System.out.println("Landed on riverbed!");
				swimmer.setSubmergedState(Swimmer.SubmergedState.SWIMMER_ON_RIVERBED);
			}
			
			@Override
			public void endContact(Contact contact) {
				Fixture fixtureA =  contact.getFixtureA();
				Fixture fixtureB = contact.getFixtureB();
				System.out.println("Contact!");
				if(fixtureA.getBody() == swimmerBody) {
					if(fixtureB.getBody() != skyBody) {
						Assets.water.play();
					}
				} else if (fixtureB.getBody() == swimmerBody) {
					if(fixtureA.getBody() != skyBody) {
						Assets.water.play();
					}
				}
			}
			
			@Override
			public void preSolve(Contact contact, Manifold oldManifold) {
				
			}
			
			@Override
			public void postSolve(Contact contact, ContactImpulse impulse) {
				
			}
		});
	}
	
	public void createRiverbedTile(float width, float depth) {
		float [] tileVertices = { lastRiverbedTileLoc, -depth,
								  lastRiverbedTileLoc + width, -depth,
								  lastRiverbedTileLoc + width, -depth - 30.0f,
								  lastRiverbedTileLoc, -depth - 30.0f
		};
		
		PolygonShape tileShape = new PolygonShape();
		tileShape.set(tileVertices);
		BodyDef tileBodyDef = new BodyDef();
		tileBodyDef.type = BodyType.StaticBody;
		Body tileBody = world.createBody(tileBodyDef);
		
		FixtureDef tileFixtureDef = new FixtureDef();
		tileFixtureDef.shape = tileShape;
		tileBody.createFixture(tileFixtureDef);
		tileShape.dispose();
		
		//here I would color in all the tiles
		

		
		//MeshActor ma = new MeshActor(lastRiverbedTileLoc, -depth - 30.0f, width, depth, riverbedColor, "", renderer);
		riverbed.add(new RiverbedTile(lastRiverbedTileLoc, -depth - 30.0f, width, 30.0f));
		
		
		lastRiverbedTileLoc += width;
		
	}
	
}