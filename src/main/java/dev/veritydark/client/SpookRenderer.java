package dev.veritydark.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.veritydark.VerityClientConfig;
import dev.veritydark.VerityDark;
import dev.veritydark.entity.AnomalyEntity;
import dev.veritydark.entity.HallucinationEntity;
import dev.veritydark.entity.StalkerEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

import java.util.function.Supplier;

/**
 * One renderer for every creature of the addon. The model name picks the files:
 * geo/entity/NAME.geo.json, animations/entity/NAME.animation.json, textures/entity/NAME.png (+ NAME_glowmask.png).
 * The opacity makes the creature a little see-through (hallucinations a lot, anomalies slightly).
 */
public class SpookRenderer<T extends Entity & GeoAnimatable> extends GeoEntityRenderer<T> {
    private final Supplier<Double> opacity;

    public SpookRenderer(EntityRendererProvider.Context context, String name, float scale, boolean glow,
                         Supplier<Double> opacity) {
        super(context, new DefaultedEntityGeoModel<>(new ResourceLocation(VerityDark.MODID, name)));
        this.opacity = opacity;
        if (glow) addRenderLayer(new AutoGlowingGeoLayer<>(this));
        withScale(scale);
    }

    public static SpookRenderer<AnomalyEntity> anomaly(EntityRendererProvider.Context ctx) {
        return new SpookRenderer<AnomalyEntity>(ctx, "anomaly", 0.8F, true, VerityClientConfig.ANOMALY_OPACITY::get);
    }

    public static SpookRenderer<StalkerEntity> stalker(EntityRendererProvider.Context ctx) {
        return new SpookRenderer<StalkerEntity>(ctx, "stalker", 0.8F, true, () -> 1.0);
    }

    public static SpookRenderer<HallucinationEntity> hallucination(EntityRendererProvider.Context ctx) {
        // same model as the anomaly, no glow layer (it would ignore the transparency)
        return new SpookRenderer<HallucinationEntity>(ctx, "anomaly", 0.8F, false, VerityClientConfig.HALLUCINATION_OPACITY::get);
    }

    @Override
    public RenderType getRenderType(T animatable, ResourceLocation texture, @Nullable MultiBufferSource bufferSource,
                                    float partialTick) {
        if (this.opacity.get() < 0.995 || animatable instanceof HallucinationEntity) {
            return RenderType.entityTranslucent(texture);
        }
        return super.getRenderType(animatable, texture, bufferSource, partialTick);
    }

    @Override
    public void actuallyRender(PoseStack poseStack, T animatable, BakedGeoModel model, @Nullable RenderType renderType,
                               MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, boolean isReRender,
                               float partialTick, int packedLight, int packedOverlay,
                               float red, float green, float blue, float alpha) {
        float a = alpha * this.opacity.get().floatValue();
        if (animatable instanceof HallucinationEntity h) a *= h.getFade(partialTick);
        super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer, isReRender,
                partialTick, packedLight, packedOverlay, red, green, blue, a);
    }
}
