package lycanite.lycanitesmobs.junglemobs;

import lycanite.lycanitesmobs.AssetManager;
import lycanite.lycanitesmobs.LycanitesMobs;
import lycanite.lycanitesmobs.ObjectManager;
import lycanite.lycanitesmobs.api.config.ConfigBase;
import lycanite.lycanitesmobs.api.dispenser.DispenserBehaviorMobEggCustom;
import lycanite.lycanitesmobs.api.info.*;
import lycanite.lycanitesmobs.api.item.ItemCustomFood;
import lycanite.lycanitesmobs.api.item.ItemTreat;
import lycanite.lycanitesmobs.api.mobevent.MobEventBase;
import lycanite.lycanitesmobs.api.mobevent.MobEventManager;
import lycanite.lycanitesmobs.api.spawning.SpawnTypeBase;
import lycanite.lycanitesmobs.api.spawning.SpawnTypeLand;
import lycanite.lycanitesmobs.api.spawning.SpawnTypeSky;
import lycanite.lycanitesmobs.junglemobs.block.BlockPoopCloud;
import lycanite.lycanitesmobs.junglemobs.block.BlockPropolis;
import lycanite.lycanitesmobs.junglemobs.block.BlockQuickWeb;
import lycanite.lycanitesmobs.junglemobs.block.BlockVeswax;
import lycanite.lycanitesmobs.junglemobs.dispenser.DispenserBehaviorPoop;
import lycanite.lycanitesmobs.junglemobs.entity.EntityConba;
import lycanite.lycanitesmobs.junglemobs.entity.EntityConcapedeHead;
import lycanite.lycanitesmobs.junglemobs.entity.EntityConcapedeSegment;
import lycanite.lycanitesmobs.junglemobs.entity.EntityGeken;
import lycanite.lycanitesmobs.junglemobs.entity.EntityPoop;
import lycanite.lycanitesmobs.junglemobs.entity.EntityTarantula;
import lycanite.lycanitesmobs.junglemobs.entity.EntityUvaraptor;
import lycanite.lycanitesmobs.junglemobs.entity.EntityVespid;
import lycanite.lycanitesmobs.junglemobs.entity.EntityVespidQueen;
import lycanite.lycanitesmobs.junglemobs.item.ItemJungleEgg;
import lycanite.lycanitesmobs.junglemobs.item.ItemPoopCharge;
import lycanite.lycanitesmobs.junglemobs.item.ItemScepterPoop;
import lycanite.lycanitesmobs.junglemobs.mobevent.MobEventPoopParty;
import lycanite.lycanitesmobs.junglemobs.mobevent.MobEventTheSwarm;
import net.minecraft.block.BlockDispenser;
import net.minecraft.block.material.Material;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.entity.monster.EntityWitch;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityCow;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.passive.EntitySheep;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.registry.EntityRegistry;
import cpw.mods.fml.common.registry.GameRegistry;

@Mod(modid = JungleMobs.modid, name = JungleMobs.name, version = LycanitesMobs.version, dependencies = "required-after:" + LycanitesMobs.modid)
public class JungleMobs {
	
	public static final String modid = "junglemobs";
	public static final String name = "Lycanites Jungle Mobs";
	public static GroupInfo group;
	
	// Instance:
	@Instance(modid)
	public static JungleMobs instance;
	
	// Proxy:
	@SidedProxy(clientSide="lycanite.lycanitesmobs.junglemobs.ClientSubProxy", serverSide="lycanite.lycanitesmobs.junglemobs.CommonSubProxy")
	public static CommonSubProxy proxy;
	
	// ==================================================
	//                Pre-Initialization
	// ==================================================
	@EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		// ========== Config ==========
		group = new GroupInfo(this, "Jungle Mobs", 3)
				.setDimensionBlacklist("-1,1").setBiomes("JUNGLE").setDungeonThemes("JUNGLE, DUNGEON")
                .setEggName("jungleegg");
		group.loadFromConfig();
		
		
		// ========== Set Current Group ==========
		ObjectManager.setCurrentGroup(group);
		
		
		// ========== Create Items ==========
		ObjectManager.addItem("jungleegg", new ItemJungleEgg());
		
		ObjectManager.addItem("concapedemeatraw", new ItemCustomFood("concapedemeatraw", group, 2, 0.5F, ItemCustomFood.FOOD_CLASS.RAW).setPotionEffect(Potion.moveSlowdown.id, 45, 2, 0.8F));
		ObjectLists.addItem("rawmeat", ObjectManager.getItem("concapedemeatraw"));
		OreDictionary.registerOre("listAllchickenraw", ObjectManager.getItem("concapedemeatraw"));
		
		ObjectManager.addItem("concapedemeatcooked", new ItemCustomFood("concapedemeatcooked", group, 6, 0.7F, ItemCustomFood.FOOD_CLASS.COOKED).setPotionEffect(Potion.jump.id, 10, 2, 1.0F).setAlwaysEdible());
		ObjectLists.addItem("cookedmeat", ObjectManager.getItem("concapedemeatcooked"));
		OreDictionary.registerOre("listAllchickencooked", ObjectManager.getItem("concapedemeatcooked"));
		
		ObjectManager.addItem("tropicalcurry", new ItemCustomFood("tropicalcurry", group, 6, 0.7F, ItemCustomFood.FOOD_CLASS.MEAL).setPotionEffect(Potion.jump.id, 60, 2, 1.0F).setAlwaysEdible().setMaxStackSize(16), 3, 1, 6);
		ObjectLists.addItem("cookedmeat", ObjectManager.getItem("tropicalcurry"));
		
		ObjectManager.addItem("poopcharge", new ItemPoopCharge());
		ObjectManager.addItem("poopscepter", new ItemScepterPoop(), 2, 1, 1);

		ObjectManager.addItem("uvaraptortreat", new ItemTreat("uvaraptortreat", group));

		
		// ========== Create Blocks ==========
		ObjectManager.addBlock("quickweb", new BlockQuickWeb());
		AssetManager.addSound("poopcloud", group, "block.poopcloud");
		ObjectManager.addBlock("poopcloud", new BlockPoopCloud());
		ObjectManager.addBlock("propolis", new BlockPropolis());
		ObjectManager.addBlock("veswax", new BlockVeswax());
		
		
		// ========== Create Mobs ==========
		BlockDispenser.dispenseBehaviorRegistry.putObject(ObjectManager.getItem("jungleegg"), new DispenserBehaviorMobEggCustom());
		MobInfo newMob;
        
        newMob = new MobInfo(group, "geken", EntityGeken.class, 0x00AA00, 0xFFFF00)
		        .setPeaceful(false).setSummonable(true).setSummonCost(2).setDungeonLevel(0)
		        .addSubspecies(new Subspecies("scarlet", "uncommon")).addSubspecies(new Subspecies("keppel", "uncommon"));
		newMob.spawnInfo.setSpawnTypes("MONSTER")
				.setSpawnWeight(8).setAreaLimit(10).setGroupLimits(1, 3).setLightDark(false, true);
		ObjectManager.addMob(newMob);

		newMob = new MobInfo(group, "uvaraptor", EntityUvaraptor.class, 0x00FF33, 0xFF00FF)
		        .setPeaceful(false).setTameable(true).setSummonCost(4).setDungeonLevel(1)
		        .addSubspecies(new Subspecies("scarlet", "uncommon")).addSubspecies(new Subspecies("violet", "uncommon"));
		newMob.spawnInfo.setSpawnTypes("MONSTER")
				.setSpawnWeight(6).setAreaLimit(10).setGroupLimits(1, 3).setLightDark(false, true);
		ObjectManager.addMob(newMob);

		newMob = new MobInfo(group, "concapede", EntityConcapedeHead.class, 0x111144, 0xDD0000)
		        .setPeaceful(true).setSummonCost(2).setDungeonLevel(2)
		        .addSubspecies(new Subspecies("azure", "uncommon")).addSubspecies(new Subspecies("verdant", "uncommon"));
		newMob.spawnInfo.setSpawnTypes("CREATURE").setDespawn(false)
				.setSpawnWeight(18).setAreaLimit(10).setGroupLimits(1, 1).setLightDark(true, true);
		ObjectManager.addMob(newMob);

		newMob = new MobInfo(group, "concapedesegment", EntityConcapedeSegment.class, 0x000022, 0x990000)
		        .setPeaceful(true).setSummonCost(1).setDungeonLevel(-1)
		        .addSubspecies(new Subspecies("azure", "uncommon")).addSubspecies(new Subspecies("verdant", "uncommon"));
		newMob.spawnInfo.setSpawnTypes("CREATURE")
				.setSpawnWeight(0).setAreaLimit(0).setGroupLimits(0, 0).setLightDark(false, false).setEnabled(false);
		ObjectManager.addMob(newMob);

		newMob = new MobInfo(group, "tarantula", EntityTarantula.class, 0x008800, 0xDD0000)
		        .setPeaceful(false).setSummonable(true).setSummonCost(2).setDungeonLevel(2)
		        .addSubspecies(new Subspecies("scarlet", "uncommon")).addSubspecies(new Subspecies("russet", "uncommon"));
		newMob.spawnInfo.setSpawnTypes("MONSTER")
				.setSpawnWeight(6).setAreaLimit(10).setGroupLimits(1, 2).setLightDark(false, true);
		ObjectManager.addMob(newMob);
        
        newMob = new MobInfo(group, "conba", EntityConba.class, 0x665500, 0xCC99BB)
		        .setPeaceful(false).setSummonCost(2).setDungeonLevel(0)
		        .addSubspecies(new Subspecies("violet", "uncommon")).addSubspecies(new Subspecies("dark", "uncommon"));
		newMob.spawnInfo.setSpawnTypes("MONSTER")
				.setSpawnWeight(10).setAreaLimit(10).setGroupLimits(1, 3).setLightDark(false, true);
		ObjectManager.addMob(newMob);
        
        newMob = new MobInfo(group, "vespid", EntityVespid.class, 0x112200, 0x998800)
		        .setPeaceful(false).setSummonCost(2).setDungeonLevel(2)
		        .addSubspecies(new Subspecies("scarlet", "uncommon")).addSubspecies(new Subspecies("ashen", "uncommon"));
		newMob.spawnInfo.setSpawnTypes("MONSTER")
				.setSpawnWeight(10).setAreaLimit(10).setGroupLimits(1, 6).setLightDark(false, true);
		ObjectManager.addMob(newMob);
        
        newMob = new MobInfo(group, "vespidqueen", EntityVespidQueen.class, 0x223300, 0xFFCC00)
		        .setPeaceful(false).setSummonCost(2).setDungeonLevel(3)
		        .addSubspecies(new Subspecies("scarlet", "uncommon")).addSubspecies(new Subspecies("ashen", "uncommon"));
		newMob.spawnInfo.setSpawnTypes("MONSTER")
				.setSpawnWeight(1).setAreaLimit(1).setGroupLimits(1, 1).setLightDark(false, true);
		ObjectManager.addMob(newMob);
		
		
		// ========== Create Projectiles ==========
		ObjectManager.addProjectile("poop", EntityPoop.class, ObjectManager.getItem("poopcharge"), new DispenserBehaviorPoop());
		
		
		// ========== Register Models ==========
		proxy.registerModels();
	}
	
	
	// ==================================================
	//                Initialization
	// ==================================================
	@EventHandler
	public void load(FMLInitializationEvent event) {
		
	}
	
	
	// ==================================================
	//                Post-Initialization
	// ==================================================
	@EventHandler
	public void postInit(FMLPostInitializationEvent event) {
		// ========== Set Current Group ==========
		ObjectManager.setCurrentGroup(group);
//		ConfigBase config = ConfigBase.getConfig(group, "spawning");
		
		
		// ========== Mob Events ==========
		// Poop Party:
        if(MobInfo.getFromName("conba") != null) {
			MobEventBase mobEvent = new MobEventPoopParty("poopparty", JungleMobs.group);
			SpawnTypeBase eventSpawner = new SpawnTypeLand("poopparty")
	            .setChance(1.0D).setBlockLimit(32).setMobLimit(3);
	        eventSpawner.materials = new Material[] {Material.air};
	        eventSpawner.ignoreBiome = true;
	        eventSpawner.ignoreLight = true;
	        eventSpawner.forceSpawning = true;
	        eventSpawner.ignoreMobConditions = true;
	        eventSpawner.addSpawn(MobInfo.getFromName("conba"));
	        mobEvent.addSpawner(eventSpawner);
			MobEventManager.instance.addWorldEvent(mobEvent);
        }
		
	     // The Swarm:
		MobEventBase theSwarmEvent = new MobEventTheSwarm("theswarm", JungleMobs.group);
		theSwarmEvent.minDay = 10;
	     
		SpawnTypeBase theSwarmLandSpawner = new SpawnTypeLand("theswarm_land")
	         .setChance(1.0D).setBlockLimit(32).setMobLimit(3);
		theSwarmLandSpawner.materials = new Material[] {Material.air};
		theSwarmLandSpawner.ignoreBiome = true;
		theSwarmLandSpawner.ignoreLight = true;
		theSwarmLandSpawner.forceSpawning = true;
		theSwarmLandSpawner.ignoreMobConditions = true;
		theSwarmLandSpawner.addSpawn(MobInfo.getFromName("conba"));
	     if(theSwarmLandSpawner.hasSpawns())
	    	 theSwarmEvent.addSpawner(theSwarmLandSpawner);
	     
		SpawnTypeBase theSwarmSkySpawner = new SpawnTypeSky("theswarm_sky")
	         .setChance(1.0D).setBlockLimit(32).setMobLimit(8);
		theSwarmSkySpawner.materials = new Material[] {Material.air};
		theSwarmSkySpawner.ignoreBiome = true;
		theSwarmSkySpawner.ignoreLight = true;
		theSwarmSkySpawner.forceSpawning = true;
		theSwarmSkySpawner.ignoreMobConditions = true;
		theSwarmSkySpawner.addSpawn(MobInfo.getFromName("vespid"));
		theSwarmSkySpawner.addSpawn(MobInfo.getFromName("vespidqueen"), 1);
	     if(theSwarmSkySpawner.hasSpawns())
	    	 theSwarmEvent.addSpawner(theSwarmSkySpawner);
	     
	     if(theSwarmEvent.hasSpawners())
	     	MobEventManager.instance.addWorldEvent(theSwarmEvent);
		
        
		// ========== Remove Vanilla Spawns ==========
		BiomeGenBase[] biomes = group.biomes;
		if(group.controlVanillaSpawns) {
			EntityRegistry.removeSpawn(EntityZombie.class, EnumCreatureType.monster, biomes);
			EntityRegistry.removeSpawn(EntitySkeleton.class, EnumCreatureType.monster, biomes);
			EntityRegistry.removeSpawn(EntityWitch.class, EnumCreatureType.monster, biomes);
			EntityRegistry.removeSpawn(EntityCow.class, EnumCreatureType.creature, biomes);
			EntityRegistry.removeSpawn(EntityPig.class, EnumCreatureType.creature, biomes);
			EntityRegistry.removeSpawn(EntitySheep.class, EnumCreatureType.creature, biomes);
		}
		
		
		// ========== Crafting ==========
		GameRegistry.addRecipe(new ShapelessOreRecipe(
				new ItemStack(ObjectManager.getItem("tropicalcurry"), 1, 0),
				new Object[] {
					Items.bowl,
					new ItemStack(Items.dye, 1, 3),
					Blocks.vine,
					ObjectManager.getItem("concapedemeatcooked")
				}
			));
		GameRegistry.addRecipe(new ShapelessOreRecipe(
				new ItemStack(ObjectManager.getItem("concapedemeatcooked"), 1, 0),
				new Object[] { ObjectManager.getItem("tropicalcurry") }
			));

        if(ItemInfo.enableWeaponRecipes) {
            GameRegistry.addRecipe(new ShapedOreRecipe(
                    new ItemStack(ObjectManager.getItem("poopscepter"), 1, 0),
                    new Object[]{"CCC", "CRC", "CRC",
                            Character.valueOf('C'), ObjectManager.getItem("poopcharge"),
                            Character.valueOf('R'), Items.blaze_rod
                    }));
        }
		
		GameRegistry.addRecipe(new ShapedOreRecipe(
				new ItemStack(ObjectManager.getItem("uvaraptortreat"), 4, 0),
				new Object[] { "TTT", "BBT", "TTT",
				Character.valueOf('T'), ObjectManager.getItem("concapedemeatcooked"),
				Character.valueOf('B'), Items.bone
			}));
		
		GameRegistry.addRecipe(new ShapelessOreRecipe(
				new ItemStack(Items.clay_ball, 4, 0),
				new Object[] {
					ObjectManager.getBlock("propolis")
				}
			));
		
		GameRegistry.addRecipe(new ShapelessOreRecipe(
				new ItemStack(Blocks.mycelium, 2, 0),
				new Object[] {
					ObjectManager.getBlock("propolis"),
					Blocks.dirt
				}
			));
		
		GameRegistry.addRecipe(new ShapelessOreRecipe(
				new ItemStack(Items.sugar, 4, 0),
				new Object[] {
					ObjectManager.getBlock("veswax")
				}
			));
		
		
		// ========== Smelting ==========
		GameRegistry.addSmelting(ObjectManager.getItem("concapedemeatraw"), new ItemStack(ObjectManager.getItem("concapedemeatcooked"), 1), 0.5f);
		GameRegistry.addSmelting(ObjectManager.getBlock("propolis"), new ItemStack(Blocks.hardened_clay, 1), 0.5f);
		GameRegistry.addSmelting(ObjectManager.getBlock("veswax"), new ItemStack(Items.sugar, 6), 0.5f);
	}
}
