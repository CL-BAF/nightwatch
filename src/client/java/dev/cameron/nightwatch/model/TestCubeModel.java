package dev.cameron.nightwatch.model;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

/**
 * Minimal compile-test model: a single 16x16x16 cube.
 * Proves the Blockbench Java-Entity shape (MeshDefinition/CubeListBuilder/
 * PartPose/LayerDefinition + EntityModel subclass) compiles on 26.3.
 * Source: models/src/test_cube.bbmodel. Texture: textures/entity/test_cube.png.
 */
public final class TestCubeModel extends EntityModel<LivingEntityRenderState> {
    public static final ModelLayerLocation LAYER =
        new ModelLayerLocation(Identifier.fromNamespaceAndPath("nightwatch", "test_cube"), "main");

    public TestCubeModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("cube",
            CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-8.0F, -8.0F, -8.0F, 16.0F, 16.0F, 16.0F),
            PartPose.offset(0.0F, 8.0F, 0.0F));
        return LayerDefinition.create(mesh, 16, 16);
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        super.setupAnim(state);
    }
}
