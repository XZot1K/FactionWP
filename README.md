**FactionsWP supports Minecraft versions 1.8 - 1.20 by using automatic version detection features and will receive updates as newer versions of Minecraft are released. 1.7.10 is supported, but not as much as 1.8+.**

![](https://i.imgur.com/WnpKYKl.png)

## **⚒ What is FactionWP? ⛏**

Faction Wands & Pickaxes (FactionWP) is an efficiently developed plugin that introduces a large variety of famous wands & pickaxes, such as the 'Trench Pickaxe', 'Harvester Hoe', and 'Sell Wand' to the Minecraft world with high control. The plugin is and was thoroughly tested on 300+ player servers to ensure performance is optimal. Still not convinced? Well, FactionWP also contains many advanced features like per-tool cooldowns, uses, and configuration to provide full control!

## **⚒ How is FactionWP Different? ⛏**

- FactionWP uses micro-optimization anywhere it can to complete tasks faster than normal, benchmarking approximately 250ms faster at all times than traditional methods.
- FactionWP's tools do not require insane configuration for different radiuses, uses, counter, or even overall looks. Instead, it uses advanced indexing to build your tools off of what is entered in the give command. This feature alone can save you up to an hour of time in terms of configuration.
- FactionWP is carefully tested and developed to ensure it will _**NOT**_ break other plugins, even if that other plugin is another tool plugin!
- Duplications are a no go, this is the last thing a server owner wants to see. This being said, the tools are stress tested and tested even in the worst conditions (Low tick rate or high ping) to make sure items are impossible to duplicate using these tools.
- All tools are given many checks and allowed to react as they do in Vanilla MC to provide the smoothest experience. These checks also prevent a player from creating fake tools.


*Below is a preview of _**Some**_ of the currently available wands & pickaxes (tools):*
(Please note that this GIF image does **_NOT_** contain every tool currently available as new tools are added nearly every couple updates.)

![](https://i.imgur.com/uZrfYvU.gif)

**Please note that each and every tool, including formats, can be entirely customized to your liking. The following images are of the _Default_ tool configurations.**

**The currently available tools are:**

```
* Trench Pickaxe
* Trench Shovel/Spade
* Tray Pickaxe
* Harvester Hoe
* Harvester Axe
* Platform Wand
* Wall Wand
* Ice Wand
* Lightning Wand
* Sell Wand
* Sand Wand
* Projectile Wand
* Craft Wand (Currently Just Nuggets & Ingots to Blocks)
```


**NOTE:** Every tool is fully customizable from the ground up, even what items they physically are regardless of their name. In total there are 12 tools as of 1.6.4-B.

## **⚒ Features ⛏**

Currently, FactionWp contains a crazy number of features. The plugin's most treasured features are as follows:

- **All-In-One** - Think it's annoying to have a separate plugin for each wand or pickaxe? Good same here. A huge benefit is FactionWP has everything inside one plugin which leaves less of a hassle in the long run!
- **Unlimited Use Tools** - By simply setting uses below zero the tool gain unlimited uses, can't get much easier than that!
- **Heavily Reliable** - FactionWP is made to be highly flexible, meaning if you were to change Minecraft versions or change something huge in the configuration the plugin will attempt to adjust to those changes rather than putting more onto the to-do list.
- **Hook Ready -** FactionWP has many hooks including WorldGuard, GriefPrevention, Factions, ShopGUIPlus, ASkyBlock, and More.
- **A growing variety of wands & pickaxes (tools)** - each containing their own unique abilities designed with the help of many server owners.
- **Per-item uses** - each wand or pickaxe (tool) has its own unique use count where when used up the item is destroyed. (This feature is fully configurable.)
- **Block Break Counter** - block-breaking based tools are able to be given a block counter.
- **Items Sold Counter** - item selling based tools are able to be given items sold counter.
- **Per-type cooldowns** - each wand or pickaxe (tool) type has a fully configurable cooldown using the system time. This feature was designed to provide server owners with more options to control the items and optimize performance on their side.
- **Global cooldowns** - each tool has the option to share a global cooldown with others.
- **High-quality code** - using timestamp testing and server reports the plugin's code is written for speed and efficiency.
- **Strong developer API** - as the plugin was developed it was thoroughly developed to have the capability of being hooked into.
- **Auto-Updating Configuration** - If your configuration contains extra/un-needed data or even is missing new messages/options, the configuration will update itself on startup!
- **Auto-Packet Server Version Detection** - The plugin finds your server version and adapts to give you a great experience!
- **Full configuration** - every feature as the plugin was developed was given its own configuration option alongside a detailed description of its purpose.
- **Expandable** - the plugin is also ready for new wands or pickaxes to be implemented into the plugin after heavy testing, so suggestions are welcome!
- **Auto-detecting packet handlers** - the plugin will auto-detect your server's current version and select packets based on the version.
- **Per Tool Modifiers/Boosters** - select tools such as the sell wand and harvester hoe can be given a custom modifier. This allows multiple versions of the same tool that use different modifiers/boosters by simply just typing a number inside the give command!
- **Drag & Drop Repairing** - most tools can be dragged and dropped on identical tools merge item uses!
- **Command Tool Tab-Completion** - tool types will be listed similar to player names when using commands.


## **⚒ Commands & Permissions ⛏**
```
⚒ Commands ⛏

* /factionwp give <player> <type> <amount> <uses> <radius/modifier> - this command will give the entered player the specified type with the defined parameters. Don’t worry, if the type requires specific parameters there will be a detailed message in assistance to the command failure by default.

* /factionwp info – provides the sender with the plugin’s information.

* /factionwp list – provides the sender with a list of all currently available tool types.

* /factionwp reload – reloads the plugin’s configuration file.


⚒ Permissions ⛏

* factionwp.admin - gives the player access to all the plugin's features.

* factionwp.all - gives the player access to all wands and pickaxes.

* factionwp.give – allows the sender to use the give command.

* factionwp.info – allows the sender to use the info command.

* factionwp.reload – allows the sender to use the reload command.

* factionwp.sandwand – allows the player to use the sand wand.

* factionwp.craftwand – allows the player to use the craft wand.

* factionwp.sellwand – allows the player to use the sell wand.

* factionwp.lightningwand – allows the player to use the lightning wand.

* factionwp.smeltwand – allows the player to use the smelt wand.

* factionwp.harvesterhoe – allows the player to use the harvester hoe.

* factionwp.harvesteraxe – allows the player to use the harvester hoe.

* factionwp.icewand – allows the player to use the ice wand.

* factionwp.traypickaxe – allows the player to use the tray pickaxe.

* factionwp.trenchpickaxe – allows the player to use the trench pickaxe.

* factionwp.trenchshovel – allows the player to use the trench shovel.

* factionwp.wallwand - allows the player to use the wall wand.

* factionwp.platformwand - allows the player to use the platform wand.

* factionwp.projectilewand - allows the player to use the projectile wand.
```

## **⚒ Installation ⛏**

1. Purchase and download the plugin.
2. Locate the FactionWP.jar and relocate it to your server's plugins folder.
3. Start or restart the server to create any missing configuration files.
4. Modify your configuration files to ensure correct materials, particle names, and sound names are used alongside any other modifications you desire.
5. Restart the server and, of course, enjoy!


**All soft-dependencies are disabled by default.**

- Vault (Optional)
- Any Hook Dependencies (Optional)

**All hooks are automatic now as of 5.3.8, but the WorldGuard hook requires the 'fwp-tools' region flag to allow access in specific regions.**

## **⚒ Developer API ⛏**

FactionWP's developer API is quite simplistic. In order to hook into the plugin you simply only need access to the plugin's instance. For ease of use, the 'FactionWP.getPluginInstance()' code can be used. Below is a list of all intended classes that are expected to be used within the API:
```
* Manager (class) - this class contains all of the important and useful methods used throughout the plugin. The instance of the class must be retrieved from the plugin's main instance.

* ToolUseEvent (Bukkit Event) - this event is called when any tool is used. This is a great resource for implementing anything on tool usage or blocking tool usage entirely.

* HookCallEvent (Bukkit Event) - this event is called when a hook check is initiated when checking if a location is safe for modification. This event can be used to implement your own hooks into the plugin!

### This class was removed in version 1.0.6.
* Direction (Enum) - this enum is used for direction methods inside the Manager class.

* WPType (Customized Enum) - this enum contains all available tool types along with values that are stored within each of them.
```
There are other classes that can be accessed; however, the above classes are the recommended API classes.

## **⚒ Terms & Conditions ⛏**

**The following cases are prohibited:**

- Decompilation, without permission.
- [Redistribution, without permission.
- Ownership claiming.
- No account-recovery.
- Review section issues or ranting. If you encounter an issue or simply do not like a feature please contact me before attempting to shame the plugin, otherwise, respect for the user will be lowered.
- Chargebacks or refunds.
- Inconveniences or Issues reported in the review section. (Please contact the developer about possible issues or inconveniences as reviews with either of these will NOT be tolerated.)
- If a feature or change is requested it will be taken into consideration; however, the request is not guaranteed to make it into an update.

**_Note_:** If you have any questions or concerns please contact me directly as I, XZot1K, am here to provide the best support possible for each user.

**These terms and conditions are further extended by the TOS found on the ZotWare official website https://www.zotware.dev/tos**
