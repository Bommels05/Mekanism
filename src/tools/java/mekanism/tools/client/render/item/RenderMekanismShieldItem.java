package mekanism.tools.client.render.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Objects;
import mekanism.client.render.item.MekanismISTER;
import mekanism.common.Mekanism;
import mekanism.common.util.RegistryUtils;
import mekanism.tools.client.ShieldTextures;
import mekanism.tools.common.registries.ToolsItems;
import net.minecraft.client.model.ShieldModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BannerRenderer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import org.jetbrains.annotations.NotNull;

public class RenderMekanismShieldItem extends MekanismISTER {

    public static final RenderMekanismShieldItem RENDERER = new RenderMekanismShieldItem();

    private ShieldModel shieldModel;

    @Override
    public void onResourceManagerReload(@NotNull ResourceManager resourceManager) {
        shieldModel = new ShieldModel(getEntityModels().bakeLayer(ModelLayers.SHIELD));
    }

    @Override
    public void renderByItem(@NotNull ItemStack stack, @NotNull ItemDisplayContext displayContext, @NotNull PoseStack matrix, @NotNull MultiBufferSource renderer,
          int light, int overlayLight) {
        Item item = stack.getItem();
        ShieldTextures textures;
        if (item == ToolsItems.BRONZE_SHIELD.asItem()) {
            textures = ShieldTextures.BRONZE;
        } else if (item == ToolsItems.LAPIS_LAZULI_SHIELD.asItem()) {
            textures = ShieldTextures.LAPIS_LAZULI;
        } else if (item == ToolsItems.OSMIUM_SHIELD.asItem()) {
            textures = ShieldTextures.OSMIUM;
        } else if (item == ToolsItems.REFINED_GLOWSTONE_SHIELD.asItem()) {
            textures = ShieldTextures.REFINED_GLOWSTONE;
        } else if (item == ToolsItems.REFINED_OBSIDIAN_SHIELD.asItem()) {
            textures = ShieldTextures.REFINED_OBSIDIAN;
        } else if (item == ToolsItems.STEEL_SHIELD.asItem()) {
            textures = ShieldTextures.STEEL;
        } else {
            Mekanism.logger.warn("Unknown item for mekanism shield renderer: {}", RegistryUtils.getName(item));
            return;
        }
        Material material = textures.getBase();
        matrix.pushPose();
        matrix.scale(1, -1, -1);
        VertexConsumer buffer = material.sprite().wrap(ItemRenderer.getFoilBufferDirect(renderer, shieldModel.renderType(material.atlasLocation()), true, stack.hasFoil()));
        BannerPatternLayers bannerPattern = stack.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY);
        DyeColor color = stack.get(DataComponents.BASE_COLOR);
        if (!bannerPattern.layers().isEmpty() || color != null) {
            shieldModel.handle().render(matrix, buffer, light, overlayLight, 1, 1, 1, 1);
            BannerRenderer.renderPatterns(matrix, renderer, light, overlayLight, shieldModel.plate(), material, false,
                  Objects.requireNonNullElse(color, DyeColor.WHITE), bannerPattern, stack.hasFoil());
        } else {
            shieldModel.renderToBuffer(matrix, buffer, light, overlayLight, 1, 1, 1, 1);
        }
        matrix.popPose();
    }
}