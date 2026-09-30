
package com.leechfinfishing;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("leechfinfishing")
public interface LeechfinFishingConfig extends Config
{
	// Our only visible configuration option.
	@ConfigItem(
			position = 0,
			keyName = "resetStreaks",
			name = "Reset streaks",
			description = "Toggle to reset current and highest note streaks.",
			warning = "Reset your current and highest note streak? Scores will not be affected."
	)
	default boolean resetStreaks()
	{
		return false;
	}

	@ConfigItem(
			position = 1,
			keyName = "resetHighScore",
			name = "Reset high score",
			description = "Clears your saved Leechfin Hero high score.",
			warning = "Are you sure you want to reset your high score?"
	)
	default boolean resetHighScore()
	{
		return false;
	}
	// ORIGINAL SETTINGS - HIDDEN BUT PRESERVED

	@ConfigItem(
			keyName = "highlightActiveTile",
			name = "Highlight active tile",
			description = "",
			hidden = true
	)
	default boolean highlightActiveTile()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
			keyName = "activeHighlightColor",
			name = "Highlight color",
			description = "",
			hidden = true
	)
	default Color activeHighlightColor()
	{
		return Color.GREEN;
	}

	@Alpha
	@ConfigItem(
			keyName = "activeFillColor",
			name = "Fill color",
			description = "",
			hidden = true
	)
	default Color activeFillColor()
	{
		return new Color(0, 0, 0, 50);
	}

	@ConfigItem(
			keyName = "highlightNextTile",
			name = "Highlight next tile",
			description = "",
			hidden = true
	)
	default boolean highlightNextTile()
	{
		return false;
	}

	@Alpha
	@ConfigItem(
			keyName = "nextHighlightColor",
			name = "Next highlight color",
			description = "",
			hidden = true
	)
	default Color nextHighlightColor()
	{
		return Color.YELLOW;
	}

	@Alpha
	@ConfigItem(
			keyName = "nextFillColor",
			name = "Next fill color",
			description = "",
			hidden = true
	)
	default Color nextFillColor()
	{
		return new Color(0, 0, 0, 50);
	}

	@Alpha
	@ConfigItem(
			keyName = "leechfinSpotColor",
			name = "Leechfin spot color",
			description = "",
			hidden = true
	)
	default Color leechfinSpotColor()
	{
		return Color.CYAN;
	}

	@Alpha
	@ConfigItem(
			keyName = "fullInventoryColor",
			name = "Full inventory color",
			description = "",
			hidden = true
	)
	default Color fullInventoryColor()
	{
		return Color.RED;
	}
}
