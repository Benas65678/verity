package dev.veritydark.command;

import com.mojang.brigadier.arguments.FloatArgumentType;
import dev.veritydark.AngerManager;
import dev.veritydark.ModContent;
import dev.veritydark.entity.AnomalyEntity;
import dev.veritydark.SanityManager;
import dev.veritydark.VerityConfig;
import dev.veritydark.VerityDark;
import dev.veritydark.VerityUtil;
import dev.veritydark.TrustManager;
import dev.veritydark.entity.StalkerEntity;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Testing commands (op level 2):
 *   /veritydark list            - entity ids found in the Verity namespace
 *   /veritydark nearby          - entities around you with their ids and whether they count as Verity
 *   /veritydark status          - your sanity and the nearest Verity's anger
 *   /veritydark sanity <value>  - set your sanity
 *   /veritydark anger <value>   - set the nearest Verity's anger (within 64 blocks)
 *   /veritydark stalker         - spawn a stalker 12 blocks ahead
 *   /veritydark trust <value>   - set the nearest Verity's trust
 *   /veritydark spawn           - spawn an anomaly in front of you 
 */
@Mod.EventBusSubscriber(modid = VerityDark.MODID)
public final class VerityCommand {

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("veritydark")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("list").executes(ctx -> list(ctx.getSource())))
                .then(Commands.literal("nearby").executes(ctx -> nearby(ctx.getSource())))
                .then(Commands.literal("status").executes(ctx -> status(ctx.getSource())))
                .then(Commands.literal("spawn").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    ServerLevel level = player.serverLevel();
                    AnomalyEntity anomaly = ModContent.ANOMALY.get().create(level);
                    if (anomaly == null) return 0;
                    Vec3 look = player.getLookAngle();
                    anomaly.moveTo(player.getX() + look.x * 7.0, player.getY(), player.getZ() + look.z * 7.0,
                            player.getYRot() + 180.0F, 0.0F);
                    anomaly.finalizeSpawn(level, level.getCurrentDifficultyAt(anomaly.blockPosition()), MobSpawnType.COMMAND, null, null);
                    level.addFreshEntity(anomaly);
                    ctx.getSource().sendSuccess(() -> Component.literal("Spawned an anomaly."), false);
                    return 1;
                }))
                .then(Commands.literal("stalker").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    ServerLevel level = player.serverLevel();
                    StalkerEntity stalker = ModContent.STALKER.get().create(level);
                    if (stalker == null) return 0;
                    Vec3 look = player.getLookAngle();
                    stalker.moveTo(player.getX() + look.x * 12.0, player.getY(), player.getZ() + look.z * 12.0,
                            player.getYRot() + 180.0F, 0.0F);
                    stalker.finalizeSpawn(level, level.getCurrentDifficultyAt(stalker.blockPosition()), MobSpawnType.COMMAND, null, null);
                    level.addFreshEntity(stalker);
                    ctx.getSource().sendSuccess(() -> Component.literal("Spawned a stalker."), false);
                    return 1;
                }))
                .then(Commands.literal("trust")
                        .then(Commands.argument("value", FloatArgumentType.floatArg(0.0F, 100.0F))
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    Entity verity = nearestVerity(player);
                                    if (verity == null) {
                                        ctx.getSource().sendFailure(Component.literal("No Verity within 64 blocks."));
                                        return 0;
                                    }
                                    float v = FloatArgumentType.getFloat(ctx, "value");
                                    TrustManager.add(verity, v - TrustManager.get(verity), player);
                                    ctx.getSource().sendSuccess(() -> Component.literal("Verity trust set to " + v), false);
                                    return 1;
                                })))
                .then(Commands.literal("sanity")
                        .then(Commands.argument("value", FloatArgumentType.floatArg(0.0F, 1000.0F))
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    float v = FloatArgumentType.getFloat(ctx, "value");
                                    SanityManager.set(player, v);
                                    ctx.getSource().sendSuccess(() -> Component.literal("Sanity set to " + v), false);
                                    return 1;
                                })))
                .then(Commands.literal("anger")
                        .then(Commands.argument("value", FloatArgumentType.floatArg(0.0F, 100.0F))
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    Entity verity = nearestVerity(player);
                                    if (verity == null) {
                                        ctx.getSource().sendFailure(Component.literal("No Verity within 64 blocks."));
                                        return 0;
                                    }
                                    float v = FloatArgumentType.getFloat(ctx, "value");
                                    AngerManager.setAnger(verity, v);
                                    ctx.getSource().sendSuccess(() -> Component.literal("Verity anger set to " + v), false);
                                    return 1;
                                }))));
    }

    private static Entity nearestVerity(ServerPlayer player) {
        List<Entity> found = player.level().getEntities(player, player.getBoundingBox().inflate(64.0), VerityUtil::isVerity);
        return found.stream().min(Comparator.comparingDouble(e -> e.distanceToSqr(player))).orElse(null);
    }

    /** Shows every entity within 24 blocks: its registry id, name and whether the addon treats it as Verity. */
    private static int nearby(CommandSourceStack src) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = src.getPlayerOrException();
        List<Entity> found = player.level().getEntities(player, player.getBoundingBox().inflate(24.0), e -> true);
        if (found.isEmpty()) {
            src.sendSuccess(() -> Component.literal("No entities within 24 blocks."), false);
            return 1;
        }
        for (Entity e : found) {
            ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(e.getType());
            String line = (key == null ? "?" : key.toString()) + "  [" + e.getClass().getSimpleName() + "]  \"" + e.getName().getString() + "\"  -> "
                    + (VerityUtil.isVerity(e) ? "VERITY" : "not Verity");
            src.sendSuccess(() -> Component.literal(line), false);
        }
        return 1;
    }

    private static int list(CommandSourceStack src) {
        String ns = VerityConfig.VERITY_NAMESPACE.get();
        String ids = ForgeRegistries.ENTITY_TYPES.getKeys().stream()
                .filter(k -> k.getNamespace().equals(ns))
                .map(ResourceLocation::toString)
                .sorted()
                .collect(Collectors.joining(", "));
        src.sendSuccess(() -> Component.literal(ids.isEmpty()
                ? "No entities registered under namespace '" + ns + "'. Check the config."
                : "Entities in '" + ns + "': " + ids), false);
        return 1;
    }

    private static int status(CommandSourceStack src) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = src.getPlayerOrException();
        Entity verity = nearestVerity(player);
        String anger = verity == null ? "no Verity nearby" : String.format("%.1f / 100", AngerManager.getAnger(verity));
        String text = String.format("Sanity %.1f / %.0f, Verity anger: %s", SanityManager.get(player), SanityManager.max(), anger);
        src.sendSuccess(() -> Component.literal(text), false);
        return 1;
    }

    private VerityCommand() {}
}
