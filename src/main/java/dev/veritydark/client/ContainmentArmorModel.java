package dev.veritydark.client;

import dev.veritydark.VerityDark;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

/**
 * The containment armor shape made in Blockbench (armor_layer_1.bbmodel, 128x64 texture).
 * One model is shared by all four pieces; the slot decides which parts get drawn:
 * helmet = head, chestplate = body + arms, leggings = belt + legs, boots = boots.
 */
public class ContainmentArmorModel extends HumanoidModel<LivingEntity> {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(new ResourceLocation(VerityDark.MODID, "containment_armor"), "main");

    private final ModelPart waist;
    private final ModelPart leftBoot;
    private final ModelPart rightBoot;
    private EquipmentSlot slot = EquipmentSlot.HEAD;

    public ContainmentArmorModel(ModelPart root) {
        super(root);
        this.waist = root.getChild("waist");
        this.leftBoot = root.getChild("left_boot");
        this.rightBoot = root.getChild("right_boot");
    }

    public void setSlot(EquipmentSlot slot) {
        this.slot = slot;
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-4.0F, -9.75F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.75F)), PartPose.ZERO);
        root.addOrReplaceChild("hat", CubeListBuilder.create().texOffs(32, 0)
                .addBox(-4.0F, -10.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(1.0F)), PartPose.ZERO);
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(16, 16)
                .addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.75F)), PartPose.ZERO);

        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(40, 16)
                .addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.75F)), PartPose.offset(-5.0F, 2.0F, 0.0F));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(40, 16).mirror()
                .addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.75F)), PartPose.offset(5.0F, 2.0F, 0.0F));

        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(82, 42)
                .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.9F)), PartPose.offset(-1.9F, 12.0F, 0.0F));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(82, 42).mirror()
                .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.9F)), PartPose.offset(1.9F, 12.0F, 0.0F));

        // extra pieces that follow the body / legs
        root.addOrReplaceChild("waist", CubeListBuilder.create().texOffs(98, 42)
                .addBox(-4.0F, 8.0F, -2.0F, 8.0F, 3.0F, 4.0F, new CubeDeformation(0.9F)), PartPose.ZERO);
        root.addOrReplaceChild("right_boot", CubeListBuilder.create().texOffs(0, 16)
                .addBox(-2.0F, 3.0F, -2.0F, 4.0F, 9.0F, 4.0F, new CubeDeformation(0.75F)), PartPose.offset(-1.9F, 12.0F, 0.0F));
        root.addOrReplaceChild("left_boot", CubeListBuilder.create().texOffs(0, 16).mirror()
                .addBox(-2.0F, 3.0F, -2.0F, 4.0F, 9.0F, 4.0F, new CubeDeformation(0.75F)), PartPose.offset(1.9F, 12.0F, 0.0F));

        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    protected Iterable<ModelPart> headParts() {
        return this.slot == EquipmentSlot.HEAD ? List.of(this.head, this.hat) : List.of();
    }

    @Override
    protected Iterable<ModelPart> bodyParts() {
        return switch (this.slot) {
            case CHEST -> List.of(this.body, this.rightArm, this.leftArm);
            case LEGS -> List.of(this.waist, this.rightLeg, this.leftLeg);
            case FEET -> List.of(this.rightBoot, this.leftBoot);
            default -> List.of();
        };
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int light, int overlay,
                               float r, float g, float b, float a) {
        this.waist.copyFrom(this.body);
        this.leftBoot.copyFrom(this.leftLeg);
        this.rightBoot.copyFrom(this.rightLeg);
        super.renderToBuffer(pose, buffer, light, overlay, r, g, b, a);
    }
}
