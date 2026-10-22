package dev.automobilityfix.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.automobilityfix.FixConfig;
import dev.automobilityfix.MenuTracker;
import dev.automobilityfix.duck.AutomobileDuck;
import dev.automobilityfix.duck.DisplacementDuck;
import io.github.foundationgames.automobility.automobile.attachment.RearAttachmentType;
import io.github.foundationgames.automobility.automobile.attachment.rear.RearAttachment;
import io.github.foundationgames.automobility.entity.AutomobileEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(AutomobileEntity.class)
public abstract class AutomobileEntityMixin implements AutomobileDuck {
    @Unique
    private static final String OWNER_KEY = "automobility_fix:owner";
    @Unique
    private static final float WHEEL_ANGLE_WRAP = 36000;

    @Shadow private RearAttachment rearAttachment;
    @Shadow private float engineSpeed;
    @Shadow private float boostSpeed;
    @Shadow private float boostPower;
    @Shadow private int boostTimer;
    @Shadow private float hSpeed;
    @Shadow private float vSpeed;
    @Shadow private float angularSpeed;
    @Shadow private float steering;
    @Shadow private float wheelAngle;
    @Shadow private float lastWheelAngle;
    @Shadow private Vec3 addedVelocity;
    @Shadow private Vec3 lastVelocity;
    @Shadow @Final private AutomobileEntity.Displacement displacement;

    @Unique
    private @Nullable UUID automobilityfix$owner;

    @Unique
    private AutomobileEntity automobilityfix$self() {
        return (AutomobileEntity) (Object) this;
    }

    // ---------------------------------------------------------------- ownership

    @Override
    public @Nullable UUID automobilityfix$getOwner() {
        return this.automobilityfix$owner;
    }

    @Override
    public void automobilityfix$setOwner(@Nullable UUID owner) {
        this.automobilityfix$owner = owner;
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void automobilityfix$saveOwner(CompoundTag nbt, CallbackInfo ci) {
        if (this.automobilityfix$owner != null) {
            nbt.putUUID(OWNER_KEY, this.automobilityfix$owner);
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void automobilityfix$loadOwner(CompoundTag nbt, CallbackInfo ci) {
        this.automobilityfix$owner = nbt.hasUUID(OWNER_KEY) ? nbt.getUUID(OWNER_KEY) : null;
    }

    // ---------------------------------------------------------------- menus / duplication

    @Inject(method = "openInventory", at = @At("TAIL"))
    private void automobilityfix$trackMenu(Player player, CallbackInfo ci) {
        MenuTracker.track(player, this.automobilityfix$self());
    }

    /** Close trunk/attachment menus before the old attachment drops its items, otherwise they could be taken twice. */
    @Inject(method = "setRearAttachment", at = @At("HEAD"))
    private void automobilityfix$closeMenusBeforeAttachmentChange(RearAttachmentType<?> type, CallbackInfo ci) {
        if (type != null && this.rearAttachment != null && this.rearAttachment.type != type &&
                !this.automobilityfix$self().level().isClientSide()) {
            MenuTracker.closeViewers(this.automobilityfix$self());
        }
    }

    // ---------------------------------------------------------------- physics

    /** Automobility rounds towards zero here, which reads the wrong block at negative coordinates. */
    @WrapOperation(method = "movementTick", at = @At(value = "NEW", target = "(III)Lnet/minecraft/core/BlockPos;", ordinal = 0))
    private BlockPos automobilityfix$floorFrictionPos(int x, int y, int z, Operation<BlockPos> original) {
        var self = this.automobilityfix$self();
        return BlockPos.containing(self.getX(), self.getY() - 0.05, self.getZ());
    }

    @WrapOperation(method = "movementTick", at = @At(value = "NEW", target = "(III)Lnet/minecraft/core/BlockPos;", ordinal = 1))
    private BlockPos automobilityfix$floorSlopePos(int x, int y, int z, Operation<BlockPos> original) {
        var self = this.automobilityfix$self();
        return BlockPos.containing(self.getX(), self.getY() - 0.51, self.getZ());
    }

    /**
     * {@code slopeStickingTimer = Math.max(0, slopeStickingTimer--)} never decrements, so after touching one sticky
     * slope the vehicle was pulled into the ground forever, which caused random launches on steps and edges.
     */
    @ModifyArg(method = "movementTick", at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(II)I"), index = 1)
    private int automobilityfix$decrementSlopeTimer(int value) {
        return value - 1;
    }

    /** Overlapping vehicles accumulated the squared overlap every tick and were flung away. */
    @Inject(method = "receiveVehicleCollisions", at = @At("TAIL"))
    private void automobilityfix$limitCollisionImpulse(CallbackInfo ci) {
        if (FixConfig.get(FixConfig.PHYSICS_SANITY)) {
            this.addedVelocity = automobilityfix$limit(this.addedVelocity, FixConfig.get(FixConfig.MAX_COLLISION_IMPULSE));
        }
    }

    /** Do not drive into chunks that are not loaded (vehicle would fall through the world) or, without a player, not ticking. */
    @Inject(method = "move", at = @At("HEAD"), cancellable = true)
    private void automobilityfix$guardUnloadedChunks(MoverType type, Vec3 movement, CallbackInfo ci) {
        if (type != MoverType.SELF || !FixConfig.get(FixConfig.GUARD_UNLOADED_CHUNKS)) {
            return;
        }

        var self = this.automobilityfix$self();
        var level = self.level();
        var target = self.position().add(movement);
        var horizontal = new Vec3(movement.x, 0, movement.z);
        if (horizontal.lengthSqr() > 1.0E-6) {
            target = target.add(horizontal.normalize().scale(self.getBbWidth() * 0.5 + 1));
        }

        var targetPos = BlockPos.containing(target);
        boolean safe = level.hasChunkAt(targetPos);
        if (safe && level instanceof ServerLevel serverLevel && !(self.getControllingPassenger() instanceof Player)) {
            safe = serverLevel.isPositionEntityTicking(targetPos);
        }

        if (!safe) {
            this.automobilityfix$stopMotion();
            ci.cancel();
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void automobilityfix$afterTick(CallbackInfo ci) {
        var self = this.automobilityfix$self();

        if (FixConfig.get(FixConfig.PHYSICS_SANITY)) {
            this.automobilityfix$sanitize(self);
        }

        this.automobilityfix$syncHitboxes();
    }

    @Unique
    private void automobilityfix$sanitize(AutomobileEntity self) {
        if (!Float.isFinite(this.engineSpeed)) this.engineSpeed = 0;
        if (!Float.isFinite(this.boostSpeed)) this.boostSpeed = 0;
        if (!Float.isFinite(this.boostPower)) this.boostPower = 0;
        if (!Float.isFinite(this.hSpeed)) this.hSpeed = 0;
        if (!Float.isFinite(this.vSpeed)) this.vSpeed = 0;
        if (!Float.isFinite(this.angularSpeed)) this.angularSpeed = 0;
        if (!Float.isFinite(this.steering)) this.steering = 0;

        if (!Float.isFinite(this.wheelAngle) || !Float.isFinite(this.lastWheelAngle)) {
            this.wheelAngle = this.lastWheelAngle = 0;
        } else if (Math.abs(this.wheelAngle) > WHEEL_ANGLE_WRAP) {
            // Keeps float precision so wheels do not visually freeze after long drives
            float shift = this.wheelAngle - (this.wheelAngle % 360);
            this.wheelAngle -= shift;
            this.lastWheelAngle -= shift;
        }

        this.addedVelocity = automobilityfix$limit(this.addedVelocity, FixConfig.get(FixConfig.MAX_ADDED_VELOCITY));
        if (!automobilityfix$isFinite(this.lastVelocity)) this.lastVelocity = Vec3.ZERO;
        if (!automobilityfix$isFinite(self.getDeltaMovement())) self.setDeltaMovement(Vec3.ZERO);

        if (!automobilityfix$isFinite(self.position())) {
            if (Double.isFinite(self.xOld) && Double.isFinite(self.yOld) && Double.isFinite(self.zOld)) {
                self.setPos(self.xOld, self.yOld, self.zOld);
            }
            this.automobilityfix$stopMotion();
        }
        if (!Float.isFinite(self.getYRot())) {
            self.setYRot(0);
        }

        ((DisplacementDuck) (Object) this.displacement).automobilityfix$sanitize(self.getY());
    }

    @Override
    public void automobilityfix$stopMotion() {
        this.engineSpeed = 0;
        this.boostSpeed = 0;
        this.boostTimer = 0;
        this.hSpeed = 0;
        this.vSpeed = 0;
        this.angularSpeed = 0;
        this.addedVelocity = Vec3.ZERO;
        this.lastVelocity = Vec3.ZERO;
        this.automobilityfix$self().setDeltaMovement(Vec3.ZERO);
    }

    /**
     * Hitboxes used to position themselves only in their own tick. In chunks where they did not tick, or before the
     * client linked them to the vehicle, they stayed behind: the vehicle could not be entered and an invisible
     * obstacle remained.
     */
    @Override
    public void automobilityfix$syncHitboxes() {
        var self = this.automobilityfix$self();
        if (self.isRemoved() || !automobilityfix$isFinite(self.position())) {
            return;
        }

        for (var hitbox : self.hitboxes) {
            if (hitbox.isRemoved()) {
                continue;
            }

            Vector3d pos = hitbox.boxOrigin();
            self.localPosToWorldSpace(pos);
            if (Double.isFinite(pos.x()) && Double.isFinite(pos.y()) && Double.isFinite(pos.z())) {
                hitbox.setPos(pos.x(), pos.y() - hitbox.height() * 0.5, pos.z());
            }
        }
    }

    @Unique
    private static boolean automobilityfix$isFinite(Vec3 vec) {
        return Double.isFinite(vec.x) && Double.isFinite(vec.y) && Double.isFinite(vec.z);
    }

    @Unique
    private static Vec3 automobilityfix$limit(Vec3 vec, double max) {
        if (!automobilityfix$isFinite(vec)) {
            return Vec3.ZERO;
        }

        double length = vec.length();
        return length > max ? vec.scale(max / length) : vec;
    }
}
