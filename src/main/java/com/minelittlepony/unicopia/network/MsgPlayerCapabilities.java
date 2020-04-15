package com.minelittlepony.unicopia.network;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.UUID;

import com.google.gson.annotations.Expose;
import com.minelittlepony.jumpingcastle.api.Channel;
import com.minelittlepony.jumpingcastle.api.Message;
import com.minelittlepony.unicopia.Race;
import com.minelittlepony.unicopia.Unicopia;
import com.minelittlepony.unicopia.entity.player.Pony;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;

public class MsgPlayerCapabilities implements Message, Message.Handler<MsgPlayerCapabilities> {
    @Expose
    Race newRace;

    @Expose
    UUID senderId;

    @Expose
    byte[] compoundTag;

    public MsgPlayerCapabilities(Race race, PlayerEntity player) {
        newRace = race;
        senderId = player.getUuid();
        compoundTag = new byte[0];
    }

    public MsgPlayerCapabilities(Pony player) {
        newRace = player.getSpecies();
        senderId = player.getOwner().getUuid();

        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
            CompoundTag nbt = player.toNBT();

            NbtIo.write(nbt, new DataOutputStream(bytes));

            compoundTag = bytes.toByteArray();
        } catch (IOException e) {
        }
    }

    @Override
    public void onPayload(MsgPlayerCapabilities message, Channel channel) {

        MinecraftServer server = channel.getServer();

        PlayerEntity self = server.getPlayerManager().getPlayer(senderId);

        if (self == null) {
            Unicopia.LOGGER.warn("[Unicopia] [CLIENT] [MsgPlayerCapabilities] Player with id %s was not found!\n", senderId.toString());
        } else {
            Pony player = Pony.of(self);

            if (compoundTag.length > 0) {
                try (ByteArrayInputStream input = new ByteArrayInputStream(compoundTag)) {
                    CompoundTag nbt = NbtIo.read(new DataInputStream(input));

                    player.fromNBT(nbt);
                } catch (IOException e) {

                }
            } else {
                player.setSpecies(newRace);
            }
        }
    }
}
