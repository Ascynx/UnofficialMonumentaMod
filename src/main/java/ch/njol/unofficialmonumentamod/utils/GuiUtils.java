package ch.njol.unofficialmonumentamod.utils;

import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Rectangle;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;

public class GuiUtils {
    public static void drawFilledPolygon(MatrixStack matrices, int originX, int originY, float radius, int sides, int color) {
        drawPartialFilledPolygon(matrices, originX, originY, radius, sides, color, 1.0);
    }

    public static void drawPartialFilledPolygon(MatrixStack matrices, int originX, int originY, float radius, int sides, int color, double percentage) {
        //percentage from 0.00 to 1.00
        float a = (float)(color >> 24 & 0xFF) / 255.0f;
        float r = (float)(color >> 16 & 0xFF) / 255.0f;
        float g = (float)(color >> 8 & 0xFF) / 255.0f;
        float b = (float)(color & 0xFF) / 255.0f;
        Matrix4f positionMatrix = matrices.peek().getPositionMatrix();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();

        bufferBuilder.begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
        bufferBuilder.vertex(positionMatrix, originX, originY, 0.0f).color(a, r, g, b).next();

        //very optimised (trust)
        for (int i = 0; i <= (sides * percentage); i++) {
            double angle = ((Math.PI * 2) * i / sides) + Math.toRadians(180);
            bufferBuilder.vertex(positionMatrix, (float) (originX + Math.sin(angle) * radius), (float) (originY + Math.cos(angle) * radius), 0.0f).color(a, r, g, b).next();
        }
        BufferBuilder.BuiltBuffer built = bufferBuilder.end();

        BufferRenderer.drawWithGlobalProgram(built);
        RenderSystem.disableBlend();
    }

    public static void drawHollowPolygon(MatrixStack matrices, int originX, int originY, int borderWidth, float radius, int sides, int color) {
        drawPartialHollowPolygon(matrices, originX, originY, borderWidth, radius, sides, color, 1.0);
    }

    private static void drawPartialPartPolygon(MatrixStack matrices, int originX, int originY, int borderWidth, float radius, int sides, double percentage, float r, float g, float b, float a) {
        Matrix4f positionMatrix = matrices.peek().getPositionMatrix();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
        bufferBuilder.begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);

        //very optimised (trust)
        for (int i = 0; i <= (sides * percentage); i++) {
            double angle = ((Math.PI * 2) * i / sides) + Math.toRadians(180);
            bufferBuilder.vertex(positionMatrix, (float) (originX + Math.sin(angle) * (radius - borderWidth)), (float) (originY + Math.cos(angle) * (radius - borderWidth)), 0.0f).color(a, r, g, b).next();
            bufferBuilder.vertex(positionMatrix, (float) (originX + Math.sin(angle) * radius), (float) (originY + Math.cos(angle) * radius), 0.0f).color(a, r, g, b).next();
        }

        BufferBuilder.BuiltBuffer built = bufferBuilder.end();
        BufferRenderer.drawWithGlobalProgram(built);
        RenderSystem.disableBlend();
    }

    public static void drawPartialHollowPolygon(MatrixStack matrices, int originX, int originY, int borderWidth, float radius, int sides, int color, double percentage) {
        float a = (float)(color >> 24 & 0xFF) / 255.0f;
        float r = (float)(color >> 16 & 0xFF) / 255.0f;
        float g = (float)(color >> 8 & 0xFF) / 255.0f;
        float b = (float)(color & 0xFF) / 255.0f;

        drawPartialPartPolygon(matrices, originX, originY, borderWidth, radius, sides, percentage, r, g, b, a);
        drawPartialPartPolygon(matrices, originX, originY, borderWidth, radius + ((float) borderWidth / 2), sides*2, percentage, r, g, b, a);
        drawPartialPartPolygon(matrices, originX, originY, borderWidth, radius - ((float) borderWidth / 2), sides*2, percentage, r, g, b, a);
    }

    private static final int OUTLINE_COLOR = 0xFFadacac;
    public static void renderOutline(MatrixStack matrices, Rectangle pos) {
        renderOutline(matrices, pos, OUTLINE_COLOR);
    }

    public static void renderOutline(MatrixStack matrices, Rectangle pos, int color) {
        //x1 x2
        DrawableHelper.fill(matrices, pos.x, pos.y - 1, (int) pos.getMaxX(), pos.y + 1, color);
        //y1 y2
        DrawableHelper.fill(matrices, (int) pos.getMaxX() - 1, pos.y, (int) pos.getMaxX() + 1, (int) pos.getMaxY(), color);
        //x2 x1
        DrawableHelper.fill(matrices, (int) pos.getMaxX(), (int) pos.getMaxY() - 1, pos.x, (int) pos.getMaxY() + 1, color);
        //y2 y1
        DrawableHelper.fill(matrices, pos.x - 1, (int) pos.getMaxY(), pos.x + 1, pos.y, color);
    }
}
