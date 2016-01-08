package com.cpioli.headabovewater.ui;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.cpioli.headabovewater.Assets;

//must implement submerged observer to change sprite state
public class SwimmerAnimation implements SubmergedObserver, OrientationObserver {

    private static final int        FRAME_COLS = 8;         // #1
    private static final int        FRAME_ROWS = 8;         // #2
    private static final int        WALK_CYCLE_FRAME_COUNT = 4;
    private static final int        SWIM_CYCLE_FRAME_COUNT = 1;
    private static final int        LEFT_ANIMATION_ROW = 1;
    private static final int        RIGHT_ANIMATION_ROW = 0;

    private static final float      WALK_ANIMATION_FRAME_DURATION = 0.025f;

    Animation                       walkLeftAnimation;          // #3
    TextureRegion[]                 walkLeftFrames;             // #5
    TextureRegion                   walkRestLeftFrame;

    Animation                       walkRightAnimation;
    TextureRegion[]                 walkRightFrames;
    TextureRegion                   walkRestRightFrame;

    TextureRegion                   leftStrokeFrame;
    TextureRegion                   swimRestLeftFrame;

    TextureRegion                   rightStrokeFrame;
    TextureRegion                   swimRestRightFrame;

    Texture                         spriteSheet;              // #4
    SpriteBatch                     spriteBatch;            // #6

    TextureRegion                   currentFrame;           // #7
    TextureRegion                   currentRestFrame;
    TextureRegion                   currentStrokeFrame;
    Animation                       currentAnimation;


    float stateTime;
    Swimmer.SubmergedState submergedState;
    Swimmer.OrientationState currentDirection;

    public SwimmerAnimation() {
        spriteSheet = Assets.swimmerSpriteSheet; // #9
        TextureRegion[][] tmp = TextureRegion.split(spriteSheet, spriteSheet.getWidth()/FRAME_COLS, spriteSheet.getHeight()/FRAME_ROWS);              // #10
        int index = 0;
        submergedState = Swimmer.SubmergedState.SWIMMER_ABOVE_WATER;
        //assign the frames to each TextureRegion

        for (int i = 0; i < FRAME_ROWS; i++) {
            for (int j = 0; j < FRAME_COLS; j++) {
                walkFrames[index++] = tmp[i][j];
            }
        }
        walkAnimation = new Animation(0.025f, walkFrames);      // #11
        spriteBatch = new SpriteBatch();                // #12
        stateTime = 0f;// #13

        currentDirection = Swimmer.OrientationState.RIGHT;
        currentAnimation = null;
        strokeFrame = null;
        restFrame = null;
        AssignRightFacingAnimations(tmp);
        AssignLeftFacingAnimations(tmp);
    }

    private void AssignRightFacingAnimations(TextureRegion[][] tmp) {
        walkRightFrames = new TextureRegion[WALK_CYCLE_FRAME_COUNT];
        for(int i = 1, index = 0; index < WALK_CYCLE_FRAME_COUNT; i++) {
               walkRightFrames[index++]=tmp[RIGHT_ANIMATION_ROW][i];
        }

        walkRightAnimation = new Animation(WALK_ANIMATION_FRAME_DURATION, walkRightFrames);
        walkRestRightFrame = tmp[RIGHT_ANIMATION_ROW][0];

        rightStrokeFrame = tmp[RIGHT_ANIMATION_ROW][6];
        swimRestRightFrame = tmp[RIGHT_ANIMATION_ROW][5];
    }

    private void AssignLeftFacingAnimations(TextureRegion[][] tmp) {
        walkLeftFrames = new TextureRegion[WALK_CYCLE_FRAME_COUNT];
        for(int i = 1, index = 0; index < WALK_CYCLE_FRAME_COUNT; i++) {
            walkLeftFrames[index++] = tmp[LEFT_ANIMATION_ROW][i];
        }

        walkLeftAnimation = new Animation(WALK_ANIMATION_FRAME_DURATION, walkLeftFrames);
        walkRestLeftFrame = tmp[LEFT_ANIMATION_ROW][0];

        leftStrokeFrame = tmp[LEFT_ANIMATION_ROW][6];
        swimRestLeftFrame = tmp[LEFT_ANIMATION_ROW][5];
    }

    //this will be a lot more complicated than it looks!
    public TextureRegion getCurrentFrame() {
        return this.currentFrame;
    }

    //TODO: implement
    //this is not a normal method. Will require changing animation or TextureRegion when a particular submergedState
    //occurs
    @Override
    public void updateSubmergedState(Swimmer.SubmergedState submergedState) {
        this.submergedState = submergedState;

        switch(submergedState) {
            case SWIMMER_ABOVE_WATER:
            case SWIMMER_UNDER_WATER:
                switchAnimationToSwim();
                break;

            case SWIMMER_ON_RIVERBED:
                switchAnimationToWalk();
                break;
        }

        stateTime = 0.0f;
    }

    public void switchAnimationToSwim() {
        if(currentDirection == Swimmer.OrientationState.RIGHT) {
            currentStrokeFrame = rightStrokeFrame;
        } else {
            currentStrokeFrame = leftStrokeFrame;
        }
    }

    public void switchAnimationToWalk() {

    }

    public void switchAnimationToRight() {

    }

    public void switchAnimationToLeft() {

    }

    @Override
    public void updateOrientationState(Swimmer.OrientationState orientationState) {
        if(currentDirection != orientationState) {
            //swap out the left and right animations
        }
        this.currentDirection = orientationState;
        //TODO: check if we need to switch out the animations

    }

    //increments animation's stateTime
    //also swaps left/right Animations
    public void update(float deltaTime) {

    }

    //Performs a swim stroke by swapping swimming textures
    public void stroke() {
        //if the swimmer is leaving the riverbed, the sprites need to change
        if(submergedState == Swimmer.SubmergedState.SWIMMER_ON_RIVERBED) {

        }
    }
}