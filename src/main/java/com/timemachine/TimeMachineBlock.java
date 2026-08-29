package com.timemachine;

import com.mojang.serialization.MapCodec;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

import org.jetbrains.annotations.Nullable;

/**
 * The machine. Right-click it to open the dial.
 *
 * <p>It has no block entity and no state beyond which way it faces: the era and the vault belong
 * to the save, not to this block, so breaking the machine loses you nothing but the way home. It
 * hums and throws sparks whenever the world is anywhere but the present, which is how you spot
 * from across the room that you are not in 2025 any more. That is read straight off {@link Era},
 * which the client already knows, rather than a block property somebody would have to keep in
 * sync with every machine in the world.
 */
public class TimeMachineBlock extends HorizontalFacingBlock {
    public static final MapCodec<TimeMachineBlock> CODEC = createCodec(TimeMachineBlock::new);

    /** How far away the dial still takes your button presses. */
    private static final double REACH = 8.0;

    public TimeMachineBlock(Settings settings) {
        super(settings);
        setDefaultState(getDefaultState().with(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<TimeMachineBlock> getCodec() {
        return CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public @Nullable BlockState getPlacementState(ItemPlacementContext ctx) {
        return getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, BlockRotation rotation) {
        return state.with(FACING, rotation.rotate(state.get(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, BlockMirror mirror) {
        return state.rotate(mirror.getRotation(state.get(FACING)));
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player,
                                 BlockHitResult hit) {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            ServerPlayNetworking.send(serverPlayer, new TimePayloads.OpenDial(pos, Era.index()));
            world.playSound(null, pos, SoundEvents.BLOCK_LEVER_CLICK, SoundCategory.BLOCKS,
                    0.5F, 1.2F);
        }
        // The dial itself opens client-side, off the back of that packet.
        return ActionResult.SUCCESS;
    }

    /** True if {@code player} is close enough to be turning this dial honestly. */
    public static boolean inReach(PlayerEntity player, BlockPos pos) {
        return player.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)
                <= REACH * REACH;
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        if (Era.isPresent()) {
            return;
        }
        double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 1.1;
        double y = pos.getY() + 0.9 + random.nextDouble() * 0.4;
        double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 1.1;
        world.addParticleClient(ParticleTypes.REVERSE_PORTAL, x, y, z, 0.0, 0.03, 0.0);
        if (random.nextInt(12) == 0) {
            world.addParticleClient(ParticleTypes.ELECTRIC_SPARK, x, y, z, 0.0, 0.0, 0.0);
        }
    }
}
