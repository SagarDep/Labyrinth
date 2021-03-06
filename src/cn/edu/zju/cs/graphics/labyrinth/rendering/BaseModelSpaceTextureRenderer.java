package cn.edu.zju.cs.graphics.labyrinth.rendering;

import cn.edu.zju.cs.graphics.labyrinth.util.GlUtils;
import cn.edu.zju.cs.graphics.labyrinth.util.ResourceUtils;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;

import java.io.IOException;
import java.nio.FloatBuffer;

import static org.lwjgl.opengles.GLES20.*;

public abstract class BaseModelSpaceTextureRenderer {

    private int mProgram;
    private int mVertexAttribute;
    private int mTextureSizeUniform;
    private int mModelMatrixUniform;
    private int mViewProjectionMatrixUniform;
    private int mTextureUniform;

    private FloatBuffer mModelMatrixBuffer = BufferUtils.createFloatBuffer(4 * 4);
    private FloatBuffer mViewProjectionMatrixBuffer = BufferUtils.createFloatBuffer(4 * 4);

    public BaseModelSpaceTextureRenderer() throws IOException {
        mProgram = GlUtils.createProgram(ResourceUtils.makeShaderResource(getVertexShaderName()),
                ResourceUtils.makeShaderResource(getFragmentShaderName()));
        mVertexAttribute = GlUtils.getAttribLocation(mProgram, "aVertex");
        glEnableVertexAttribArray(mVertexAttribute);
        mTextureSizeUniform = GlUtils.getUniformLocation(mProgram, "uTextureSize");
        mModelMatrixUniform = GlUtils.getUniformLocation(mProgram, "uModelMatrix");
        mViewProjectionMatrixUniform = GlUtils.getUniformLocation(mProgram,
                "uViewProjectionMatrix");
        mTextureUniform = GlUtils.getUniformLocation(mProgram, "uTexture");
    }

    protected int getProgram() {
        return mProgram;
    }

    abstract protected String getVertexShaderName();

    protected String getFragmentShaderName() {
        return "generic.fs";
    }

    public void render(int vertexArrayBuffer, int positionSize, int elementArrayBuffer,
                       int elementCount, Matrix4f modelMatrix, Matrix4f viewProjectionMatrix,
                       int texture, float textureWidth, float textureLength) {
        glUseProgram(mProgram);
        glBindBuffer(GL_ARRAY_BUFFER, vertexArrayBuffer);
        GlUtils.vertexAttribPointer(mVertexAttribute, positionSize);
        glUniform2f(mTextureSizeUniform, textureWidth, textureLength);
        glUniformMatrix4fv(mModelMatrixUniform, false, modelMatrix.get(mModelMatrixBuffer));
        glUniformMatrix4fv(mViewProjectionMatrixUniform, false,
                viewProjectionMatrix.get(mViewProjectionMatrixBuffer));
        GlUtils.uniformTexture(mTextureUniform, GL_TEXTURE0, texture);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, elementArrayBuffer);
        onDrawElements();
        glDrawElements(GL_TRIANGLES, elementCount, GL_UNSIGNED_INT, 0);
        onElementsDrawn();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glUseProgram(0);
    }

    protected void onDrawElements() {}

    protected void onElementsDrawn() {}
}
