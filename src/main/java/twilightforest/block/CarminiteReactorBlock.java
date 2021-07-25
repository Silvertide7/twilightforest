package twilightforest.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import twilightforest.tileentity.*;

import javax.annotation.Nullable;
import java.util.Arrays;

import net.minecraft.world.level.block.state.BlockBehaviour.Properties;

import net.minecraft.core.Direction;

public class CarminiteReactorBlock extends Block {

	public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

	public CarminiteReactorBlock(Properties props) {
		super(props);
		this.registerDefaultState(stateDefinition.any().setValue(ACTIVE, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(ACTIVE);
	}

	@Override
	@Deprecated
	public void neighborChanged(BlockState state, Level world, BlockPos pos, Block blockIn, BlockPos fromPos, boolean isMoving) {
		if (world.isClientSide) return;

		if (!state.getValue(ACTIVE) && isReactorReady(world, pos)) {
			// check if we should fire up the reactor
			world.setBlockAndUpdate(pos, state.setValue(ACTIVE, true));
		}
	}

	/**
	 * Check if the reactor has all the specified things around it
	 */
	private boolean isReactorReady(Level world, BlockPos pos) {
		return Arrays.stream(Direction.values())
				.allMatch(e -> world.getBlockState(pos.relative(e)).getBlock() == Blocks.REDSTONE_BLOCK);
	}

	@Override
	public boolean hasTileEntity(BlockState state) {
		return state.getValue(ACTIVE);
	}

	@Nullable
	@Override
	public BlockEntity createTileEntity(BlockState state, BlockGetter world) {
		return hasTileEntity(state) ? new ActiveCarminiteReactorTileEntity() : null;
	}
}