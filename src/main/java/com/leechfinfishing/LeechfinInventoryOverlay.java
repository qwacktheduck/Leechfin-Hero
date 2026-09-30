
package com.leechfinfishing;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import javax.inject.Inject;

import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.ui.overlay.WidgetItemOverlay;

public class LeechfinInventoryOverlay extends WidgetItemOverlay
{
    private static final int LEECHFIN_ITEM_ID = 33621;

    private final LeechfinFishingPlugin plugin;

    @Inject
    public LeechfinInventoryOverlay(LeechfinFishingPlugin plugin)
    {
        this.plugin = plugin;
        showOnInventory();
    }

    @Override
    public void renderItemOverlay(
            Graphics2D graphics,
            int itemId,
            WidgetItem widgetItem)
    {
        // Only highlight actual leechfin inventory items.
        if (itemId != LEECHFIN_ITEM_ID)
        {
            return;
        }

        // Only highlight during fishing, while fish 10-14 are active.
        if (!plugin.isLeechfinFishing() ||
                !plugin.isStarPowerFish(plugin.getActiveLeechfin()))
        {
            return;
        }


        Rectangle bounds = widgetItem.getCanvasBounds();

        if (bounds == null)
        {
            return;
        }

// STAR POWER PULSE
// 600 milliseconds = approximately one OSRS game tick.
        long pulsePeriod = 600_000_000L;

        double phase = (System.nanoTime() % pulsePeriod)
                / (double) pulsePeriod;

// Smoothly oscillates between 0 and 1.
        double pulse = (Math.sin(2 * Math.PI * phase) + 1) / 2;

// Calculate the changing brightness and thickness.
        int fillAlpha = (int) (25 + pulse * 110);
        int borderAlpha = (int) (110 + pulse * 145);
        float borderWidth = 1.5f + (float) (pulse * 2.0);

        Stroke previousStroke = graphics.getStroke();

// Soft outer glow.
        graphics.setColor(new Color(
                0, 255, 255, (int) (15 + pulse * 45)
        ));

        graphics.setStroke(new BasicStroke(borderWidth + 4));

        graphics.drawRoundRect(
                bounds.x - 2,
                bounds.y - 2,
                bounds.width + 3,
                bounds.height + 3,
                8, 8
        );

// Pulsing translucent fill.
        graphics.setColor(new Color(0, 255, 255, fillAlpha));

        graphics.fillRoundRect(
                bounds.x,
                bounds.y,
                bounds.width,
                bounds.height,
                6, 6
        );

// Pulsing cyan border.
        graphics.setColor(new Color(0, 255, 255, borderAlpha));
        graphics.setStroke(new BasicStroke(borderWidth));

        graphics.drawRoundRect(
                bounds.x,
                bounds.y,
                bounds.width - 1,
                bounds.height - 1,
                6, 6
        );

// Restore original graphics settings.
        graphics.setStroke(previousStroke);

    }
}
