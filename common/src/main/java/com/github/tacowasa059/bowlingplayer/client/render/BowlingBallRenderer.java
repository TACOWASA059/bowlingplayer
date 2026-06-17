package com.github.tacowasa059.bowlingplayer.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.joml.AxisAngle4f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

public final class BowlingBallRenderer {
    public static final float DEFAULT_RADIUS = 0.475F;

    private BowlingBallRenderer() {
    }

    public static void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, int overlay, ResourceLocation texture, float radius) {
        drawTexturedSphere(poseStack, buffer, texture, radius, 32, 0.0F, 0.0F, packedLight, true, overlay);
    }

    public static void drawTexturedSphere(PoseStack poseStack, MultiBufferSource buffer,
                                          ResourceLocation texture, float radius, int segments, float x, float z,
                                          int packedLight, boolean lightmap2, int overlay) {
        Matrix4f positionMatrix = poseStack.last().pose();
        PoseStack.Pose pose = poseStack.last();
        poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.PI / 4, 0, 1, 0)));

        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityCutoutNoCull(texture));
        addBottomSphere(radius, segments, x, z, vertexConsumer, positionMatrix, pose, 0.1875F, false, packedLight, lightmap2, overlay);
        addBottomSphere(radius * 1.01F, segments, x, z, vertexConsumer, positionMatrix, pose, 0.1875F + 0.5F, false, packedLight, lightmap2, overlay);
        addBottomSphere(radius, segments, x, z, vertexConsumer, positionMatrix, pose, 0.1875F + 0.125F, true, packedLight, lightmap2, overlay);
        addBottomSphere(radius * 1.01F, segments, x, z, vertexConsumer, positionMatrix, pose, 0.1875F + 0.125F + 0.5F, true, packedLight, lightmap2, overlay);
        addSideSphere(radius, segments, x, z, vertexConsumer, positionMatrix, pose, false, packedLight, lightmap2, overlay);
        addSideSphere(radius * 1.01F, segments, x, z, vertexConsumer, positionMatrix, pose, true, packedLight, lightmap2, overlay);

        poseStack.mulPose(new Quaternionf(new AxisAngle4f(-(float) Math.PI / 4, 0, 1, 0)));
    }

    private static void addBottomSphere(float radius, int segments, float x, float z, VertexConsumer vertexBuilder,
                                        Matrix4f positionMatrix, PoseStack.Pose pose, float u0, boolean isLower,
                                        int packedLight, boolean lightmap2, int overlay) {
        float cubeSize = 0.0625F;
        for (int j = 0; j < Math.round(segments / 4F); j++) {
            float theta = (float) (Math.PI * j / (4 * Math.round(segments / 4F)));
            float sinTheta = (float) Math.sin(theta);
            float cosTheta = (float) Math.cos(theta);

            float nextTheta = (float) (Math.PI * (j + 1) / (4 * Math.round(segments / 4F)));
            float nextSinTheta = (float) Math.sin(nextTheta);
            float nextCosTheta = (float) Math.cos(nextTheta);

            for (int i = 0; i < segments; i++) {
                double angle = -Math.PI * 2 * i / segments;
                double nextAngle = -Math.PI * 2 * (i + 1) / segments;
                if (isLower) {
                    angle = -angle;
                    nextAngle = -nextAngle;
                }

                float dx = (float) (radius * sinTheta * Math.cos(angle));
                float dz = (float) (radius * sinTheta * Math.sin(angle));
                float nextIdx = (float) (radius * sinTheta * Math.cos(nextAngle));
                float nextIdz = (float) (radius * sinTheta * Math.sin(nextAngle));
                float nextJdx = (float) (radius * nextSinTheta * Math.cos(angle));
                float nextJdz = (float) (radius * nextSinTheta * Math.sin(angle));
                float nextIJdx = (float) (radius * nextSinTheta * Math.cos(nextAngle));
                float nextIJdz = (float) (radius * nextSinTheta * Math.sin(nextAngle));

                float[] posList;
                if (isLower) {
                    posList = new float[]{
                            dx, -radius * cosTheta, dz,
                            nextIdx, -radius * cosTheta, nextIdz,
                            nextJdx, -radius * nextCosTheta, nextJdz,
                            nextIJdx, -radius * nextCosTheta, nextIJdz
                    };
                } else {
                    posList = new float[]{
                            dx, radius * cosTheta, dz,
                            nextIdx, radius * cosTheta, nextIdz,
                            nextJdx, radius * nextCosTheta, nextJdz,
                            nextIJdx, radius * nextCosTheta, nextIJdz
                    };
                }

                float x1 = (float) Math.cos(angle - Math.PI / 4) * sinTheta * (float) Math.sqrt(2.0F);
                float z1 = (float) Math.sin(angle - Math.PI / 4) * sinTheta * (float) Math.sqrt(2.0F);
                float nextIx1 = (float) Math.cos(nextAngle - Math.PI / 4) * sinTheta * (float) Math.sqrt(2.0F);
                float nextIz1 = (float) Math.sin(nextAngle - Math.PI / 4) * sinTheta * (float) Math.sqrt(2.0F);
                float nextJx1 = (float) Math.cos(angle - Math.PI / 4) * nextSinTheta * (float) Math.sqrt(2.0F);
                float nextJz1 = (float) Math.sin(angle - Math.PI / 4) * nextSinTheta * (float) Math.sqrt(2.0F);
                float nextIJx1 = (float) Math.cos(nextAngle - Math.PI / 4) * nextSinTheta * (float) Math.sqrt(2.0F);
                float nextIJz1 = (float) Math.sin(nextAngle - Math.PI / 4) * nextSinTheta * (float) Math.sqrt(2.0F);

                float squareX = getSquareX(x1, z1, cubeSize);
                float squareZ = getSquareZ(x1, z1, cubeSize);
                float nextISquareX = getSquareX(nextIx1, nextIz1, cubeSize);
                float nextISquareZ = getSquareZ(nextIx1, nextIz1, cubeSize);
                float nextJSquareX = getSquareX(nextJx1, nextJz1, cubeSize);
                float nextJSquareZ = getSquareZ(nextJx1, nextJz1, cubeSize);
                float nextIJSquareX = getSquareX(nextIJx1, nextIJz1, cubeSize);
                float nextIJSquareZ = getSquareZ(nextIJx1, nextIJz1, cubeSize);

                float[] uvList = new float[]{
                        u0 + squareX, 0.0625F + squareZ,
                        u0 + nextISquareX, 0.0625F + nextISquareZ,
                        u0 + nextJSquareX, 0.0625F + nextJSquareZ,
                        u0 + nextIJSquareX, 0.0625F + nextIJSquareZ
                };
                addSphereQuads(vertexBuilder, positionMatrix, pose, x, z, posList, uvList, packedLight, lightmap2, overlay);
            }
        }
    }

    private static void addSideSphere(float radius, int segments, float x, float z, VertexConsumer vertexBuilder,
                                      Matrix4f positionMatrix, PoseStack.Pose pose, boolean isInner,
                                      int packedLight, boolean lightmap2, int overlay) {
        for (int j = Math.round(segments / 4F); j < Math.round(3 * segments / 4F); j++) {
            float theta = (float) (Math.PI * j / (4 * Math.round(segments / 4F)));
            float sinTheta = (float) Math.sin(theta);
            float cosTheta = (float) Math.cos(theta);

            float nextTheta = (float) (Math.PI * (j + 1) / (4 * Math.round(segments / 4F)));
            float nextSinTheta = (float) Math.sin(nextTheta);
            float nextCosTheta = (float) Math.cos(nextTheta);

            for (int i = 0; i < segments; i++) {
                double angle = -Math.PI / 2 - Math.PI * 2 * i / segments;
                double nextAngle = -Math.PI / 2 - Math.PI * 2 * (i + 1) / segments;

                float dx = (float) (radius * Math.cos(angle) * sinTheta);
                float dz = (float) (radius * Math.sin(angle) * sinTheta);
                float nextIdx = (float) (radius * Math.cos(nextAngle) * sinTheta);
                float nextIdz = (float) (radius * Math.sin(nextAngle) * sinTheta);
                float nextJdx = (float) (radius * Math.cos(angle) * nextSinTheta);
                float nextJdz = (float) (radius * Math.sin(angle) * nextSinTheta);
                float nextIJdx = (float) (radius * Math.cos(nextAngle) * nextSinTheta);
                float nextIJdz = (float) (radius * Math.sin(nextAngle) * nextSinTheta);

                float[] posList = new float[]{
                        dx, radius * cosTheta, dz,
                        nextIdx, radius * cosTheta, nextIdz,
                        nextJdx, radius * nextCosTheta, nextJdz,
                        nextIJdx, radius * nextCosTheta, nextIJdz
                };

                float u = 0.5F * i / segments;
                float v = 0.125F + 0.125F * (j - Math.round(segments / 4F)) / Math.round(segments / 2F);
                float nextIu = 0.5F * (i + 1) / segments;
                float nextJu = 0.5F * i / segments;
                float nextJv = 0.125F + 0.125F * (j + 1 - Math.round(segments / 4F)) / Math.round(segments / 2F);
                float nextIJu = 0.5F * (i + 1) / segments;

                if (isInner) {
                    u += 0.5F;
                    nextIu += 0.5F;
                    nextJu += 0.5F;
                    nextIJu += 0.5F;
                }
                float[] uvList = new float[]{u, v, nextIu, v, nextJu, nextJv, nextIJu, nextJv};
                addSphereQuads(vertexBuilder, positionMatrix, pose, x, z, posList, uvList, packedLight, lightmap2, overlay);
            }
        }
    }

    private static void addSphereQuads(VertexConsumer vertexConsumer, Matrix4f positionMatrix, PoseStack.Pose pose,
                                       float x, float z, float[] posList, float[] uvList,
                                       int packedLight, boolean lightmap2, int overlay) {
        if (lightmap2) {
            addVertex(vertexConsumer, positionMatrix, pose, posList[0] + x, posList[1], posList[2] + z, uvList[0], uvList[1], packedLight, overlay, posList[0], posList[1], posList[2]);
            addVertex(vertexConsumer, positionMatrix, pose, posList[3] + x, posList[4], posList[5] + z, uvList[2], uvList[3], packedLight, overlay, posList[3], posList[4], posList[5]);
            addVertex(vertexConsumer, positionMatrix, pose, posList[9] + x, posList[10], posList[11] + z, uvList[6], uvList[7], packedLight, overlay, posList[9], posList[10], posList[11]);
            addVertex(vertexConsumer, positionMatrix, pose, posList[6] + x, posList[7], posList[8] + z, uvList[4], uvList[5], packedLight, overlay, posList[6], posList[7], posList[8]);
        }
    }

    private static void addVertex(VertexConsumer vertexConsumer, Matrix4f positionMatrix, PoseStack.Pose pose,
                                  float x, float y, float z, float u, float v, int packedLight, int overlay,
                                  float nx, float ny, float nz) {
        vertexConsumer.vertex(positionMatrix, x, y, z)
                .color(255, 255, 255, 255)
                .uv(u, v)
                .overlayCoords(overlay)
                .uv2(packedLight)
                .normal(pose.normal(), nx, ny, nz)
                .endVertex();
    }

    private static float getSquareZ(float x1, float z1, float cubeSize) {
        return (float) (cubeSize * (0.5 * Math.sqrt(2 - x1 * x1 + z1 * z1 + 2 * Math.sqrt(2.0) * z1)
                - 0.5 * Math.sqrt(2 - x1 * x1 + z1 * z1 - 2 * Math.sqrt(2.0) * z1)));
    }

    private static float getSquareX(float x1, float z1, float cubeSize) {
        return (float) (cubeSize * (0.5 * Math.sqrt(2 + x1 * x1 - z1 * z1 + 2 * Math.sqrt(2.0) * x1)
                - 0.5 * Math.sqrt(2 + x1 * x1 - z1 * z1 - 2 * Math.sqrt(2.0) * x1)));
    }
}
