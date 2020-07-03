package me.frep.thotpatrol.checks.movement.speed;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import me.frep.thotpatrol.checks.movement.ascension.AscensionA;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import me.frep.thotpatrol.ThotPatrol;
import me.frep.thotpatrol.checks.Check;
import me.frep.thotpatrol.data.DataPlayer;
import me.frep.thotpatrol.events.SharedEvents;
import me.frep.thotpatrol.utils.UtilCheat;
import me.frep.thotpatrol.utils.UtilMath;
import me.frep.thotpatrol.utils.UtilPlayer;
import me.frep.thotpatrol.utils.UtilTime;

public class SpeedA extends Check {

    public static Map<UUID, Map.Entry<Integer, Long>> speedTicks;
    public static Map<UUID, Map.Entry<Integer, Long>> tooFastTicks;
    public static Map<UUID, Long> lastHit;
    public static Map<UUID, Double> velocity;
    public List<UUID> jumpingOnIce = new ArrayList<>();

    public SpeedA(ThotPatrol ThotPatrol) {
        super("SpeedA", "Speed (Type A)", ThotPatrol);
        setEnabled(true);
        setBannable(true);
        setMaxViolations(6);
        speedTicks = new HashMap<>();
        tooFastTicks = new HashMap<>();
        lastHit = new HashMap<>();
        velocity = new HashMap<>();
    }

    @EventHandler(ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent e) {
        if (e.getEntity() instanceof Player) {
            Player player = (Player) e.getEntity();
            lastHit.put(player.getUniqueId(), System.currentTimeMillis());
        }
    }

    public boolean isOnIce(final Player player) {
        Location a = player.getLocation();
        a.setY(a.getY() - 1.0);
        if (a.getBlock().getType().equals(Material.ICE)) {
            return true;
        }
        a.setY(a.getY() - 1.0);
        return a.getBlock().getType().equals(Material.ICE);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onLog(PlayerQuitEvent e) {
        speedTicks.remove(e.getPlayer().getUniqueId());
        tooFastTicks.remove(e.getPlayer().getUniqueId());
        lastHit.remove(e.getPlayer().getUniqueId());
        velocity.remove(e.getPlayer().getUniqueId());
    }

    @SuppressWarnings("deprecation")
	@EventHandler
    public void CheckSpeed(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        if ((event.getFrom().getX() == event.getTo().getX()) && (event.getFrom().getZ() == event.getFrom().getZ())
                || !getThotPatrol().isEnabled()
                || player.getAllowFlight()
                || player.getVehicle() != null
                || SpeedC.highKb.contains(player.getUniqueId())
                || player.hasPermission("thotpatrol.bypass")
                || player.getVelocity().length() + 0.1 < velocity.getOrDefault(uuid, -1.0D)
                || (getThotPatrol().LastVelocity.containsKey(uuid)
                || !UtilTime.elapsed(AscensionA.toggleFlight.getOrDefault(uuid, 0L), 5000L)
                && !player.hasPotionEffect(PotionEffectType.POISON)
                && !player.hasPotionEffect(PotionEffectType.WITHER) && player.getFireTicks() == 0)) return;
        long lastHitDiff = lastHit.containsKey(uuid) ? lastHit.get(uuid) - System.currentTimeMillis() : 2001L;
        int Count = 0;
        long Time = UtilTime.nowlong();
        if (speedTicks.containsKey(uuid)) {
            Count = speedTicks.get(uuid).getKey();
            Time = speedTicks.get(uuid).getValue();
        }
        int TooFastCount = 0;
        double percent = 0D;
        if (tooFastTicks.containsKey(uuid)) {
            double OffsetXZ = UtilMath.offset(UtilMath.getHorizontalVector(event.getFrom().toVector()),
                    UtilMath.getHorizontalVector(event.getTo().toVector()));
            double LimitXZ = 0.0D;
            if ((UtilPlayer.isOnGround(player)) && (player.getVehicle() == null)) {
                LimitXZ = 0.34D;
            } else {
                LimitXZ = 0.39D;
            }
            if (lastHitDiff < 800L) {
                ++LimitXZ;
            } else if (lastHitDiff < 1600L) {
                LimitXZ += 0.4;
            } else if (lastHitDiff < 2000L) {
                LimitXZ += 0.1;
            }
            if (UtilCheat.slabsNear(player.getLocation())) {
                LimitXZ += 0.05D;
            }
            Location b = UtilPlayer.getEyeLocation(player);
            b.add(0.0D, 1.0D, 0.0D);
            if ((b.getBlock().getType() != Material.AIR) && (!UtilCheat.canStandWithin(b.getBlock()))) {
                LimitXZ = 0.69D;
            }
            Location below = event.getPlayer().getLocation().clone().add(0.0D, -1.0D, 0.0D);

            if (UtilCheat.isStair(below.getBlock())) {
                LimitXZ += 0.6;
            }
            if (isOnIce(player)) {
                if ((b.getBlock().getType() != Material.AIR) && (!UtilCheat.canStandWithin(b.getBlock()))) {
                    LimitXZ = 1.0D;
                } else {
                    LimitXZ = 0.75D;
                }
            }
            float speed = player.getWalkSpeed();
            if (speed > .25) {
            	LimitXZ += speed * .33;
            }
            LimitXZ += (speed > 0.2F ? speed * 10.0F * 0.33F : 0.0F);
            for (PotionEffect effect : player.getActivePotionEffects()) {
                if (effect.getType().equals(PotionEffectType.SPEED)) {
                    if (UtilPlayer.isOnGround(player)) {
                        LimitXZ += 0.1D * (effect.getAmplifier() + 1);
                    } else {
                        LimitXZ += 0.1D * (effect.getAmplifier() + 1);
                    }
                }
            }
            if (OffsetXZ > LimitXZ && !UtilTime.elapsed(tooFastTicks.get(uuid).getValue(), 150L)) {
                percent = (OffsetXZ - LimitXZ) * 100;
                TooFastCount = tooFastTicks.get(uuid).getKey()
                        + 3;
            } else {
                TooFastCount = TooFastCount--;
            }
            if (player.getLocation().clone().add(0, .5, 0).getBlock().getType().toString().contains("DOOR")
                    || player.getLocation().clone().add(0, 1, 0).getBlock().getType().toString().contains("DOOR")) {
                //todo
                return;
            }
        }
        if (TooFastCount >= 11) {
            TooFastCount = 0;
            Count++;
            dumplog(player, "New Count: " + Count);
        }
        if (jumpingOnIce.contains(uuid)) {
        	return;
        }
        Material below = player.getLocation().subtract(0, 1.5, 0).getBlock().getType();
        Material below2 = player.getLocation().subtract(0, 1, 0).getBlock().getType();
        if (below.equals(Material.ICE) || below.equals(Material.PACKED_ICE) 
        		|| below2.equals(Material.ICE) || below2.equals(Material.PACKED_ICE)) {
        	jumpingOnIce.add(uuid);
        	Bukkit.getScheduler().scheduleAsyncDelayedTask(ThotPatrol.Instance, new Runnable() {
        		@Override
        		public void run() {
        			jumpingOnIce.remove(uuid);
        		}
        	}, 40);
        }
        if (jumpingOnIce.contains(uuid)) {
        	return;
        }
        if (speedTicks.containsKey(uuid) && UtilTime.elapsed(Time, 30000L)) {
            Count = 0;
            Time = UtilTime.nowlong();
        }
		double tps = getThotPatrol().getLag().getTPS();
		double ping = getThotPatrol().getLag().getPing(player);
        if (Count > 2
        		&& getThotPatrol().getConfig().getBoolean("instantBans.SpeedA.enabled")
        		&& percent > getThotPatrol().getConfig().getInt("instantBans.SpeedA.maxSpeedPercentage")
        		&& isBannable()
        		&& !getThotPatrol().getNamesBanned().containsKey(player.getName())
        		&& tps > getThotPatrol().getConfig().getDouble("instantBans.SpeedA.minTPS")
        		&& ping < getThotPatrol().getConfig().getDouble("instantBans.SpeedA.maxPing")
        		&& ping > 1) {
            getThotPatrol().banPlayer(player, this);
        	String banAlertMessage = getThotPatrol().getConfig().getString("instantBans.SpeedA.banAlertMessage");
        	getThotPatrol().alert(ChatColor.translateAlternateColorCodes('&', banAlertMessage.replaceAll("%player%", player.getName())
        			.replaceAll("%speed%", Double.toString(Math.round(percent)))));
        	dumplog(player, "[Instant Ban] Average Speed: " + percent + "% | TPS: " + tps + " | Ping: " + ping);
        	getThotPatrol().logToFile(player, this, "Average [Instant Ban]", "Percent: " + percent + "% | TPS: " + tps + " | Ping: " + ping);
        }
        if (Count >= 3) {
            dumplog(player, "Logged for Speed. Count: " + Count);
            Count = 0;
            getThotPatrol().logCheat(this, player, Math.round(percent) + "% faster than normal | Ping: " + ping + " | TPS: " + tps);
        	getThotPatrol().logToFile(player, this, "Average", "Percent: " + Math.round(percent)
        	+ " | TPS: " + tps + " | Ping: " + ping);
        }
        if (!UtilPlayer.isOnGround(player)) {
            velocity.put(uuid, player.getVelocity().length());
        } else {
            velocity.put(uuid, -1.0D);
        }
        tooFastTicks.put(uuid, new AbstractMap.SimpleEntry<>(TooFastCount, System.currentTimeMillis()));
        speedTicks.put(uuid, new AbstractMap.SimpleEntry<>(Count, Time));
    }
}