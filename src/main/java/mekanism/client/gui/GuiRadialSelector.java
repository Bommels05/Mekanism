package mekanism.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import mekanism.client.render.MekanismRenderer;
import mekanism.common.item.interfaces.IRadialSelectorEnum;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldVertexBufferUploader;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.text.StringTextComponent;
import org.lwjgl.opengl.GL11;

public class GuiRadialSelector<TYPE extends Enum<TYPE> & IRadialSelectorEnum<TYPE>> extends Screen {

    private static final float DRAWS = 300;

    private static final float INNER = 40, OUTER = 100;
    private static final float SELECT_RADIUS = 10;

    private final Minecraft minecraft = Minecraft.getInstance();
    private final Class<TYPE> enumClass;
    private final TYPE[] types;
    private final Supplier<TYPE> curSupplier;
    private final Consumer<TYPE> changeHandler;

    private TYPE selection = null;

    public GuiRadialSelector(Class<TYPE> enumClass, Supplier<TYPE> curSupplier, Consumer<TYPE> changeHandler) {
        super(new StringTextComponent("Radial Selector Screen"));
        this.enumClass = enumClass;
        this.curSupplier = curSupplier;
        this.changeHandler = changeHandler;
        types = enumClass.getEnumConstants();
    }

    @Override
    public void render(@Nonnull MatrixStack matrix, int mouseX, int mouseY, float partialTick) {
        // center of screen
        float centerX = minecraft.getMainWindow().getScaledWidth() / 2F;
        float centerY = minecraft.getMainWindow().getScaledHeight() / 2F;

        matrix.push();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.defaultAlphaFunc();
        matrix.translate(centerX, centerY, 0);
        RenderSystem.disableTexture();

        // draw base
        RenderSystem.color4f(0.3F, 0.3F, 0.3F, 0.5F);
        drawTorus(matrix, 0, 360);

        TYPE cur = curSupplier.get();
        if (cur != null) {
            // draw current selected
            if (cur.getColor() == null) {
                RenderSystem.color4f(0.4F, 0.4F, 0.4F, 0.7F);
            } else {
                MekanismRenderer.color(cur.getColor(), 0.3F);
            }
            drawTorus(matrix, -90F + 360F * (-0.5F + cur.ordinal()) / types.length, 360F / types.length);

            double xDiff = mouseX - centerX;
            double yDiff = mouseY - centerY;
            if (Math.sqrt(xDiff * xDiff + yDiff * yDiff) >= SELECT_RADIUS) {
                // draw mouse selection highlight
                float angle = (float) Math.toDegrees(Math.atan2(yDiff, xDiff));
                RenderSystem.color4f(0.8F, 0.8F, 0.8F, 0.3F);
                drawTorus(matrix, 360F * (-0.5F / types.length) + angle, 360F / types.length);

                float selectionAngle = angle + 90F + (360F * (0.5F / types.length));
                while (selectionAngle < 0) {
                    selectionAngle += 360F;
                }
                selection = types[(int) (selectionAngle * (types.length / 360F))];
                // draw hovered selection
                RenderSystem.color4f(0.6F, 0.6F, 0.6F, 0.7F);
                drawTorus(matrix, -90F + 360F * (-0.5F + selection.ordinal()) / types.length, 360F / types.length);
            } else {
                selection = null;
            }
        }

        MekanismRenderer.resetColor();

        RenderSystem.enableTexture();
        for (int i = 0; i < types.length; i++) {
            double angle = Math.toRadians(270 + 360 * ((float) i / types.length));
            float x = (float) Math.cos(angle) * (INNER + OUTER) / 2F;
            float y = (float) Math.sin(angle) * (INNER + OUTER) / 2F;
            // draw icon
            minecraft.textureManager.bindTexture(types[i].getIcon());
            blit(matrix, Math.round(x - 12), Math.round(y - 20), 24, 24, 0, 0, 18, 18, 18, 18);
            // draw label
            matrix.push();
            int width = minecraft.fontRenderer.getStringWidth(types[i].getShortText().getString());
            matrix.translate(x, y, 0);
            matrix.scale(0.6F, 0.6F, 0.6F);
            minecraft.fontRenderer.func_238422_b_(matrix, types[i].getShortText(), -width / 2F, 8, 0xCCFFFFFF);
            matrix.pop();
        }

        MekanismRenderer.resetColor();
        matrix.pop();
    }

    @Override
    public void removed() {
        updateSelection();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // handle & ignore all key events
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        updateSelection();
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void drawTorus(MatrixStack matrix, float startAngle, float sizeAngle) {
        BufferBuilder vertexBuffer = Tessellator.getInstance().getBuffer();
        Matrix4f matrix4f = matrix.getLast().getMatrix();
        vertexBuffer.begin(GL11.GL_QUAD_STRIP, DefaultVertexFormats.POSITION);
        float draws = DRAWS * (sizeAngle / 360F);
        for (int i = 0; i <= draws; i++) {
            float angle = (float) Math.toRadians(startAngle + (i / DRAWS) * 360);
            vertexBuffer.pos(matrix4f, (float) (OUTER * Math.cos(angle)), (float) (OUTER * Math.sin(angle)), 0).endVertex();
            vertexBuffer.pos(matrix4f, (float) (INNER * Math.cos(angle)), (float) (INNER * Math.sin(angle)), 0).endVertex();
        }
        vertexBuffer.finishDrawing();
        WorldVertexBufferUploader.draw(vertexBuffer);
    }

    public void updateSelection() {
        if (selection != null) {
            changeHandler.accept(selection);
        }
    }

    public Class<TYPE> getEnumClass() {
        return enumClass;
    }
}
