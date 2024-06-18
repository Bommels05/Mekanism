package mekanism.client.render.obj;

import java.util.function.Function;
import appeng.client.render.cablebus.CableBusModel;
import appeng.client.render.cablebus.FacadeBuilder;
import mekanism.common.Mekanism;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;
import net.neoforged.neoforge.client.model.geometry.IUnbakedGeometry;
import net.neoforged.neoforge.client.model.obj.ObjModel;
import org.jetbrains.annotations.Nullable;

public class TransmitterModel implements IUnbakedGeometry<TransmitterModel> {

    private final ObjModel internal;
    @Nullable
    private final ObjModel glass;

    public TransmitterModel(ObjModel internalModel, @Nullable ObjModel glass) {
        this.internal = internalModel;
        this.glass = glass;
    }

    @Override
    public BakedModel bake(IGeometryBakingContext owner, ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelTransform,
          ItemOverrides overrides) {
        if (Mekanism.hooks.AE2Loaded) {
            BakedModel facadeModel = baker.bake(CableBusModel.TRANSLUCENT_FACADE_MODEL, modelTransform, spriteGetter);
            FacadeBuilder facadeBuilder = new FacadeBuilder(baker, facadeModel);
            return new TransmitterBakedModel(internal, glass, owner, baker, spriteGetter, modelTransform, overrides, facadeBuilder);
        } else {
            return new TransmitterBakedModel(internal, glass, owner, baker, spriteGetter, modelTransform, overrides, null);
        }
    }

    @Override
    public void resolveParents(Function<ResourceLocation, UnbakedModel> modelGetter, IGeometryBakingContext context) {
        internal.resolveParents(modelGetter, context);
        if (glass != null) {
            glass.resolveParents(modelGetter, context);
        }
    }
}