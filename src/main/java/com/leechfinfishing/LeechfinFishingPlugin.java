package com.leechfinfishing;

import com.google.inject.Provides;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.inject.Inject;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.Constants;
import net.runelite.api.GameObject;
import net.runelite.api.ItemContainer;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.Projectile;
import net.runelite.api.Tile;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.events.GameTick;
import net.runelite.api.gameval.InventoryID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.IdentityHashMap;
import java.util.Map;

@PluginDescriptor(
	name = "Leechfin Fishing",
	description = "Highlighting for leechfin fishing.",
	tags = {"leechfin", "fishing", "highlight", "vampyrium"}
)
public class LeechfinFishingPlugin extends Plugin
{
	public static final Logger log = LoggerFactory.getLogger(LeechfinFishingPlugin.class);

	// manually coded ids until api updated
	// varbit used to determine if actively fishing leechfin spot
	public static final int ACTIVE_VARBIT_ID = 15454;
	// leechfin projectile and leechfin fishing spot game object ids
	public static final int LEECHFIN_ID = 3992;
	public static final int LEECHFIN_FISHING_SPOT_ID = 62193;

	private static final int CLIENT_TICKS_PER_GAME_TICK = Constants.GAME_TICK_LENGTH / Constants.CLIENT_TICK_LENGTH;
	private static final int MAX_NORTH_DISTANCE = 5 * Perspective.LOCAL_TILE_SIZE;
	private static final int MAX_X_OFFSET = 2 * Perspective.LOCAL_TILE_SIZE;

	private @Getter List<LocalPoint> leechfinFishingPoints;
	private @Getter LocalPoint closestLeechfinFishingPoint;
	private @Getter Projectile activeLeechfin;
	private @Getter LocalPoint activeLeechfinPoint;
	private @Getter Projectile nextLeechfin;
	private @Getter LocalPoint nextLeechfinPoint;

	private final Map<Projectile, Integer> fishNumbers =
			new IdentityHashMap<>();

	private int waveFishCount = 0;
	private int lastFishStartCycle = -1;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private Client client;

	@Inject
	private LeechfinFishingConfig config;

	@Inject
	private LeechfinFishingOverlay overlay;

	@Inject
	private LeechfinInventoryOverlay inventoryOverlay;

	@Override
	protected void startUp() throws Exception
	{
		log.debug("Leechfin Fishing started!");

		overlayManager.add(overlay);
		overlayManager.add(inventoryOverlay);
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.debug("Leechfin Fishing stopped!");

		overlayManager.remove(overlay);
		overlayManager.remove(inventoryOverlay);
	}

	@Provides
	LeechfinFishingConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(LeechfinFishingConfig.class);
	}
	@Subscribe
	public void onGameTick(GameTick event)
	{
		WorldView worldView = client.getTopLevelWorldView();

		leechfinFishingPoints = findLeechfinFishingPoints(worldView);
		// no fishing spots found
		if (leechfinFishingPoints.isEmpty())
		{
			activeLeechfin = null;
			activeLeechfinPoint = null;
			nextLeechfin = null;
			nextLeechfinPoint = null;
			return;
		}

		closestLeechfinFishingPoint = findClosestLeechfinFishingPoint(client.getLocalPlayer());
		if (closestLeechfinFishingPoint == null)
		{
			activeLeechfin = null;
			activeLeechfinPoint = null;
			nextLeechfin = null;
			nextLeechfinPoint = null;
			return;
		}

		List<Projectile> leechfin = getSortedLeechfin();

		if (isLeechfinFishing())
		{
			trackStarPowerFish(leechfin);
		}
		else
		{
			fishNumbers.clear();
			waveFishCount = 0;
			lastFishStartCycle = -1;
		}
		// no leechfin on screen
		if (leechfin.isEmpty())
		{
			activeLeechfin = null;
			activeLeechfinPoint = null;
			nextLeechfin = null;
			nextLeechfinPoint = null;
			return;
		}

		// ensure active is still valid, set if not
		if (activeLeechfin == null || !leechfin.contains(activeLeechfin))
		{
			activeLeechfin = leechfin.get(0);
		}
		activeLeechfinPoint = findPoint(worldView, activeLeechfin);

		// now find next leechfin with a different x position
		nextLeechfin = findNextLeechfin(activeLeechfin, leechfin);
		nextLeechfinPoint = findPoint(worldView, nextLeechfin);
	}

	private List<Projectile> getSortedLeechfin()
	{
		List<Projectile> list = new ArrayList<>();
		for (Projectile leechfinProjectile : client.getProjectiles())
		{
			if (!isValidLeechfin(leechfinProjectile))
			{
				continue;
			}
			list.add(leechfinProjectile);
		}
		list.sort(Comparator.comparingDouble(Projectile::getRemainingCycles));

		return list;
	}

	private boolean isValidLeechfin(Projectile leechfinProjectile)
	{
		// if projectile is not a leechfin, don't include
		if (leechfinProjectile.getId() != LEECHFIN_ID)
		{
			return false;
		}

		LocalPoint localSourcePoint = LocalPoint.fromWorld(client, leechfinProjectile.getSourcePoint());
		if (localSourcePoint == null)
		{
			return false;
		}

		// if source is more than 5 tiles north, don't include
		if (localSourcePoint.getY() > closestLeechfinFishingPoint.dy(MAX_NORTH_DISTANCE).getY())
		{
			return false;
		}

		// if source is more 2 tiles east or west, don't include
		if (Math.abs(localSourcePoint.getX() - closestLeechfinFishingPoint.getX()) > MAX_X_OFFSET)
		{
			return false;
		}

		// if remaining cycles is less than a tick, don't include, it's too late to click
		if (leechfinProjectile.getRemainingCycles() <= CLIENT_TICKS_PER_GAME_TICK)
		{
			return false;
		}

		return true;
	}

	private Projectile findNextLeechfin(Projectile active, List<Projectile> list)
	{
		if (active == null)
		{
			return list.isEmpty() ? null : list.get(0);
		}

		int idx = list.indexOf(active);
		// if list does not contain active
		if (idx == -1)
		{
			return list.isEmpty() ? null : list.get(0);
		}
		// if active is last list index, next would be out of range, so return null
		if (idx + 1 >= list.size())
		{
			return null;
		}
		// if next has the same x as active, skip to the next after that
		if (list.get(idx).getSourcePoint().getX() == list.get(idx + 1).getSourcePoint().getX())
		{
			return findNextLeechfin(list.get(idx + 1), list);
		}

		return list.get(idx + 1);
	}

	public static boolean isInventoryFull(Client client)
	{
		ItemContainer inventory = client.getItemContainer(InventoryID.INV);
		return inventory != null && inventory.size() <= inventory.count();
	}

	public boolean isLeechfinFishing()
	{
		return client.getVarbitValue(LeechfinFishingPlugin.ACTIVE_VARBIT_ID) == 1;
	}

	private LocalPoint findPoint(WorldView worldView, Projectile projectile)
	{
		if (projectile == null)
		{
			return null;
		}

		int x = (int) projectile.getX();
		int y = (int) projectile.getY();

		return new LocalPoint(x, y, worldView);
	}

	private LocalPoint findClosestLeechfinFishingPoint(Player player)
	{
		if (player == null || leechfinFishingPoints.isEmpty())
		{
			return null;
		}

		// have to compare LocalPoint with LocalPoint, so use getLocalLocation
		LocalPoint playerLocation = player.getLocalLocation();

		return leechfinFishingPoints.stream()
			.min(Comparator.comparingInt(
				spot -> spot.distanceTo(playerLocation)
			))
			.orElse(null);
	}

	private List<LocalPoint> findLeechfinFishingPoints(WorldView worldview)
	{
		List<LocalPoint> leechfinFishingSpots = new ArrayList<>();

		Tile[][][] tiles = worldview.getScene().getTiles();
		int z = worldview.getPlane();

		for (int x = 0; x < tiles[z].length; ++x)
		{
			for (int y = 0; y < tiles[z][x].length; ++y)
			{
				Tile tile = tiles[z][x][y];
				if (tile == null)
				{
					continue;
				}

				GameObject[] gameObjects = tile.getGameObjects();
				for (GameObject gameObject : gameObjects)
				{
					if (gameObject != null && gameObject.getId() == LEECHFIN_FISHING_SPOT_ID)
					{
						leechfinFishingSpots.add(gameObject.getLocalLocation());
					}
				}
			}
		}

		return leechfinFishingSpots;
	}

	private void trackStarPowerFish(List<Projectile> fish)
	{
		// Remove fish that have disappeared.
		List<Projectile> present = new ArrayList<>();

		for (Projectile p : client.getProjectiles())
		{
			if (p.getId() == LEECHFIN_ID)
			{
				present.add(p);
			}
		}

		fishNumbers.keySet().retainAll(present);

		// Identify newly spawned fish.
		List<Projectile> newFish = new ArrayList<>();

		for (Projectile p : fish)
		{
			if (!fishNumbers.containsKey(p))
			{
				newFish.add(p);
			}
		}

		// Process them in spawning order.
		newFish.sort(Comparator.comparingInt(Projectile::getStartCycle));

		for (Projectile p : newFish)
		{
			int startCycle = p.getStartCycle();

			// A gap indicates a new wave.
			if (lastFishStartCycle >= 0 &&
					startCycle - lastFishStartCycle > CLIENT_TICKS_PER_GAME_TICK * 2)
			{
				waveFishCount = 0;
			}

			waveFishCount = (waveFishCount % 22) + 1;

			fishNumbers.put(p, waveFishCount);
			lastFishStartCycle = startCycle;
		}
	}

	public boolean isStarPowerFish(Projectile fish)
	{
		Integer number = fishNumbers.get(fish);

		return number != null && number >= 10 && number <= 14;
	}

}
