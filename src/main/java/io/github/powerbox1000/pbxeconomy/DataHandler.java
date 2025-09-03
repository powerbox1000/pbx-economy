package io.github.powerbox1000.pbxeconomy;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.UUID;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

public class DataHandler extends SavedData {
    public static class PlayerDataEntry {
        public int cash = 1000;
    }

    public static class BusinessEntry {
        public String name = "";
        public int cash = 0;
        public ArrayList<UUID> owners = new ArrayList<>();
        public ArrayList<UUID> managers = new ArrayList<>();
        public ArrayList<UUID> employees = new ArrayList<>();

        public void setEmployeeRole(UUID playerId, String newRole) {
            // This is crap but idgaf im rushing
            String oldRole = null;
            if (employees.contains(playerId)) {
                oldRole = "employee";
            } else if (managers.contains(playerId)) {
                oldRole = "manager";
            } else if (owners.contains(playerId)) {
                oldRole = "owner";
            }

            if (oldRole == null) throw new IllegalArgumentException("Player does not have any role");
            if (oldRole == "owner" && owners.size() == 1) throw new IllegalArgumentException("Cannot remove last owner");
            if (newRole.equals(oldRole)) return; // No change

            if (newRole.equals("employee")) {
                employees.add(playerId);
            } else if (newRole.equals("manager")) {
                managers.add(playerId);
            } else if (newRole.equals("owner")) {
                owners.add(playerId);
            }

            if (oldRole.equals("employee")) {
                employees.remove(playerId);
            } else if (oldRole.equals("manager")) {
                managers.remove(playerId);
            } else if (oldRole.equals("owner")) {
                owners.remove(playerId);
            }
        }
    }

    public HashMap<UUID, PlayerDataEntry> players = new HashMap<>();
    public HashMap<String, BusinessEntry> businesses = new HashMap<>();

    private DataHandler() {}

    public CompoundTag writeNbt(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        CompoundTag playersNbt = new CompoundTag();
        players.forEach((uuid, data) -> {
            CompoundTag playerNbt = new CompoundTag();
 
            playerNbt.putInt("cash", data.cash);
 
            playersNbt.put(uuid.toString(), playerNbt);
        });
        nbt.put("players", playersNbt);

        CompoundTag businessNbt = new CompoundTag();
        businesses.forEach((name, data) -> {
            CompoundTag businessData = new CompoundTag();
            businessData.putString("name", data.name);
            businessData.putInt("cash", data.cash);

            ListTag ownerData = new ListTag();
            data.owners.forEach(uuid -> {
                ownerData.add(StringTag.valueOf(uuid.toString()));
            });
            businessData.put("owners", ownerData);

            ListTag managerData = new ListTag();
            data.managers.forEach(uuid -> {
                managerData.add(StringTag.valueOf(uuid.toString()));
            });
            businessData.put("managers", managerData);

            ListTag employeeData = new ListTag();
            data.employees.forEach(uuid -> {
                employeeData.add(StringTag.valueOf(uuid.toString()));
            });
            businessData.put("employees", employeeData);

            businessNbt.put(name, businessData);
        });
        
        nbt.put("businesses", businessNbt);

        return nbt;
    }

    public static DataHandler createFromNbt(CompoundTag tag, HolderLookup.Provider registryLookup) {
        DataHandler state = new DataHandler();

        CompoundTag playersNbt = tag.getCompound("players").get();
        playersNbt.keySet().forEach(key -> {
            PlayerDataEntry playerData = new PlayerDataEntry();

            playerData.cash = playersNbt.getCompound(key).get().getInt("cash").orElse(1000);

            UUID uuid = UUID.fromString(key);
            state.players.put(uuid, playerData);
        });

        CompoundTag businessNbt = tag.getCompound("businesses").get();
        businessNbt.keySet().forEach(key -> {
            BusinessEntry businessData = new BusinessEntry();

            businessData.name = businessNbt.getCompound(key).get().getString("name").get(); // Required
            businessData.cash = businessNbt.getCompound(key).get().getInt("cash").orElse(0);

            ListTag ownerData = businessNbt.getCompound(key).get().getList("owners").get(); // Required
            ownerData.forEach(uuidTag -> {
                UUID uuid = UUID.fromString(uuidTag.asString().get());
                businessData.owners.add(uuid);
            });

            ListTag managerData = businessNbt.getCompound(key).get().getList("managers").get(); // Required
            managerData.forEach(uuidTag -> {
                UUID uuid = UUID.fromString(uuidTag.asString().get());
                businessData.managers.add(uuid);
            });

            ListTag employeeData = businessNbt.getCompound(key).get().getList("employees").get(); // Required
            employeeData.forEach(uuidTag -> {
                UUID uuid = UUID.fromString(uuidTag.asString().get());
                businessData.employees.add(uuid);
            });

            state.businesses.put(key, businessData);
        });

        return state;
    }

    public static DataHandler createNew(Context ctx) {
        DataHandler state = new DataHandler();
        state.players = new HashMap<>();
        state.businesses = new HashMap<>();
        return state;
    }

    public static Codec<DataHandler> getCodec(Context ctx) {
        return new Codec<DataHandler>() {
            @Override
            public <T> DataResult<Pair<DataHandler, T>> decode(DynamicOps<T> ops, T input) {
                return DataResult.success(new Pair<>(DataHandler.createFromNbt(CompoundTag.CODEC.parse(ops, input).getOrThrow(), ctx.levelOrThrow().registryAccess()), input));
            }

            @Override
            public <T> DataResult<T> encode(DataHandler data, DynamicOps<T> ops, T prefix) {
                return CompoundTag.CODEC.encode(data.writeNbt(new CompoundTag(), ctx.levelOrThrow().registryAccess()), ops, prefix);
            }
        };
    }

    private static final SavedDataType<DataHandler> type = new SavedDataType<>(
            Economy.MOD_ID,
            DataHandler::createNew,
            DataHandler::getCodec,
            null
    );
 
    public static DataHandler getServerState(MinecraftServer server) {
        ServerLevel ServerLevel = server.getLevel(Level.OVERWORLD);
        assert ServerLevel != null;
 
        DataHandler state = ServerLevel.getDataStorage().computeIfAbsent(type);
        state.setDirty();
 
        return state;
    }

    public PlayerDataEntry getPlayerState(LivingEntity player) {
        PlayerDataEntry playerState = this.players.computeIfAbsent(player.getUUID(), uuid -> new PlayerDataEntry());
        return playerState;
    }

    public BusinessEntry getBusinessState(String businessName) {
        return getBusinessState(businessName, false);
    }

    public BusinessEntry getBusinessState(String businessName, boolean computeIfAbsent) {
        if (computeIfAbsent) {
            return this.businesses.computeIfAbsent(businessName, name -> new BusinessEntry());
        }
        return this.businesses.get(businessName);
    }

    public BusinessEntry createBusiness(String name, UUID owner) {
        return createBusiness(name, new ArrayList<>(Collections.singletonList(owner)));
    }

    public BusinessEntry createBusiness(String name, ArrayList<UUID> owners) {
        if (this.businesses.containsKey(name)) {
            return null; // Business already exists
        }
        BusinessEntry business = new BusinessEntry();
        business.name = name;
        business.owners.addAll(owners);
        this.businesses.put(name, business);
        this.setDirty();
        return business;
    }
}