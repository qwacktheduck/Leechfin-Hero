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

public class LeechfinFishingOverlay extends Overlay {
	private final Client client;

	private final LeechfinFishingPlugin plugin;

	private final LeechfinFishingConfig config;

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

		if (plugin.isLeechfinFishing()) {
			renderLeechfinTiles(graphics, worldView);

			// Our new projectile highlights
			renderLeechfinProjectiles(graphics);
		} else {
			// render only the central tile when not actively fishing
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
}
