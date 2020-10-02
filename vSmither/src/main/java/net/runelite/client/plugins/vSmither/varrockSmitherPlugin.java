/*
 * Copyright (c) 2018, SomeoneWithAnInternetConnection
 * Copyright (c) 2018, oplosthee <https://github.com/oplosthee>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.client.plugins.vSmither;

import com.google.inject.Provides;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.events.*;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginType;
import net.runelite.client.plugins.botutils.BotUtils;
import org.pf4j.Extension;
import net.runelite.api.ItemID;


@Extension
@PluginDependency(BotUtils.class)
@PluginDescriptor(
	name = "Varrock Smither",
	enabledByDefault = false,
	description = "Smiths item of choice at varrock",
	tags = {"smith","varrock smithing"},
	type = PluginType.SKILLING
)
@Slf4j
public class varrockSmitherPlugin extends Plugin {
	@Inject
	private Client client;

	@Inject
	private varrockSmitherConfiguration config;

	@Inject
	private BotUtils utils;

	@Inject
	private ConfigManager configManager;

	@Inject
	private ItemManager itemManager;

	boolean startSmithing;
	MenuEntry targetMenu;
	Player player;
	int timeOut = 0;
	boolean moving;
	boolean animating;
	boolean bankOpen;
	GameObject targetObject;
	String status;
	int barID;
	GameObject bankStand;
	LocalPoint beforeLoc = new LocalPoint(0, 0);
	long sleepLength;
	int tickDelay = 0;
	boolean firstTime;
	boolean withDrawNow;
	int smithingWidg;
	//String item  = "SMITHING_ANVIL_DAGGER";
	List<Integer> REQUIRED_ITEMS = new ArrayList<>();
	@Provides
	varrockSmitherConfiguration provideConfig(ConfigManager configManager) {
		return configManager.getConfig(varrockSmitherConfiguration.class);
	}

	@Override
	protected void startUp() {

		withDrawNow  =false;
		firstTime = true;
		startSmithing = false;
		//barID = 2349;

		REQUIRED_ITEMS.add(ItemID.HAMMER);

		utils.sendGameMessage("plogin Started");

	}

	@Override
	protected void shutDown() {
	}

	@Subscribe
	public void onConfigButtonClicked(ConfigButtonClicked event) {

		if (!event.getGroup().equalsIgnoreCase("varrocksmither")) {
			return;
		}

		if (event.getKey().equals("startButton")) {
			utils.sendGameMessage("starting");

			barID = config.getBarType().getId();
			itemToSmith();
			startSmithing = true;
		} else if (event.getKey().equals("stopButton")) {
			utils.sendGameMessage("stopping");
			startSmithing = false;
		}
	}

	private String getStatus() {
		Widget smithingInterface = client.getWidget(WidgetInfo.SMITHING_INVENTORY_ITEMS_CONTAINER);
		Widget levelUp = client.getWidget(WidgetInfo.LEVEL_UP_SKILL);
		if (timeOut > 0) {
			return "TIMEOUT";
		}
		if (utils.isMoving(beforeLoc))
		{
			timeOut = tickDelay();
			return "MOVING";
		}
		if (client.getLocalPlayer().getAnimation() == 898) {
			return "IN_ANIMATION";
		}
		if (levelUp != null && !levelUp.isHidden() && utils.inventoryItemContainsAmount(barID, 4, false, false)) {
			return "ANVILING";
		}
		if (utils.isBankOpen()) {
			return bankState();
		}


		if (smithingInterface != null && !smithingInterface.isHidden()) {
			return "CRAFT_ITEM";
		}
		if(utils.inventoryItemContainsAmount(barID,3,false,false)){
			return "ANVILING";
		}
		if(utils.inventoryItemContainsAmount(barID,2,false,false)|| utils.inventoryItemContainsAmount(barID,1,false,false)|| utils.inventoryItemContainsAmount(barID,0,false,false)){
			return "BANKING";
		}

		if (!utils.inventoryFull()) {
			return "BANKING";
		}
		if (utils.inventoryItemContainsAmount(barID, 27, false, false)) {
			return "ANVILING";
		}

		if (utils.isAnimating() || utils.isMoving(beforeLoc)) {

			utils.sendGameMessage("you are moving");
			return "WAITING";
		} else {
			return "UNKNOWN";
		}
	}

	private String bankState() {

		if (utils.inventoryContains(ItemID.HAMMER) && withDrawNow) {
			withDrawNow =false;
			return "WITHDRAW_BAR";
		}
		if (utils.inventoryEmpty()) {
			return "WITHDRAW_HAMMER";
		}


		if( utils.inventoryItemContainsAmount(barID,2,false,true)|| utils.inventoryItemContainsAmount(barID,1,false,true) || utils.inventoryItemContainsAmount(barID,0,false,true)){
			firstTime = false;
			withDrawNow = true;
			return "DEPOSIT";
		}


		if (!utils.inventoryFull() && !utils.inventoryEmpty()) {
			withDrawNow = true;
			return "DEPOSIT";
		}



		if (utils.inventoryFull()) {
			firstTime = true;
			return "CLOSE_BANK";
		}

		else {
			return "UNKNOWN";
		}
	}

	@Subscribe
	public void onGameTick(GameTick event) {
		player = client.getLocalPlayer();

		if (!startSmithing) {
			return;
		}



		if (client != null && player != null && client.getGameState() == GameState.LOGGED_IN) {
			status = getStatus();
			utils.sendGameMessage(status);
			switch (status) {
				case "TIMEOUT":
					utils.handleRun(30, 20);
					timeOut--;
					break;
				case "MOVING":
					utils.handleRun(30, 20);
					timeOut = tickDelay();
					break;
				case "ANVILING":
					utils.sendGameMessage("Walking to Anvil");
					firstTime = true;
					goToAnvil();
					utils.handleRun(30, 20);
					timeOut = tickDelay();
					break;
				case "IN_ANIMATION":
					utils.handleRun(30, 20);
					timeOut = 5;
					break;
				case "CRAFT_ITEM":
					utils.sendGameMessage("ITS OPEN BRO");
					itemToSmith();
					chooseItem();
					timeOut = 5;
					break;
				case "BANKING":
					utils.sendGameMessage("GOING TO BANK");
					goToBank();
					utils.handleRun(30, 20);
					timeOut = tickDelay();
					break;
				case "UNKNOWN":
					//utils.sendGameMessage("UNKNOWN");
					break;
				case "DEPOSIT":

					utils.depositAllExcept(REQUIRED_ITEMS);
					timeOut = tickDelay();
					break;
				case "WITHDRAW_HAMMER":
					utils.withdrawItemAmount(ItemID.HAMMER, 1);
					timeOut = tickDelay();
					break;
				case "WITHDRAW_BAR":
					utils.withdrawAllItem(barID);
					timeOut = tickDelay();
					break;
				case "CLOSE_BANK":
					utils.closeBank();
					utils.sendGameMessage("GOING TO WALK TO FURNACE");
					timeOut = tickDelay();
					break;


				//utils.sendGameMessage("WITHDRAWING ITEMS");


			}
			beforeLoc = player.getLocalLocation();

		}

	}
	private void itemToSmith(){
		switch(config.craftItem()){
			case Dagger:
				smithingWidg  = WidgetInfo.SMITHING_ANVIL_DAGGER.getId();
				break;
			case Sword:
				smithingWidg = WidgetInfo.SMITHING_ANVIL_SWORD.getId();
				break;
				//smithingWidg = 20447242;
			case Scimitar:
				smithingWidg = WidgetInfo.SMITHING_ANVIL_SCIMITAR.getId();
				break;
			case Long_sword:
				smithingWidg = WidgetInfo.SMITHING_ANVIL_LONG_SWORD.getId();
				break;
			case Axe:
				smithingWidg = WidgetInfo.SMITHING_ANVIL_AXE.getId();
				break;
			case Mace:
				smithingWidg = WidgetInfo.SMITHING_ANVIL_MACE.getId();
				break;
			case Warhammer:
				smithingWidg = WidgetInfo.SMITHING_ANVIL_WARHAMMER.getId();
				break;
			case Battle_axe:
				smithingWidg = WidgetInfo.SMITHING_ANVIL_BATTLE_AXE.getId();
				break;
			case Double_Handed_Sword:
				smithingWidg = WidgetInfo.SMITHING_ANVIL_TWO_H_SWORD.getId();
				break;
			case Chain_Body:
				smithingWidg = WidgetInfo.SMITHING_ANVIL_CHAIN_BODY.getId();
				break;
			case Plate_legs:
				smithingWidg = WidgetInfo.SMITHING_ANVIL_PLATE_LEGS.getId();
				break;
			case Plate_skirt:
				smithingWidg = WidgetInfo.SMITHING_ANVIL_PLATE_SKIRT.getId();
				break;
			case Plate_body:
				smithingWidg = WidgetInfo.SMITHING_ANVIL_PLATE_BODY.getId();
				break;
			case Nails:
				smithingWidg = WidgetInfo.SMITHING_ANVIL_NAILS.getId();
				break;
			case Medium_Helm:
				smithingWidg = WidgetInfo.SMITHING_ANVIL_MED_HELM.getId();
				break;
			case Full_helm:
				smithingWidg = WidgetInfo.SMITHING_ANVIL_FULL_HELM.getId();
				break;
			case Square_shield:
				smithingWidg = WidgetInfo.SMITHING_ANVIL_SQ_SHIELD.getId();
				break;
			case Kite_shield:
				smithingWidg = WidgetInfo.SMITHING_ANVIL_KITE_SHIELD.getId();
				break;
			case Dart_tips:
				smithingWidg = WidgetInfo.SMITHING_ANVIL_DART_TIPS.getId();
				break;
			case Arrowtips:
				smithingWidg = WidgetInfo.SMITHING_ANVIL_ARROW_HEADS.getId();
				break;
			case Knives:
				smithingWidg = WidgetInfo.SMITHING_ANVIL_KNIVES.getId();
				break;
			case Darts:
				smithingWidg = WidgetInfo.SMITHING_ANVIL_DART_TIPS.getId();
				break;
		}
	}

	private void chooseItem() {
utils.sendGameMessage(Integer.toString(smithingWidg));
		targetMenu = new MenuEntry("", "", 1, MenuOpcode.CC_OP.getId(), -1, smithingWidg, false);
		if (targetMenu != null) {
			utils.sendGameMessage("not null");
			utils.setMenuEntry(targetMenu);
			utils.delayMouseClick(targetObject.getConvexHull().getBounds(), 80);
		} else {
			utils.sendGameMessage("its null");
		}

		utils.delayClickRandomPointCenter(-75, 75, sleepDelay());
		//utils.delayMouseClick(getRandomNullPoint(),sleepDelay());
		//utils.del
		//utils.setMenuEntry(targetMenu);
		//utils.delayMouseClick(targetObject.getConvexHull().getBounds(), 80);
	}
	/*
	private void interactAnvil()
	{
		Widget smithingInterface = client.getWidget(WidgetInfo.SMITHING_INVENTORY_ITEMS_CONTAINER);
		if (smithingInterface != null && !smithingInterface.isHidden()) {
			// do the stuff you would do if the interface is open here
		}
	}
	*/

	private long sleepDelay() {
		sleepLength = utils.randomDelay(false, 60, 300, 10, 100);
		return sleepLength;
	}

	private Point getRandomNullPoint() {
		if (client.getWidget(161, 34) != null) {
			Rectangle nullArea = client.getWidget(161, 34).getBounds();
			return new Point((int) nullArea.getX() + utils.getRandomIntBetweenRange(0, nullArea.width), (int) nullArea.getY() + utils.getRandomIntBetweenRange(0, nullArea.height));
		}

		return new Point(client.getCanvasWidth() - utils.getRandomIntBetweenRange(0, 2), client.getCanvasHeight() - utils.getRandomIntBetweenRange(0, 2));
	}

	private int tickDelay() {
		int tickLength = (int) utils.randomDelay(false, 1, 3, 1, 2);
		log.debug("tick delay for {} ticks", tickLength);
		return tickLength;
	}

	private void goToBank() {
		targetObject = utils.findNearestGameObjectWithin(player.getWorldLocation(), 25, 34810);
		if (targetObject != null) {
			targetMenu = new MenuEntry("", "", targetObject.getId(), 4,
					targetObject.getSceneMinLocation().getX(), targetObject.getSceneMinLocation().getY(), false);
			utils.setMenuEntry(targetMenu);
			utils.delayMouseClick(targetObject.getConvexHull().getBounds(), 80);
		} else {
			utils.sendGameMessage("Cannot find bank");
		}
	}

	private void goToAnvil() {
		targetObject = utils.findNearestGameObjectWithin(player.getWorldLocation(), 25, 2097);
		if (targetObject != null) {
			targetMenu = new MenuEntry("", "", targetObject.getId(), 3,
					targetObject.getSceneMinLocation().getX(), targetObject.getSceneMinLocation().getY(), false);
			utils.setMenuEntry(targetMenu);
			utils.delayMouseClick(targetObject.getConvexHull().getBounds(), 80);
		}
	}
}