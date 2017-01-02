package lycanite.lycanitesmobs;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.registry.EntityRegistry;
import cpw.mods.fml.common.registry.GameRegistry;
import lycanite.lycanitesmobs.api.block.BlockSummoningPedestal;
import lycanite.lycanitesmobs.api.command.CommandMain;
import lycanite.lycanitesmobs.api.config.ConfigBase;
import lycanite.lycanitesmobs.api.entity.EntityFear;
import lycanite.lycanitesmobs.api.entity.EntityHitArea;
import lycanite.lycanitesmobs.api.entity.EntityPortal;
import lycanite.lycanitesmobs.api.info.*;
import lycanite.lycanitesmobs.api.item.*;
import lycanite.lycanitesmobs.api.mobevent.MobEventManager;
import lycanite.lycanitesmobs.api.mobevent.SharedMobEvents;
import lycanite.lycanitesmobs.api.mods.DLDungeons;
import lycanite.lycanitesmobs.api.network.PacketHandler;
import lycanite.lycanitesmobs.api.spawning.CustomSpawner;
import lycanite.lycanitesmobs.api.spawning.SpawnTypeBase;
import lycanite.lycanitesmobs.api.tileentity.TileEntitySummoningPedestal;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.Achievement;
import net.minecraftforge.common.AchievementPage;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

@Mod(modid = LycanitesMobs.modid, name = LycanitesMobs.name, version = LycanitesMobs.version)
public class LycanitesMobs {
	
	public static final String modid = "lycanitesmobs";
	public static final String name = "Lycanites Mobs";
	public static final String version = "1.13.0.4 - MC 1.7.10";
	public static final String website = "http://lycanitesmobs.com";
	public static final String websiteAPI = "http://api.lycanitesmobs.com";
	public static final String websitePatreon = "https://www.patreon.com/lycanite";
	
	public static final PacketHandler packetHandler = new PacketHandler();

    public static GroupInfo group;
    public static ConfigBase config;
	
	// Instance:
	@Instance(modid)
	public static LycanitesMobs instance;
	
	// Proxy:
	@SidedProxy(clientSide="lycanite.lycanitesmobs.ClientProxy", serverSide="lycanite.lycanitesmobs.CommonProxy")
	public static CommonProxy proxy;
	
	// Spawning:
	public static CustomSpawner customSpawner;
	public static MobEventManager mobEventManager;
	
	// Creative Tab:
	public static final CreativeTabs itemsTab = new CreativeTabItems(CreativeTabs.getNextID(), modid + ".items");
	public static final CreativeTabs creaturesTab = new CreativeTabCreatures(CreativeTabs.getNextID(), modid + ".creatures");
	
	// Texture Path:
	public static String texturePath = "mods/lycanitesmobs/";

    // Achievements:
    public static AchievementPage achievementPage;
    public static int achievementGlobalBaseID = 5500;

	// Extra Config Settings:
	public static boolean disableNausea = false;
	
	// Debug Helper
	public static EntityLivingBase debugEntity;	
	
	// ==================================================
	//                Pre-Initialization
	// ==================================================
	@EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		// ========== Config ==========
		group = new GroupInfo(this, name, achievementGlobalBaseID);
        ConfigBase.versionCheck("1.11.0.1", version);
		group.loadFromConfig();
		config = ConfigBase.getConfig(group, "general");
		config.setCategoryComment("Debug", "Set debug options to true to show extra debugging information in the console.");
		config.setCategoryComment("Extras", "Other extra config settings, some of the aren't necessarily specific to Lycanites Mobs.");
		disableNausea = config.getBool("Extras", "Disable Nausea Debuff", disableNausea, "Set to true to disable the nausea debuff on players.");

        config.setCategoryComment("Admin", "Special tools for server admins.");
        ExtendedEntity.FORCE_REMOVE_ENTITY_IDS = config.getStringList("Admin", "Force Remove Entity Names", new String[0], "Here you can add a list of entity IDs for entity that you want to be forcefully removed.");
        if(ExtendedEntity.FORCE_REMOVE_ENTITY_IDS != null && ExtendedEntity.FORCE_REMOVE_ENTITY_IDS.length > 0) {
            printInfo("", "Lycanites Mobs will forcefully remove the following entities based on their registered IDs:");
            for (String removeEntityID : ExtendedEntity.FORCE_REMOVE_ENTITY_IDS)
                printInfo("", removeEntityID);
        }
        ExtendedEntity.FORCE_REMOVE_ENTITY_TICKS = config.getInt("Admin", "Force Remove Entity Ticks", 40, "How many ticks it takes for an entity to be forcefully removed (1 second = 20 ticks). This only applies to EntityLiving, other entities are instantly removed.");

		LycanitesMobs.packetHandler.init();
		
		
		// ========== Custom Potion Effects ==========
		config.setCategoryComment("Potion Effects", "Here you can override each potion effect ID from the automatic ID, use 0 if you want it to stay automatic. Overrides should only be needed if you are running a lot of mods that add custom effects.");
		if(config.getBool("Potion Effects", "Enable Custom Effects", true, "Set to false to disable the custom potion effects.")) {
			PotionBase.reserveEffectIDSpace();
			ObjectManager.addPotionEffect("paralysis", config, true, 0xFFFF00, 1, 0, false);
			ObjectManager.addPotionEffect("leech", config, false, 0x00FF99, 7, 0, true);
			ObjectManager.addPotionEffect("penetration", config, true, 0x222222, 7, 1, false);
			ObjectManager.addPotionEffect("recklessness", config, true, 0xFF0044, 4, 0, false);
			ObjectManager.addPotionEffect("rage", config, true, 0xFF4400, 4, 0, false);
			ObjectManager.addPotionEffect("weight", config, true, 0x000022, 1, 0, false);
			ObjectManager.addPotionEffect("swiftswimming", config, false, 0x0000FF, 0, 2, true);
            ObjectManager.addPotionEffect("fear", config, false, 0x220022, 7, 0, false);
            ObjectManager.addPotionEffect("fallresist", config, false, 0xDDFFFF, 0, 0, false);
			MinecraftForge.EVENT_BUS.register(new PotionEffects());
		}
		
		
		// ========== Mob Info ==========
		MobInfo.loadGlobalSettings();
		
		
		// ========== Spawning ==========
		customSpawner = new CustomSpawner();
		SpawnTypeBase.loadSpawnTypes();
		MinecraftForge.EVENT_BUS.register(customSpawner);
		FMLCommonHandler.instance().bus().register(customSpawner);
		
		SpawnInfo.loadGlobalSettings();
		
		mobEventManager = new MobEventManager();
		mobEventManager.loadMobEvents();
		FMLCommonHandler.instance().bus().register(mobEventManager);


        // ========== Item Info ==========
        ItemInfo.loadGlobalSettings();


        // ========== Altar Info ==========
        AltarInfo.loadGlobalSettings();
		
		
		// ========== Register Event Listeners ==========
		MinecraftForge.EVENT_BUS.register(new EventListener());
        proxy.registerEvents();
		
        
		// ========== Set Current Mod ==========
		ObjectManager.setCurrentGroup(group);


        // ========== Create Blocks ==========
        ObjectManager.addBlock("summoningpedestal", new BlockSummoningPedestal(group));
		
		
		// ========== Create Items ==========
		ObjectManager.addItem("soulgazer", new ItemSoulgazer());
		ObjectManager.addItem("soulstone", new ItemSoulstone());
        ObjectManager.addItem("soulkey", new ItemSoulkey());
		ObjectManager.addItem("summoningstaff", new ItemStaffSummoning());
		ObjectManager.addItem("stablesummoningstaff", new ItemStaffStable());
		ObjectManager.addItem("bloodsummoningstaff", new ItemStaffBlood());
		ObjectManager.addItem("sturdysummoningstaff", new ItemStaffSturdy());
		ObjectManager.addItem("savagesummoningstaff", new ItemStaffSavage());
		
		// Super Foods:
		ObjectManager.addItem("battleburrito", new ItemFoodBattleBurrito("battleburrito", group, 6, 0.7F).setAlwaysEdible().setMaxStackSize(16));
		ObjectManager.addItem("explorersrisotto", new ItemFoodExplorersRisotto("explorersrisotto", group, 6, 0.7F).setAlwaysEdible().setMaxStackSize(16));
		
		// Seasonal Items:
		ObjectManager.addItem("halloweentreat", new ItemHalloweenTreat());
        ObjectManager.addItem("wintergift", new ItemWinterGift());
        ObjectManager.addItem("wintergiftlarge", new ItemWinterGiftLarge());

        ObjectManager.addItem("mobtoken", new ItemMobToken(group));


        // ========== Create Tile Entities ==========
        ObjectManager.addTileEntity("summoningpedestal", TileEntitySummoningPedestal.class);


        // ========== Call Object Lists Setup ==========
        ObjectLists.createCustomItems();
		ObjectLists.createLists();

		
		// ========== Mod Support ==========
		DLDungeons.init();
	}
	
	
	// ==================================================
	//                  Initialization
	// ==================================================
	@EventHandler
    public void load(FMLInitializationEvent event) {
		// ========== Register and Initialize Handlers ==========
		NetworkRegistry.INSTANCE.registerGuiHandler(this, new GuiHandler());
		
		
		// ========== Register Special Entities ==========
		int specialEntityID = 0;
		EntityRegistry.registerModEntity(EntityPortal.class, "summoningportal", specialEntityID++, instance, 64, 1, true);
		/*MobInfo newMob =*/ new MobInfo(group, "fear", EntityFear.class, 0x000000, 0x000000)
			.setPeaceful(true).setSummonable(false).setSummonCost(0).setDungeonLevel(0).setDummy(true);
		EntityRegistry.registerModEntity(EntityFear.class, "fear", specialEntityID++, instance, 64, 1, true);
		AssetManager.addSound("effect_fear", group, "effect.fear");
        EntityRegistry.registerModEntity(EntityHitArea.class, "hitarea", specialEntityID++, instance, 64, 1, true);


        // ========== Load All Mob Info from Configs ==========
        MobInfo.loadAllFromConfigs();
	}
	
	
	// ==================================================
	//                Post-Initialization
	// ==================================================
	@EventHandler
    public void postInit(FMLPostInitializationEvent event) {

        // ========== Assign Mob Spawning ==========
        GroupInfo.loadAllSpawningFromConfigs();
        MobInfo.loadAllSpawningFromConfigs();


		// ========== Register and Initialize Handlers/Objects ==========
		proxy.registerAssets();
		proxy.registerTileEntities();
		proxy.registerRenders();
		
		
		// ========== Mob Events ==========
        SharedMobEvents.createSharedEvents(LycanitesMobs.group);


        // ========== Seasonal Item Lists ==========
        ItemHalloweenTreat.createObjectLists();
        ItemWinterGift.createObjectLists();
		
        
		// ========== Crafting ==========
		GameRegistry.addRecipe(new ShapedOreRecipe(
				new ItemStack(ObjectManager.getItem("soulgazer"), 1, 0),
				new Object[] { "GBG", "BDB", "GBG",
				Character.valueOf('G'), Items.gold_ingot,
				Character.valueOf('D'), Items.diamond,
				Character.valueOf('B'), Items.bone
			}));

		GameRegistry.addRecipe(new ShapedOreRecipe(
				new ItemStack(ObjectManager.getItem("soulstone"), 1, 0),
				new Object[] { "DPD", "PSP", "DPD",
						Character.valueOf('D'), Items.diamond,
						Character.valueOf('S'), ObjectManager.getItem("soulgazer"),
						Character.valueOf('P'), Items.ender_pearl
				}));

        GameRegistry.addRecipe(new ShapedOreRecipe(
                new ItemStack(ObjectManager.getItem("soulkey"), 1, 0),
                new Object[] { "DND", "DSD", "DDD",
                        Character.valueOf('N'), Items.nether_star,
                        Character.valueOf('S'), ObjectManager.getItem("soulgazer"),
                        Character.valueOf('D'), Items.diamond
                }));

        GameRegistry.addRecipe(new ShapedOreRecipe(
                new ItemStack(ObjectManager.getBlock("summoningpedestal"), 1, 0),
                new Object[] { "GNG", "DSD", "GDG",
                        Character.valueOf('N'), Items.nether_star,
                        Character.valueOf('S'), ObjectManager.getItem("soulstone"),
                        Character.valueOf('D'), Blocks.diamond_block,
                        Character.valueOf('G'), Blocks.gold_block
                }));
		
		GameRegistry.addRecipe(new ShapedOreRecipe(
				new ItemStack(ObjectManager.getItem("summoningstaff"), 1, 0),
				new Object[] { " E ", " B ", " G ",
				Character.valueOf('E'), Items.ender_pearl,
				Character.valueOf('B'), Items.bone,
				Character.valueOf('G'), Items.gold_ingot
			}));
		
		GameRegistry.addRecipe(new ShapedOreRecipe(
				new ItemStack(ObjectManager.getItem("stablesummoningstaff"), 1, 0),
				new Object[] { " D ", " S ", " G ",
				Character.valueOf('S'), ObjectManager.getItem("summoningstaff"),
				Character.valueOf('G'), Items.gold_ingot,
				Character.valueOf('D'), Items.diamond
			}));
		
		GameRegistry.addRecipe(new ShapedOreRecipe(
				new ItemStack(ObjectManager.getItem("bloodsummoningstaff"), 1, 0),
				new Object[] { "RRR", "BSB", "NDN",
				Character.valueOf('S'), ObjectManager.getItem("summoningstaff"),
				Character.valueOf('R'), Items.redstone,
				Character.valueOf('B'), Items.bone,
				Character.valueOf('N'), Items.nether_wart,
				Character.valueOf('D'), Items.diamond
			}));
		
		GameRegistry.addRecipe(new ShapedOreRecipe(
				new ItemStack(ObjectManager.getItem("sturdysummoningstaff"), 1, 0),
				new Object[] { "III", "ISI", " O ",
				Character.valueOf('S'), ObjectManager.getItem("summoningstaff"),
				Character.valueOf('I'), Items.iron_ingot,
				Character.valueOf('O'), Blocks.obsidian
			}));
		
		GameRegistry.addRecipe(new ShapedOreRecipe(
				new ItemStack(ObjectManager.getItem("savagesummoningstaff"), 1, 0),
				new Object[] { "LLL", "BSB", "GGG",
				Character.valueOf('S'), ObjectManager.getItem("summoningstaff"),
				Character.valueOf('B'), Items.bone,
				Character.valueOf('G'), Items.ghast_tear,
				Character.valueOf('L'), new ItemStack(Items.dye, 1, 4)
			}));
		
		// Super Food:
		if(ObjectManager.getItem("pinkymeatcooked") != null && ObjectManager.getItem("makameatcooked") != null
				&& ObjectManager.getItem("arisaurmeatcooked") != null && ObjectManager.getItem("yetimeatcooked") != null
				&& ObjectManager.getItem("aspidmeatcooked") != null) {
			GameRegistry.addRecipe(new ShapelessOreRecipe(
					new ItemStack(ObjectManager.getItem("battleburrito"), 1, 0),
					new Object[] {
						ObjectManager.getItem("pinkymeatcooked"),
						ObjectManager.getItem("makameatcooked"),
						ObjectManager.getItem("arisaurmeatcooked"),
						ObjectManager.getItem("yetimeatcooked"),
						ObjectManager.getItem("aspidmeatcooked")
					}
				));
		}

		if(ObjectManager.getItem("joustmeatcooked") != null && ObjectManager.getItem("yalemeatcooked") != null
				&& ObjectManager.getItem("ikameatcooked") != null && ObjectManager.getItem("concapedemeatcooked") != null) {
			GameRegistry.addRecipe(new ShapelessOreRecipe(
					new ItemStack(ObjectManager.getItem("explorersrisotto"), 1, 0),
					new Object[] {
						ObjectManager.getItem("joustmeatcooked"),
						ObjectManager.getItem("yalemeatcooked"),
						ObjectManager.getItem("ikameatcooked"),
						ObjectManager.getItem("concapedemeatcooked")
					}
				));
		}


        // ========== Achievement Page ==========
        achievementPage = new AchievementPage(name, ObjectManager.achievements.values().toArray(new Achievement[ObjectManager.achievements.values().size()]));
        AchievementPage.registerAchievementPage(achievementPage);
    }
	
	
    // ==================================================
    //                    Server Load
    // ==================================================
	@EventHandler
	public void serverLoad(FMLServerStartingEvent event) {
		// ========== Commands ==========
		event.registerServerCommand(new CommandMain());
	}
	
	
	// ==================================================
	//                     Debugging
	// ==================================================
    public static void printInfo(String key, String message) {
        if("".equals(key) || config.getBool("Debug", key, false)) {
            System.out.println("[LycanitesMobs] [Info] " + message);
        }
    }

    public static void printDebug(String key, String message) {
        if("".equals(key) || config.getBool("Debug", key, false)) {
            System.out.println("[LycanitesMobs] [Debug] " + message);
        }
    }

    public static void printWarning(String key, String message) {
		if("".equals(key) || config.getBool("Debug", key, false)) {
			System.err.println("[LycanitesMobs] [WARNING] " + message);
		}
	}
}
