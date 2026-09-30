package com.leechfinfishing;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.api.Projectile;
import net.runelite.api.Point;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.GradientPaint;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import net.runelite.api.Player;

public class LeechfinFishingOverlay extends Overlay {
	private final Client client;

	private final LeechfinFishingPlugin plugin;

	private final LeechfinFishingConfig config;
	// How long each flame lasts (450 milliseconds).
	private static final long FLAME_DURATION_NS = 100_000_000L;

	private static final Color[] LANE_COLORS = {
			Color.GREEN,
			Color.RED,
			Color.BLUE
	};

	// Remember each projectile's previous position.
	private final Map<Projectile, Double> previousFishY =
			new IdentityHashMap<>();

	// Store the animation start time and color for each ring.
	private final long[] flameStartNs = new long[3];

	private final Color[] flameColors = {
			Color.GREEN,
			Color.RED,
			Color.BLUE
	};
	@Inject
	public LeechfinFishingOverlay(Client client, LeechfinFishingPlugin plugin, LeechfinFishingConfig config) {
		this.client = client;
		this.plugin = plugin;
		this.config = config;

		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D graphics) {
		WorldView worldView = client.getTopLevelWorldView();
		if (worldView == null) {
			return null;
		}

		if (plugin.isLeechfinFishing())
		{
			renderLeechfinTiles(graphics, worldView);
			renderFretRings(graphics, worldView);
			renderLeechfinProjectiles(graphics);

			// Guitar Hero hit effects!
			renderHitFlames(graphics, worldView);
		}
		else
		{
			previousFishY.clear();
			renderLeechfinFishingSpotTile(graphics);
		}

		return null;
	}

	private void renderLeechfinFishingSpotTile(Graphics2D graphics) {
		Color color = LeechfinFishingPlugin.isInventoryFull(client)
				? config.fullInventoryColor()
				: config.leechfinSpotColor();
		for (LocalPoint leechfinFishingSpot : plugin.getLeechfinFishingPoints()) {
			renderTileOverlay(
					graphics,
					leechfinFishingSpot,
					color
			);
		}
	}

	private void renderLeechfinTiles(Graphics2D graphics, WorldView worldView) {
		if (plugin.getClosestLeechfinFishingPoint() == null) {
			return;
		}
		if (config.highlightActiveTile()) {
			renderLeechfinTileOverlay(
					graphics,
					worldView,
					plugin.getActiveLeechfinPoint(),
					0,  // render at same tile as fishing spot
					config.activeHighlightColor(),
					config.activeFillColor()
			);
		}

		if (config.highlightNextTile()) {
			renderLeechfinTileOverlay(
					graphics,
					worldView,
					plugin.getNextLeechfinPoint(),
					1,  // render one tile north of fishing spot
					config.nextHighlightColor(),
					config.nextFillColor()
			);
		}
	}

	private void renderLeechfinTileOverlay(
			Graphics2D graphics,
			WorldView worldView,
			LocalPoint leechfinPoint,
			int yOffset,
			Color highlightColor,
			Color fillColor
	) {
		if (leechfinPoint == null) {
			return;
		}

		// use x position of leechfin, y position of fishing spot with yOffset applied
		LocalPoint tileToHighlight = new LocalPoint(
				leechfinPoint.getX(),
				plugin.getClosestLeechfinFishingPoint()
						.dy(yOffset * Perspective.LOCAL_TILE_SIZE).
						getY(),
				worldView
		);

		renderTileOverlay(graphics, tileToHighlight, highlightColor, fillColor);

	}

	private void renderTileOverlay(Graphics2D graphics, LocalPoint localPoint, Color color) {
		Polygon tilePoly = Perspective.getCanvasTilePoly(client, localPoint);
		if (tilePoly == null) {
			return;
		}

		OverlayUtil.renderPolygon(graphics, tilePoly, color);
	}

	private void renderTileOverlay(Graphics2D graphics, LocalPoint localPoint, Color color, Color fillColor) {
		Polygon tilePoly = Perspective.getCanvasTilePoly(client, localPoint);
		if (tilePoly == null) {
			return;
		}

		OverlayUtil.renderPolygon(graphics, tilePoly, color, fillColor, new BasicStroke(2));
	}

	private void renderLeechfinProjectiles(Graphics2D graphics) {
		LocalPoint center = plugin.getClosestLeechfinFishingPoint();

		if (center == null) {
			return;
		}

		for (Projectile fish : client.getProjectiles()) {
			// Ignore anything that isn't a leechfin.
			if (fish.getId() != LeechfinFishingPlugin.LEECHFIN_ID) {
				continue;
			}

			// Get the fish's original spawn location.
			LocalPoint source = LocalPoint.fromWorld(
					client, fish.getSourcePoint()
			);

			if (source == null) {
				continue;
			}

			// Determine its lane relative to the center.
			int lane = Math.round(
					(float) (source.getX() - center.getX())
							/ Perspective.LOCAL_TILE_SIZE
			);

			// Assign a Guitar Hero color to each lane.
			Color color;

			switch (lane)
			{
				case -1: color = Color.GREEN; break;
				case  0: color = Color.RED;   break;
				case  1: color = Color.BLUE;  break;
				default: continue;
			}
// STAR POWER!
			if (plugin.isStarPowerFish(fish))
			{
				color = Color.CYAN;
			}

			// Convert current fish position to screen coordinates.
			Point screenPoint = Perspective.localToCanvas(
					client,
					(int) fish.getX(),
					(int) fish.getY(),
					(int) fish.getZ()
			);

			if (screenPoint == null) {
				continue;
			}

			int x = screenPoint.getX();
			int y = screenPoint.getY();

			if (plugin.isStarPowerFish(fish))
			{
				graphics.setColor(new Color(0, 255, 255, 45));
				graphics.fillOval(x - 21, y - 21, 42, 42);
			}

			// Colored translucent fill.
			graphics.setColor(new Color(
					color.getRed(),
					color.getGreen(),
					color.getBlue(),
					65
			));

			graphics.fillOval(x - 14, y - 14, 28, 28);

			// Bright colored outline.
			graphics.setColor(color);
			graphics.drawOval(x - 14, y - 14, 28, 28);
		}
	}

	private void renderFretRings(Graphics2D graphics, WorldView worldView)
	{
		LocalPoint center = plugin.getClosestLeechfinFishingPoint();

		if (center == null)
		{
			return;
		}

		int tileSize = Perspective.LOCAL_TILE_SIZE;

		// Ring radius (approximately 40% of one tile).
		int radius = (int) (tileSize * 0.40);

		// Catch line position.
		int hitLineY = center.getY();

		// Three Guitar Hero lane colors.
		Color[] colors = {
				Color.GREEN,
				Color.RED,
				Color.BLUE
		};

		// Cyan fretboard during Star Power!
		boolean starPower = plugin.isStarPowerFish(
				plugin.getActiveLeechfin()
		);

		Graphics2D g = (Graphics2D) graphics.create();

		try
		{
			g.setRenderingHint(
					RenderingHints.KEY_ANTIALIASING,
					RenderingHints.VALUE_ANTIALIAS_ON
			);

			// Draw the three rings.
			for (int lane = -1; lane <= 1; lane++)
			{
				int cx = center.getX() + lane * tileSize;
				int cy = hitLineY;

				LocalPoint ringCenter = new LocalPoint(
						cx, cy, worldView
				);

				// Position ring slightly above the ground.
				int height = Perspective.getTileHeight(
						client, ringCenter, worldView.getPlane()
				) - 30;

				// Construct a circular ring in 3D space.
				Path2D ring = new Path2D.Double();

				int segments = 32;
				boolean valid = true;

				for (int i = 0; i < segments; i++)
				{
					double angle = 2 * Math.PI * i / segments;

					int px = cx + (int) (Math.cos(angle) * radius);
					int py = cy + (int) (Math.sin(angle) * radius);

					// Convert each point to screen coordinates.
					Point point = Perspective.localToCanvas(
							client, px, py, height
					);

					if (point == null)
					{
						valid = false;
						break;
					}

					if (i == 0)
					{
						ring.moveTo(point.getX(), point.getY());
					}
					else
					{
						ring.lineTo(point.getX(), point.getY());
					}
				}

				if (!valid)
				{
					continue;
				}

				ring.closePath();

				Color color = starPower
						? Color.CYAN
						: colors[lane + 1];

				// Dark translucent interior.
				g.setColor(new Color(0, 0, 0, 65));
				g.fill(ring);

				// Wide, soft colored glow.
				g.setStroke(new BasicStroke(
						11f,
						BasicStroke.CAP_ROUND,
						BasicStroke.JOIN_ROUND
				));

				g.setColor(new Color(
						color.getRed(),
						color.getGreen(),
						color.getBlue(),
						55
				));

				g.draw(ring);

				// Bright outer ring.
				g.setStroke(new BasicStroke(
						3f,
						BasicStroke.CAP_ROUND,
						BasicStroke.JOIN_ROUND
				));

				g.setColor(color);
				g.draw(ring);
			}
		}
		finally
		{
			g.dispose();
		}
	}

	private void renderHitFlames(Graphics2D graphics, WorldView worldView)
	{
		LocalPoint center = plugin.getClosestLeechfinFishingPoint();

		if (center == null)
		{
			previousFishY.clear();
			return;
		}

		int tileSize = Perspective.LOCAL_TILE_SIZE;
		double hitLineY = center.getY();
		long now = System.nanoTime();

		Set<Projectile> visibleFish = Collections.newSetFromMap(
				new IdentityHashMap<Projectile, Boolean>()
		);

		// Detect fish crossing the fretboard.
		for (Projectile fish : client.getProjectiles())
		{
			if (fish.getId() != LeechfinFishingPlugin.LEECHFIN_ID)
			{
				continue;
			}

			visibleFish.add(fish);

			double currentY = fish.getY();
			Double previousY = previousFishY.put(fish, currentY);

			if (previousY == null)
			{
				continue;
			}

			// Fish swim south. Check if one crossed the hit line.
			if (previousY > hitLineY && currentY <= hitLineY)
			{
				LocalPoint source = LocalPoint.fromWorld(
						client, fish.getSourcePoint()
				);

				if (source == null)
				{
					continue;
				}

				int lane = Math.round(
						(float)(source.getX() - center.getX()) / tileSize
				);

				if (lane < -1 || lane > 1)
				{
					continue;
				}

// Only trigger if the player is facing the correct lane.
				if (!isFacingLane(lane, center))
				{
					// Fish passed while facing the wrong lane.
					plugin.breakCatchStreak();
					continue;
				}

				int index = lane + 1;

				// Start (or restart) this ring's flame animation.
				flameStartNs[index] = now;

				// Cyan flames during Star Power!
				flameColors[index] = plugin.isStarPowerFish(fish)
						? Color.CYAN
						: LANE_COLORS[index];
			}
		}

		// Forget projectiles that have disappeared.
		previousFishY.keySet().retainAll(visibleFish);

		// Render currently active flame animations.
		for (int lane = -1; lane <= 1; lane++)
		{
			int index = lane + 1;
			long started = flameStartNs[index];

			if (started == 0L)
			{
				continue;
			}

			double progress =
					(now - started) / (double) FLAME_DURATION_NS;

			if (progress < 0 || progress >= 1)
			{
				continue;
			}

			// Use the exact same world coordinates as our fret rings.
			int cx = center.getX() + lane * tileSize;
			int cy = center.getY();

			LocalPoint ringCenter = new LocalPoint(cx, cy, worldView);

			int height = Perspective.getTileHeight(
					client, ringCenter, worldView.getPlane()
			) - 30;

			Point point = Perspective.localToCanvas(
					client, cx, cy, height
			);

			if (point == null)
			{
				continue;
			}

			drawHitFlame(
					graphics,
					point.getX(),
					point.getY(),
					flameColors[index],
					progress
			);
		}
	}

	private void drawHitFlame(
			Graphics2D graphics,
			int x,
			int y,
			Color color,
			double progress)
	{
		// progress: 0 = animation starts, 1 = finished.
		double fade = 1.0 - progress;

		int flameHeight = (int)(
				40 + 15 * Math.sin(progress * Math.PI)
		);

		int width = (int)(16 + 8 * fade);
		int alpha = (int)(210 * fade);

		// Slight movement makes the flame look organic.
		double sway = Math.sin(progress * 14) * 5;

		Graphics2D g = (Graphics2D) graphics.create();

		try
		{
			g.setRenderingHint(
					RenderingHints.KEY_ANTIALIASING,
					RenderingHints.VALUE_ANTIALIAS_ON
			);

			// 1. EXPANDING SHOCKWAVE
			int spread = (int)(13 + 22 * progress);

			g.setColor(new Color(
					color.getRed(),
					color.getGreen(),
					color.getBlue(),
					(int)(120 * fade)
			));

			g.setStroke(new BasicStroke((float)(2 + 3 * fade)));

			g.drawOval(
					x - spread,
					y - spread / 3,
					spread * 2,
					spread * 2 / 3
			);

			// 2. BRIGHT GLOW AT THE BASE
			g.setColor(new Color(
					color.getRed(),
					color.getGreen(),
					color.getBlue(),
					(int)(95 * fade)
			));

			g.fillOval(
					x - width - 8,
					y - 11,
					(width + 8) * 2,
					22
			);

			// 3. OUTER COLORED FLAME
			Path2D outer = new Path2D.Double();

			outer.moveTo(x - width, y + 3);

			outer.curveTo(
					x - width - 7, y - flameHeight * 0.35,
					x - 8, y - flameHeight * 0.72,
					x + sway, y - flameHeight
			);

			outer.curveTo(
					x + 8, y - flameHeight * 0.75,
					x + width + 6, y - flameHeight * 0.35,
					x + width, y + 3
			);

			outer.closePath();

			g.setPaint(new GradientPaint(
					x, y,
					new Color(
							color.getRed(),
							color.getGreen(),
							color.getBlue(),
							alpha
					),
					x, y - flameHeight,
					new Color(
							color.getRed(),
							color.getGreen(),
							color.getBlue(),
							alpha / 4
					)
			));

			g.fill(outer);

			// 4. WHITE-HOT INNER FLAME
			int innerHeight = (int)(flameHeight * 0.65);
			int innerWidth = Math.max(5, width / 2);

			Path2D inner = new Path2D.Double();

			inner.moveTo(x - innerWidth, y + 1);

			inner.curveTo(
					x - innerWidth, y - innerHeight * 0.35,
					x - 3, y - innerHeight * 0.65,
					x + sway * 0.5, y - innerHeight
			);

			inner.curveTo(
					x + 4, y - innerHeight * 0.65,
					x + innerWidth, y - innerHeight * 0.3,
					x + innerWidth, y + 1
			);

			inner.closePath();

			g.setPaint(new GradientPaint(
					x, y,
					new Color(255, 255, 255, (int)(220 * fade)),
					x, y - innerHeight,
					new Color(255, 255, 210, (int)(70 * fade))
			));

			g.fill(inner);
		}
		finally
		{
			g.dispose();
		}
	}

	private boolean isFacingLane(int fishLane, LocalPoint center)
	{
		Player player = client.getLocalPlayer();

		if (player == null)
		{
			return false;
		}

		LocalPoint position = player.getLocalLocation();

		// The player's target facing direction.
		int playerAngle = player.getOrientation();

		int tileSize = Perspective.LOCAL_TILE_SIZE;

		// Positions of the three fishing targets.
		int targetY = Math.max(
				center.getY(),
				position.getY() + tileSize
		);

		int closestLane = 99;
		int closestDifference = Integer.MAX_VALUE;

		// Determine which of the three lanes the player faces.
		for (int lane = -1; lane <= 1; lane++)
		{
			double dx = center.getX()
					+ lane * tileSize - position.getX();

			double dy = targetY - position.getY();

			// Convert the target direction to RuneLite's
			// 0–2047 orientation system.
			int expectedAngle = (int) Math.round(
					Math.atan2(-dx, -dy) * 1024.0 / Math.PI
			) & 2047;

			int difference = Math.abs(playerAngle - expectedAngle);

			difference = Math.min(
					difference,
					2048 - difference
			);

			if (difference < closestDifference)
			{
				closestDifference = difference;
				closestLane = lane;
			}
		}

		// Require the player to face the corresponding lane.
		return closestLane == fishLane && closestDifference <= 300;
	}

}
