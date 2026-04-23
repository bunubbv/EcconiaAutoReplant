package de.ecconia.fabric.autoreplant;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;

public class AutoReplant implements ClientModInitializer
{
	@Override
	public void onInitializeClient()
	{
		Minecraft client = Minecraft.getInstance();
		UseBlockCallback.EVENT.register((Player player, Level world, InteractionHand _, BlockHitResult hit) -> {
			BlockPos pos = hit.getBlockPos();
			BlockState state = world.getBlockState(hit.getBlockPos());
			Block block = state.getBlock();
			
			if(client.gameMode == null || client.player == null)
			{
				return InteractionResult.PASS;
			}
			
			if(block instanceof NetherWartBlock netherWarts)
			{
				if(!netherWarts.isRandomlyTicking(state))
				{
					Item seed = Items.NETHER_WART;
					return doTheHarvestThing(client, hit, player, pos, seed);
				}
			}
			else if(block instanceof CropBlock cropBlock)
			{
				if(cropBlock.isMaxAge(state))
				{
					Item seed = cropBlock.getBaseSeedId().asItem();
					return doTheHarvestThing(client, hit, player, pos, seed);
				}
			}
			return InteractionResult.PASS;
		});
	}
	
	private InteractionResult doTheHarvestThing(Minecraft client, BlockHitResult hit, Player player, BlockPos pos, Item seed)
	{
		if(!client.gameMode.startDestroyBlock(hit.getBlockPos(), hit.getDirection()))
		{
			player.sendSystemMessage(Component.literal("[AutoReplant] Not able to break block..."));
			//At this point, normal interaction may happen:
			return InteractionResult.PASS;
		}
		//From here on, the script must fail.
		
		//Select wheat seeds in the hotbar (given there are some in there...)
		Inventory playerInventory = player.getInventory();
		int matchingSlot = -1;
		for(int i = 0; i < 9; i++)
		{
			if(playerInventory.getItem(i).getItem() == seed)
			{
				matchingSlot = i;
				break;
			}
		}
		if(matchingSlot == -1)
		{
			//End life:
			return InteractionResult.FAIL;
		}
		playerInventory.setSelectedSlot(matchingSlot);
		
		BlockPos fieldBlockPos = pos.below();
		if(!client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND, new BlockHitResult(
				new Vec3(fieldBlockPos.getX() + 0.5f, fieldBlockPos.getY() + 0.9375f, fieldBlockPos.getZ() + 0.5f),
				Direction.UP,
				hit.getBlockPos().below(),
				false)
		).consumesAction())
		{
			player.sendSystemMessage(Component.literal("[AutoReplant] Failed to interact with block..."));
		}
		return InteractionResult.FAIL;
	}
}
