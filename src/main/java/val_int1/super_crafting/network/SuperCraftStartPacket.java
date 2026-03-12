package val_int1.super_crafting.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;
import val_int1.super_crafting.blockentity.SuperCraftingTableBE;

import java.util.function.Supplier;

public record SuperCraftStartPacket(BlockPos pos) {

    public static void encode(SuperCraftStartPacket pkt, FriendlyByteBuf buf) {
        buf.writeBlockPos(pkt.pos);
    }

    public static SuperCraftStartPacket decode(FriendlyByteBuf buf) {
        return new SuperCraftStartPacket(buf.readBlockPos());
    }

    public static void handle(SuperCraftStartPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            Level level = player.level();
            BlockEntity be = level.getBlockEntity(pkt.pos);
            if (be instanceof SuperCraftingTableBE tableBE) {
                tableBE.startCrafting();
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
