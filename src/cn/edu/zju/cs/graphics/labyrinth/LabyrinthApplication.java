package cn.edu.zju.cs.graphics.labyrinth;

import cn.edu.zju.cs.graphics.labyrinth.model.Ball;
import cn.edu.zju.cs.graphics.labyrinth.model.BaseHole;
import cn.edu.zju.cs.graphics.labyrinth.model.Entity;
import cn.edu.zju.cs.graphics.labyrinth.model.FinishHole;
import cn.edu.zju.cs.graphics.labyrinth.model.Hole;
import cn.edu.zju.cs.graphics.labyrinth.model.Labyrinth;
import cn.edu.zju.cs.graphics.labyrinth.model.Magnet;
import cn.edu.zju.cs.graphics.labyrinth.model.Wall;
import cn.edu.zju.cs.graphics.labyrinth.rendering.LabyrinthRenderer;
import cn.edu.zju.cs.graphics.labyrinth.rendering.ShadowMapRenderer;
import cn.edu.zju.cs.graphics.labyrinth.util.GlUtils;
import cn.edu.zju.cs.graphics.labyrinth.util.MathUtils;
import cn.edu.zju.cs.graphics.labyrinth.util.MatrixUtils;
import org.dyn4j.dynamics.BodyFixture;
import org.dyn4j.dynamics.contact.ContactPoint;
import org.dyn4j.geometry.Vector2;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.Sys;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWFramebufferSizeCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.glfw.GLFWWindowSizeCallback;
import org.lwjgl.openal.*;
import org.lwjgl.openal.ALC11;
import org.lwjgl.openal.ALC10.*;
import org.lwjgl.opengles.GLES;
import org.lwjgl.openal.EXTThreadLocalContext.*;
import org.lwjgl.system.Configuration;
import org.lwjgl.util.WaveData;
import java.util.List;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.*;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengles.GLES20.*;
import static org.lwjgl.system.MemoryUtil.*;

public class LabyrinthApplication implements Labyrinth.Listener {

    private static final double KEY_ROTATION_STEP_DEGREES = 3;

    private long mWindow;
    private int mWidth = 480;
    private int mHeight = 320;
    private int mFrameBufferWidth = mWidth;
    private int mFrameBufferHeight = mHeight;

    private boolean mNavi = false;
    private Ball mBall;

    private Matrix4f mProjectionMatrix = new Matrix4f();
    private Matrix4f mViewMatrix = new Matrix4f();
    private Matrix4f mViewProjectionMatrix = new Matrix4f();

    private ShadowMapRenderer mShadowMapRenderer;
    private LabyrinthRenderer mLabyrinthRenderer;

    private Labyrinth mLabyrinth;

    private GLFWFramebufferSizeCallback mFramebufferSizeCallback;
    private GLFWWindowSizeCallback mWindowSizeCallback;
    private GLFWKeyCallback mKeyCallback;
    private GLFWCursorPosCallback mCursorPositionCallback;

    private IntBuffer audioBuffer;
    private IntBuffer audioSource;
    private static final int BALL_ROLL = 0;
    private static final int BALL_HIT = 1;
    private static final int BALL_FALL = 2;

    private void init() throws IOException {
        // init audio
        audioBuffer = BufferUtils.createIntBuffer(3);
        audioSource = BufferUtils.createIntBuffer(3);
        //AL.createCapabilities()
        long device = ALC10.alcOpenDevice((ByteBuffer)null);
        ALCCapabilities deviceCaps = ALC.createCapabilities(device);
       
        String defaultDeviceSpecifier = ALC10.alcGetString(NULL, ALC10.ALC_DEFAULT_DEVICE_SPECIFIER);

            long context = ALC10.alcCreateContext(device,(IntBuffer) null);
        ALC10.alcMakeContextCurrent(context);
        AL.createCapabilities(deviceCaps);

        AL10.alGenBuffers(audioBuffer);
        if(AL10.alGetError() != AL10.AL_NO_ERROR)
            return;

        java.io.FileInputStream fin1 = new java.io.FileInputStream("res/cn/edu/zju/cs/graphics/labyrinth/sound/ball-roll-wood.wav");
        InputStream fin = new BufferedInputStream(fin1);
        WaveData ballRoll = WaveData.create(fin);
        AL10.alBufferData(audioBuffer.get(BALL_ROLL), ballRoll.format, ballRoll.data, ballRoll.samplerate);

        fin1 = new java.io.FileInputStream("res/cn/edu/zju/cs/graphics/labyrinth/sound/ball-collision-wood.wav");
        fin = new BufferedInputStream(fin1);
        WaveData ballHit = WaveData.create(fin);
        AL10.alBufferData(audioBuffer.get(BALL_HIT), ballHit.format,ballHit.data,ballHit.samplerate);

        fin1 = new java.io.FileInputStream("res/cn/edu/zju/cs/graphics/labyrinth/sound/hole-metal.wav");
        fin = new BufferedInputStream(fin1);
        WaveData ballFall = WaveData.create(fin);
        AL10.alBufferData(audioBuffer.get(BALL_FALL), ballFall.format,ballFall.data,ballFall.samplerate);

        FloatBuffer listenerOri =
                BufferUtils.createFloatBuffer(6).put(new float[] { 0.0f, 0.0f, -1.0f,  0.0f, 1.0f, 0.0f });

        AL10.alGenSources(audioSource);
        AL10.alSourcei(audioSource.get(BALL_ROLL), AL10.AL_BUFFER, audioBuffer.get(BALL_ROLL));
        AL10.alSourcef(audioSource.get(BALL_ROLL),AL10.AL_GAIN, 0.2f);
        AL10.alSourcef(audioSource.get(BALL_ROLL),AL10.AL_LOOPING, AL10.AL_TRUE);

        AL10.alSourcei(audioSource.get(BALL_HIT), AL10.AL_BUFFER, audioBuffer.get(BALL_HIT));
        AL10.alSourcef(audioSource.get(BALL_HIT),AL10.AL_LOOPING, AL10.AL_FALSE);

        AL10.alSourcei(audioSource.get(BALL_FALL), AL10.AL_BUFFER, audioBuffer.get(BALL_FALL));
        AL10.alSourcef(audioSource.get(BALL_FALL),AL10.AL_GAIN, 0.4f);
        AL10.alSourcef(audioSource.get(BALL_FALL),AL10.AL_LOOPING, AL10.AL_FALSE);



        if (!glfwInit()) {
            throw new IllegalStateException("Failed to initialize GLFW");
        }

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        // Enable multi-sampling.
        glfwWindowHint(GLFW_SAMPLES, 8);
        mWindow = glfwCreateWindow(mWidth, mHeight, "Labyrinth", NULL, NULL);
        if (mWindow == NULL) {
            throw new AssertionError("Failed to create the GLFW window");
        }

        System.out.println("Change gravity with W/A/S/D, reset gravity with 0");
        glfwSetFramebufferSizeCallback(mWindow, mFramebufferSizeCallback =
                new GLFWFramebufferSizeCallback() {
                    @Override
                    public void invoke(long window, int width, int height) {
                        if (width > 0 && height > 0
                                && (mFrameBufferWidth != width || mFrameBufferHeight != height)) {
                            mFrameBufferWidth = width;
                            mFrameBufferHeight = height;
                        }
                    }
                });
        glfwSetWindowSizeCallback(mWindow, mWindowSizeCallback = new GLFWWindowSizeCallback() {
            @Override
            public void invoke(long window, int width, int height) {
                if (width > 0 && height > 0 && (mWidth != width || mHeight != height)) {
                    mWidth = width;
                    mHeight = height;
                }
            }
        });
        glfwSetKeyCallback(mWindow, mKeyCallback = new GLFWKeyCallback() {
            @Override
            public void invoke(long window, int key, int scancode, int action, int mods) {
                if (action != GLFW_RELEASE) {
                    return;
                }
                switch (key) {
                    case GLFW_KEY_ESCAPE:
                        glfwSetWindowShouldClose(window, true);
                        break;
                    case GLFW_KEY_LEFT:
                    case GLFW_KEY_A:
                        mLabyrinth.addRotationX(-KEY_ROTATION_STEP_DEGREES);
                        break;
                    case GLFW_KEY_N:
                        mNavi = !mNavi;
                        break;
                    case GLFW_KEY_RIGHT:
                    case GLFW_KEY_F:
                        mLabyrinth.addRotationX(KEY_ROTATION_STEP_DEGREES);
                        break;
                    case GLFW_KEY_DOWN:
                    case GLFW_KEY_D:
                        mLabyrinth.addRotationY(-KEY_ROTATION_STEP_DEGREES);
                        break;
                    case GLFW_KEY_UP:
                    case GLFW_KEY_W:
                        mLabyrinth.addRotationY(KEY_ROTATION_STEP_DEGREES);
                        break;
                    case GLFW_KEY_0:
                        mLabyrinth.setRotationX(0).setRotationY(0);
                        break;
                }
            }
        });
        glfwSetCursorPosCallback(mWindow, mCursorPositionCallback = new GLFWCursorPosCallback() {
            @Override
            public void invoke(long window, double x, double y) {
                mLabyrinth
                        .setRotationX((x / mWidth - 0.5d) * 4d * Labyrinth.ROTATION_MAX_DEGREES)
                        .setRotationY(-(y / mHeight - 0.5d) * 4d * Labyrinth.ROTATION_MAX_DEGREES);
            }
        });

        GLFWVidMode videoMode = glfwGetVideoMode(glfwGetPrimaryMonitor());
        glfwSetWindowPos(mWindow, (videoMode.width() - mWidth) / 2,
                (videoMode.height() - mHeight) / 2);
        glfwMakeContextCurrent(mWindow);
        glfwSwapInterval(0);
        glfwShowWindow(mWindow);
        glfwSetCursorPos(mWindow, mWidth / 2, mHeight / 2);

        IntBuffer framebufferSize = BufferUtils.createIntBuffer(2);
        nglfwGetFramebufferSize(mWindow, memAddress(framebufferSize),
                memAddress(framebufferSize) + 4);
        mFrameBufferWidth = framebufferSize.get(0);
        mFrameBufferHeight = framebufferSize.get(1);

        GLES.createCapabilities();
        Configuration.DEBUG.set(true);
        Configuration.DEBUG_LOADER.set(true);
        //debugProc = glDebugMessageCallback();

        glClearColor(1f, 1f, 1f, 1f);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        System.out.println("GL_VERSION: " + glGetString(GL_VERSION));
        System.out.println("GL_MAX_TEXTURE_SIZE: " + glGetInteger(GL_MAX_TEXTURE_SIZE));

        mShadowMapRenderer = ShadowMapRenderer.getInstance();
        mLabyrinthRenderer = LabyrinthRenderer.getInstance();


        final float wallThickness = 15f;
        mBall = new Ball(20d + Ball.RADIUS, 20d + Ball.RADIUS);
        mLabyrinth = new Labyrinth()
                .addEntity(new Hole(135.11f, 125.78f))
                .addEntity(new Hole(285.33f, 32f))
                .addEntity(new Hole(420.44f, 90.67f))
                .addEntity(new Hole(450.78f, 184.89f))
                .addEntity(new FinishHole(433.78, 281.78))
                .addEntity(new Wall(59.56f, wallThickness, 44.89f, 55.11f)) // Wall 1
                .addEntity(new Wall(85.33f, wallThickness, 57.78f, 183.11f)) // Wall 2
                .addEntity(new Wall(wallThickness, 50.22f, 108f, 183.11f)) // Wall 3
                .addEntity(new Wall(wallThickness, 17.78f, 71.56f, 241.33f)) // Wall 4
                .addEntity(new Wall(88f, wallThickness, 108f, 257.78f)) // Wall 5
                .addEntity(new Wall(wallThickness, 156.5f, 159.56f, 187.11f)) // Wall 6
                .addEntity(new Wall(96.89f, wallThickness, 118.67f, 101.33f)) // Wall 7
                .addEntity(new Wall(wallThickness, 78.67f, 121.33f, 54.44f)) // Wall 8
                .addEntity(new Wall(32, wallThickness, 184f, 54.67f)) // Wall 9
                .addEntity(new Wall(wallThickness, 258.67f, 207.56f, 176.44f)) // Wall 10
                //.addEntity(new Wall(91.56f, wallThickness, 260.89f, 137.33f)) // Wall 11
                .addEntity(new Wall(91.56f, wallThickness, 260.89f, 223.56f + 4f)) // Wall 12
                .addEntity(new Wall(wallThickness, 87.11f, 261.78f, 58.67f)) // Wall 13
                .addEntity(new Wall(87.11f, wallThickness, 312.89f, 94.67f)) // Wall 14
                .addEntity(new Wall(wallThickness, 173.33f + 2f, 348.61f, 188.89f + 1f)) // Wall 15
                //.addEntity(new Wall(75.56f, wallThickness, 303.67f, 181.78f)) // Wall 16
                .addEntity(new Wall(75.56f, wallThickness, 303.67f, 267.89f + 2f)) // Wall 17
                .addEntity(new Wall(84.44f, wallThickness, 362.22f, 52.22f)) // Wall 18
                .addEntity(new Wall(wallThickness, 248.22f, 396.89f, 183.78f)) // Wall 19
                .addEntity(new Magnet(240f + 5f, 192f - 5f))
                .addEntity(new Hole(231f, 144f))
                .addEntity(new Hole(259f, 144f))
//                .addEntity(new Hole(20d + Hole.RADIUS, Labyrinth.LENGTH - (20d + Hole.RADIUS)))
//                .addEntity(new FinishHole(Labyrinth.WIDTH - (20d + Hole.RADIUS), 20d + Hole.RADIUS))
//                .addEntity(new Magnet(Labyrinth.WIDTH / 2d, Labyrinth.LENGTH / 2d))

                .addEntity(mBall)
                .setListener(this);
    }

    @Override
    public void onBallAttractedByMagnet(Ball ball, Magnet magnet, BodyFixture fieldFixture) {
        Vector2 fixtureCenter = fieldFixture.getShape().getCenter();
        Vector2 attraction = new Vector2(magnet.getPositionX() + fixtureCenter.x,
                magnet.getPositionY() + fixtureCenter.y)
                .subtract(ball.getPositionX(), ball.getPositionY());
        double distance = attraction.getMagnitude();
        attraction.setMagnitude(200000 * ball.getMass() / MathUtils.square(distance));
        ball.setForce(attraction);
    }

    @Override
    public void onBallFallingTowardsHole(Ball ball, BaseHole hole) {
        Vector2 displacement = new Vector2(hole.getPositionX(), hole.getPositionY())
                .subtract(ball.getPositionX(), ball.getPositionY());
        double angle = Math.acos((BaseHole.RADIUS - displacement.getMagnitude()) / Ball.RADIUS);
        Vector2 gravity = Vector2.create(
                10 * ball.getMass() * mLabyrinth.getGravity() * Math.cos(angle) * Math.sin(angle),
                displacement.getDirection());
        ball.setForce(gravity);
        Vector2 velocity = ball.getVelocity();
        velocity
                .setMagnitude(velocity.dot(displacement) / displacement.getMagnitude())
                .setDirection(displacement.getDirection());
    }

    @Override
    public void onBallFallenIntoHole(Ball ball, BaseHole hole) {
        ball.stopMovement();
//        AL10.alSource3f(audioSource.get(BALL_FALL), AL10.AL_POSITION, (float) ball.getPositionX(), (float) ball.getPositionY(), 1.0f);
        AL10.alSourcePlay(audioSource.get(BALL_FALL));
        if (hole instanceof Hole) {
            // TODO: Die.
            ball
                    .setPositionX(Wall.THICKNESS_DEFAULT + Ball.RADIUS)
                    .setPositionY(Wall.THICKNESS_DEFAULT + Ball.RADIUS);
        } else if (hole instanceof FinishHole) {
            // TODO: Victory.
            glfwSetWindowShouldClose(mWindow, true);
        } else {
            throw new IllegalStateException("Unknown type of hole: " + hole);
        }
    }

    @Override
    public void onBallHitEntity(Ball ball, Entity entity, ContactPoint point) {
        // TODO: Audio.
        float v = (float)Math.sqrt(ball.getVelocity().x * ball.getVelocity().x + ball.getVelocity().y * ball.getVelocity().y);
        AL10.alSourcef(audioSource.get(BALL_HIT), AL10.AL_GAIN, v/500);
        AL10.alSourcePlay(audioSource.get(BALL_HIT));
    }

    @Override
    public void onBallRolling(Ball ball, Vector2 movement) {
        // TODO: Audio.
        float v = (float)Math.sqrt(ball.getVelocity().x * ball.getVelocity().x + ball.getVelocity().y * ball.getVelocity().y);
        AL10.alSourcef(audioSource.get(BALL_ROLL), AL10.AL_GAIN, v/300);

        if (AL10.alGetSourcei(audioSource.get(BALL_ROLL), AL10.AL_SOURCE_STATE) != AL10.AL_PLAYING) {
//            AL10.alSource3f(audioSource.get(BALL_ROLL), AL10.AL_POSITION, (float) ball.getPositionX(), (float) ball.getPositionY(), 1.0f);
            AL10.alSourcePlay(audioSource.get(BALL_ROLL));
        }
    }

    @Override
    public void onBallStop() {
        // TODO: Audio.
        //IntBuffer state;
        if (AL10.alGetSourcei(audioSource.get(BALL_ROLL), AL10.AL_SOURCE_STATE) == AL10.AL_PLAYING)
            AL10.alSourceStop(audioSource.get(BALL_ROLL));
    }


    private void update() {

        mLabyrinth.update();

        mViewMatrix
                .setLookAt(
                        (float) Labyrinth.WIDTH / 2f, (float) Labyrinth.LENGTH / 2f, 1f,
                        (float) Labyrinth.WIDTH / 2f, (float) Labyrinth.LENGTH / 2f, 0f,
                        0f, 1f, 0f
                )
                .translate((float) Labyrinth.WIDTH / 2f, (float) Labyrinth.LENGTH / 2f, 0);
        MatrixUtils.skewXAroundY(mViewMatrix,
                (float) Math.toRadians(mLabyrinth.getRotationX() * 0.75d));
        MatrixUtils.skewYAroundX(mViewMatrix,
                (float) Math.toRadians(mLabyrinth.getRotationY() * 0.75d));
//        mViewMatrix.translate((float) -Labyrinth.WIDTH / 2f, (float) -Labyrinth.LENGTH / 2f,
//                (float) -Labyrinth.HEIGHT);
        if (!mNavi)
            mViewMatrix.translate((float) -Labyrinth.WIDTH / 2f, (float) -Labyrinth.LENGTH / 2f,
                    (float) -Labyrinth.HEIGHT);
        else {
            mViewMatrix.rotateX((float) Math.toRadians(-30)).translate(-(float) mBall.getPositionX(), -(float) mBall.getPositionY(),
                    (float) -Labyrinth.HEIGHT);
        }
        mProjectionMatrix.setOrtho((float) -Labyrinth.WIDTH / 2f, (float) Labyrinth.WIDTH / 2f,
                (float) -Labyrinth.LENGTH / 2f, (float) Labyrinth.LENGTH / 2f,
                -1000f, 1000f);
        mProjectionMatrix.mul(mViewMatrix, mViewProjectionMatrix);
    }

    private void render() {

        glEnable(GL_DEPTH_TEST);

        mShadowMapRenderer.render(mLabyrinth);

        glViewport(0, 0, mFrameBufferWidth, mFrameBufferHeight);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        mLabyrinthRenderer.render(mLabyrinth, mViewProjectionMatrix,
                mShadowMapRenderer.getLightMatrix(), mShadowMapRenderer.getShadowMap());

        GlUtils.checkError();
    }

    private void loop() {
        while (!glfwWindowShouldClose(mWindow)) {
            glfwPollEvents();
            update();
            render();
            glfwSwapBuffers(mWindow);
        }
    }

    public void run() {
        try {
            init();
            loop();
            mFramebufferSizeCallback.free();
            mWindowSizeCallback.free();
            mKeyCallback.free();
            mCursorPositionCallback.free();
            glfwDestroyWindow(mWindow);
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            glfwTerminate();
        }
    }
}
