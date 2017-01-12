package cn.edu.zju.cs.graphics.labyrinth.rendering;

import cn.edu.zju.cs.graphics.labyrinth.model.Labyrinth;
import cn.edu.zju.cs.graphics.labyrinth.util.GlUtils;
import cn.edu.zju.cs.graphics.labyrinth.util.ResourceUtils;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.io.IOException;

public class FloorRenderer {

    private static FloorRenderer sInstance;

    private RectangleRenderer mRectangleRenderer;
    private Matrix4f mModelMatrix = new Matrix4f()
            .translate((float) Labyrinth.WIDTH / 2f, (float) Labyrinth.LENGTH / 2f, 0f)
            .scale((float) Labyrinth.WIDTH, (float) Labyrinth.LENGTH, 1f);
    private Matrix3f mTextureMatrix = new Matrix3f()
            // TODO: Correct?
            .scale((float) Labyrinth.WIDTH / (float) Labyrinth.SIZE,
                    (float) Labyrinth.LENGTH / (float) Labyrinth.SIZE, 1f);
    private int mTexture;

    public static FloorRenderer getInstance() throws IOException {
        if (sInstance == null) {
            sInstance = new FloorRenderer();
        }
        return sInstance;
    }

    private FloorRenderer() throws IOException {
        mRectangleRenderer = RectangleRenderer.getInstance();
        mTexture = GlUtils.createTexture(ResourceUtils.makeTextureResource("floor.jpg"));
    }

    public void render(Matrix4f viewProjectionMatrix) {
        mRectangleRenderer.render(mModelMatrix, viewProjectionMatrix, mTextureMatrix, mTexture);
    }
}
