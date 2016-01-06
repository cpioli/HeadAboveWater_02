package com.cpioli.headabovewater;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton.ImageButtonStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.assets.loaders.SkinLoader;

public class Assets {
	
	public static AssetManager manager;
	
	public static BitmapFont andaleMono24;
	public static BitmapFont myriadPro24;
	public static BitmapFont myriadPro48;
	
	public static ImageButton bigRestartButtonDIED;
	public static ImageButton bigRestartButtonWIN;
	public static ImageButton bigRestartButtonPAUSE;
	public static ImageButton bigResumeButton;
	
	public static LabelStyle labelStyle;
	public static LabelStyle bigLabelStyle;
	public static Label oxygenMeterLabel;
	public static Label staminaMeterLabel;
	public static Label bigClockNumbers;
	public static Label bigFinalTime;
	
	public static Sound underwaterStroke;
	public static Sound surfaceStroke;
	public static Sound blubStroke;
	public static Sound drown;
	public static Sound sploosh;
	public static Sound water; //play after the sploosh, when sinking into water
	public static Sound gasp;
	public static Sound tired;
	
	public static Sound swimmerVO_01;
	public static Sound swimmerVO_02;
	public static Sound swimmerVO_03;
	public static Sound swimmerVO_04;
	
	public static Music belowSurfaceAmbience;
	public static Music aboveSurfaceAmbience;
	
	public static Skin bigOverlaySkin;
	public static TextureAtlas bigOverlayAtlas;
	public static Texture playerSide;
	public static Texture playerTexture;
	public static Texture sky;
	public static Texture swimmerSpriteSheet;

	public static void load() {
		
		System.out.println("Loading Assets!");
		if(manager == null) {
			manager = new AssetManager();
		}

		if(!manager.containsAsset("overlay.json")) {
			manager.load("overlay.json", Skin.class);
			manager.load("overlay.atlas", TextureAtlas.class);
		}
		
		manager.load("myriadPro24.fnt", BitmapFont.class);
		manager.load("AndaleMonoWhite24.fnt", BitmapFont.class);
		manager.load("myriadPro48.fnt", BitmapFont.class);
		manager.load("bigOverlay.json", Skin.class);
		
		manager.load("sfx/Bluib.wav", Sound.class);
		//manager.load("bubble.WAV", Sound.class);
		manager.load("vo/mario-drown.WAV", Sound.class);
		manager.load("sfx/splash.WAV", Sound.class);
		manager.load("sfx/swim-above.WAV", Sound.class);
		manager.load("sfx/swim-below.WAV", Sound.class);
		manager.load("sfx/water.WAV", Sound.class);
		manager.load("vo/pain-gasp.wav", Sound.class);
		manager.load("vo/mario-lowonhealth.WAV", Sound.class);
		manager.load("vo/HeAbWa_swimmer_vo_01.wav", Sound.class);
		manager.load("vo/HeAbWa_swimmer_vo_02.wav", Sound.class);
		manager.load("vo/HeAbWa_swimmer_vo_03.wav", Sound.class);
		manager.load("vo/HeAbWa_swimmer_vo_04.wav", Sound.class);
		
		manager.load("Pioli_Side.png", Texture.class);
		manager.load("Pioli_Wk2_Sheet.png", Texture.class);
		manager.load("sky.png", Texture.class);
		manager.load("HeadAboveWater_SpriteSheet_OutputV2.png", Texture.class);
		
		manager.load("music/SurfaceRiverAmbianceFinal.ogg", Music.class);
		manager.load("music/BelowSurfaceAmbianceFinal.ogg", Music.class);
		manager.finishLoading();
		
		underwaterStroke = manager.get("sfx/swim-below.WAV"); //used
		surfaceStroke = manager.get("sfx/swim-above.WAV"); //used
		blubStroke = manager.get("sfx/Bluib.wav"); 
		drown = manager.get("vo/mario-drown.WAV"); //used
		sploosh = manager.get("sfx/splash.WAV"); 
		water = manager.get("sfx/water.WAV"); //used
		gasp = manager.get("vo/pain-gasp.wav"); //used
		tired = manager.get("vo/mario-lowonhealth.WAV");
		
		swimmerVO_01 = manager.get("vo/HeAbWa_swimmer_vo_01.wav");
		swimmerVO_02 = manager.get("vo/HeAbWa_swimmer_vo_02.wav");
		swimmerVO_03 = manager.get("vo/HeAbWa_swimmer_vo_03.wav");
		swimmerVO_04 = manager.get("vo/HeAbWa_swimmer_vo_04.wav");
		
		myriadPro24 = manager.get("myriadPro24.fnt");
		myriadPro48 = manager.get("myriadPro48.fnt");
		bigOverlayAtlas = manager.get("overlay.atlas");
		bigOverlaySkin = manager.get("bigOverlay.json");
		bigRestartButtonDIED = new ImageButton(bigOverlaySkin.get("overlayRestart", ImageButtonStyle.class));
		bigRestartButtonWIN = new ImageButton(bigOverlaySkin.get("overlayRestart", ImageButtonStyle.class));
		bigRestartButtonPAUSE = new ImageButton(bigOverlaySkin.get("overlayRestart", ImageButtonStyle.class));
		bigResumeButton = new ImageButton(bigOverlaySkin.get("overlayResume", ImageButtonStyle.class));
		
		bigLabelStyle = new LabelStyle(myriadPro48, Color.WHITE);
		
		bigClockNumbers = new Label("0:00.00", bigLabelStyle);
		bigFinalTime = new Label("0:00.00", bigLabelStyle);

		aboveSurfaceAmbience = manager.get("music/SurfaceRiverAmbianceFinal.ogg");
		belowSurfaceAmbience = manager.get("music/BelowSurfaceAmbianceFinal.ogg");
		
		
		labelStyle = new LabelStyle(myriadPro24, Color.WHITE);
		oxygenMeterLabel = new Label("Oxygen", labelStyle);
		staminaMeterLabel = new Label("Stamina", labelStyle);
		
		playerSide = manager.get("Pioli_Side.png");
		playerTexture = manager.get("Pioli_Wk2_Sheet.png");

		sky = manager.get("sky.png");
		swimmerSpriteSheet = manager.get("HeadAboveWater_SpriteSheet_OutputV2.png");
		System.out.println("The program has completed loading.");
	}
	
	public static void dispose() {
		underwaterStroke.dispose();
		surfaceStroke.dispose();
		blubStroke.dispose();
		drown.dispose();
		sploosh.dispose();
		water.dispose();
		gasp.dispose();
		tired.dispose();
		bigOverlayAtlas.dispose();
		bigOverlaySkin.dispose();
		aboveSurfaceAmbience.dispose();
		belowSurfaceAmbience.dispose();
		swimmerVO_01.dispose();
		swimmerVO_02.dispose();
		swimmerVO_03.dispose();
		swimmerVO_04.dispose();
	}
}