package cn.edu.zju.cs.graphics.labyrinth.model;

import cn.edu.zju.cs.graphics.labyrinth.rendering.Renderable;
import cn.edu.zju.cs.graphics.labyrinth.rendering.Renderer;
import org.dyn4j.dynamics.Body;
import org.joml.Matrix4f;

public abstract class Entity<EntityType extends Entity<EntityType>> implements Renderable {

    private Body mBody;
    private Renderer<EntityType> mRenderer;

    public static <EntityType extends Entity<EntityType>> EntityType ofBody(Body body) {
        //noinspection unchecked
        return (EntityType) body.getUserData();
    }

    public Entity(Body body, Renderer<EntityType> renderer) {
        mBody = body;
        mBody.setUserData(this);
        mRenderer = renderer;
    }

    Body getBody() {
        return mBody;
    }

    public double getPositionX() {
        return mBody.getTransform().getTranslationX();
    }

    public EntityType setPositionX(double positionX) {
        mBody.getTransform().setTranslationX(positionX);
        //noinspection unchecked
        return (EntityType) this;
    }

    public double getPositionY() {
        return mBody.getTransform().getTranslationY();
    }

    public EntityType setPositionY(double positionY) {
        mBody.getTransform().setTranslationY(positionY);
        //noinspection unchecked
        return (EntityType) this;
    }

    public double getRotation() {
        return mBody.getTransform().getRotation();
    }

    public EntityType setRotation(double rotation) {
        mBody.getTransform().setRotation(rotation);
        //noinspection unchecked
        return (EntityType) this;
    }

    public Renderer<EntityType> getRenderer() {
        return mRenderer;
    }

    @Override
    public void render(Matrix4f viewProjectionMatrix) {
        //noinspection unchecked
        mRenderer.render((EntityType) this, viewProjectionMatrix);
    }
}
