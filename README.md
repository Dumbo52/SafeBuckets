SafeBuckets
===========

Prevents flowing and physics events from affecting bucket-placed fluids, so
that player-placed water and lava never flow. Doesn't catch blocks placed with
actual fluid blocks, so people with access to place them can still add normal
water.

Commands and Usage
------------------

**/sb reload**

Reloads SafeBuckets configuration.

**/sb tool [on|off|give]**

Turns on, turns off, or gives you the SafeBuckets dispenser inspector tool. If no options are specified, your tool status will be toggled.

Left clicking a dispenser with it will the dispenser inspector tool will register or unregister the dispenser. Registered dispensers will dispense unsafe water, while unregistered dispensers will dispense safe water. Right clicking a dispenser with it will check the status of the dispenser. The default dispenser inspector tool is the blaze rod.

**/sb toolblock [on|off|give]**

Turns on, turns off, or gives you the SafeBuckets fluid inspector tool. If no options are specified, your tool status will be toggled.

Left clicking a block face with the fluid inspector tool which the fluid source is against will either set safe or set unsafe the source. Safe sources will not flow unless unsafe water flows into them, while unsafe sources act like normal fluid sources. Placing it where a fluid source is will check the status of the source. The default fluid inspector tool is lapis lazuli ore. 

**/sb setunsafe**, **/sb flow**

Sets unsafe all fluid sources within your WorldEdit selection. WorldEdit must be installed and you must have a cuboid region selected for this to work.

**/sb setsafe**, **/sb static**

Sets safe all fluid sources within your WorldEdit selection. WorldEdit must be installed and you must have a cuboid region selected for this to work.

**/sb help**

Displays information about these commands in-game.

Configuration
-------------

**debug.console** (boolean)

Show SafeBuckets debug information in the console.

**debug.players** (boolean)

Show SafeBuckets debug information to players with the *safebuckets.debug* permission.

**tool.block** (material)

The [material name](http://jd.bukkit.org/beta/apidocs/org/bukkit/Material.html#enum_constant_summary) of the fluid inspector tool block.

**tool.item** (material)

The [material name](http://jd.bukkit.org/beta/apidocs/org/bukkit/Material.html#enum_constant_summary) of the dispenser inspector tool.

**tool.default-on** (boolean)

Inspector tools enabled by default if the player has permission for them.

**bucket.enabled** (boolean)

Buckets can be emptied.

**bucket.safe** (boolean)

Prevent safe sources from flowing.

**bucket.place-safe** (boolean)

Fluids placed with buckets are automatically set safe.

**dispenser.enabled** (boolean)

Dispensers can dispense fluids.

**dispenser.safe** (boolean)

Unregistered dispensers will dispense safe fluids.

**dispenser.place-safe** (boolean)

Dispensers are initially unregistered when placed.

**dispenser.whitelist** (boolean)

Already existing dispensers act as unregistered dispensers.

**region.maximum-volume** (integer)

The maximum volume of a WorldEdit selection when setting the selection safe or unsafe. Set to -1 for no limit.

**flow.maximum-depth** (integer)

The maximum recursion depth for removing child flows when setting fluids safe. Set to -1 for no limit.
