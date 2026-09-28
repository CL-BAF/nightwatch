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
 * The Watcher — a PART of the Unraveller pushing through (canon v2).
 * ~2.9 blocks tall, 0.6 wide. Reads as a FRAGMENT, never a whole creature:
 * mismatched limbs (left arm longer, right leg shorter), blank face-plates
 * that REPEAT across the form (head, chest, thigh — featureless ovoids),
 * thread-strand edges (#dddde6) cut off mid-air at shoulder and ankle.
 * Source: models/src/watcher.bbmodel. Texture: textures/entity/watcher.png.
 * Animation is code-driven (no GeckoLib) with per-part DESYNC phase offsets;
 * seen-freeze via setSeen() (Fabric calls when the player looks at it).
 */
public final class WatcherModel extends EntityModel<LivingEntityRenderState> {
    public static final ModelLayerLocation LAYER =
        new ModelLayerLocation(Identifier.fromNamespaceAndPath("nightwatch", "watcher"), "main");

    private final ModelPart torso;
    private final ModelPart head;
    private final ModelPart armLeft;
    private final ModelPart armRight;
    private final ModelPart threadShoulder;
    private final ModelPart threadAnkle;
    private final ModelPart legLeft;
    private final ModelPart legRight;
    private boolean seen;
    private float targetForward;
    private float targetStrafe;
    private float lagForward;
    private float lagStrafe;

    public WatcherModel(ModelPart root) {
        super(root);
        this.torso = root.getChild("torso");
        this.head = this.torso.getChild("head");
        this.armLeft = this.torso.getChild("arm_left");
        this.armRight = this.torso.getChild("arm_right");
        this.threadShoulder = this.torso.getChild("thread_shoulder");
        this.legLeft = root.getChild("leg_left");
        this.legRight = root.getChild("leg_right");
        this.threadAnkle = this.legRight.getChild("thread_ankle");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition torso = root.addOrReplaceChild("torso",
            CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-5.0F, 0.0F, -3.0F, 10.0F, 14.0F, 6.0F)
                // Repeat motif: blank chest face-plate (static merged geometry).
                .texOffs(24, 20)
                .addBox(-2.0F, 4.0F, -3.5F, 4.0F, 5.0F, 0.5F),
            PartPose.offset(0.0F, 8.0F, 0.0F));

        torso.addOrReplaceChild("head",
            CubeListBuilder.create()
                .texOffs(0, 20)
                .addBox(-3.0F, -6.0F, -3.0F, 6.0F, 6.0F, 6.0F),
            PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.14F, 0.0F, 0.06F));

        // Mismatched arms: left reaches the knees, right stops at the hip.
        torso.addOrReplaceChild("arm_left",
            CubeListBuilder.create()
                .texOffs(32, 0)
                .addBox(-1.5F, 0.0F, -1.5F, 3.0F, 21.0F, 3.0F),
            PartPose.offsetAndRotation(-6.5F, 12.0F, 0.0F, 0.0F, 0.0F, 0.05F));

        torso.addOrReplaceChild("arm_right",
            CubeListBuilder.create()
                .texOffs(32, 0)
                .addBox(-1.5F, 0.0F, -1.5F, 3.0F, 17.0F, 3.0F),
            PartPose.offsetAndRotation(6.5F, 12.0F, 0.0F, 0.0F, 0.0F, -0.09F));

        // Thread continuity: strand cut off mid-air above the right shoulder.
        torso.addOrReplaceChild("thread_shoulder",
            CubeListBuilder.create()
                .texOffs(48, 32)
                .addBox(-0.5F, -8.0F, -0.5F, 1.0F, 8.0F, 1.0F),
            PartPose.offset(6.5F, 12.0F, 0.0F));

        // Mismatched legs: left full-length, right shorter with higher pivot (feet level).
        root.addOrReplaceChild("leg_left",
            CubeListBuilder.create()
                .texOffs(44, 0)
                .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 22.0F, 4.0F)
                // Repeat motif: blank thigh face-plate (static merged geometry).
                .texOffs(24, 28)
                .addBox(-1.5F, 6.0F, -2.5F, 3.0F, 4.0F, 0.5F),
            PartPose.offset(-2.0F, 2.0F, 0.0F));

        PartDefinition legRight = root.addOrReplaceChild("leg_right",
            CubeListBuilder.create()
                .texOffs(44, 0)
                .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 19.0F, 4.0F),
            PartPose.offset(2.0F, 5.0F, 0.0F));

        // Thread continuity: strand cut off mid-air below the right ankle.
        legRight.addOrReplaceChild("thread_ankle",
            CubeListBuilder.create()
                .texOffs(52, 32)
                .addBox(-0.5F, 19.0F, -0.5F, 1.0F, 7.0F, 1.0F),
            PartPose.ZERO);

        return LayerDefinition.create(mesh, 64, 64);
    }

    /** Called by the renderer/effects each tick: true once the player looks at it. */
    public void setSeen(boolean seen) {
        this.seen = seen;
    }

    /**
     * Velocity feed for lagged follow-through (canon: movements out of sync).
     * Fabric calls this from extractRenderState; may be omitted (defaults to 0).
     */
    public void setMotion(float forward, float strafe) {
        this.targetForward = forward;
        this.targetStrafe = strafe;
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        super.setupAnim(state);
        if (this.seen) {
            return;
        }
        // Canon desync: each part drifts on its own phase so the whole never
        // moves as one creature. Amplitudes stay subtle (distant read).
        float t = state.ageInTicks * 0.5F;
        this.torso.xRot = (float) Math.sin(t) * 0.02F;
        this.head.yRot = (float) Math.sin(t * 0.3F + 0.6F) * 0.05F;
        this.armLeft.xRot = (float) Math.sin(t + 1.0F) * 0.03F;
        this.armRight.xRot = (float) Math.sin(t * 0.9F + 2.1F) * 0.025F;
        this.legLeft.xRot = (float) Math.sin(t * 0.7F + 0.3F) * 0.015F;
        this.legRight.xRot = (float) Math.sin(t * 0.8F + 1.7F) * 0.015F;
        this.threadShoulder.xRot = (float) Math.sin(t * 1.3F + 2.6F) * 0.06F;
        this.threadAnkle.xRot = (float) Math.sin(t * 1.1F + 0.9F) * 0.06F;
        // Lagged follow-through: the form trails its own motion, eased, so it
        // never moves as one creature. Arms lag most (loose ends trail).
        this.lagForward += (this.targetForward - this.lagForward) * 0.12F;
        this.lagStrafe += (this.targetStrafe - this.lagStrafe) * 0.12F;
        this.torso.xRot += this.lagForward * 0.15F;
        this.torso.zRot = this.lagStrafe * 0.1F;
        this.armLeft.xRot += this.lagForward * 0.25F;
        this.armRight.xRot += this.lagForward * 0.2F;
        this.head.yRot += this.lagStrafe * 0.2F;
    }
}
