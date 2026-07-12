package com.cobblemon.mod.block;

import com.mojang.serialization.MapCodec;

import com.cobblemon.mod.network.OpenPcPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Placeable Cobblemon PC — tall terminal that opens mon storage.
 */
public class PcBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<PcBlock> CODEC = simpleCodec(PcBlock::new);

    /** Cobblemon PC mesh is ~2 blocks tall (elements up to y≈31). */
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(1, 0, 1, 15, 16, 15),
            Block.box(2, 16, 2, 14, 31, 14)
    );

    public PcBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public MapCodec<? extends PcBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            // Heal party lightly when using PC (Cobblemon PC heal box vibe)
            com.cobblemon.mod.party.PartyHelper.healAll(serverPlayer);
            PacketDistributor.sendToPlayer(serverPlayer, new OpenPcPayload());
            serverPlayer.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "PC booted — party healed. Manage storage in the PC screen."));
        }
        return InteractionResult.SUCCESS;
    }
}
