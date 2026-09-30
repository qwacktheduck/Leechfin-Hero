
package com.leechfinfishing;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;

import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.components.LineComponent;

public class LeechfinScoreOverlay extends OverlayPanel
{
    private final LeechfinFishingPlugin plugin;

    @Inject
    public LeechfinScoreOverlay(LeechfinFishingPlugin plugin)
    {
        this.plugin = plugin;

        setPosition(OverlayPosition.TOP_LEFT);
        setLayer(OverlayLayer.ABOVE_WIDGETS);

        panelComponent.setPreferredSize(new Dimension(150, 0));
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        // Only display while fishing or near the fishing spot.
        if (!plugin.isScoreSessionActive()
                && (plugin.getLeechfinFishingPoints() == null
                || plugin.getLeechfinFishingPoints().isEmpty()))
        {
            return null;
        }

        int multiplier = plugin.getScoreMultiplier();

        Color multiplierColor = Color.WHITE;

        if (multiplier == 2)
        {
            multiplierColor = Color.YELLOW;
        }
        else if (multiplier == 3)
        {
            multiplierColor = Color.ORANGE;
        }
        else if (multiplier == 4)
        {
            multiplierColor = Color.CYAN;
        }

        // HEADER
        panelComponent.getChildren().add(
                LineComponent.builder()
                        .left("LEECHFIN HERO")
                        .leftColor(Color.CYAN)
                        .build()
        );

        // CURRENT SCORE
        panelComponent.getChildren().add(
                LineComponent.builder()
                        .left("Score")
                        .right(String.format("%,d", plugin.getRunScore()))
                        .build()
        );

        // CURRENT STREAK
        panelComponent.getChildren().add(
                LineComponent.builder()
                        .left("Note Streak")
                        .right(String.valueOf(plugin.getCatchStreak()))
                        .build()
        );

        // MULTIPLIER
        panelComponent.getChildren().add(
                LineComponent.builder()
                        .left("Multiplier")
                        .right("x" + multiplier)
                        .rightColor(multiplierColor)
                        .build()
        );

        // Divider between current stats and personal records
        panelComponent.getChildren().add(
                LineComponent.builder()
                        .left("─────────────")
                        .leftColor(Color.GRAY)
                        .build()
        );

        // HIGHEST STREAK
        panelComponent.getChildren().add(
                LineComponent.builder()
                        .left("Highest Streak")
                        .right(String.valueOf(plugin.getHighestStreak()))
                        .rightColor(Color.YELLOW)
                        .build()
        );

        // PERSONAL BEST
        panelComponent.getChildren().add(
                LineComponent.builder()
                        .left("High Score")
                        .right(String.format("%,d",
                                plugin.getHeroHighScore()))
                        .rightColor(Color.YELLOW)
                        .build()
        );

        return super.render(graphics);
    }
}
