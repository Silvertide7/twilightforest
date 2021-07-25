package twilightforest.entity;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import twilightforest.TFSounds;
import twilightforest.entity.ai.BreathAttackGoal;
import twilightforest.util.TFDamageSources;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;

public class FireBeetleEntity extends Monster implements IBreathAttacker {

	private static final EntityDataAccessor<Boolean> BREATHING = SynchedEntityData.defineId(FireBeetleEntity.class, EntityDataSerializers.BOOLEAN);
	private static final int BREATH_DURATION = 10;
	private static final int BREATH_DAMAGE = 2;

	public FireBeetleEntity(EntityType<? extends FireBeetleEntity> type, Level world) {
		super(type, world);
		this.fireImmune();
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(2, new BreathAttackGoal<>(this, 5F, 30, 0.1F));
		this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0F, false));
		this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 1.0D));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();
		entityData.define(BREATHING, false);
	}

	public static AttributeSupplier.Builder registerAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 25.0D)
				.add(Attributes.MOVEMENT_SPEED, 0.23D)
				.add(Attributes.ATTACK_DAMAGE, 4.0D);
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return TFSounds.FIRE_BEETLE_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return TFSounds.FIRE_BEETLE_DEATH;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState block) {
		playSound(TFSounds.FIRE_BEETLE_STEP, 0.15F, 1.0F);
	}

	@Override
	public boolean isBreathing() {
		return entityData.get(BREATHING);
	}

	@Override
	public void setBreathing(boolean flag) {
		entityData.set(BREATHING, flag);
	}

	@Override
	public void aiStep() {
		super.aiStep();

		// when breathing fire, spew particles
		if (isBreathing()) {
			Vec3 look = this.getLookAngle();

			double dist = 0.9;
			double px = this.getX() + look.x * dist;
			double py = this.getY() + 0.25 + look.y * dist;
			double pz = this.getZ() + look.z * dist;

			for (int i = 0; i < 2; i++) {
				double dx = look.x;
				double dy = look.y;
				double dz = look.z;

				double spread = 5 + this.getRandom().nextDouble() * 2.5;
				double velocity = 0.15 + this.getRandom().nextDouble() * 0.15;

				// spread flame
				dx += this.getRandom().nextGaussian() * 0.007499999832361937D * spread;
				dy += this.getRandom().nextGaussian() * 0.007499999832361937D * spread;
				dz += this.getRandom().nextGaussian() * 0.007499999832361937D * spread;
				dx *= velocity;
				dy *= velocity;
				dz *= velocity;

				level.addParticle(ParticleTypes.FLAME, px, py, pz, dx, dy, dz);
			}

			playSound(TFSounds.FIRE_BEETLE_SHOOT, random.nextFloat() * 0.5F, random.nextFloat() * 0.5F);
		}
	}

	@Override
	public float getBrightness() {
		if (isBreathing()) {
			return 15728880;
		} else {
			return super.getBrightness();
		}
	}

	@Override
	public int getMaxHeadXRot() {
		return 500;
	}

	@Override
	public float getEyeHeight(Pose pose) {
		return this.getBbHeight() * 0.6F;
	}

	@Override
	public MobType getMobType() {
		return MobType.ARTHROPOD;
	}

	@Override
	public void doBreathAttack(Entity target) {
		if (!target.fireImmune() && target.hurt(TFDamageSources.SCORCHED(this), BREATH_DAMAGE)) {
			target.setSecondsOnFire(BREATH_DURATION);
		}
	}

	@Override
	public boolean doHurtTarget(Entity entityIn) {
		if (isBreathing()) {
			entityIn.hurt(TFDamageSources.SCORCHED(this), BREATH_DAMAGE);
		}
		return super.doHurtTarget(entityIn);
	}
}