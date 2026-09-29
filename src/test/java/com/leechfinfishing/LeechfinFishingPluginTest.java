package com.leechfinfishing;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class LeechfinFishingPluginTest
{
	public static void main(String[] args) throws Exception
	{
		System.out.println("TEST LAUNCHER STARTED");

		ExternalPluginManager.loadBuiltin(LeechfinFishingPlugin.class);

		System.out.println("PLUGIN REGISTERED");

		RuneLite.main(args);
	}
}