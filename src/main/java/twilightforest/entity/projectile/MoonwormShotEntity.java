package twilightforest.entity.projectile;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.context.DirectionalPlaceContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.damagesource.IndirectEntityDamageSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import twilightforest.block.TFBlocks;
import twilightforest.entity.TFEntities;

public class MoonwormShotEntity extends TFThrowableEntity {

	public MoonwormShotEntity(EntityType<? extends MoonwormShotEntity> type, Level world) {
		super(type, world);
	}

	public MoonwormShotEntity(EntityType<? extends MoonwormShotEntity> type, Level world, LivingEntity thrower) {
		super(type, world, thrower);
		shootFromRotation(thrower, thrower.xRot, thrower.yRot, 0F, 1.5F, 1.0F);
	}
	public MoonwormShotEntity(Level worldIn, double x, double y, double z) {
		super(TFEntities.moonworm_shot, worldIn, x, y, z);
	}


	@Override
	public void tick() {
		super.tick();
		makeTrail();
	}

	@Override
	public float getBrightness() {
		return 1.0F;
	}

	private void makeTrail() {
//		for (int i = 0; i < 5; i++) {
//			double dx = posX + 0.5 * (rand.nextDouble() - rand.nextDouble()); 
//			double dy = posY + 0.5 * (rand.nextDouble() - rand.nextDouble()); 
//			double dz = posZ + 0.5 * (rand.nextDouble() - rand.nextDouble()); 
//			
//			double s1 = ((rand.nextFloat() * 0.5F) + 0.5F) * 0.17F;
//			double s2 = ((rand.nextFloat() * 0.5F) + 0.5F) * 0.80F;
//			double s3 = ((rand.nextFloat() * 0.5F) + 0.5F) * 0.69F;
//
//			world.spawnParticle("mobSpell", dx, dy, dz, s1, s2, s3);
//		}
	}

	@Override
	public boolean isPickable() {
		return true;
	}

	@Override
	public float getPickRadius() {
		return 1.0F;
	}

	@Override
	protected float getGravity() {
		return 0.03F;
	}

	@OnlyIn(Dist.CLIENT)
	@Override
	public void handleEntityEvent(byte id) {
		if (id == 3) {
			for (int i = 0; i < 8; ++i) {
				this.level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, TFBlocks.moonworm.get().defaultBlockState()), false, this.getX(), this.getY(), this.getZ(), 0.0D, 0.0D, 0.0D);
			}
		} else {
			super.handleEntityEvent(id);
		}
	}

	@Override
	protected void onHit(HitResult ray) {
		if (!level.isClientSide) {
			if (ray instanceof BlockHitResult) {
				BlockHitResult blockray = (BlockHitResult) ray;
				BlockPos pos = blockray.getBlockPos().relative(blockray.getDirection());
				BlockState currentState = level.getBlockState(pos);

				DirectionalPlaceContext context = new DirectionalPlaceContext(level, pos, blockray.getDirection(), ItemStack.EMPTY, blockray.getDirection().getOpposite());
				if (currentState.canBeReplaced(context)) {
					level.setBlockAndUpdate(pos, TFBlocks.moonworm.get().defaultBlockState().setValue(DirectionalBlock.FACING, ((BlockHitResult) ray).getDirection()));
					// todo sound
				} else {
					ItemEntity squish = new ItemEntity(level, pos.getX(), pos.getY(), pos.getZ());
					squish.spawnAtLocation(Items.LIME_DYE);
				}
			}

			if (ray instanceof EntityHitResult) {
				if (((EntityHitResult)ray).getEntity() != null) {
					((EntityHitResult)ray).getEntity().hurt(new IndirectEntityDamageSource("moonworm", this, this), random.nextInt(3) == 0 ? 1 : 0);
				}
			}

			this.level.broadcastEntityEvent(this, (byte) 3);
			this.remove();
		}
	}
}