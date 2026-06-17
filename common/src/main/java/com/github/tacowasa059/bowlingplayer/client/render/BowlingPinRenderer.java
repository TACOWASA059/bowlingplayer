package com.github.tacowasa059.bowlingplayer.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.AxisAngle4f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class BowlingPinRenderer {
    private static final float MAX_MOVE_TILT_DEGREES = 4.0F;
    private static final float MAX_MOVE_YAW_DEGREES = 7.0F;
    private static final float MAX_HEALTH_TILT_DEGREES = 18.0F;
    private static final float MAX_DEATH_TILT_DEGREES = 85.0F;

    private BowlingPinRenderer() {
    }

    public static void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, int overlay, float tiltDegrees, float yawDegrees, ResourceLocation texture) {
        poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(yawDegrees), 0, 1, 0)));
        poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(tiltDegrees), 0, 0, 1)));
        drawTexturedBowlingPin(poseStack, buffer, texture, packedLight, overlay);
    }

    public static float computeMoveTilt(float walkSpeed, float animationTime) {
        if (walkSpeed <= 0.01F) {
            return 0.0F;
        }
        float speedFactor = Mth.clamp(walkSpeed * 2.5F, 0.0F, 1.0F);
        return Mth.sin(animationTime * 0.9F) * MAX_MOVE_TILT_DEGREES * speedFactor;
    }

    public static float computeMoveYaw(float walkSpeed, float animationTime) {
        if (walkSpeed <= 0.01F) {
            return 0.0F;
        }
        float speedFactor = Mth.clamp(walkSpeed * 2.2F, 0.0F, 1.0F);
        return Mth.sin(animationTime * 0.45F + 0.7F) * MAX_MOVE_YAW_DEGREES * speedFactor;
    }

    public static float computeHealthTilt(float currentHealth, float maxHealth) {
        if (maxHealth <= 0.0F) {
            return 0.0F;
        }
        float healthRatio = Mth.clamp(currentHealth / maxHealth, 0.0F, 1.0F);
        return (1.0F - healthRatio) * MAX_HEALTH_TILT_DEGREES;
    }

    public static float computeDeathTilt(int deathTime, float partialTick) {
        if (deathTime <= 0) {
            return 0.0F;
        }
        float progress = ((float) deathTime + partialTick - 1.0F) / 20.0F * 1.6F;
        progress = Mth.sqrt(Math.min(progress, 1.0F));
        return progress * MAX_DEATH_TILT_DEGREES;
    }

    public static void drawTexturedBowlingPin(PoseStack poseStack, MultiBufferSource buffer, ResourceLocation texture, int packedLight, int overlay) {
        poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.PI / 4F, 0, 1, 0)));
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(texture));
        drawHead(poseStack, consumer, true, packedLight, overlay);
        drawHead(poseStack, consumer, false, packedLight, overlay);
        drawBody(poseStack, consumer, true, packedLight, overlay);
        drawBody(poseStack, consumer, false, packedLight, overlay);
        poseStack.mulPose(new Quaternionf(new AxisAngle4f(-(float) Math.PI / 4F, 0, 1, 0)));
    }

    private static void drawHead(PoseStack poseStack, VertexConsumer consumer, boolean isInner, int packedLight, int overlay) {
        Matrix4f positionMatrix = poseStack.last().pose();
        int heightLines = 16;
        int longitudeLines = 16;

        for (int i = 0; i < heightLines; i++) {
            for (int j = 0; j < longitudeLines; j++) {
                float y = 0.5F * i / heightLines + 1;
                float nextY = 0.5F * (i + 1) / heightLines + 1;
                float radius = (float) calcRadius(y);
                float nextRadius = (float) calcRadius(nextY);
                if (!isInner) {
                    radius *= 1.01F;
                    nextRadius *= 1.01F;
                }

                float theta = (float) ((2 * Math.PI * j / longitudeLines) - 3 * Math.PI / 2);
                float nextTheta = (float) ((2 * Math.PI * (j + 1) / longitudeLines) - 3 * Math.PI / 2);

                Vector3f vec = new Vector3f((float) (radius * Math.cos(theta)), y, -(float) (radius * Math.sin(theta)));
                Vector3f nextIVec = new Vector3f((float) (nextRadius * Math.cos(theta)), nextY, -(float) (nextRadius * Math.sin(theta)));
                Vector3f nextJVec = new Vector3f((float) (radius * Math.cos(nextTheta)), y, -(float) (radius * Math.sin(nextTheta)));
                Vector3f nextIJVec = new Vector3f((float) (nextRadius * Math.cos(nextTheta)), nextY, -(float) (nextRadius * Math.sin(nextTheta)));

                float[] uv;
                float[] nextIuv;
                float[] nextJuv;
                float[] nextIJuv;

                if (i >= 12) {
                    double r = calcRadius(0.5F * 12 / heightLines + 1);
                    uv = calcUVHeadTopBottom(radius, (float) r, theta + (float) Math.PI / 4, true, isInner);
                    nextIuv = calcUVHeadTopBottom(nextRadius, (float) r, theta + (float) Math.PI / 4, true, isInner);
                    nextJuv = calcUVHeadTopBottom(radius, (float) r, nextTheta + (float) Math.PI / 4, true, isInner);
                    nextIJuv = calcUVHeadTopBottom(nextRadius, (float) r, nextTheta + (float) Math.PI / 4, true, isInner);
                } else if (i < 4) {
                    double r = calcRadius(0.5F * 4 / heightLines + 1);
                    uv = calcUVHeadTopBottom(radius, (float) r, theta + (float) Math.PI / 4, false, isInner);
                    nextIuv = calcUVHeadTopBottom(nextRadius, (float) r, theta + (float) Math.PI / 4, false, isInner);
                    nextJuv = calcUVHeadTopBottom(radius, (float) r, nextTheta + (float) Math.PI / 4, false, isInner);
                    nextIJuv = calcUVHeadTopBottom(nextRadius, (float) r, nextTheta + (float) Math.PI / 4, false, isInner);
                } else {
                    uv = calcUVHeadSide(theta, y, heightLines, isInner);
                    nextIuv = calcUVHeadSide(theta, nextY, heightLines, isInner);
                    nextJuv = calcUVHeadSide(nextTheta, y, heightLines, isInner);
                    nextIJuv = calcUVHeadSide(nextTheta, nextY, heightLines, isInner);
                }
                addQuads(positionMatrix, consumer, vec, nextJVec, nextIJVec, nextIVec, uv, nextJuv, nextIJuv, nextIuv, theta, packedLight, overlay);
            }
        }
    }

    private static void drawBody(PoseStack poseStack, VertexConsumer consumer, boolean isInner, int packedLight, int overlay) {
        Matrix4f positionMatrix = poseStack.last().pose();
        int heightLines = 12;
        int longitudeLines = 16;

        for (int i = 0; i < heightLines; i++) {
            for (int j = 0; j < longitudeLines; j++) {
                float y = (float) i / heightLines;
                float nextY = (float) (i + 1) / heightLines;
                float radius = (float) calcRadius(y);
                float nextRadius = (float) calcRadius(nextY);
                if (!isInner) {
                    radius *= 1.01F;
                    nextRadius *= 1.01F;
                }

                float theta = (float) ((2 * Math.PI * j / longitudeLines) - 3 * Math.PI / 2 + Math.PI / 12);
                float nextTheta = (float) ((2 * Math.PI * (j + 1) / longitudeLines) - 3 * Math.PI / 2 + Math.PI / 12);

                Vector3f vec = new Vector3f((float) (radius * Math.cos(theta)), y, -(float) (radius * Math.sin(theta)));
                Vector3f nextIVec = new Vector3f((float) (nextRadius * Math.cos(theta)), nextY, -(float) (nextRadius * Math.sin(theta)));
                Vector3f nextJVec = new Vector3f((float) (radius * Math.cos(nextTheta)), y, -(float) (radius * Math.sin(nextTheta)));
                Vector3f nextIJVec = new Vector3f((float) (nextRadius * Math.cos(nextTheta)), nextY, -(float) (nextRadius * Math.sin(nextTheta)));

                float[] uv = calcUVBodySide(theta, y, heightLines, isInner);
                float[] nextIuv = calcUVBodySide(theta, nextY, heightLines, isInner);
                float[] nextJuv = calcUVBodySide(nextTheta, y, heightLines, isInner);
                float[] nextIJuv = calcUVBodySide(nextTheta, nextY, heightLines, isInner);
                addQuads(positionMatrix, consumer, vec, nextJVec, nextIJVec, nextIVec, uv, nextJuv, nextIJuv, nextIuv, theta, packedLight, overlay);
            }
        }

        for (int i = 0; i < heightLines; i++) {
            for (int j = 0; j < longitudeLines; j++) {
                float y = 0;
                float radius = (float) calcRadius(y) * i / heightLines;
                float nextRadius = (float) calcRadius(y) * (i + 1) / heightLines;
                if (!isInner) {
                    radius *= 1.01F;
                    nextRadius *= 1.01F;
                }

                float theta = (float) ((2 * Math.PI * j / longitudeLines) - 3 * Math.PI / 2);
                float nextTheta = (float) ((2 * Math.PI * (j + 1) / longitudeLines) - 3 * Math.PI / 2);

                Vector3f vec = new Vector3f((float) (radius * Math.cos(theta)), y, -(float) (radius * Math.sin(theta)));
                Vector3f nextIVec = new Vector3f((float) (nextRadius * Math.cos(theta)), y, -(float) (nextRadius * Math.sin(theta)));
                Vector3f nextJVec = new Vector3f((float) (radius * Math.cos(nextTheta)), y, -(float) (radius * Math.sin(nextTheta)));
                Vector3f nextIJVec = new Vector3f((float) (nextRadius * Math.cos(nextTheta)), y, -(float) (nextRadius * Math.sin(nextTheta)));

                double r = calcRadius(0);
                float[] uv = calcUVBodyTopBottom(radius, (float) r, theta + (float) Math.PI / 4, false, isInner);
                float[] nextIuv = calcUVBodyTopBottom(nextRadius, (float) r, theta + (float) Math.PI / 4, false, isInner);
                float[] nextJuv = calcUVBodyTopBottom(radius, (float) r, nextTheta + (float) Math.PI / 4, false, isInner);
                float[] nextIJuv = calcUVBodyTopBottom(nextRadius, (float) r, nextTheta + (float) Math.PI / 4, false, isInner);
                addQuads(positionMatrix, consumer, vec, nextJVec, nextIJVec, nextIVec, uv, nextJuv, nextIJuv, nextIuv, theta, packedLight, overlay);
            }
        }
    }

    private static double calcRadius(double y0) {
        double y = 10 * y0;
        return Math.sqrt(1.2 + 5.32 * y - 1.485 * y * y + 0.135 * Math.pow(y, 3.0) - 0.004 * Math.pow(y, 4.0)) / 10;
    }

    private static float[] calcUVHeadTopBottom(float radius, float limit, float theta, boolean isUpper, boolean isInner) {
        float[] uv = new float[]{0, 0.0625F};
        uv[0] += isUpper ? 0.1875F : 0.3125F;
        float size = 0.0625F;

        float x = (float) (radius * Math.cos(theta) / (limit + 0.002));
        float z = -(float) (radius * Math.sin(theta) / (limit + 0.002));
        double sqrt = Math.sqrt(x * x + z * z);
        if (x * x >= z * z) {
            uv[1] += x != 0 ? size * (float) (sqrt * z * Math.signum(x) / x) : size * Math.signum(z);
            uv[0] += size * (float) (sqrt * Math.signum(x));
        } else {
            uv[0] += z != 0 ? size * (float) (sqrt * x * Math.signum(z) / z) : size * Math.signum(x);
            uv[1] += size * (float) (sqrt * Math.signum(z));
        }
        if (!isInner) {
            uv[0] += 0.5F;
        }
        return uv;
    }

    private static float[] calcUVHeadSide(float theta, float y, int heightLines, boolean isInner) {
        float[] uv = new float[]{0, 0.25F};
        uv[0] += Math.max(Math.min((theta + 3 * (float) Math.PI / 2) * 0.5F / (2 * (float) Math.PI), 0.4999F), 0.0001F);
        if (y <= 0.5F * 4 / heightLines + 1) {
            y = 0.5F * 4 / heightLines + 1.00001F;
        } else if (y >= 0.5F * 12 / heightLines + 1) {
            y = 0.5F * 12 / heightLines + 0.99999F;
        }
        uv[1] -= 0.125F * (y - (0.5F * 4 / heightLines + 1)) / (0.5F * 8 / heightLines);
        if (!isInner) {
            uv[0] += 0.5F;
        }
        return uv;
    }

    private static float[] calcUVBodySide(float theta, float y, int heightLines, boolean isInner) {
        float[] uv = new float[]{0.25F, 0.5F};
        uv[0] += (theta + 3 * (float) Math.PI / 2 - (float) Math.PI / 12) * 0.375F / (2 * (float) Math.PI);
        uv[1] -= 0.1875F * y;
        if (!isInner) {
            uv[1] += 0.25F;
        }
        return uv;
    }

    private static float[] calcUVBodyTopBottom(float radius, float limit, float theta, boolean isUpper, boolean isInner) {
        float[] uv = new float[]{0, 0.28125F};
        uv[0] += isUpper ? 0.375F : 0.5F;
        float size = 0.0625F;

        float x = (float) (radius * Math.cos(theta) / (limit + 0.002));
        float z = -(float) (radius * Math.sin(theta) / (limit + 0.002));
        double sqrt = Math.sqrt(x * x + z * z);
        if (x * x >= z * z) {
            uv[1] += x != 0 ? size * (float) (sqrt * z * Math.signum(x) / x) / 2 : size * Math.signum(z) / 2;
            uv[0] += size * (float) (sqrt * Math.signum(x));
        } else {
            uv[0] += z != 0 ? size * (float) (sqrt * x * Math.signum(z) / z) : size * Math.signum(x);
            uv[1] += size * (float) (sqrt * Math.signum(z)) / 2;
        }
        if (!isInner) {
            uv[1] += 0.25F;
        }
        return uv;
    }

    private static void addQuads(Matrix4f positionMatrix, VertexConsumer builder, Vector3f pos1, Vector3f pos2, Vector3f pos3, Vector3f pos4,
                                 float[] uv1, float[] uv2, float[] uv3, float[] uv4, float theta, int packedLight, int overlay) {
        float dx = (float) Math.cos(theta);
        float dz = -(float) Math.sin(theta);
        addVertex(positionMatrix, builder, pos1, uv1, dx, dz, packedLight, overlay);
        addVertex(positionMatrix, builder, pos2, uv2, dx, dz, packedLight, overlay);
        addVertex(positionMatrix, builder, pos3, uv3, dx, dz, packedLight, overlay);
        addVertex(positionMatrix, builder, pos4, uv4, dx, dz, packedLight, overlay);
    }

    private static void addVertex(Matrix4f positionMatrix, VertexConsumer builder, Vector3f pos, float[] uv, float dx, float dz, int packedLight, int overlay) {
        builder.vertex(positionMatrix, pos.x(), pos.y(), pos.z())
                .color(255, 255, 255, 255)
                .uv(uv[0], uv[1])
                .overlayCoords(overlay)
                .uv2(packedLight)
                .normal(dx, 0.0F, dz)
                .endVertex();
    }
}
