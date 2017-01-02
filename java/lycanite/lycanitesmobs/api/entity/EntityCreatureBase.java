package lycanite.lycanitesmobs.api.entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import Reika.RotaryCraft.API.Interfaces.FlyingMob;
import lycanite.lycanitesmobs.AssetManager;
import lycanite.lycanitesmobs.ExtendedEntity;
import lycanite.lycanitesmobs.ExtendedWorld;
import lycanite.lycanitesmobs.GuiHandler;
import lycanite.lycanitesmobs.LycanitesMobs;
import lycanite.lycanitesmobs.ObjectManager;
import lycanite.lycanitesmobs.api.entity.ai.EntityAIMoveRestriction;
import lycanite.lycanitesmobs.api.entity.ai.EntityAITargetAttack;
import lycanite.lycanitesmobs.api.entity.ai.EntityAITargetRevenge;
import lycanite.lycanitesmobs.api.entity.ai.FlightNavigator;
import lycanite.lycanitesmobs.api.info.*;
import lycanite.lycanitesmobs.api.inventory.ContainerCreature;
import lycanite.lycanitesmobs.api.inventory.InventoryCreature;
import lycanite.lycanitesmobs.api.pets.PetEntry;
import lycanite.lycanitesmobs.api.spawning.SpawnTypeBase;
import net.minecraft.block.Block;
import net.minecraft.block.BlockColored;
import net.minecraft.block.material.Material;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.boss.BossStatus;
import net.minecraft.entity.boss.IBossDisplayData;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.passive.IAnimals;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityMobSpawner;
import net.minecraft.util.*;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public abstract class EntityCreatureBase extends EntityLiving implements FlyingMob {
	/** A snapshot of the base health for each mob. This is used when calculating subspecies or tamed health. **/
	public static Map<Class, Double> baseHealthMap = new HashMap<Class, Double>();
    
	// Info:
	/** A class that contains information about this mob, this class also links to the SpawnInfo class relevant to this mob. **/
	public MobInfo mobInfo;
    /** The group that this mob belongs to, this provides config settings, asset locations and more. **/
	public GroupInfo group;
	/** The Subspecies of this creature, if null this creature is the default common species. **/
	public Subspecies subspecies = null;
    /** What attribute is this creature, used for effects such as Bane of Arthropods. **/
	public EnumCreatureAttribute attribute = EnumCreatureAttribute.UNDEAD;
	/** A class that opens up extra stats and behaviours for NBT based customization.**/
	public ExtraMobBehaviour extraMobBehaviour;
	/** The name of the event that spawned this mob if any, an empty string ("") if none. **/
	public String spawnEventType = "";
	/** The number of the event that spawned this mob. Used for despawning this mob when a new event starts. Ignored if the spawnEventType is blank or the count is less than 0. **/
	public int spawnEventCount = -1;
    /** The Pet Entry for this mob, this binds this mob to a entity for special interaction, it will also cause this mob to be removed if the entity it is bound to is removed or dead. **/
    public PetEntry petEntry;
    /** If true, this mob will be treated as if it was spawned from an Altar, this is typically called directly by AltarInfo or by events triggered by AltarInfo. **/
    public boolean altarSummoned = false;
    /** If true, this mob will show a boss health bar, regardless of other properties, unless overridden by a subclass. **/
    public boolean forceBossHealthBar = false;
    /** The living update tick. **/
    public long updateTick = 0;
	
	// Size:
    /** The width of this mob. XZ axis. **/
	public float setWidth = 0.6F;
    /** The depth of this mob. Overrides width's Z axis. This currently doesn't work, use width only for now. **/
	public float setDepth = 0.6F;
    /** The height of this mob. Y axis. **/
	public float setHeight = 1.8F;
    /** The size scale of this mob. Randomly varies normally by 10%. **/
	public double sizeScale = 1.0D;
    /** An array of additional hitboxes for large entities. **/
    public EntityHitArea[][][] hitAreas;
    /** A scale relative to this entity's width for melee and ranged hit collision. **/
    public float hitAreaScale = 1;
	
	// Stats:
	/** The defense rating of this mob. This is how much damage it can withstand.
	 * For example, a damage of 4 with a defense of 1 will result in a new damage of 3.
	 * Defense stat multipliers are applied to this value too, nor whole results are rounded.
	**/
	public int defense = 0;
    /** How much experience this mob drops (overridden to 0 if it is a minion). **/
	public int experience = 5;
    /** Which attack phase this mob is on. This will be replaced with a better system for boss mobs. **/
	public byte attackPhase = 0;
    /** How many attack phases this mob has. This will be replaced with a better system for boss mobs. **/
	public byte attackPhaseMax = 0;
    /** How long this mob should run away for before it stops. **/
	public int fleeTime = 200;
    /** How long has this mob been running away for. **/
	public int currentFleeTime = 0;
    /** What percentage of health this mob will run away at, from 0.0F to 1.0F **/
	public float fleeHealthPercent = 0;
    /** The current Battle Phase of this mob, each Phase uses different behaviours. Used by bosses. **/
    public int battlePhase = 0;
    /** The maximum amount of damage this mob can take. If 0 or less, this is ignored. **/
    public int damageMax = 0;
	
	// Abilities:
    /** If true, this mob is to be treated as a boss. Boss mobs gain some defensive abilities. **/
    public boolean boss = false;
    /** The battle range of this boss mob, anything out of this range cannot harm the boss. This will also affect other things related to the boss. **/
    public int bossRange = 60;
	/** Whether or not this mob is hostile by default. Use isHostile() when check if this mob is hostile. **/
	public boolean isHostileByDefault = true;
    /** Whether if this mob is on fire, it should spread it to other entities when melee attacking. **/
	public boolean spreadFire = false;
    /** Used to check if the mob was stealth last update. **/
	public boolean stealthPrev = false;
	/** When above 0 this mob will be considered blocking and this will count down to 0. Blocking mobs have additional defense. **/
	public int currentBlockingTime = 0;
	/** How long this mob should usually block for in ticks. **/
	public int blockingTime = 60;
	/** The entity picked up by this entity (if any). **/
    public Entity pickupEntity;
	
	// Positions:
    /** A location used for mobs that stick around a certain home spot. **/
	private ChunkCoordinates homePosition = new ChunkCoordinates(0, 0, 0);
    /** How far this mob can move from their home spot. **/
    private float homeDistanceMax = -1.0F;
    
    // Spawning:
    /** Use the onSpawn() method and not this variable. True if this creature has spawned for the first time (naturally or via spawn egg, etc, not reloaded from a saved chunk). **/
    public boolean firstSpawn = true;
    /** This will contain the Spawn Type used to spawn this entity (this should only be used for spawn checks really as it isn't persistent). Null if spawned from egg, spawner, vanilla, etc. **/
    public SpawnTypeBase spawnedFromType = null;
    /** Should this mob only spawn in darkness. **/
    //public boolean spawnsInDarkness = false;
    /** Should this mob only spawn in light. **/
    //public boolean spawnsOnlyInLight = false;
    /** Should this mob check for block collisions when spawning? **/
    public boolean spawnsInBlock = false;
    /** Can this mob spawn where it can't see the sky above? **/
    public boolean spawnsUnderground = true;
    /** Can this mob spawn on land (not in liquids)? Note that setting a mob to WATERCREATURE means that they will only spawn in water anyway. **/
    public boolean spawnsOnLand = true;
    /** Does this mob spawn inside liquids? **/
    public boolean spawnsInWater = false;
    /** If true, this creature will swim in and if set, will suffocate without lava instead of without water. **/
    public boolean isLavaCreature = false;
    /** Is this mob a minion? (Minions don't drop items and other things). **/
    public boolean isMinion = false;
    /** If true, this mob is temporary and will eventually despawn once the temporaryDuration is at or below 0. **/
	public boolean isTemporary = false;
    /** If this mob is temporary, this will count down to 0, once per tick. Once it hits 0, this creature will despawn. **/
	public int temporaryDuration = 0;
    /** If true, this mob will not despawn naturally regardless of other rules. **/
	public boolean forceNoDespawn = false;
    /** Can be set to true by custom spawners in rare cases. If true, this mob has a higher chance of being a subspecies. **/
    public boolean spawnedRare = false;
    
    // Movement:
    /** Whether the mob should use it's leash AI or not. **/
    private boolean leashAIActive = false;
    /** Movement AI for mobs that are leashed. **/
    private EntityAIBase leashMoveTowardsRestrictionAI = new EntityAIMoveRestriction(this);
    /** The flight navigator class, a makeshift class that handles flight and free swimming movement, replaces the pathfinder. **/
    public FlightNavigator flightNavigator;
    
    // Targets:
    /** A target used for alpha creatures or connected mobs such as following concapede segements. **/
    private EntityLivingBase masterTarget;
    /** A target used usually for child mobs or connected mobs such as leading concapede segments. **/
    private EntityLivingBase parentTarget;
    /** A target that this mob should usually run away from. **/
    private EntityLivingBase avoidTarget;
	
	// Client:
	/** A list of player entities that need to have their GUI of this mob reopened on refresh. **/
	public List<EntityPlayer> guiViewers = new ArrayList<EntityPlayer>();
	/** Counts from the guiRefreshTime down to 0 when a GUI refresh has been scheduled. **/
	public int guiRefreshTick = 0;
	/** The amount of ticks to wait before a GUI refresh. **/
	public int guiRefreshTime = 2;
    /** Used for attack animations, the server uses this more as a boolean, the client uses it as a timer. **/
	public short justAttacked = 0;
    /** The duration of attack animations, used by the server as a boolean (true when greater than 0 then instantly set to 0), the client uses it as the animation time (counts down per tick). **/
	public short justAttackedTime = 5;
    /** True if this mob should play a sound when attacking. Ranged mobs usually don't use this as their projectiles makes an attack sound instead. **/
	public boolean hasAttackSound = false;
    /** True if this mob should play a sound when walking. Usually footsteps. **/
	public boolean hasStepSound = true;
    /** True if this mob should play a sound when jumping, used mostly for mounts. **/
	public boolean hasJumpSound = false;
    /** The delay in ticks between flying sounds such as wing flapping, set to 0 for no flight sounds. **/
	public int flySoundSpeed = 0;
    /** An extra animation boolean. **/
    public boolean extraAnimation01 = false;
    /** Holds Information for this mobs boss health should it be displayed in the boss health bar. Used by bosses and rare subspecies. **/
    protected BossHealth bossHealth;
	
	// Data Watcher:
    /** The starting point for the datawatcher IDs used by this mod, lower IDs are used by vanilla code. **/
	private static byte watcherID = 12;
    /** A collection of IDs used by the datawatcher (used to sync clients and the server with certain values). **/
	public static enum WATCHER_ID {
		HEALTH(watcherID++), TARGET(watcherID++), ANIMATION(watcherID++), ATTACK_PHASE(watcherID++),
		CLIMBING(watcherID++), STEALTH(watcherID++), HUNGER(watcherID++), STAMINA(watcherID++),
		AGE(watcherID++), LOVE(watcherID++),
		TAMED(watcherID++), OWNER(watcherID++), COLOR(watcherID++), SIZE(watcherID++), SUBSPECIES(watcherID++), LAST(watcherID++),
        SPECIAL(watcherID++),
		EQUIPMENT(watcherID++);
		
		public final byte id;
	    private WATCHER_ID(byte value) { this.id = value; }
	    public byte getValue() { return id; }
	}
    /** Used for the TARGET watcher bitmap, bitmaps save on many packets and make network performance better! **/
	public static enum TARGET_ID {
		ATTACK((byte)1), MASTER((byte)2), PARENT((byte)4), AVOID((byte)8), RIDER((byte)16);
		public final byte id;
	    private TARGET_ID(byte value) { this.id = value; }
	    public byte getValue() { return id; }
	}
    /** Used for the ANIM_ID watcher bitmap, bitmaps save on many packets and make network performance better! **/
	public static enum ANIM_ID {
		ATTACKED((byte)1), GROUNDED((byte)2), BLOCKING((byte)4), MINION((byte)8), EXTRA01((byte)8);
		public final byte id;
	    private ANIM_ID(byte value) { this.id = value; }
	    public byte getValue() { return id; }
	}
	/** If true, this object has initiated and it is safe to use the datawatcher. **/
	public boolean initiated = false;
	
	// Interact:
	/** Used for the tidier interact code, these are commonly used right click item command priorities. **/
	public static enum CMD_PRIOR {
		OVERRIDE(0), IMPORTANT(1), EQUIPPING(2), ITEM_USE(3), EMPTY_HAND(4), MAIN(5);
		public final int id;
	    private CMD_PRIOR(int value) { this.id = value; }
	    public int getValue() { return id; }
	}
	
	// GUI Commands:
	/** A list of GUI command IDs to be used by pet or creature GUIs via a network packet. **/
	public static enum GUI_COMMAND_ID {
		CLOSE((byte)0), SITTING((byte)1), FOLLOWING((byte)2), PASSIVE((byte)3), STANCE((byte)4), PVP((byte)5), TELEPORT((byte)6), SPAWNING((byte)7), RELEASE((byte)8);
		public byte id;
		private GUI_COMMAND_ID(byte i) { id = i; }
	}
	
	// Items:
    /** The inventory object of the creature, this is used for managing and using the creature's inventory. **/
	public InventoryCreature inventory;
    /** A collection of DropRate classes which are used when randomly drop items on death. **/
    public List<DropRate> drops = new ArrayList<DropRate>();
    
    // Override AI:
    public EntityAITargetAttack aiTargetPlayer = new EntityAITargetAttack(this).setTargetClass(EntityPlayer.class);
    public EntityAITargetRevenge aiDefendAnimals = new EntityAITargetRevenge(this).setHelpClasses(IAnimals.class);
	
    // ==================================================
  	//                    Constructor
  	// ==================================================
    public EntityCreatureBase(World world) {
        super(world);
        // Anything that needs to be loaded first should be in applyEntityAttributes().
    }
    
    // ========== Setup ==========
    /** This should be called by the specific mob entity and set the default starting values. **/
    public void setupMob() {
        // Size:
        this.updateSize();
        
        // Stats:
        this.stepHeight = 0.5F;
        this.experienceValue = this.experience;
        this.inventory = new InventoryCreature(this.getCommandSenderName(), this);
        if(this.mobInfo.defaultDrops)
        	this.loadItemDrops();
        this.loadCustomDrops();
        this.experienceValue = this.experience;
        
        // Fire Immunity:
        this.isImmuneToFire = !this.canBurn();
    }
    
    // ========== Load Item Drops ==========
    /** Loads all default item drops, will be ignored if the Enable Default Drops config setting for this mob is set to false, should be overridden to add drops. **/
    public void loadItemDrops() {}
    
    // ========== Load Custom Drops ==========
    /** Loads custom item drops from the config. **/
    public void loadCustomDrops() {
        for(DropRate drop : this.mobInfo.customDrops) {
            DropRate newDrop = new DropRate(drop.item.copy(), drop.chance).setMinAmount(drop.minAmount).setMaxAmount(drop.maxAmount).setChance(drop.chance).setSubspecies(drop.subspeciesID).setBurningDrop(drop.burningItem);
            this.drops.add (newDrop);
        }
    }
    
    // ========== Attributes ==========
    /** Creates and sets all the entity attributes with default values. This should be overriden by specific entities and should always run applyEntityAttributes(HashMap<String, Double> baseAttributes). **/
    @Override
    protected void applyEntityAttributes() {
        HashMap<String, Double> baseAttributes = new HashMap<String, Double>();
		baseAttributes.put("maxHealth", 0D);
		baseAttributes.put("movementSpeed", 0D);
		baseAttributes.put("knockbackResistance", 0D);
		baseAttributes.put("followRange", 0D);
		baseAttributes.put("attackDamage", 0D);
        this.applyEntityAttributes(baseAttributes);
    }
    
    /** Creates and sets all the entity attributes using a HashMap of values. This must always be called. **/
    protected void applyEntityAttributes(HashMap<String, Double> baseAttributes) {
        this.mobInfo = MobInfo.mobClassToInfo.get(this.getClass());
        this.group = mobInfo.group;
        this.extraMobBehaviour = new ExtraMobBehaviour(this);
        this.flightNavigator = new FlightNavigator(this);

        super.applyEntityAttributes();
        this.getAttributeMap().registerAttribute(SharedMonsterAttributes.attackDamage);
        if(baseAttributes.containsKey("maxHealth")) {
        	this.getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue((baseAttributes.get("maxHealth") * this.getHealthMultiplier()) + this.getHealthBoost());
        	baseHealthMap.put(this.getClass(), baseAttributes.get("maxHealth"));
        }
        if(baseAttributes.containsKey("movementSpeed"))
        	this.getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(baseAttributes.get("movementSpeed"));
        if(baseAttributes.containsKey("knockbackResistance"))
        	this.getEntityAttribute(SharedMonsterAttributes.knockbackResistance).setBaseValue(baseAttributes.get("knockbackResistance"));
        if(baseAttributes.containsKey("followRange"))
        	this.getEntityAttribute(SharedMonsterAttributes.followRange).setBaseValue(baseAttributes.get("followRange"));
        if(baseAttributes.containsKey("attackDamage"))
        	this.getEntityAttribute(SharedMonsterAttributes.attackDamage).setBaseValue(baseAttributes.get("attackDamage"));
    }
	
	// ========== Init ==========
    /** Initiates the entity setting all the values to be watched by the datawatcher. **/
    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataWatcher.addObject(WATCHER_ID.TARGET.id, (byte)0);
        this.dataWatcher.addObject(WATCHER_ID.ATTACK_PHASE.id, (byte)0);
        this.dataWatcher.addObject(WATCHER_ID.ANIMATION.id, (byte)0);
        this.dataWatcher.addObject(WATCHER_ID.CLIMBING.id, (byte)0);
        this.dataWatcher.addObject(WATCHER_ID.STEALTH.id, (float)0.0F);
        this.dataWatcher.addObject(WATCHER_ID.COLOR.id, (byte)0);
        this.dataWatcher.addObject(WATCHER_ID.SIZE.id, (float)1D);
        this.dataWatcher.addObject(WATCHER_ID.SUBSPECIES.id, (byte)0);
        this.initiated = true;
    }
    
    // ========== Name ==========
    /** Returns the name that this entity uses when checking the config class. Also it's spawner name, etc. **/
    public String getConfigName() {
    	return this.mobInfo.name;
    }
    
    /** Returns the display name of this entity. Use this when displaying it's name. **/
    @Override
    public String getCommandSenderName() {
    	if(this.hasCustomNameTag())
    		return this.getCustomNameTag();
    	else
    		return this.getFullName();
    }

    /** Returns the full name of this entity. **/
    public String getFullName() {
    	String name = "";
    	if(!"".equals(getAgeName()))
    		name += getAgeName() + " ";
    	if(!"".equals(getSubspeciesTitle()))
    		name += getSubspeciesTitle() + " ";
    	return name + getSpeciesName();
    }
    
    /** Returns the species name of this entity. **/
    public String getSpeciesName() {
    	String entityName = EntityList.getEntityString(this);
    	if(entityName == null)
    		return "Creature";
    	return StatCollector.translateToLocal("entity." + entityName + ".name");
    }

    /** Returns the subpsecies title (translated name) of this entity, returns a blank string if this is a base species mob. **/
    public String getSubspeciesTitle() {
    	if(this.getSubspecies() != null) {
    		return this.getSubspecies().getTitle();
    	}
    	return "";
    }

    /** Gets the name of this entity relative to it's age, more useful for EntityCreatureAgeable. **/
    public String getAgeName() {
    	return "";
    }
    
    
    // ==================================================
  	//                     Spawning
  	// ==================================================
    // ========== Can Spawn Here ==========
    /** Checks if the creature is able to spawn at it's initial position. **/
    @Override
    public boolean getCanSpawnHere() {
		int i = MathHelper.floor_double(this.posX);
	    int j = MathHelper.floor_double(this.boundingBox.minY);
	    int k = MathHelper.floor_double(this.posZ);
	    return this.spawnCheck(this.worldObj, i, j, k);
    }
    
    public boolean spawnCheck(World world, int i, int j, int k) {
        if(world.isRemote)
            return false;

    	LycanitesMobs.printDebug("MobSpawns", " ~O==================== Spawn Check: " + this.getConfigName() + " ====================O~");
    	LycanitesMobs.printDebug("MobSpawns", "Attempting to Spawn: " + this.getConfigName());
    	
    	// Peaceful Check:
    	LycanitesMobs.printDebug("MobSpawns", "Checking for peaceful difficulty...");
        if(!this.mobInfo.peacefulDifficulty && this.worldObj.difficultySetting == EnumDifficulty.PEACEFUL) return false;
        
    	LycanitesMobs.printDebug("MobSpawns", "Target Spawn Location: x" + i + " y" + j + " z" + k);
        
        // Fixed Spawning Checks:
    	LycanitesMobs.printDebug("MobSpawns", "Fixed spawn check (light level, obstacles, etc)...");
        if(!this.fixedSpawnCheck(world, i, j, k))
        	return false;
        
    	// Spawner Check:
    	LycanitesMobs.printDebug("MobSpawns", "Checking for nearby spawner...");
        if(this.isSpawnerNearby(world, i, j, k)) {
        	LycanitesMobs.printDebug("MobSpawns", "Spawner found, skpping other checks.");
        	LycanitesMobs.printDebug("MobSpawns", "Spawn Check Passed!");
        	return true;
        }
    	LycanitesMobs.printDebug("MobSpawns", "No spawner found.");
        
        // Natural Spawning Checks:
    	LycanitesMobs.printDebug("MobSpawns", "Natural spawn check (dimension, area limit, ground type, water, lava, underground)...");
        if(!this.naturalSpawnCheck(world, i, j, k))
        	return false;
        
        // Forced Spawn Chance:
    	LycanitesMobs.printDebug("MobSpawns", "All enviroment checks passed.");
        if(this.mobInfo.spawnInfo.spawnChance < 1.0D) {
        	if(this.mobInfo.spawnInfo.spawnChance <= 0) {
        		LycanitesMobs.printDebug("MobSpawns", "Applying Forced Spawn Chance - Chance is 0 = No Spawning");
        		return false;
        	}
        	else {
	        	double spawnRoll = this.rand.nextDouble();
		        LycanitesMobs.printDebug("MobSpawns", "Applying Forced Spawn Chance - Rolled: " + spawnRoll + " Must be less than: " + this.mobInfo.spawnInfo.spawnChance);
	        	if(spawnRoll > this.mobInfo.spawnInfo.spawnChance)
	        		return false;
        	}
		}
        LycanitesMobs.printDebug("MobSpawns", "Spawn Check Passed!");
        return true;
    }

    // ========== Fixed Spawn Check ==========
    /** First stage checks for spawning, if this check fails the creature will not spawn. **/
    public boolean fixedSpawnCheck(World world, int i, int j, int k) {
    	if(this.spawnedFromType == null || (this.spawnedFromType != null && !this.spawnedFromType.ignoreLight)) {
            byte light = this.testLightLevel(i, j, k);
            boolean validLight = false;
//            Block spawnBlock = world.getBlock(i, j, k);

	    	LycanitesMobs.printDebug("MobSpawns", "Checking light level: Darkness");
	    	if(this.mobInfo.spawnInfo.spawnsInDark && light <= 1)
                validLight = true;

	    	LycanitesMobs.printDebug("MobSpawns", "Checking light level: Lightness");
	    	if(this.mobInfo.spawnInfo.spawnsInLight && light >= 2)
                validLight = true;

            if(!validLight)
                return false;
    	}

    	LycanitesMobs.printDebug("MobSpawns", "Checking entity collision.");
        if(!this.worldObj.checkNoEntityCollision(this.boundingBox))
        	return false;

    	LycanitesMobs.printDebug("MobSpawns", "Checking solid block collision.");
        if(!this.spawnsInBlock && !this.worldObj.getCollidingBoundingBoxes(this, this.boundingBox).isEmpty()) {
        	return false;
        }

    	return true;
    }
    
    // ========== Natural Spawn Check ==========
    /** Second stage checks for spawning, this check is ignored if there is a valid monster spawner nearby. **/
    public boolean naturalSpawnCheck(World world, int i, int j, int k) {
    	LycanitesMobs.printDebug("MobSpawns", "Checking dimension.");
    	if(!this.isNativeDimension(this.worldObj))
    		return false;
    	LycanitesMobs.printDebug("MobSpawns", "Block preference.");
        if(this.getBlockPathWeight(i, j, k) < 0.0F && this.spawnedFromType == null)
        	return false;
    	LycanitesMobs.printDebug("MobSpawns", "Checking for liquid (water, lava, ooze, etc).");
        if(!this.spawnsInWater && this.worldObj.isAnyLiquid(this.boundingBox))
        	return false;
        else if(!this.spawnsOnLand && !this.worldObj.isAnyLiquid(this.boundingBox))
        	return false;
    	LycanitesMobs.printDebug("MobSpawns", "Checking for underground.");
        if(!this.spawnsUnderground && this.isBlockUnderground(i, j + 1, k))
        	return false;
    	LycanitesMobs.printDebug("MobSpawns", "Checking required blocks.");
        if(!spawnBlockCheck(world, i, j, k))
        	return false;
    	LycanitesMobs.printDebug("MobSpawns", "Counting mobs of the same kind, max allowed is: " + this.mobInfo.spawnInfo.spawnAreaLimit);
        if(!this.spawnLimitCheck(world, i, j, k))
        	return false;
        LycanitesMobs.printDebug("MobSpawns", "Checking for nearby bosses.");
        List bosses = this.getNearbyEntities(IBossDisplayData.class, SpawnInfo.spawnLimitRange);
        if(bosses.size() > 0)
            return false;
        	
        return true;
    }
    
    // ========== Spawn Dimension Check ==========
    public boolean isNativeDimension(World world) {
    	if(this.spawnedFromType != null) {
    		if(this.spawnedFromType.ignoreDimension) {
                LycanitesMobs.printDebug("MobSpawns", "Dimension check is ignored by " + this.spawnedFromType.typeName + " spawn type.");
                return true;
            }
    	}
    	if(world == null || world.provider == null || this.mobInfo == null || this.mobInfo.spawnInfo == null || this.mobInfo.spawnInfo.dimensionTypes == null) {
            LycanitesMobs.printDebug("MobSpawns", "No dimension spawn settings were found for this mob, defaulting to valid.");
            return true;
        }
    	
    	// Check Types:
		for(String spawnDimensionType : this.mobInfo.spawnInfo.dimensionTypes) {
    		if("ALL".equalsIgnoreCase(spawnDimensionType)) {
                LycanitesMobs.printDebug("MobSpawns", "All dimensions allowed.");
    			return true;
    		}
    		if("VANILLA".equalsIgnoreCase(spawnDimensionType)) {
                LycanitesMobs.printDebug("MobSpawns", "Vanilla only: Overworld, Nether and End.");
    			return world.provider.dimensionId > -2 && world.provider.dimensionId < 2;
    		}
            if("GROUP".equalsIgnoreCase(spawnDimensionType)) {
            	for(String groupSpawnDimensionType : this.mobInfo.group.dimensionTypes) {
	            	if("ALL".equalsIgnoreCase(groupSpawnDimensionType)) {
                        LycanitesMobs.printDebug("MobSpawns", "All dimensions allowed by group.");
	        			return true;
	        		}
	        		if("VANILLA".equalsIgnoreCase(groupSpawnDimensionType)) {
                        LycanitesMobs.printDebug("MobSpawns", "Vanilla only by group: Overworld, Nether and End.");
	        			return world.provider.dimensionId > -2 && world.provider.dimensionId < 2;
	        		}
            	}
                for(int spawnDimension : this.mobInfo.group.dimensionBlacklist) {
                    if(world.provider.dimensionId == spawnDimension) {
                        LycanitesMobs.printDebug("MobSpawns", "Dimension is in group " + (this.mobInfo.group.dimensionWhitelist ? "whitelist, allowed" : "blacklist, not allowed") + ".");
                        return this.mobInfo.group.dimensionWhitelist;
                    }
                }
                if(this.mobInfo.spawnInfo.dimensionBlacklist == null || this.mobInfo.spawnInfo.dimensionBlacklist.length == 0) {
                    LycanitesMobs.printDebug("MobSpawns", "Dimension was not in group " + (this.mobInfo.group.dimensionWhitelist ? "whitelist, not allowed" : "blacklist, allowed") + " and there are no entries in the mob specific black/whitelist.");
                    return !this.mobInfo.group.dimensionWhitelist;
                }
            }
    	}
		
		// Check IDs:
    	for(int spawnDimension : this.mobInfo.spawnInfo.dimensionBlacklist) {
    		if(world.provider.dimensionId == spawnDimension) {
                LycanitesMobs.printDebug("MobSpawns", "Dimension is in " + (this.mobInfo.spawnInfo.dimensionWhitelist ? "whitelist, allowed" : "blacklist, not allowed") + ".");
    			return this.mobInfo.spawnInfo.dimensionWhitelist;
    		}
    	}
        LycanitesMobs.printDebug("MobSpawns", "Dimension was not in " + (this.mobInfo.spawnInfo.dimensionWhitelist ? "whitelist, not allowed" : "blacklist, allowed") + ".");
        return !this.mobInfo.spawnInfo.dimensionWhitelist;
    }
    
    // ========== Spawn Limit Check ==========
    /** Checks for nearby entities of this type from the ijk (xyz) location, mobs use this so that too many don't spawn in the same area. Returns true if the mob should spawn. **/
    public boolean spawnLimitCheck(World world, int i, int j, int k) {
    	 int spawnLimit = this.mobInfo.spawnInfo.spawnAreaLimit;
    	 double range = SpawnInfo.spawnLimitRange;
    	 LycanitesMobs.printDebug("MobSpawns", "Checking spawn area limit. Limit of: " + spawnLimit + " Range of: " + range);
         if(spawnLimit > 0 && range > 0) {
         	List targets = this.getNearbyEntities(this.mobInfo.entityClass, range);
         	LycanitesMobs.printDebug("MobSpawns", "Found " + targets.size() + " of this mob within the radius (class is " + this.mobInfo.entityClass + ").");
         	if(targets.size() >= spawnLimit)
         		return false;
         }
         return true;
    }
    
    // ========== Spawn Block Check ==========
    /** Checks for nearby blocks from the xyz block location, Cinders use this when spawning by Fire Blocks. **/
    public boolean spawnBlockCheck(World world, int x, int y, int z) {
        if(this.spawnedFromType != null && SpawnInfo.enforceBlockCost) {
        	int blocksFound = 0;
        	if(this.spawnedFromType.materials != null) {
        		for(int i = x - this.spawnedFromType.range; i <= x + this.spawnedFromType.range; i++)
                	for(int j = y - this.spawnedFromType.range; j <= y + this.spawnedFromType.range; j++)
                    	for(int k = z - this.spawnedFromType.range; k <= z + this.spawnedFromType.range; k++) {
                    		Material blockMaterial = world.getBlock(i, j, k).getMaterial();
                    		for(Material validMaterial : this.spawnedFromType.materials) {
    							if(blockMaterial == validMaterial) {
    								if(++blocksFound >= this.mobInfo.spawnInfo.spawnBlockCost)
                        				return true;
    							}
    						}
                    	}
        	}
        	if(this.spawnedFromType.blocks != null) {
        		for(int i = x - this.spawnedFromType.range; i <= x + this.spawnedFromType.range; i++)
                	for(int j = y - this.spawnedFromType.range; j <= y + this.spawnedFromType.range; j++)
                    	for(int k = z - this.spawnedFromType.range; k <= z + this.spawnedFromType.range; k++) {
                    		Block block = world.getBlock(i, j, k);
                    		for(Block validBlock : this.spawnedFromType.blocks) {
    							if(block == validBlock) {
    								if(++blocksFound >= this.mobInfo.spawnInfo.spawnBlockCost)
                        				return true;
    							}
    						}
                    	}
        	}
        	if(this.spawnedFromType.blockStrings != null) {
        		for(int i = x - this.spawnedFromType.range; i <= x + this.spawnedFromType.range; i++)
                	for(int j = y - this.spawnedFromType.range; j <= y + this.spawnedFromType.range; j++)
                    	for(int k = z - this.spawnedFromType.range; k <= z + this.spawnedFromType.range; k++) {
                    		Block block = world.getBlock(i, j, k);
                    		for(String validBlockString : this.spawnedFromType.blockStrings) {
    							if(block == ObjectManager.getBlock(validBlockString)) {
    								if(++blocksFound >= this.mobInfo.spawnInfo.spawnBlockCost)
                        				return true;
    							}
    						}
                    	}
        	}
        	return false;
        }
    	return true;
    }
    
    // ========== Egg Spawn ==========
    /** Called once this mob is spawned with a Spawn Egg. **/
    @Override
    public IEntityLivingData onSpawnWithEgg(IEntityLivingData livingData) {
    	livingData = super.onSpawnWithEgg(livingData);
        return livingData;
    }
    
    // ========== Despawning ==========
    /** Returns whether this mob should despawn overtime or not. Config defined forced despawns override everything except tamed creatures and tagged creatures. **/
    @Override
    protected boolean canDespawn() {
    	if(this.mobInfo.spawnInfo.despawnForced)
    		return true;
    	if(!this.mobInfo.spawnInfo.despawnNatural)
    		return false;
    	if(this.forceNoDespawn)
    		return false;
        if(this.boss || this.getSubspeciesIndex() >= 3)
            return false;
    	if(this.isPersistant() || this.getLeashed() || (this.hasCustomNameTag() && "".equals(this.spawnEventType)))
    		return false;
    	return super.canDespawn();
    }
    
    /** Returns true if this mob should not despawn in unloaded chunks.
     * Most farmable mobs never despawn, but can be set to despawn in the config where this will kick in.
     * Here mobs can check if they have ever been fed or bred or moved from their home dimension.
     * Farmable mobs can then be set to despawn unless they have been farmed by a player.
     * Useful for the Pinky Nether invasion issues! Also good for water animals that can't spawn as CREATURE.
     * Leashed mobs don't ever despawn naturally and don't rely on this.
     * There is also the vanilla variable persistenceRequired which is handled in vanilla code too.
    **/
    public boolean isPersistant() {
    	return false;
    }
    
    /** A check that is constantly done, if this returns true, this entity will be removed, used normally for peaceful difficulty removal and temporary minions. **/
    public boolean despawnCheck() {
        if(this.worldObj.isRemote)
        	return false;
        
        // Disabled Mobs:
        if(!this.mobInfo.mobEnabled)
        	return true;
        
        // Temporary Mobs:
        if(this.isTemporary && this.temporaryDuration-- <= 0)
        	return true;
        
        // Mob Event Despawning:
        if(this.getLeashed() || this.isPersistant()) {
        	this.spawnEventType = "";
        	this.spawnEventCount = -1;
        }
        else {
        	if(!this.mobInfo.peacefulDifficulty && this.worldObj.difficultySetting == EnumDifficulty.PEACEFUL && !this.hasCustomNameTag())
            	return true;
        	
        	ExtendedWorld worldExt = ExtendedWorld.getForWorld(this.worldObj);
        	if(worldExt != null) {
        		if(!"".equals(this.spawnEventType) && this.spawnEventCount >= 0 && this.spawnEventCount != worldExt.getWorldEventCount())
        			return true;
        	}
        }
        return false;
    }
    
    // ========== Spawner Checking ==========
    /** Checks if a Monster Spawner that spawns this mob is near the XYZ locations, checks within an 8 block radius. **/
    public boolean isSpawnerNearby(World world, int x, int y, int z) {
        if(world == null)
            return false;
    	int checkRange = 8;
        if(!world.doChunksNearChunkExist(x, y, z, checkRange))
            return false;
    	for(int i = x - checkRange; i <= x + checkRange; i++)
        	for(int j = y - checkRange; j <= y + checkRange; j++)
            	for(int k = z - checkRange; k <= z + checkRange; k++) {
            		Block spawnerBlock = world.getBlock(i, j, k);
            		if(spawnerBlock != null) {
	            		TileEntity tileEntity = world.getTileEntity(i, j, k);
	            		if(tileEntity != null && tileEntity instanceof TileEntityMobSpawner) {
	            			if(((TileEntityMobSpawner)tileEntity).func_145881_a().getEntityNameToSpawn().equals(ObjectManager.entityLists.get(this.group.filename).getEntityString(this))) //getSpawnerLogic()
	            				return true;
	            		}
	            	}	
            	}
    	return false;
    }
    
    // ========== Block Checking ==========
    /** Checks if the specified block is underground (unable to see the sky above it). This checks through leaves, plants, grass and vine materials. **/
    public boolean isBlockUnderground(int x, int y, int z) {
    	if(this.worldObj.canBlockSeeTheSky(x, y, z))
    		return false;
    	for(int j = y; j < this.worldObj.getHeight(); j++) {
    		Material blockMaterial = this.worldObj.getBlock(x, j, z).getMaterial();
    		if(blockMaterial != Material.air
    				&& blockMaterial != Material.leaves
    				&& blockMaterial != Material.plants
    				&& blockMaterial != Material.grass
    				&& blockMaterial != Material.vine)
    			return true;
    	}
    	return false;
    }

    // ========== Boss ==========
    /** Returns whether or not this mob is a boss. **/
    public boolean isBoss() {
        return this.boss;
    }


    // ========== Summoning ==========
    public void summonMinion(EntityLivingBase minion, double angle, double distance) {
        double angleRadians = Math.toRadians(angle);
        double x = this.posX + ((this.width + distance) * Math.cos(angleRadians) - Math.sin(angleRadians));
        double y = this.posY + 1;
        double z = this.posZ + ((this.width + distance) * Math.sin(angleRadians) + Math.cos(angleRadians));
        minion.setLocationAndAngles(x, y, z, this.rand.nextFloat() * 360.0F, 0.0F);
        if(minion instanceof EntityCreatureBase) {
            ((EntityCreatureBase)minion).setMinion(true);
            ((EntityCreatureBase)minion).setSubspecies(this.getSubspeciesIndex(), true);
            ((EntityCreatureBase)minion).setMasterTarget(this);
        }
        this.worldObj.spawnEntityInWorld(minion);
        if(this.getAttackTarget() != null)
            minion.setRevengeTarget(this.getAttackTarget());
    }

    // ========== Minion Update ==========
    public void onMinionUpdate(EntityLivingBase minion, long tick) {}

    // ========== Minion Death ==========
    public void onMinionDeath(EntityLivingBase minion) {}

    // ========== Minion ==========
    /** Set whether this mob is a minion or not, this should be used if this mob is summoned. **/
    public void setMinion(boolean minion) { this.isMinion = minion; }
    /** Returns whether or not this mob is a minion. **/
    public boolean isMinion() {
        return this.isMinion;
    }
    
    // ========== Temporary Mob ==========
    /** Make this mob temporary where it will desapwn once the specified duration (in ticks) reaches 0. **/
    public void setTemporary(int duration) {
    	this.temporaryDuration = duration;
    	this.isTemporary = true;
    }
    /** Remove the temporary life duration of this mob, note that this mob will still despawn naturally unless it is set as persistent through other means. **/
    public void unsetTemporary() {
    	this.isTemporary = false;
    	this.temporaryDuration = 0;
    }

    // ========== Minion ==========
    /** Returns true if this mob has a pet entry and is thus bound to another entity. **/
    public boolean isBoundPet() {
        return this.hasPetEntry();
    }

    /** Returns true if this mob has a pet entry. **/
    public boolean hasPetEntry() {
        return this.getPetEntry() != null;
    }

    /** Returns this mob's pet entry if it has one. **/
    public PetEntry getPetEntry() {
        return this.petEntry;
    }

    /** Sets the pet entry for this mob. Mobs with Pet Entries will be removed when te world is reloaded as the pet Entry will spawn a new instance of them in on load. **/
    public void setPetEntry(PetEntry petEntry) {
        this.petEntry = petEntry;
    }

    /** Returns true if this creature has a pet entry and matches the provided entry type. **/
    public boolean isPetType(String type) {
        if(!this.hasPetEntry())
            return false;
        return type.equals(this.getPetEntry().getType());
    }
    
    // ========== On Spawn ==========
    /** This is called when the mob is first spawned to the world either through natural spawning or from a Spawn Egg. **/
    public void onFirstSpawn() {
        if(MobInfo.subspeciesSpawn)
    	    this.getRandomSubspecies();
        if(MobInfo.randomSizes)
    	    this.getRandomSize();
    }
    
    // ========== Get Random Subspecies ==========
    public void getRandomSubspecies() {
    	if(this.subspecies == null && !this.isMinion()) {
    		this.subspecies = this.mobInfo.getRandomSubspecies(this, this.spawnedRare);
    		this.applySubspeciesHealthMultiplier();
    		if(this.subspecies != null)
    			LycanitesMobs.printDebug("Subspecies", "Setting " + this.getSpeciesName() + " to " + this.subspecies.getTitle());
    		else
    			LycanitesMobs.printDebug("Subspecies", "Setting " + this.getSpeciesName() + " to base species.");
    	}
    }
    
    // ========== Get Random Size ==========
    public void getRandomSize() {
    	this.sizeScale = 1.0D + (0.35D * (0.5D - this.getRNG().nextDouble()));
    	this.updateSize();
    }
	
	
	// ==================================================
	//            Stat Multipliers and Boosts
	// ==================================================
    /** Returns the base health for this mob. This is not the current max health. **/
    public double getBaseHealth() {
    	if(baseHealthMap.containsKey(this.getClass()))
    		return (baseHealthMap.get(this.getClass()) * this.getHealthMultiplier()) + this.getHealthBoost();
    	return 10D;
    }

    /** Applies the subspecies health multipler for this mob. **/
    public void applySubspeciesHealthMultiplier() {
    	if(this.getSubspeciesIndex() < 1) {
    		this.getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(this.getBaseHealth());
    		this.setHealth((float)(this.getBaseHealth()));
    	}
    	else if(this.getSubspeciesIndex() < 3) {
    		this.getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(this.getBaseHealth() * 4);
    		this.setHealth((float)(this.getBaseHealth() * Subspecies.uncommonHealthScale));
    	}
    	else {
    		this.getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(this.getBaseHealth() * 10);
    		this.setHealth((float)(this.getBaseHealth() * Subspecies.rareHealthScale));
    	}
    }
    
    /** Returns the shared multiplier for all stats based on difficulty. **/
	public double getDifficultyMultiplier(String stat) {
        if(this.worldObj == null || this.worldObj.difficultySetting == null)
            return MobInfo.difficultyMutlipliers.get("NORMAL" + "-" + stat.toUpperCase());
		EnumDifficulty difficulty = this.worldObj.difficultySetting;
		String difficultyName = "Easy";
		if(difficulty.getDifficultyId() >= 3)
			difficultyName = "Hard";
		else if(difficulty == EnumDifficulty.NORMAL)
			difficultyName = "Normal";
		return MobInfo.difficultyMutlipliers.get(difficultyName.toUpperCase() + "-" + stat.toUpperCase());
	}

    /** Returns the Altar multiplier, usually used by Altar 'mini-boss' rare subspecies. **/
    public double getAltarMultiplier(String stat) {
        return AltarInfo.rareSubspeciesMutlipliers.get(stat.toUpperCase());
    }

    // ========= Health ==========
    /** Returns the health scale of this mob. **/
    public double getHealthMultiplier() {
        double multiplier = this.mobInfo.multiplierHealth * this.getDifficultyMultiplier("health");
        if(this.altarSummoned)
            multiplier *= this.getAltarMultiplier("health");
        if(this.extraMobBehaviour != null)
            multiplier *= this.extraMobBehaviour.multiplierHealth;
        return multiplier;
    }
    /** Returns the health boost of this mob. **/
    public int getHealthBoost() {
        int boost = this.mobInfo.boostHealth;
        if(this.extraMobBehaviour != null)
            boost += this.extraMobBehaviour.boostHealth;
        return boost;
    }
    
    // ========= Defense ==========
    /** Returns the defense scale of this mob, see getDamageAfterDefense() for the logic. **/
    public double getDefenseMultiplier() {
    	double multiplier = this.mobInfo.multiplierDefense * this.getDifficultyMultiplier("defense");
        if(this.altarSummoned)
            multiplier *= this.getAltarMultiplier("defense");
    	if(this.extraMobBehaviour != null)
    		multiplier *= this.extraMobBehaviour.multiplierDefense;
    	return multiplier;
    }
    /** Returns the defense boost of this mob. **/
    public int getDefenseBoost() {
    	int boost = this.mobInfo.boostDefense;
    	if(this.extraMobBehaviour != null)
    		boost += this.extraMobBehaviour.boostDefense;
    	return boost;
    }
    
    // ========= Speed ==========
    /** Returns the speed scale of this mob. **/
    public double getSpeedMultiplier() {
    	double multiplier = this.mobInfo.multiplierSpeed * this.getDifficultyMultiplier("speed");
        if(this.altarSummoned)
            multiplier *= this.getAltarMultiplier("speed");
    	if(this.extraMobBehaviour != null)
    		multiplier *= this.extraMobBehaviour.multiplierSpeed;
    	return multiplier;
    }
    /** Returns the speed boost of this mob. **/
    public float getSpeedBoost() {
    	int boost = this.mobInfo.boostSpeed / 100;
    	if(this.extraMobBehaviour != null)
    		boost += this.extraMobBehaviour.boostSpeed;
    	return boost;
    }
    
    // ========= Damage ==========
    /**Returns the damage scale of this mob. **/
    public double getDamageMultiplier() {
    	double multiplier = this.mobInfo.multiplierDamage * this.getDifficultyMultiplier("damage");
        if(this.altarSummoned)
            multiplier *= this.getAltarMultiplier("damage");
    	if(this.extraMobBehaviour != null)
    		multiplier *= this.extraMobBehaviour.multiplierDamage;
    	return multiplier;
    }
    /**Returns the damage boost of this mob. **/
    public int getDamageBoost() {
    	int boost = this.mobInfo.boostDamage;
    	if(this.extraMobBehaviour != null)
    		boost += this.extraMobBehaviour.boostDamage;
    	return boost;
    }
    
    // ========= Haste ==========
    /** Used to scale the rate of abilities such as attack speed. Note: Abilities are normally capped at around 10 ticks minimum due to performance issues and the entity update rate. **/
    public double getHasteMultiplier() {
    	double multiplier = this.mobInfo.multiplierHaste * this.getDifficultyMultiplier("haste");
        if(this.altarSummoned)
            multiplier *= this.getAltarMultiplier("haste");
    	if(this.extraMobBehaviour != null)
    		multiplier *= this.extraMobBehaviour.multiplierHaste;
    	return multiplier;
    }
    /** Used to boost the rate of abilities such as attack speed. Note: Abilities are normally capped at around 10 ticks minimum due to performance issues and the entity update rate. **/
    public int getHasteBoost() {
    	int boost = this.mobInfo.boostHaste;
    	if(this.extraMobBehaviour != null)
    		boost += this.extraMobBehaviour.boostHaste;
    	return boost;
    }
    /** When given a base delay (in ticks) this will return the scaled delay with difficulty and other modifiers taken into account
     * ticks - The base duration in ticks between actions (such as between ranged attacks).
    **/
    public int getHaste(int ticks) {
    	ticks -= this.getHasteBoost();
    	double ticksScale = 1 / this.getHasteMultiplier();
    	ticks = Math.round((float)ticks * (float)ticksScale);
		return Math.max(0, ticks);
    }
    
    // ========= Effect ==========
    /** Returns the duration scale of any effects that this mob uses, can include both buffs and debuffs on the enemy. **/
    public double getEffectMultiplier() {
    	double multiplier = this.mobInfo.multiplierEffect * this.getDifficultyMultiplier("effect");
        if(this.altarSummoned)
            multiplier *= this.getAltarMultiplier("effect");
    	if(this.extraMobBehaviour != null)
    		multiplier *= this.extraMobBehaviour.multiplierEffect;
    	return multiplier;
    }
    /** Returns the duration boost of any effects that this mob uses, can include both buffs and debuffs on the enemy. **/
    public int getEffectBoost() {
    	int boost = this.mobInfo.boostEffect;
    	if(this.extraMobBehaviour != null)
    		boost += this.extraMobBehaviour.boostEffect;
    	return boost;
    }
    /** When given a base time (in seconds) this will return the scaled time with difficulty and other modifiers taken into account
     * seconds - The base duration in seconds that this effect should last for.
    **/
    public int getEffectDuration(int seconds) {
		return Math.round(((float)seconds * (float)(this.getEffectMultiplier())) * 20) + (int)this.getEffectBoost();
    }
    /** When given a base effect strngth value such as a life drain amount, this will return the scaled value with difficulty and other modifiers taken into account
     * value - The base effect strength.
     **/
    public float getEffectStrength(float value) {
        return Math.round((value * (float)(this.getEffectMultiplier()))) + this.getEffectBoost();
    }
    
    // ========= Pierce ==========
    /** Returns the armor piercing multipler. **/
    public double getPierceMultiplier() {
    	double multiplier = this.mobInfo.multiplierPierce * this.getDifficultyMultiplier("pierce");
        if(this.altarSummoned)
            multiplier *= this.getAltarMultiplier("pierce");
    	if(this.extraMobBehaviour != null)
    		multiplier *= this.extraMobBehaviour.multiplierPierce;
    	return multiplier;
    }
    /** Returns the armor piercing boost. **/
    public double getPierceBoost() {
    	int boost = this.mobInfo.boostPierce;
    	if(this.extraMobBehaviour != null)
    		boost += this.extraMobBehaviour.boostPierce;
    	return boost;
    }
    /** Returns the base armor piercing value. This should really be left unchanged. **/
    public double getPierceBase() {
    	return 5;
    }
    /** Returns the calculated armor piercing value. This is with all multipliers and boosts applied. Cannot be less than 1 where all damage will pierce. **/
    public double getPierceValue() {
    	double value = this.getPierceBase();
    	value *= 1 / this.getPierceMultiplier();
    	value -= Math.max(0, this.getPierceBoost());
    	return Math.max(1.0D, value);
    }


    // ==================================================
    //                  Battle Phases
    // ==================================================
    /** Called every update, this usually manages which phase this mob is using health but it can use any aspect of the mob to determine the Battle Phase and could even be random. **/
    public void updateBattlePhase() {

    }

    /** Returns the current battle phase. **/
    public int getBattlePhase() {
        return this.battlePhase;
    }
    
    
    // ==================================================
  	//                    Subspecies
  	// ==================================================
    /** Sets the subspecies of this mob by index. If not a valid ID or 0 it will be set to null which is for base species. **/
    public void setSubspecies(int subspeciesIndex, boolean resetHealth) {
    	this.subspecies = this.mobInfo.getSubspecies(subspeciesIndex);
        int scaledExp = this.experience;
        if(subspeciesIndex == 1 || subspeciesIndex == 2)
            scaledExp = Math.round((float)(this.experience * Subspecies.uncommonExperienceScale));
        else if(subspeciesIndex >= 3)
            scaledExp = Math.round((float)(this.experience * Subspecies.rareExperienceScale));
        this.experienceValue = scaledExp;
    	if(resetHealth)
    		this.applySubspeciesHealthMultiplier();
    }

    /** Gets the subspecies of this mob, will return null if this is a base species mob. **/
    public Subspecies getSubspecies() {
    	return this.subspecies;
    }

    /** Gets the subspecies index of this mob.
     * 0 = Base Subspecies
     * 1/2 = Uncommon Species
     * 3+ = Rare Species
     * Most mobs have 2 uncommon subspecies, some have rare subspecies.
     * **/
    public int getSubspeciesIndex() {
    	return this.getSubspecies() != null ? this.getSubspecies().index : 0;
    }
    
    
    // ==================================================
  	//                     Updates
  	// ==================================================
    // ========== Main ==========
    /** The main update method, all the important updates go here. **/
    public void onUpdate() {
    	if(this.dataWatcher != null)
    		this.onSyncUpdate();
        super.onUpdate();

        if(!this.worldObj.isRemote)
            this.updateHitAreas();
        
        if(this.despawnCheck()) {
            if(!this.isBoundPet())
        	    this.inventory.dropInventory();
        	this.setDead();
        }
        
        // Fire Immunity:
        this.isImmuneToFire = !this.canBurn();
        
        // Not Walking on Land:
        if((!this.canWalk() && !this.canFly() && !this.isInWater() && this.isMoving()) || !this.canMove())
        	this.clearMovement();
        
        // Climbing:
        if(!this.worldObj.isRemote) {
        	this.setBesideClimbableBlock(this.isCollidedHorizontally);
        	if(this.flySoundSpeed > 0 && this.ticksExisted % 20 == 0)
        		this.playFlySound();
        }
        
        // GUI Refresh Tick:
        if(!this.worldObj.isRemote && this.guiViewers.size() <= 0)
        	this.guiRefreshTick = 0;
        if(!this.worldObj.isRemote && this.guiRefreshTick > 0) {
        	if(--this.guiRefreshTick <= 0) {
        		this.refreshGUIViewers();
        		this.guiRefreshTick = 0;
        	}
        }
    }
    
    // ========== AI ==========
    /** Runs through all the AI tasks this mob has on the update, will update the flight navigator if this mob is using it too. **/
    @Override
    protected void updateAITasks() {
		if(this.useFlightNavigator()) flightNavigator.updateFlight();
        super.updateAITasks();
    }
    
    // ========== Living ==========
    /** The main update method, behaviour and custom update logic should go here. **/
    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        this.updateBattlePhase();
        this.updateArmSwingProgress();
        
        // First Spawn:
        if(!this.worldObj.isRemote && this.firstSpawn) {
        	this.onFirstSpawn();
        	this.firstSpawn = false;
        }

        // Prevent Creative Attack Target:
        if(this.hasAttackTarget()) {
            if(this.getAttackTarget() instanceof EntityPlayer) {
                EntityPlayer targetPlayer = (EntityPlayer)this.getAttackTarget();
                if(targetPlayer.capabilities.isCreativeMode)
                    this.setAttackTarget(null);
            }
        }
        
        // Fleeing:
        if(this.hasAvoidTarget()) {
        	if(this.currentFleeTime-- <= 0)
        		this.setAvoidTarget(null);
        }
        
        // Gliding:
        if(!this.onGround && this.motionY < 0.0D) {
            this.motionY *= this.getFallingMod();
        }
        
        // Sunlight Damage:
        if(!this.worldObj.isRemote && this.daylightBurns() && this.worldObj.isDaytime()) {
        	float brightness = this.getBrightness(1.0F);
            if(brightness > 0.5F && this.rand.nextFloat() * 30.0F < (brightness - 0.4F) * 2.0F && this.worldObj.canBlockSeeTheSky(MathHelper.floor_double(this.posX), MathHelper.floor_double(this.posY), MathHelper.floor_double(this.posZ))) {
                boolean shouldBurn = true;
                ItemStack helmet = this.inventory.getEquipmentStack("head");
                if(helmet != null) {
                    if(helmet.isItemStackDamageable()) {
                    	helmet.setItemDamage(helmet.getItemDamageForDisplay() + this.rand.nextInt(2));
                        if(helmet.getItemDamageForDisplay() >= helmet.getMaxDamage()) {
                            this.renderBrokenItemStack(helmet);
                            this.setCurrentItemOrArmor(4, (ItemStack)null);
                        }
                    }
                    shouldBurn = false;
                }
                if(shouldBurn)
                    this.setFire(8);
            }
        }
        
        // Water Damage:
        if(!this.worldObj.isRemote && this.waterDamage() && this.isWet()) {
            this.attackEntityFrom(DamageSource.drown, 1.0F);
        }
        
        // Out of Water Suffocation:
        if(!this.worldObj.isRemote && !this.canBreatheAboveWater()) {
	        int currentAir = this.getAir();
	        if(this.isEntityAlive()) {
	        	if((!this.isLavaCreature && !this.waterContact())
	        		|| (this.isLavaCreature && !this.lavaContact())) {
		        	currentAir--;
		            this.setAir(currentAir);
		            if(this.getAir() <= -200) {
		                this.setAir(-180);
		                this.attackEntityFrom(DamageSource.drown, 2.0F);
		            }
		        }
		        else {
		            this.setAir(299);
		        }
	        }
        }
        
        // Time Out Quicker In Light:
        float light = this.getBrightness(1.0F);
        if(!this.mobInfo.spawnInfo.spawnsInLight && light > 0.5F)
            this.entityAge += 2;
        
	    // Stealth Invisibility:
    	if(!this.worldObj.isRemote) {
	        if(this.isStealthed() && !this.isInvisible())
	        	setInvisible(true);
	        else if(!this.isStealthed() && this.isInvisible() && !this.isPotionActive(Potion.invisibility.id))
	        	setInvisible(false);
    	}
        if(this.isStealthed()) {
        	if(this.stealthPrev != this.isStealthed())
        		startStealth();
        	onStealth();
        }
        else if(this.isInvisible() && !this.isPotionActive(Potion.invisibility.id))
        	setInvisible(false);
        this.stealthPrev = this.isStealthed();
        
        // Blocking:
        if(this.currentBlockingTime > 0) {
        	this.currentBlockingTime--;
        }
        if(this.currentBlockingTime < 0)
        	this.currentBlockingTime = 0;
        
        // Pickup Items:
        if(this.ticksExisted % 10 == 0 && !this.worldObj.isRemote && this.isEntityAlive() && this.canPickupItems())
        	this.pickupItems();
        
        // Entity Pickups:
        if(this.pickupEntity != null) {
			if(!this.pickupEntity.isEntityAlive())
				this.dropPickupEntity();
			else if(this.pickupEntity instanceof EntityLivingBase) {
				if(((EntityLivingBase)this.pickupEntity).getHealth() <= 0)
					this.dropPickupEntity();
			}
			else if(this.getDistanceSqToEntity(this.pickupEntity) > 32D) {
				this.dropPickupEntity();
			}
        }

        // Boss Health Bar:
        if(this.worldObj.isRemote && this.showBossHealthBar()) {
            if(this.bossHealth == null)
                this.bossHealth = new BossHealth(this);
            BossStatus.setBossStatus(this.bossHealth, true);
        }

        // Minion To Master Update:
        if(this.getMasterTarget() != null && this.getMasterTarget() instanceof EntityCreatureBase)
            ((EntityCreatureBase)this.getMasterTarget()).onMinionUpdate(this, this.updateTick);

        this.updateTick++;
    }
    
    // ========== Sync Update ==========
    /** An update that is called to sync things with the client and server such as various entity targets, attack phases, animations, etc. **/
    public void onSyncUpdate() {
    	// Sync Target Status:
    	if(!this.worldObj.isRemote) {
    		byte targets = 0;
    		if(this.getAttackTarget() != null)
    			targets += TARGET_ID.ATTACK.id;
    		if(this.getMasterTarget() != null)
    			targets += TARGET_ID.MASTER.id;
    		if(this.getParentTarget() != null)
    			targets += TARGET_ID.PARENT.id;
    		if(this.getAvoidTarget() != null)
    			targets += TARGET_ID.AVOID.id;
    		if(this.getRiderTarget() != null)
    			targets += TARGET_ID.RIDER.id;
    		this.dataWatcher.updateObject(WATCHER_ID.TARGET.id, targets);
    	}
    	
		// Attack Phase:
    	if(!this.worldObj.isRemote)
    		this.dataWatcher.updateObject(WATCHER_ID.ATTACK_PHASE.id, this.attackPhase);
        
    	// Animations Server:
        if(!this.worldObj.isRemote) {
        	byte animations = 0;
        	
        	// Atttacked Animation and Sound:
        	if(this.justAttacked == this.justAttackedTime) {
        		animations += ANIM_ID.ATTACKED.id;
        		this.justAttacked = 0;
        		this.playAttackSound();
        	}
        	
        	// Airborne Animation:
        	if(this.onGround)
        		animations += ANIM_ID.GROUNDED.id;
        	
        	// Blocking Animation:
        	if(this.isBlocking())
        		animations += ANIM_ID.BLOCKING.id;
        	
        	// Blocking Animation:
        	if(this.isMinion())
        		animations += ANIM_ID.MINION.id;
        	
        	// Extra Animation 01:
        	if(this.extraAnimation01())
        		animations += ANIM_ID.EXTRA01.id;
        	
        	this.dataWatcher.updateObject(WATCHER_ID.ANIMATION.id, animations);
        }
        
        // Animations Client:
        else if(this.worldObj.isRemote) {
        	byte animations = this.dataWatcher.getWatchableObjectByte(WATCHER_ID.ANIMATION.id);
        	if(this.justAttacked > 0)
        		this.justAttacked--;
        	else if((animations & ANIM_ID.ATTACKED.id) > 0)
        		this.setJustAttacked();
        	this.onGround = (animations & ANIM_ID.GROUNDED.id) > 0;
        	this.extraAnimation01 = (animations & ANIM_ID.EXTRA01.id) > 0;
        }
        
        // Is Minon:
        if(this.worldObj.isRemote) {
    		this.isMinion = (this.dataWatcher.getWatchableObjectByte(WATCHER_ID.ANIMATION.id) & ANIM_ID.MINION.id) > 0;
        }
        
        // Subspecies:
        if(!this.worldObj.isRemote) {
    		this.dataWatcher.updateObject(WATCHER_ID.SUBSPECIES.id, Byte.valueOf((byte)this.getSubspeciesIndex()));
        }
        else {
        	if(this.getSubspeciesIndex() != this.dataWatcher.getWatchableObjectByte(WATCHER_ID.SUBSPECIES.id))
        		this.setSubspecies(this.dataWatcher.getWatchableObjectByte(WATCHER_ID.SUBSPECIES.id), false);
        }
        
        // Size:
        if(!this.worldObj.isRemote) {
    		this.dataWatcher.updateObject(WATCHER_ID.SIZE.id, Float.valueOf((float)this.sizeScale));
        }
        else {
        	if(this.sizeScale != this.dataWatcher.getWatchableObjectFloat(WATCHER_ID.SIZE.id)) {
        		this.sizeScale = this.dataWatcher.getWatchableObjectFloat(WATCHER_ID.SIZE.id);
        		this.updateSize();
        	}
        }
    }

    // ========== Hit Areas ==========
    public void updateHitAreas() {
        int hitAreaHeightCount = Math.max(1, Math.round(this.height / 4));
        int hitAreaWidthCount =  Math.max(1, Math.round(this.width / 4));
        if(hitAreaHeightCount < 2 && hitAreaWidthCount < 2) {
            this.hitAreas = null;
            return;
        }

        if(this.hitAreas == null || this.hitAreas[0] == null || this.hitAreas[0][0] == null ||
                this.hitAreas.length != hitAreaHeightCount || this.hitAreas[0].length != hitAreaWidthCount || this.hitAreas[0][0].length != hitAreaWidthCount)
            this.hitAreas = new EntityHitArea[hitAreaHeightCount][hitAreaWidthCount][hitAreaWidthCount];

        for(int y = 0; y < hitAreaHeightCount; y++) {
            for(int x = 0; x < hitAreaWidthCount; x++) {
                for(int z = 0; z < hitAreaWidthCount; z++) {
                    if(y != 0 && y != hitAreaHeightCount - 1 && x != 0 && x != hitAreaWidthCount - 1 && z != 0 && z != hitAreaWidthCount - 1)
                        continue;
                    if(this.hitAreas[y][x][z] == null) {
                        this.hitAreas[y][x][z] = new EntityHitArea(this, (this.width / hitAreaWidthCount) * this.hitAreaScale, this.height / hitAreaHeightCount);
                        this.worldObj.spawnEntityInWorld(this.hitAreas[y][x][z]);
                    }
                    this.hitAreas[y][x][z].posX = this.posX - ((this.width * this.hitAreaScale) / 2) + (((this.width * this.hitAreaScale) / hitAreaWidthCount) / 2) + (((this.width * this.hitAreaScale) / hitAreaWidthCount) * x);
                    this.hitAreas[y][x][z].posY = this.posY + ((this.height / hitAreaHeightCount) * y);
                    this.hitAreas[y][x][z].posZ = this.posZ - ((this.width * this.hitAreaScale) / 2) + (((this.width * this.hitAreaScale) / hitAreaWidthCount) / 2) + (((this.width * this.hitAreaScale) / hitAreaWidthCount) * z);
                    this.hitAreas[y][x][z].rotationYaw = this.rotationYaw;
                }
            }
        }
    }
    
    
    // ==================================================
  	//                     Movement
  	// ==================================================
    /**
		Returns the importance of blocks when pathfinding, also used when checking if this mob can spawn.
	    Returns a float where 0.0F is a standard path, anything higher is a preferred path.
	    For example, animals that spawn on grass return 10.0F for path blocks that are Grass.
	    Mobs that prefer the darkness will return a higher value for darker blocks.
    **/
    // ========== Get Block Path Weight ==========
    public float getBlockPathWeight(int par1, int par2, int par3) {
        if(this.mobInfo.spawnInfo.spawnsInDark && !this.mobInfo.spawnInfo.spawnsInLight)
        	return 0.5F - this.worldObj.getLightBrightness(par1, par2, par3);
        if(this.mobInfo.spawnInfo.spawnsInLight && !this.mobInfo.spawnInfo.spawnsInDark)
        	return this.worldObj.getLightBrightness(par1, par2, par3) - 0.5F;
    	return 0.0F;
    }
    
    /** ========== Use Flight Navigator ==========
     * Returns true if this entity should use its flight navigator.
     * Used for flying mobs and free-swimming mobs.
     */
    public boolean useFlightNavigator() {
    	boolean freeSwimming = this.canSwim() && this.isInWater();
    	if(this.canFly() || freeSwimming)
    		return true;
    	return false;
    }
    
    // ========== Move with Heading ==========
    /** Moves the entity, redirects to the flight navigator if this mob should use that instead. **/
    @Override
    public void moveEntityWithHeading(float moveStrafe, float moveForward) {
    	if(!this.useFlightNavigator()) super.moveEntityWithHeading(moveStrafe, moveForward);
    	else this.flightNavigator.flightMovement(moveStrafe, moveForward);
    }
    
    // ========== Clear Movement ==========
    /** Cuts off all movement for this update, will clear any pathfinder paths, works with the flight navigator too. **/
    public void clearMovement() {
    	if(!useFlightNavigator())
        	this.getNavigator().clearPathEntity();
        else
        	this.flightNavigator.clearTargetPosition(1.0D);
    }
    
    // ========== Leash ==========
    /** The leash update that manages all behaviour to do with the entity being leashed or unleashed. **/
    @Override
    protected void updateLeashedState() {
        super.updateLeashedState();
        if(this.getLeashed() && this.getLeashedToEntity() != null && this.getLeashedToEntity().worldObj == this.worldObj) {
            Entity entity = this.getLeashedToEntity();
            this.setHome((int)entity.posX, (int)entity.posY, (int)entity.posZ, 5);
            float distance = this.getDistanceToEntity(entity);
            this.testLeash(distance);
            
            if(!this.leashAIActive) {
                this.tasks.addTask(2, this.leashMoveTowardsRestrictionAI);
                this.getNavigator().setAvoidsWater(false);
                this.leashAIActive = true;
            }

            if(distance > 4.0F)
                this.getNavigator().tryMoveToEntityLiving(entity, 1.0D);

            if(distance > 6.0F) {
                double d0 = (entity.posX - this.posX) / (double)distance;
                double d1 = (entity.posY - this.posY) / (double)distance;
                double d2 = (entity.posZ - this.posZ) / (double)distance;
                this.motionX += d0 * Math.abs(d0) * 0.4D;
                this.motionY += d1 * Math.abs(d1) * 0.4D;
                this.motionZ += d2 * Math.abs(d2) * 0.4D;
            }

            if(distance > 10.0F)
                this.clearLeashed(true, true);
        }
        else if(!this.getLeashed() && this.leashAIActive) {
            this.leashAIActive = false;
            this.tasks.removeTask(this.leashMoveTowardsRestrictionAI);
            this.getNavigator().setAvoidsWater(true);
            this.detachHome();
        }
    }
    
    /** ========== Pushed By Water ==========
     * Returns true if this mob should be pushed by water currents.
     * This will usually return false if the mob canSwim()
     */
    @Override
	public boolean isPushedByWater() {
        return !this.canSwim();
    }
    
    // ========== Is Moving ==========
    /** Returns true if this entity is moving towards a destination (doesn't check if this entity is being pushed, etc though). **/
    public boolean isMoving() {
    	if(!this.useFlightNavigator())
        	return this.getNavigator().getPath() != null;
        else
        	return !this.flightNavigator.atTargetPosition();
    }
    
    @Override
    /** Returns whether or not this entity can be leashed at all. **/
    public boolean allowLeashing() { return false; }

    // ========== Can Be Pushed ==========
    @Override
    public boolean canBePushed() {
        return super.canBePushed();
    }
    
    // ========== Can leash ==========
    /** Returns whether or not this entity can be leashed to the specified player. Useful for tamed entites. **/
    public boolean canLeash(EntityPlayer player) {
	    return false;
    }
    
    // ========== Test Leash ==========
    /** Called on the update to see if the leash should snap at the given distance. **/
    public void testLeash(float distance) {}
    
    // ========== Set AI Speed ==========
    /** Used when setting the movement speed of this mob, called by AI classes before movement and is given a speed modifier, a local speed modifier is also applied here. **/
    @Override
    public void setAIMoveSpeed(float speed) {
        super.setAIMoveSpeed(((speed + this.getSpeedBoost()) * this.getAISpeedModifier()) * (float)this.getSpeedMultiplier());
    }
    
    // ========== Movement Speed Modifier ==========
    /** The local speed modifier of this mob, AI classes will also provide their own modifiers that will be multiplied by this modifier. To be used dynamically by various mob behaviours. Not to be confused with getSpeedMultiplier(). **/
    public float getAISpeedModifier() {
    	return 1.0F;
    }
    
    // ========== Falling Speed Modifier ==========
    /** Used to change the falling speed of this entity, 1.0D does nothing. **/
    public double getFallingMod() {
    	return 1.0D;
    }
    
    // ========== Leap ==========
    /**
     * When called, this entity will leap forwards with the given distance and height.
     * This is very sensitive, a large distance or height can cause the entity to zoom off for thousands of blocks!
     * A distance of 1.0D is around 10 blocks forwards, a height of 0.5D is about 10 blocks up.
     * Tip: Use a negative height for flying and swimming mobs so that they can swoop down in the air or water.
    **/
    public void leap(double distance, double leapHeight) {
    	double angle = Math.toRadians(this.rotationYaw);
    	double xAmount = -Math.sin(angle);
    	double zAmount = Math.cos(angle);
        this.motionX = xAmount * distance + this.motionX * 0.2D;
        this.motionZ = zAmount * distance + this.motionZ * 0.2D;
        this.motionY = leapHeight;
    }
    
    // ========== Leap to Target ==========
    /** 
     * When called, this entity will leap towards the given target entity with the given height.
     * This is very sensitive, a large distance or height can cause the entity to zoom off for thousands of blocks!
     * A distance of 1.0D is around 10 blocks forwards, a height of 0.5D is about 10 blocks up.
     * Tip: Use a negative height for flying and swimming mobs so that they can swoop down in the air or water
    **/
    public void leap(float range, double leapHeight, Entity target) {
        if(target == null) return;
        float distance = target.getDistanceToEntity(this);
    	if(distance > 2.0F && distance <= range) {
            double xDist = this.getAttackTarget().posX - this.posX;
            double zDist = this.getAttackTarget().posZ - this.posZ;
            float mixedDist = MathHelper.sqrt_double(xDist * xDist + zDist * zDist);
            this.motionX = xDist / (double)mixedDist * 0.5D * 0.8D + this.motionX * 0.2D;
            this.motionZ = zDist / (double)mixedDist * 0.5D * 0.8D + this.motionZ * 0.2D;
            this.motionY = leapHeight;
        }
    }
    
    
    // ==================================================
  	//                     Positions
  	// ==================================================
    // ========== Home ==========
    /** Sets the home position for this entity to stay around and the distance it is allowed to stray from. **/
    public void setHome(int x, int y, int z, float distance) {
    	this.setHomePosition(x, y, z);
    	this.setHomeDistanceMax(distance);
    }
    /** Sets the home position for this entity to stay around. **/
    public void setHomePosition(int x, int y, int z) {
    	this.homePosition = new ChunkCoordinates(x, y, z);
    }
    /** Sets the distance this mob is allowed to stray from it's home. -1 will turn off the home restriction. **/
    public void setHomeDistanceMax(float newDist) { this.homeDistanceMax = newDist; }
    /** Returns the home position in ChunkCoordinates. **/
    public ChunkCoordinates getHomePosition() { return this.homePosition; }
    /** Gets the distance this mob is allowed to stray from it's home. -1 is used to unlimited distance. **/
    public float getHomeDistanceMax() { return this.homeDistanceMax; }
    /** Clears the current home position. **/
    public void detachHome() {
    	this.setHomeDistanceMax(-1);
    }
    /** Returns whether or not this mob has a home set. **/
    public boolean hasHome() {
    	return this.getHomePosition() != null && this.getHomeDistanceMax() >= 0;
    }
    /** Returns whether or not the given XYZ position is near this entity's home position, returns true if no home is set. **/
    public boolean positionNearHome(int x, int y, int z) {
        if(!hasHome()) return true;
        return this.getDistanceFromHome(x, y, z) < this.getHomeDistanceMax() * this.getHomeDistanceMax();
    }
    /** Returns the distance that the specified XYZ position is from the home position. **/
    public float getDistanceFromHome(int x, int y, int z) {
    	if(!hasHome()) return 0;
    	return this.homePosition.getDistanceSquared(x, y, z);
    }
    /** Returns the distance that the entity's position is from the home position. **/
    public float getDistanceFromHome() {
    	return this.homePosition.getDistanceSquared((int)this.posX, (int)this.posY, (int)this.posZ);
    }

    // ========== Get Wander Position ==========
    /** Takes an initial chunk coordinate for a random wander position and ten allows the entity to make changes to the position or react to it. **/
    public ChunkCoordinates getWanderPosition(ChunkCoordinates wanderPosition) {
        return wanderPosition;
    }

    // ========== Restrict Y Height From Ground ==========
    /** Takes an initial chunk coordinate and returns an altered Y position relative to the ground using a minimum and maximum distance. **/
    public int restrictYHeightFromGround(ChunkCoordinates coords, int minY, int maxY) {
        int groundY = this.getGroundY(coords.posX, coords.posY, coords.posZ);
        int airYMax = Math.min(this.getAirY(coords.posX, coords.posY, coords.posZ), groundY + maxY);
        int airYMin = Math.min(airYMax, groundY + minY);
        if(airYMin >= airYMax)
            return airYMin;
        return airYMin + this.getRNG().nextInt(airYMax - airYMin);
    }

    // ========== Get Ground Y Position ==========
    /** Returns the Y position of the ground from the starting X, Y, Z position, this will work for getting the ground of caves or indoor areas too.
     * The Y position returned will be the last air block found before the ground it hit and will thus not be the ground block Y position itself but the air above it. **/
    public int getGroundY(int x, int y, int z) {
        if(y <= 0)
            return 0;
        Block startBlock = this.worldObj.getBlock(x, y, z);
        if(startBlock == null || startBlock.isAir(this.worldObj, x, y, z)) {
            for(int possibleGroundY = Math.max(0, y - 1); possibleGroundY >= 0; possibleGroundY--) {
                Block possibleGroundBlock = this.worldObj.getBlock(x, possibleGroundY, z);
                if(possibleGroundBlock == null || possibleGroundBlock.isAir(this.worldObj, x, possibleGroundY, z))
                    y = possibleGroundY;
                else
                    break;
            }
        }
        return y;
    }

    // ========== Get Air Y Position ==========
    /** Returns the Y position of the highest air block from the starting x, y, z position until either a solid block is hit or the sky is accessible. **/
    public int getAirY(int x, int y, int z) {
        int yMax = this.worldObj.provider.getActualHeight() - 1;
        if(y >= yMax)
            return yMax;
        if(this.worldObj.canBlockSeeTheSky(x, y, z))
            return yMax;

        Block startBlock = this.worldObj.getBlock(x, y, z);
        if(startBlock == null || startBlock.isAir(this.worldObj, x, y, z)) {
            for(int possibleAirY = Math.min(yMax, y + 1); possibleAirY <= yMax; possibleAirY++) {
                Block possibleGroundBlock = this.worldObj.getBlock(x, possibleAirY, z);
                if(possibleGroundBlock == null || possibleGroundBlock.isAir(this.worldObj, x, possibleAirY, z))
                    y = possibleAirY;
                else
                    break;
            }
        }
        return y;
    }

    // ========== Get Water Surface Y Position ==========
    /** Returns the Y position of the water surface (the first air block found when searching up in water).
     * If the water is covered by a solid block, the highest Y water position will be returned instead.
     * This will search up to 24 blocks up. **/
    public int getWaterSurfaceY(int x, int y, int z) {
        if(y <= 0)
            return 0;
        int yMax = this.worldObj.provider.getActualHeight() - 1;
        if(y >= yMax)
            return yMax;
        int yLimit = 24;
        yMax = Math.min(yMax, y + yLimit);
        Block startBlock = this.worldObj.getBlock(x, y, z);
        if(startBlock != null && startBlock.getMaterial() == Material.water) {
            int possibleSurfaceY = y;
            for(possibleSurfaceY += 1; possibleSurfaceY <= yMax; possibleSurfaceY++) {
                Block possibleSurfaceBlock = this.worldObj.getBlock(x, possibleSurfaceY, z);
                if(possibleSurfaceBlock != null && possibleSurfaceBlock.isAir(this.worldObj, x, possibleSurfaceY, z))
                    return possibleSurfaceY;
                else if(possibleSurfaceBlock == null || possibleSurfaceBlock.getMaterial() != Material.water)
                    return possibleSurfaceY - 1;
            }
            return Math.max(possibleSurfaceY - 1, y);
        }
        return y;
    }

	
	// ==================================================
  	//                        Size
  	// ==================================================
    /** Sets the width and height of this mob. This applies sizeScale to the provided arguments. **/
	@Override
	protected void setSize(float width, float height) {
        super.setSize(width * (float)this.sizeScale, height * (float)this.sizeScale);
        this.hitAreas = null;
    }

    /** When called, this reapplies the initial width and height of this mob and then applies sizeScale. **/
	public void updateSize() {
        this.setSize(Math.max(this.setWidth, 0.5F), Math.max(this.setHeight, 0.5F));
    }

    /** Sets the size scale and updates the mobs size. **/
	public void setSizeScale(double scale) {
		this.sizeScale = scale;
        this.updateSize();
    }

    /** Returns the model scale for rendering. **/
    public double getRenderScale() {
        return this.sizeScale;
    }
    
    
    // ==================================================
  	//                      Attacks
  	// ==================================================
    // ========== Can Attack ==========
    /** Returns whether or not this mob is allowed to attack the given target class. **/
	@Override
	public boolean canAttackClass(Class targetClass) {
		if(!MobInfo.mobsAttackVillagers && targetClass == EntityVillager.class)
			return false;
        if(this.isBlocking())
            return false;
		return true;
	}

    /** Returns whether or not this mob is allowed to attack the given target entity. **/
	public boolean canAttackEntity(EntityLivingBase targetEntity) {
		if(!MobInfo.mobsAttackVillagers && targetEntity instanceof EntityVillager)
			return false;
        if(targetEntity instanceof EntityPlayer) {
            EntityPlayer targetPlayer = (EntityPlayer)targetEntity;
            if(targetPlayer.capabilities.isCreativeMode)
                return false;
        }
        if(targetEntity instanceof EntityCreatureBase) {
            if(((EntityCreatureBase)targetEntity).getMasterTarget() == this)
                return false;
            if (!(this instanceof IBossDisplayData)) {
                if (this.getOwner() == null && targetEntity instanceof IBossDisplayData && !((EntityCreatureBase)targetEntity).canAttackEntity(this))
                    return false;
            }
        }
		return true;
	}
	
    // ========== Targets ==========
    /** Gets the attack target of this entity's Master Target Entity. **/
    public EntityLivingBase getMasterAttackTarget() {
    	if(this.masterTarget == null) return null;
    	if(this.masterTarget instanceof EntityLiving)
    		return ((EntityLiving)this.masterTarget).getAttackTarget();
    	return null;
    }

    /** Gets the attack target of this entity's Parent Target Entity. **/
    public EntityLivingBase getParentAttackTarget() {
    	if(this.parentTarget == null) return null;
    	if(this.parentTarget instanceof EntityCreatureBase)
    		return ((EntityCreatureBase)this.parentTarget).getAttackTarget();
    	else if(this.parentTarget instanceof net.minecraft.entity.EntityCreature)
    		return ((net.minecraft.entity.EntityCreature)this.parentTarget).getAttackTarget();
    	return null;
    }

    // ========== Revenge Target ==========
    /**
     * Used when giving this entity a revenge target, usually used when this entity is attacked by another entity.
     * Can be called by anything to tell this entity to attack the given target, however it will still check canAttackClass() and canAttackEntity() first.
     * Mobs with a fleeHealthPercent set above 0 will flee instead if it's health percentage is not above the fleehealthPercent value.
    **/
    @Override
    public void setRevengeTarget(EntityLivingBase entityLivingBase) {
    	boolean aggressiveOverride = false;
    	if(this.extraMobBehaviour != null)
    		aggressiveOverride = this.extraMobBehaviour.aggressiveOverride;
    	if(!aggressiveOverride && this.fleeHealthPercent > 0 && this.getHealth() / this.getMaxHealth() <= this.fleeHealthPercent)
    		this.setAvoidTarget(entityLivingBase);
    	else
    		super.setRevengeTarget(entityLivingBase);
    }
    
    // ========== Melee ==========
    /** Used to make this entity perform a melee attack on the target entity with the given damage scale. **/
    public boolean meleeAttack(Entity target, double damageScale) {
    	boolean success = true;
    	if(this.attackEntityAsMob(target, damageScale)) {
    		
    		// Spread Fire:
        	if(this.spreadFire && this.isBurning() && this.rand.nextFloat() < this.getEffectMultiplier())
        		target.setFire(this.getEffectDuration(4) / 20);
        	
    	}
    	this.setJustAttacked();
    	return success;
    }

    // ========== Ranged ==========
    /** Used to make this entity fire a ranged attack at the target entity, range is also passed which can be used. **/
    public void rangedAttack(Entity target, float range) {
    	this.setJustAttacked();
    }
    
    // ========== Phase ==========
    /** Returns the current attack phase of this mob, used when deciding which attack to use and which animations to use. **/
    public byte getAttackPhase() {
    	return this.dataWatcher.getWatchableObjectByte(WATCHER_ID.ATTACK_PHASE.id);
    }
    /** Sets the current attack phase of this mobs. **/
    public void setAttackPhase(byte setAttackPhase) { attackPhase = setAttackPhase; }
    /** Moves the attack phase to the next step, will loop back to 0 when the max is passed. **/
    public void nextAttackPhase() {
    	if(++attackPhase > (attackPhaseMax - 1))
    		attackPhase = 0;
    }
    
    // ========== Deal Damage ==========
    /** Called when attacking and makes this entity actually deal damage to the target entity. Not used by projectile based attacks. **/
    public boolean attackEntityAsMob(Entity target, double damageScale) {
        if(!this.isEntityAlive())
            return false;
        if(target == null)
            return false;
        if(!this.canEntityBeSeen(target))
            return false;

        float damage = this.getAttackDamage(damageScale);
        int i = 0;
        
        if(target instanceof EntityLivingBase) {
        	damage += EnchantmentHelper.getEnchantmentModifierLiving(this, (EntityLivingBase)target);
            i += EnchantmentHelper.getKnockbackModifier(this, (EntityLivingBase)target);
        }
        
        boolean attackSuccess = false;
        float pierceDamage = 1 + (float)Math.floor(damage / this.getPierceValue());
        if(damage <= pierceDamage)
        	attackSuccess = target.attackEntityFrom(this.getDamageSource(null).setDamageBypassesArmor().setDamageIsAbsolute(), damage);
        else {
        	int hurtResistantTimeBefore = target.hurtResistantTime;
        	target.attackEntityFrom(this.getDamageSource(null).setDamageBypassesArmor().setDamageIsAbsolute(), pierceDamage);
        	target.hurtResistantTime = hurtResistantTimeBefore;
    		damage -= pierceDamage;
        	attackSuccess = target.attackEntityFrom(this.getDamageSource(null), damage);
        }
        
        if(attackSuccess) {
            if(i > 0) {
            	target.addVelocity((double)(-MathHelper.sin(this.rotationYaw * (float)Math.PI / 180.0F) * (float)i * 0.5F), 0.1D, (double)(MathHelper.cos(this.rotationYaw * (float)Math.PI / 180.0F) * (float)i * 0.5F));
                this.motionX *= 0.6D;
                this.motionZ *= 0.6D;
            }
            
            int fireEnchantDuration = EnchantmentHelper.getFireAspectModifier(this);
            if(fireEnchantDuration > 0)
            	target.setFire(fireEnchantDuration * 4);
            
            //if(target instanceof EntityLivingBase)
                //EnchantmentThorns.func_151367_b(this, (EntityLivingBase)target, this.rand);
        }
        
        return attackSuccess;
    }
    
    // ========== Get Attack Damage ==========
    /** Returns how much attack damage this mob does. **/
    public float getAttackDamage(double damageScale) {
    	float damage = (float)this.getEntityAttribute(SharedMonsterAttributes.attackDamage).getAttributeValue();
    	damage += this.getDamageBoost();
        damage *= this.getAttackDamageScale();
        damage *= damageScale;
        return damage;
    }
    
    // ========= Get Attack Damage Scale ==========
    /** Used to scale how much damage this mob does, this is used by getAttackDamage() for melee and should be passed to projectiles for ranged attacks. **/
    public double getAttackDamageScale() {
    	return this.getDamageMultiplier();
    }

    // ========= Get Damage Source ==========
    /**
     * Returns the damage source to be used by this mob when dealing damage.
     * @param nestedDamageSource This can be null or can be a passed damage source for all kinds of use, mainly for minion damage sources. This will override the damage source for EntityBase+Ageable.
     * @return
     */
     public DamageSource getDamageSource(EntityDamageSource nestedDamageSource) {
         if(nestedDamageSource != null)
             return nestedDamageSource;
        return DamageSource.causeMobDamage(this);
    }
    
    
    // ==================================================
   	//                    Taking Damage
   	// ==================================================
    // ========== Attacked From ==========
    /** Called when this entity has been attacked, uses a DamageSource and damage value. **/
    @Override
    public boolean attackEntityFrom(DamageSource damageSrc, float damage) {
    	if(this.worldObj.isRemote) return false;
        if(this.isEntityInvulnerable()) return false;
        if(!this.isDamageTypeApplicable(damageSrc.getDamageType())) return false;
        if(!this.isDamageEntityApplicable(damageSrc.getEntity())) return false;
        
        if(damageSrc.damageType != "mob")
        {
        	//only consider defenses if the mobs aren't fighting each other
        	damage = this.getDamageAfterDefense(damage);
        }
        damage *= this.getDamageModifier(damageSrc);
        if(this.isBoss()) {
            if (!(damageSrc.getEntity() instanceof EntityPlayer))
                damage *= 0.25F;
        }
        
        if(super.attackEntityFrom(damageSrc, damage)) {
        	this.onDamage(damageSrc, damage);
            Entity entity = damageSrc.getSourceOfDamage();
            if(entity instanceof EntityThrowable)
            	entity = ((EntityThrowable)entity).getThrower();
            
            if(entity != null && !(entity instanceof EntityPlayer))
            	damage = (damage + 1.0F) / 2.0F;
            
            if(entity instanceof EntityLivingBase && this.riddenByEntity != entity && this.ridingEntity != entity) {
                if(entity != this)
                    this.setRevengeTarget((EntityLivingBase)entity);
                return true;
            }
            else
                return true;
        }
        return false;
    }

    // ========== Attacked From Part ==========
    /** Called when this entity has been attacked from a specific hit area entity, uses the HitArea Entity, DamageSource and damage value. **/
    public boolean attackEntityFromArea(EntityHitArea entityHitArea, DamageSource damageSrc, float damage) {
        return this.attackEntityFrom(damageSrc, damage);
    }
    
    // ========== Defense ==========
    /** This is provided with how much damage this mob will take and returns the reduced (or sometimes increased) damage with defense applied. Note: Damage Modifiers are applied after this. This also applies the blocking ability. **/
    public float getDamageAfterDefense(float damage) {
    	float baseDefense = (float)(this.defense + this.getDefenseBoost());
    	float scaledDefense = baseDefense * (float)this.getDefenseMultiplier();
    	float minDamage = 0F;
    	if(this.isBlocking()) {
	    	if(scaledDefense <= 0)
	    		scaledDefense = 1;
	    	scaledDefense *= this.getBlockingMultiplier();
    	}
        damage -= scaledDefense;
        if(this.damageMax > 0)
            damage = Math.min(damage, this.damageMax);
    	return Math.max(damage, minDamage);
    }
    
    // ========== On Damage ==========
    /** Called when this mob has received damage. **/
    public void onDamage(DamageSource damageSrc, float damage) {}
    
    // ========== Damage Modifier ==========
    /** A multiplier that alters how much damage this mob receives from the given DamageSource, use for resistances and weaknesses. Note: The defense multiplier is handled before this. **/
    public float getDamageModifier(DamageSource damageSrc) {
    	return 1.0F;
    }
    
    
    // ==================================================
   	//                      Death
   	// ==================================================
    /** Called when this entity dies, drops items from the inventory. **/
    @Override
    public void onDeath(DamageSource damageSource) {
        super.onDeath(damageSource);
        if(!this.dead)
            return;
        if(!this.worldObj.isRemote) {
            if(!this.isBoundPet())
                this.inventory.dropInventory();
            if(damageSource.getEntity() != null) {
                if(damageSource.getEntity() instanceof EntityPlayer)
                    ((EntityPlayer)damageSource.getEntity()).addStat(ObjectManager.getAchievement(this.mobInfo.name + ".kill"), 1);
            }
        }
        if(this.getMasterTarget() != null && this.getMasterTarget() instanceof EntityCreatureBase)
            ((EntityCreatureBase)this.getMasterTarget()).onMinionDeath(this);
    }
    
    
    // ==================================================
  	//                      Targets
  	// ==================================================
    /** Returns true if this mob should attack it's attack targets. Used mostly by attack AIs and update methods. **/
    public boolean isAggressive() {
    	if(this.extraMobBehaviour != null)
    		if(this.extraMobBehaviour.aggressiveOverride)
    			return true;
    	return true;
    }
    
    /** Returns true if this mob should defend other entities that cry for help. Used mainly by the revenge AI. **/
    public boolean isProtective(Entity entity) { return true; }

    /** Returns true if this mob has an Attack Target. **/
    public boolean hasAttackTarget() {
    	if(!this.worldObj.isRemote)
    		return this.getAttackTarget() != null;
    	else
    		return (this.dataWatcher.getWatchableObjectByte(WATCHER_ID.TARGET.id) & TARGET_ID.ATTACK.id) > 0;
    }

    /** Returns this entity's Master Target. **/
    public EntityLivingBase getMasterTarget() { return this.masterTarget; }
    /** Sets this entity's Master Target **/
    public void setMasterTarget(EntityLivingBase setTarget) { this.masterTarget = setTarget; }
    /** Returns true if this mob has a Master Target **/
    public boolean hasMaster() {
    	if(!this.worldObj.isRemote)
    		return this.getMasterTarget() != null;
    	else
    		return (this.dataWatcher.getWatchableObjectByte(WATCHER_ID.TARGET.id) & TARGET_ID.MASTER.id) > 0;
    }

    /** Returns this entity's Parent Target. **/
    public EntityLivingBase getParentTarget() { return this.parentTarget; }
    /** Sets this entity's Parent Target **/
    public void setParentTarget(EntityLivingBase setTarget) { this.parentTarget = setTarget; }
    /** Returns true if this mob has a Parent Target **/
    public boolean hasParent() {
    	if(!this.worldObj.isRemote)
    		return this.getParentTarget() != null;
    	else
    		return (this.dataWatcher.getWatchableObjectByte(WATCHER_ID.TARGET.id) & TARGET_ID.PARENT.id) > 0;
    }

    /** Returns this entity's Avoid Target. **/
    public EntityLivingBase getAvoidTarget() { return this.avoidTarget; }
    /** Sets this entity's Avoid Target **/
    public void setAvoidTarget(EntityLivingBase setTarget) {
    	this.currentFleeTime = this.fleeTime;
    	this.avoidTarget = setTarget;
    }
    /** Returns true if this mob has a Avoid Target **/
    public boolean hasAvoidTarget() {
    	if(!this.worldObj.isRemote)
    		return this.getAvoidTarget() != null;
    	else
    		return (this.dataWatcher.getWatchableObjectByte(WATCHER_ID.TARGET.id) & TARGET_ID.AVOID.id) > 0;
    }

    /** Returns this entity's Owner Target. **/
    public EntityLivingBase getOwner() { return null; }

    /** Returns this entity's Rider Target as an EntityLivingBase or null if it isn't one, see getRiderTarget(). **/
    public EntityLivingBase getRider() {
    	if(this.riddenByEntity instanceof EntityLivingBase)
    		return (EntityLivingBase)this.riddenByEntity;
    	else
    		return null;
    }
    /** Returns this entity's Rider Target as an Entity, use getRider() for it as an EntityLivingBase or null if it is one. **/
    public Entity getRiderTarget() { return this.riddenByEntity; }
    /** Sets this entity's Rider Target **/
    public void setRiderTarget(Entity setTarget) { this.riddenByEntity = setTarget; }
    /** Returns true if this mob has a Rider Target **/
    public boolean hasRiderTarget() {
    	if(!this.worldObj.isRemote)
    		return this.getRiderTarget() != null;
    	else
    		return (this.dataWatcher.getWatchableObjectByte(WATCHER_ID.TARGET.id) & TARGET_ID.RIDER.id) > 0;
    }
    
    // ========== Get Facing Coords ==========
    /** Returns the XYZ coordinate in front or behind this entity (using its rotation angle) with the given distance, use a negative distance for behind. **/
    public double[] getFacingPosition(double distance) {
        return this.getFacingPosition(this, distance, 0D);
    }

    /** Returns the XYZ coordinate in front or behind the provided entity with the given distance and angle offset (in degrees), use a negative distance for behind. **/
    public double[] getFacingPosition(Entity entity, double distance, double angleOffset) {
        /*double angle = Math.toRadians(entity.rotationYaw) + angleOffset;
        double xAmount = -Math.sin(angle);
        double zAmount = Math.cos(angle);
        double[] coords = new double[3];
        coords[0] = entity.posX + (distance * xAmount);
        coords[1] = entity.posY;
        coords[2] = entity.posZ + (distance * zAmount);
        return coords;*/
        return this.getFacingPosition(entity.posX, entity.posY, entity.posZ, distance, Math.toRadians(entity.rotationYaw) + angleOffset);
    }

    /** Returns the XYZ coordinate in front or behind the provided XYZ coords with the given distance and angle (in degrees), use a negative distance for behind. **/
    public double[] getFacingPosition(double x, double y, double z, double distance, double angle) {
    	double xAmount = -Math.sin(angle);
    	double zAmount = Math.cos(angle);
    	double[] coords = new double[3];
        coords[0] = x + (distance * xAmount);
        coords[1] = y;
        coords[2] = z + (distance * zAmount);
        return coords;
    }
    
    
    // ==================================================
  	//                     Abilities
  	// ==================================================
    /** Returns whether or not this mob is hostile towards players, changes if a mob is tamed, etc too. **/
    public boolean isHostile() {
    	return this.isHostileByDefault;
    }
    
    /** Overrides the vanilla method when check for EnumCreatureType.monster, it will return true if this mob is hostile and false if it is not regardless of this creature's actual EnumCreatureType. Takes tameable mobs into account too. **/
    @Override
	public boolean isCreatureType(EnumCreatureType type, boolean forSpawnCount) {
    	// If the mob spawner is checking then we should return if it should take a place in the mob spawn count or not.
    	if(forSpawnCount) {
    		if(this.isMinion()) // Minions shouldn't take up the spawn count.
    			return false;
    		for(EnumCreatureType creatureType : this.mobInfo.spawnInfo.creatureTypes) {
    			if(creatureType == type)
    				return true;
    		}
    		return false;
    	}
    	
		if(type.getCreatureClass() == IMob.class) // If checking for EnumCretureType.monster (IMob) return whether or not this creature is hostile instead.
			return this.isHostile();
        return type.getCreatureClass().isAssignableFrom(this.getClass());
    }
    
    // ========== Movement ==========
    /** Can this entity move currently? **/
    public boolean canMove() { return !this.isBlocking(); }
    /** Can this entity move across land currently? Usually used for swimming mobs to prevent land movement. **/
    public boolean canWalk() { return true; }
    /** Can this entity free swim currently? (This doesn't stop the entity from moving in water but is used for smooth flight-like swimming). **/
    public boolean canSwim() {
    	if(this.extraMobBehaviour != null)
    		if(this.extraMobBehaviour.swimmingOverride)
    			return true;
    	return false;
    }
    /** Can this entity jump currently? **/
    public boolean canJump() { return !this.isBlocking(); }
    /** Can this entity climb currently? **/
    public boolean canClimb() { return false; }
    /** Can this entity fly currently? If true it will use the flight navigator. **/
    public boolean canFly() {
    	if(this.extraMobBehaviour != null)
    		if(this.extraMobBehaviour.flightOverride)
    			return true;
    	return false;
    }
    /** Returns true if this mob is currently flying. **/
    public boolean isCurrentlyFlying() { return this.canFly(); }
    /** Can this entity by tempted (usually lured by an item) currently? **/
    public boolean canBeTempted() { return this.getSubspeciesIndex() < 3; }
    
    /** Called when the creature has eaten. Some special AIs use this such as EntityAIEatBlock. **/
    public void onEat() {}
    
    // ========== Stealth ==========
    /** Can this entity stealth currently? **/
    public boolean canStealth() {
    	if(this.extraMobBehaviour != null)
    		if(this.extraMobBehaviour.stealthOverride)
    			return true;
    	return false;
    }

    /** Get the current stealth percentage, 0.0F = not stealthed, 1.0F = completely stealthed, used for animation such as burrowing crusks. **/
    public float getStealth() {
    	return this.dataWatcher.getWatchableObjectFloat(WATCHER_ID.STEALTH.id);
    }

    /** Sets the current stealth percentage. **/
    public void setStealth(float setStealth) {
    	setStealth = Math.min(setStealth, 1);
    	setStealth = Math.max(setStealth, 0);
    	if(this.worldObj != null && !this.worldObj.isRemote)
    		this.dataWatcher.updateObject(WATCHER_ID.STEALTH.id, setStealth);
    }

    /** Returns true if this mob is fully stealthed (1.0F or above). **/
    public boolean isStealthed() {
    	return this.getStealth() >= 1.0F;
    }

    /** Called when this mob is just started stealthing (reach 1.0F or above). **/
    public void startStealth() {}

    /** Called while this mob is stealthed on the update, can be used to clear enemies targets that are targeting this mob, although a new event listener is in placenow to handle this. The main EventListener also helps handling anti-targeting. **/
    public void onStealth() {
    	if(!this.worldObj.isRemote) {
    		if(this.getAttackTarget() != null && this.getAttackTarget() instanceof EntityLiving)
    			if(((EntityLiving) this.getAttackTarget()).getAttackTarget() != null)
    				((EntityLiving)this.getAttackTarget()).setAttackTarget(null);
    	}
    }
    
    // ========== Climbing ==========
    /** Returns true if this entity is climbing a ladder or wall, can be used for animation. **/
    @Override
    public boolean isOnLadder() {
    	if(this.useFlightNavigator() || this.canSwim()) return false;
    	if(this.canClimb())
    		return (this.dataWatcher.getWatchableObjectByte(WATCHER_ID.CLIMBING.id) & 1) != 0;
    	else
    		return super.isOnLadder();
    }
    
    /** Used to set whether this mob is climbing up a block or not. **/
    public void setBesideClimbableBlock(boolean collided) {
    	if(this.canClimb()) {
	        byte climbing = this.dataWatcher.getWatchableObjectByte(WATCHER_ID.CLIMBING.id);
	        if(collided) climbing = (byte)(climbing | 1);
	        else climbing &= -2;
	        this.dataWatcher.updateObject(WATCHER_ID.CLIMBING.id, Byte.valueOf(climbing));
    	}
    }

    /** Returns whether or not this mob is next to a climbable blocks or not. **/
    public boolean isBesideClimbableBlock() {
        return (this.dataWatcher.getWatchableObjectByte(WATCHER_ID.CLIMBING.id) & 1) != 0;
    }
    
    // ========== Falling ==========
    /** 
     * Called when the mob has hit the ground after falling, fallDistance is how far it fell and can be translated into fall damage.
     * getFallResistance() is used to reduce falling damage, if it is at or above 100 no falling damage is taken at all.
     * **/
    @Override
    protected void fall(float fallDistance) {
    	if(this.useFlightNavigator())
    		return;
    	fallDistance -= this.getFallResistance();
    	if(this.getFallResistance() >= 100)
    		fallDistance = 0;
    	super.fall(fallDistance);
    }
    
    /** Called when this mob is falling, fallDistance is how far the mob has fell so far and onGround is true when it has hit the ground. **/
    @Override
    protected void updateFallState(double fallDistance, boolean onGround) {
    	if(!this.useFlightNavigator()) super.updateFallState(fallDistance, onGround);
    }
    
    // ========== Blocking ==========
    /** When called, this will set the mob as blocking, can be overriden to randomize the blocking duration. **/
    public void setBlocking() {
    	this.currentBlockingTime = this.blockingTime;
    }
    
    /** Returns true if this mob is blocking. **/
    public boolean isBlocking() {
    	if(this.worldObj.isRemote)
    		return (this.dataWatcher.getWatchableObjectByte(WATCHER_ID.ANIMATION.id) & ANIM_ID.BLOCKING.id) > 0;
    	return this.currentBlockingTime > 0;
    }
    
    /** Returns the blocking defense multiplier, when blocking this mobs defense is multiplied by this, also if this mobs defense is below 1 it will be moved up to one. **/
    public int getBlockingMultiplier() {
    	return 4;
    }
    
    // ========== Pickup ==========
    public boolean canPickupEntity(Entity entity) {
        if(this.getPickupEntity() == entity)
            return false;
    	ExtendedEntity extendedEntity = ExtendedEntity.getForEntity(entity);
		if(extendedEntity == null)
			return false;
		if((entity.ridingEntity != null && !(entity.ridingEntity instanceof EntityBoat) && !(entity.ridingEntity instanceof EntityMinecart)) || entity.riddenByEntity != null)
			return false;
        if(entity instanceof EntityLivingBase && ObjectManager.getPotionEffect("weight") != null)
            if(((EntityLivingBase)entity).isPotionActive(ObjectManager.getPotionEffect("weight")))
                return false;
		return extendedEntity.pickedUpByEntity == null || extendedEntity.pickedUpByEntity instanceof EntityFear;
    }
    
    public void pickupEntity(Entity entity) {
    	ExtendedEntity extendedEntity = ExtendedEntity.getForEntity(entity);
		if(extendedEntity != null)
			extendedEntity.setPickedUpByEntity(this);
    	this.pickupEntity = entity;
    }
    
    public Entity getPickupEntity() {
    	return this.pickupEntity;
    }
    
    public boolean hasPickupEntity() {
    	return this.getPickupEntity() != null;
    }
    
    public void dropPickupEntity() {
    	ExtendedEntity extendedEntity = ExtendedEntity.getForEntity(this.getPickupEntity());
		if(extendedEntity != null)
			extendedEntity.setPickedUpByEntity(null);
    	this.pickupEntity = null;
    }
    
    public double[] getPickupOffset(Entity entity) {
    	return new double[]{0, 0, 0};
    }
    
    // ========== Destroy Blocks ==========
    public void destroyArea(int x, int y, int z, float strength, boolean drop) {
    	destroyArea(x, y, z, strength, drop, 0);
    }
    public void destroyArea(int x, int y, int z, float strength, boolean drop, int range) {
    	for(int w = -((int)Math.ceil(this.width) + range); w <= (Math.ceil(this.width) + range); w++)
        	for(int d = -((int)Math.ceil(this.width) + range); d <= (Math.ceil(this.width) + range); d++)
		    	for(int h = 0; h <= Math.ceil(this.height); h++) {
		    		Block block = this.worldObj.getBlock(x + w, y + h, z + d);
		    		if(block instanceof Block && block != null) {
			    		float hardness = block.getBlockHardness(this.worldObj, x + w, y + h, z + d);
			    		Material material = block.getMaterial();
			    		if(hardness >= 0 && strength >= hardness && strength >= block.getExplosionResistance(this) && material != Material.water && material != Material.lava)
			    			this.worldObj.func_147480_a(x + w, y + h, z + d, drop); // destroyBlock()
		    		}
		    	}
    }
    
    // ========== Extra Animations ==========
    /** An additional animation boolean that is passed to all clients through the animation mask. **/
    public boolean extraAnimation01() { return this.extraAnimation01; }
    
    
    // ==================================================
   	//                      Drops
   	// ==================================================
    // ========== Item ID ==========
    /** Gets the item ID of what this mob mostly drops. This is provided for compatibility but is not used by the DropRate code. **/
    @Override
    protected Item getDropItem() {
        if(this.drops != null && this.drops.get(0) != null && !this.isMinion() && !this.isBoundPet())
        	return this.drops.get(0).item.getItem();
        else
        	return null;
    }
    
    // ========== Drop Items ==========
    /** Cycles through all of this entity's DropRates and drops random loot, usually called on death. If this mob is a minion, this method is cancelled. **/
    @Override
    protected void dropFewItems(boolean playerKill, int lootLevel) {
    	if(this.worldObj.isRemote || this.isMinion() || this.isBoundPet()) return;
    	int subspeciesScale = 1;
    	if(this.getSubspeciesIndex() > 2)
    		subspeciesScale = Subspecies.rareDropScale;
    	else if(this.getSubspeciesIndex() > 0)
    		subspeciesScale = Subspecies.uncommonDropScale;

    	for(DropRate dropRate : this.drops) {
            if(dropRate.subspeciesID >= 0 && dropRate.subspeciesID != this.getSubspeciesIndex())
                continue;
    		int quantity = dropRate.getQuantity(this.rand, lootLevel);
            if(dropRate.subspeciesID < 0)
                quantity *= subspeciesScale;
    		if(this.extraMobBehaviour != null && this.extraMobBehaviour.itemDropMultiplierOverride != 1)
    			quantity = Math.round((float)quantity * (float)this.extraMobBehaviour.itemDropMultiplierOverride);
    		ItemStack dropStack = null;
    		if(quantity > 0)
    			dropStack = dropRate.getItemStack(this, quantity);
    		if(dropStack != null)
    			this.dropItem(dropStack);
    	}
    }
    
    // ========== Rare Drop ==========
    /** Called when doing a rare drop, this is part of the vanilla code and is not used, instead the custom DropRate code is used in dropFewItems(). **/
    @Override
    protected void dropRareDrop(int par1) {
    	if(this.isMinion() || this.isBoundPet()) return;
    	super.dropRareDrop(par1);
    }
    
    // ========== Drop Item ==========
    /** Tells this entity to drop the specified itemStack, used by DropRate and InventoryCreature, can be used by anything though. **/
    public void dropItem(ItemStack itemStack) {
    	this.entityDropItem(itemStack, 0.0F);
    }

    // ========== Entity Drop Item ==========
    /** The vanilla item drop method, overriden to make use of the EntityItemCustom class. I recommend using dropItem() instead. **/
    @Override
    public EntityItem entityDropItem(ItemStack itemStack, float heightOffset) {
        if(itemStack.stackSize != 0 && itemStack.getItem() != null) {
            EntityItemCustom entityitem = new EntityItemCustom(this.worldObj, this.posX, this.posY + (double)heightOffset, this.posZ, itemStack);
            entityitem.delayBeforeCanPickup = 10;
            this.applyDropEffects(entityitem);
            
            if(captureDrops) {
                capturedDrops.add(entityitem);
            }
            else {
                this.worldObj.spawnEntityInWorld(entityitem);
            }
            return entityitem;
        }
        else {
            return null;
        }
    }
    
    // ========== Apply Drop Effects ==========
    /** Used to add effects or alter the dropped entity item. **/
    public void applyDropEffects(EntityItemCustom entityitem) {}
    
    
    // ==================================================
    //                     Interact
    // ==================================================
    // ========== GUI ==========
    /** This adds the provided PlayerEntity to the guiViewers array list, where on the next GUI refresh it will open the GUI. **/
    public void openGUI(EntityPlayer player) {
    	if(this.worldObj.isRemote)
    		return;
    	this.addGUIViewer(player);
    	this.refreshGUIViewers();
    	this.openGUIToPlayer(player);
    }
    
    /** This adds the provided PlayerEntity to the guiViewers array list, where on the next GUI refresh it will open the GUI. **/
    public void addGUIViewer(EntityPlayer player) {
    	if(!this.worldObj.isRemote)
    		this.guiViewers.add(player);
    }
    
    /** This removes the provided PlayerEntity from the guiViewers array list. **/
    public void removeGUIViewer(EntityPlayer player) {
    	if(!this.worldObj.isRemote)
    		this.guiViewers.remove(player);
    }
    
    /** Called when all players viewing their entity's gui need to be refreshed. Usually after a GUI command on inventory change. Should be called using scheduleGUIRefresh(). **/
    public void refreshGUIViewers() {
    	if(this.worldObj.isRemote)
    		return;
    	if(this.guiViewers.size() > 0) {
        	for(EntityPlayer player : this.guiViewers.toArray(new EntityPlayer[this.guiViewers.size()])) {
        		if(player.openContainer != null && player.openContainer instanceof ContainerCreature) {
        			if(((ContainerCreature)player.openContainer).creature == this)
        				this.openGUIToPlayer(player);
        			else
        				this.removeGUIViewer(player);
        		}
        	}
    	}
    }
    
    /** Actually opens the GUI to the player, should be used by openGUI() for an initial opening and then by refreshGUIViewers() for constant updates. **/
    public void openGUIToPlayer(EntityPlayer player) {
    	if(player != null)
    		player.openGui(LycanitesMobs.instance, GuiHandler.GuiType.ENTITY.id, this.worldObj, this.getEntityId(), 0, 0);
    }
    
    /** Schedules a GUI refresh, normally takes 2 ticks for everything to update for display. **/
    public void scheduleGUIRefresh() {
    	this.guiRefreshTick = this.guiRefreshTime + 1;
    }

    /** The main interact method that is called when a player right clicks this entity. **/
    @Override
    public boolean interact(EntityPlayer player) {
	    ItemStack itemStack = player.inventory.getCurrentItem();
	    if(assessInteractCommand(getInteractCommands(player, itemStack), player, itemStack))
	    	return true;
	    return super.interact(player);
    }

    // ========== Assess Interact Command ==========
    /** Performs the best possible command and returns true or false if there isn't one. **/
    public boolean assessInteractCommand(HashMap<Integer, String> commands, EntityPlayer player, ItemStack itemStack) {
    	if(commands.isEmpty())
    		return false;
    	int priority = 100;
    	for(int testPriority : commands.keySet())
    		if(testPriority < priority)
    			priority = testPriority;
    	if(!commands.containsKey(priority))
    		return false;
    	performCommand(commands.get(priority), player, itemStack);
    	return true;
    }
    
    // ========== Get Interact Commands ==========
    /** Gets a map of all possible interact events with the key being the priority, lower is better. **/
    public HashMap<Integer, String> getInteractCommands(EntityPlayer player, ItemStack itemStack) {
    	HashMap<Integer, String> commands = new HashMap<Integer, String>();
    	
    	// Item Commands:
    	if(itemStack != null) {
    		// Leash:
    		if(itemStack.getItem() == Items.lead && this.canLeash(player))
    			commands.put(CMD_PRIOR.ITEM_USE.id, "Leash");
    		
    		// Name Tag:
    		if(itemStack.getItem() == Items.name_tag) {
    			if(this.canNameTag(player))
    				return new HashMap<Integer, String>(); // Cancels all commands so that vanilla can take care of name tagging.
    			else
    				commands.put(CMD_PRIOR.ITEM_USE.id, "Name Tag"); // Calls nothing and therefore cancels name tagging.
    		}
    		
    		// Coloring:
    		if(this.canBeColored(player) && itemStack.getItem() == Items.dye)
    			commands.put(CMD_PRIOR.ITEM_USE.id, "Color");
    				
    	}
    	
    	return commands;
    }
    
    // ========== Perform Command ==========
    /** Performs the given interact command. Could be used outside of the interact method if needed. **/
    public void performCommand(String command, EntityPlayer player, ItemStack itemStack) {
    	
    	// Leash:
    	if("Leash".equals(command)) {
    		this.setLeashedToEntity(player, true);
    		this.consumePlayersItem(player, itemStack);
    	}
    	
    	// Name Tag:
    	// Vanilla takes care of this, it is in getInteractCommands so that other commands don't override it.
    	
    	// Color:
    	if("Color".equals(command)) {
    		int colorID = BlockColored.func_150032_b(itemStack.getItemDamage()); // getBlockFromDye()
            if(colorID != this.getColor()) {
                this.setColor(colorID);
        		this.consumePlayersItem(player, itemStack);
            }
    	}
    }
    
    // ========== Can Name Tag ==========
    /** Returns true if this mob can be given a new name with a name tag by the provided player entity. **/
    public boolean canNameTag(EntityPlayer player) {
    	return true;
    }
    
    // ========== Get Render Name Tag ==========
    /** Gets whether this mob should always display its nametag client side. **/
    @SideOnly(Side.CLIENT)
    public boolean getAlwaysRenderNameTagForRender() {
        if(this.renderSubspeciesNameTag() && this.getSubspecies() != null)
    		return MobInfo.subspeciesTags;
        return super.getAlwaysRenderNameTagForRender();
    }
    
    // ========== Render Subspecies Name Tag ==========
    /** Gets whether this mob should always display its nametag if it's a subspecies. **/
    public boolean renderSubspeciesNameTag() {
    	return true;
    }
    
    // ========== Consume Player's Item ==========
    /** Consumes 1 item from the the item stack currently held by the specified player. **/
    public void consumePlayersItem(EntityPlayer player, ItemStack itemStack) {
    	consumePlayersItem(player, itemStack, 1);
    }
    /** Consumes the specified amount from the item stack currently held by the specified player. **/
    public void consumePlayersItem(EntityPlayer player, ItemStack itemStack, int amount) {
    	if(!player.capabilities.isCreativeMode)
            itemStack.stackSize -= amount;
        if(itemStack.stackSize <= 0)
        	player.inventory.setInventorySlotContents(player.inventory.currentItem, (ItemStack)null);
    }

    // ========== Replace Player's Item ==========
    /** Replaces 1 of the specified itemstack with a new itemstack. **/
    public void replacePlayersItem(EntityPlayer player, ItemStack itemStack, ItemStack newStack) {
    	replacePlayersItem(player, itemStack, 1, newStack);
    }
    /** Replaces the specified itemstack and amount with a new itemstack. **/
    public void replacePlayersItem(EntityPlayer player, ItemStack itemStack, int amount, ItemStack newStack) {
    	if(!player.capabilities.isCreativeMode)
            itemStack.stackSize -= amount;
    	
        if(itemStack.stackSize <= 0)
    		 player.inventory.setInventorySlotContents(player.inventory.currentItem, newStack);
         
    	 else if(!player.inventory.addItemStackToInventory(newStack))
        	 player.dropPlayerItemWithRandomChoice(newStack, false);
    	
    }
    
    // ========== Perform GUI Command ==========
    public void performGUICommand(EntityPlayer player, byte guiCommandID) {
    	scheduleGUIRefresh();
    }
    
    
    // ==================================================
    //                     Equipment
    // ==================================================
    /** Returns true if this mob is able to carry items. **/
    public boolean canCarryItems() { return getInventorySize() > 0; }
    /** Returns the current size of this mob's inventory. (Some mob inventories can vary in size such as mounts with and without bag items equipped.) **/
    public int getInventorySize() { return this.inventory.getSizeInventory(); }
    /** Returns the maximum possible size of this mob's inventory. (The creature inventory is not actually resized, instead some slots are locked and made unavailalbe.) **/
    public int getInventorySizeMax() { return Math.max(this.getNoBagSize(), this.getBagSize()); }
    /** Returns true if this mob is equipped with a bag item. **/
    public boolean hasBag() {
    	return this.inventory.getEquipmentStack("bag") != null;
    }
    /** Returns the size of this mob's inventory when it doesn't have a bag item equipped. **/
    public int getNoBagSize() {
    	if(this.extraMobBehaviour != null)
    		if(this.extraMobBehaviour.inventorySizeOverride > 0)
    			return this.extraMobBehaviour.inventorySizeOverride;
    	return 0;
    }
    /** Returns the size that this mob's inventory increases by when it is provided with a bag item. (Look at this as the size of the bag item, not the new total creature inventory size.) **/
    public int getBagSize() { return 5; }
    
    /** Returns true if this mob is able to pick items up off the ground. **/
    public boolean canPickupItems() {
    	if(this.extraMobBehaviour != null)
    		if(this.extraMobBehaviour.itemPickupOverride)
    			return true;
    	return false;
    }
    /** Returns how much of the specified item stack this creature's inventory can hold. (Stack size, not empty slots, this allows the creature to merge stacks when picking up.) **/
    public int getSpaceForStack(ItemStack pickupStack) {
    	return this.inventory.getSpaceForStack(pickupStack);
    }

    /** Returns true if the player is allowed to equip this creature with items such as armor or saddles. **/
    public boolean canEquip() {
        return this.mobInfo.isTameable();
    }
    
    // ========== Set Equipment ==========
    // Vanilla Conversion: 0 = Weapon/Item,  1 = Feet -> 4 = Head
    /**
     * A vanilla method for setting this mobs equipment, takes a slot ID and a stack.
     * 0 = Weapons, Tools or the item to hold out (like how vanilla zombies hold dropped items).
     * 1 = Feet, 2 = Legs, 3 = Chest and 4 = Head
     * 100 = Not used by vanilla but will convert to the bag slot for other mods to use.
     **/
    @Override
    public void setCurrentItemOrArmor(int slot, ItemStack itemStack) {
        String type = "item";
    	if(slot == 0) type = "weapon";
    	if(slot == 1) type = "feet";
    	if(slot == 2) type = "legs";
    	if(slot == 3) type = "chest";
    	if(slot == 4) type = "head";
    	if(slot == 100) type = "bag";
    	this.inventory.setEquipmentStack(type, itemStack);
    }

    // ========== Get Equipment ==========
    /**
     * Returns the equipment grade, used mostly for texturing the armor.
     * For instance "gold" is returned if it is wearing gold chest armor.
     * Type is a string that is the equipment slot, it can be: feet, legs, chest or head. All lower case.
    **/
    public String getEquipmentName(String type) {
    	if(this.inventory.getEquipmentGrade(type) != null)
    		return type + this.inventory.getEquipmentGrade(type);
    	return null;
    }
    
    // ========== Get Total Armor Value ==========
    /** Returns the total armor value of this mob. **/
    @Override
    public int getTotalArmorValue() {
    	return this.inventory.getArmorValue();
    }
    
    // ========== Pickup Items ==========
    /** Called on the update if this mob is able to pickup items. Searches for all nearby item entities and picks them up. **/
    public void pickupItems() {
    	 List list = this.worldObj.getEntitiesWithinAABB(EntityItem.class, this.boundingBox.expand(1.0D, 0.0D, 1.0D));
         Iterator iterator = list.iterator();

         while (iterator.hasNext()) {
             EntityItem entityItem = (EntityItem)iterator.next();
             if(!entityItem.isDead && entityItem.getEntityItem() != null) {
            	 ItemStack itemStack = entityItem.getEntityItem();
            	 int space = this.getSpaceForStack(itemStack);
            	 if(space > 0) {
            		 this.onPickupStack(itemStack);
            		 this.doItemPickup(entityItem);
            	 }
             }
         }
    }

    /** Called when this mob picks up an item entity, provides the itemStack it has picked up. **/
    public void onPickupStack(ItemStack itemStack) {}
    
    public void doItemPickup(EntityItem entityItem) {
    	if(!entityItem.isDead && entityItem.getEntityItem() != null) {
    		ItemStack leftoverStack = this.inventory.autoInsertStack(entityItem.getEntityItem());
    		if(leftoverStack != null)
    			entityItem.setEntityItemStack(leftoverStack);
    		else
    			entityItem.setDead();
    	}
    }
    
    
    // ==================================================
  	//                     Immunities
  	// ==================================================
    // ========== Damage ==========
    @Override
    public boolean isEntityInvulnerable() {
        return super.isEntityInvulnerable();
    }

    /** Returns whether or not the given damage type is applicable, if not no damage will be taken. **/
    public boolean isDamageTypeApplicable(String type) {
        if(("inWall".equals(type) || "cactus".equals(type)) && (this.getSubspeciesIndex() >= 3 || this.isBoss()))
            return false;
        return true;
    }

    /** Returns whether or not this entity can be harmed by the specified entity. **/
    public boolean isDamageEntityApplicable(Entity entity) {
        if(this.isBoss()) {
            if(entity == null)
                return false;
            return this.getDistanceToEntity(entity) <= this.bossRange;
        }
        return true;
    }

    /** Returns whether or not the specified potion effect can be applied to this entity. **/
    @Override
    public boolean isPotionApplicable(PotionEffect potionEffect) {
        return super.isPotionApplicable(potionEffect);
    }
    /** Returns whether or not this entity can be set on fire, this will block both the damage and the fire effect, use isDamageTypeApplicable() to block fire but keep the effect. **/
    public boolean canBurn() {
    	if(this.extraMobBehaviour != null)
    		if(this.extraMobBehaviour.fireImmunityOverride)
    			return false;
    	return true;
    }
    /** Returns true if this mob should be damaged by the sun. **/
    public boolean daylightBurns() { return false; }

    /** Returns true if this mob should be damaged by water. **/
    public boolean waterDamage() { return false; }
    
    // ========== Environmental ==========
    /** If true, this mob isn't slowed down by webs. **/
    public boolean webproof() { return false; }
    /** If webproof() is false, this mob will be affected by webbing on the update that this is called. **/
    @Override
    public void setInWeb() { if(!webproof()) super.setInWeb(); }
    
    // Breathing:
    /** If true, this mob wont lose air when underwater. **/
    @Override
    public boolean canBreatheUnderwater() {
    	if(this.extraMobBehaviour != null)
    		if(this.extraMobBehaviour.waterBreathingOverride)
    			return true;
    	return false;
    }
    /** If false, this mob will lose air when above water or lava if isLavaCreature is true. **/
    public boolean canBreatheAboveWater() { return true; }
    /** Sets the current amount of air this mob has. **/
	@Override
	public void setAir(int air) {
		if(air == 300 && !this.canBreatheAboveWater()) return;
    	super.setAir(air);
    }
	
	/** Returns true if this mob is in water. If this mob is a lava creature, this will return true if it is in lava too.
	 * Use waterContact() or lavaContact() to check for damage, speed boosts, etc.
	**/
	@Override
	public boolean isInWater() {
		if(this.isLavaCreature)
			return this.handleLavaMovement();
		else
			return super.isInWater();
	}
    
    /** Returns true if this mob is in water the rain. Uses the vanilla isWet() but takes dripping leaves, etc into account. **/
    public boolean waterContact() {
    	if(this.isWet())
    		return true;
    	if(this.worldObj.isRaining() && !this.isBlockUnderground((int)this.posX, (int)this.posY, (int)this.posZ))
    		return true;
    	return false;
    }
    
    /** Returns true if this mob is in lava. **/
    public boolean lavaContact() {
    	return this.handleLavaMovement();
    }
    
    /** Returns true if the target location has a block that this mob can breathe in (air, water, lava, depending on the creature). **/
    public boolean canBreatheAtLocation(int x, int y, int z) {
    	Block block = this.worldObj.getBlock(x, y, z);
    	if(block == null)
    		return true;
    	if(this.canBreatheAboveWater() && block.getMaterial() == Material.air)
    		return true;
    	if(this.canBreatheUnderwater()) {
	    	if(!this.isLavaCreature && block.getMaterial() == Material.water)
	    		return true;
	    	if(this.isLavaCreature && block.getMaterial() == Material.lava)
	    		return true;
    	}
    	return false;
    }
	
	/** Returns true if the specified xyz coordinate is in water swimmable by this mob. (Checks for lava for lava creatures).
	 * @param x Block x position.
	 * @param y Block y position.
	 * @param z Block z position.
	 * @return True if swimmable.
	 */
	public boolean isSwimmable(int x, int y, int z) {
		Block block = this.worldObj.getBlock(x, y, z);
		if(block == null)
			return false;
		if(this.isLavaCreature && Material.lava.equals(block.getMaterial()))
			return true;
		else if(Material.water.equals(block.getMaterial()))
			return true;
		return false;
	}
    
    /** Returns how many extra blocks this mob can fall for, the default is around 3.0F I think, if this is set to or above 100 then this mob wont receive falling damage at all. **/
    public float getFallResistance() {
    	return 0;
    }
    
    
    // ==================================================
  	//                     Utilities
  	// ==================================================
    // ========== Get Light Type ==========
    /** Returns a light rating for the light level of this mob's current position.
     * Dark enough for spawnsInDarkness: 0 = Dark, 1 = Dim
     * Light enough for spawnsInLight: 2 = Light, 3 = Bright
    **/
    public byte testLightLevel() {
        int i = MathHelper.floor_double(this.posX);
        int j = MathHelper.floor_double(this.boundingBox.minY);
        int k = MathHelper.floor_double(this.posZ);
    	return testLightLevel(i, j, k);
    }

    /** Returns a light rating for the light level the specified XYZ position.
     * Dark enough for spawnsInDarkness: 0 = Dark, 1 = Dim
     * Light enough for spawnsInLight: 2 = Light, 3 = Bright
    **/
    public byte testLightLevel(int x, int y, int z) {
        /*if(this.worldObj.getSavedLightValue(EnumSkyBlock.Sky, i, j, k) > this.rand.nextInt(32))
            return false;*/
        Block spawnBlock = this.worldObj.getBlock(x, y, z);
        if(y < 0)
            return 0;
        if(spawnBlock != null && spawnBlock.getMaterial() == Material.water && SpawnInfo.useSurfaceLightLevel)
            y = this.getWaterSurfaceY(x, y, z);
        else
            y = this.getGroundY(x, y, z);

        int light = this.worldObj.getBlockLightValue(x, y, z);
        if(this.worldObj.isThundering()) {
            int i1 = this.worldObj.skylightSubtracted;
            this.worldObj.skylightSubtracted = 10;
            light = this.worldObj.getBlockLightValue(x, y, z);
            this.worldObj.skylightSubtracted = i1;
        }

        if(light == 0) return 0;
        if(light <= 7) return 1;
        if(light <= 14) return 2;
        return 3;
    }
    
    /** A client and server friendly solution to check if it is daytime or not. **/
    public boolean isDaytime() {
    	if(!this.worldObj.isRemote)
    		return this.worldObj.isDaytime();
    	long time = this.worldObj.getWorldTime();
    	if(time < 12500)
    		return true;
    	if(time >= 12542 && time < 23460)
    		return false;
    	return true;
    }
    
    // Nearby Creature Count:
    /** Returns how many entities of the specified class around within the specified ranged, used mostly for mobs that summon other mobs and other group behaviours. **/
    public int nearbyCreatureCount(Class targetClass, double range) {
    	return this.getNearbyEntities(targetClass, range).size();
    }
    
    // ========== Advanced AI ==========
    /** This should always be true, the old AI system doesn't work with this base class. **/
    @Override
    protected boolean isAIEnabled() { return true; }
    
    // ========== Creature Attribute ==========
    /** Returns this creature's attriute. **/
   	@Override
    public EnumCreatureAttribute getCreatureAttribute() { return this.attribute; }

    // ========== Y Offset ==========
    /** A Y Offset used to position the mob that is riding this mob. **/
   	@Override
    public double getYOffset() { return super.getYOffset() - 0.5D; }
    
   	// ========== Get Nearby Entities ==========
   	/** Get entities that are near this entity. **/
   	public List getNearbyEntities(Class targetClass, double range) {
   		return this.worldObj.getEntitiesWithinAABB(targetClass, this.boundingBox.expand(range, range, range));
   	}
    
    // ==================================================
    //                        NBT
    // ==================================================
   	// ========== Read ===========
    /** Used when loading this mob from a saved chunk. **/
    @Override
    public void readEntityFromNBT(NBTTagCompound nbtTagCompound) {
    	if(nbtTagCompound.hasKey("FirstSpawn")) {
            this.firstSpawn = nbtTagCompound.getBoolean("FirstSpawn");
    	}
    	else {
    		this.firstSpawn = false;
    	}
    	
    	if(nbtTagCompound.hasKey("SpawnEventType")) {
    		this.spawnEventType = nbtTagCompound.getString("SpawnEventType");
    	}
    	
    	if(nbtTagCompound.hasKey("SpawnEventCount")) {
    		this.spawnEventCount = nbtTagCompound.getInteger("SpawnEventCount");
    	}
    	
    	if(nbtTagCompound.hasKey("Stealth")) {
    		this.setStealth(nbtTagCompound.getFloat("Stealth"));
    	}
    	
    	if(nbtTagCompound.hasKey("IsMinion")) {
    		this.setMinion(nbtTagCompound.getBoolean("IsMinion"));
    	}
    	
    	if(nbtTagCompound.hasKey("IsTemporary") && nbtTagCompound.getBoolean("IsTemporary") && nbtTagCompound.hasKey("TemporaryDuration")) {
    		this.setTemporary(nbtTagCompound.getInteger("TemporaryDuration"));
    	}
    	else {
    		this.unsetTemporary();
    	}

        if(nbtTagCompound.hasKey("IsBoundPet")) {
            if(nbtTagCompound.getBoolean("IsBoundPet")) {
                if(!this.hasPetEntry())
                    this.setDead();
            }
        }
    	
    	if(nbtTagCompound.hasKey("ForceNoDespawn")) {
    		this.forceNoDespawn = nbtTagCompound.getBoolean("ForceNoDespawn");
    	}
    	
    	if(nbtTagCompound.hasKey("Color")) {
    		this.setColor(nbtTagCompound.getByte("Color"));
    	}

        if(nbtTagCompound.hasKey("Subspecies")) {
            this.setSubspecies(nbtTagCompound.getByte("Subspecies"), false);
        }
    	
    	if(nbtTagCompound.hasKey("Size")) {
    		this.sizeScale = nbtTagCompound.getDouble("Size");
    		this.updateSize();
    	}
    	
        super.readEntityFromNBT(nbtTagCompound);
        this.inventory.readFromNBT(nbtTagCompound);
        
        if(nbtTagCompound.hasKey("ExtraBehaviour")) {
        	this.extraMobBehaviour.readFromNBT(nbtTagCompound.getCompoundTag("ExtraBehaviour"));
        }
        
        if(nbtTagCompound.hasKey("HomeX") && nbtTagCompound.hasKey("HomeY") && nbtTagCompound.hasKey("HomeZ") && nbtTagCompound.hasKey("HomeDistanceMax")) {
        	this.setHome(nbtTagCompound.getInteger("HomeX"), nbtTagCompound.getInteger("HomeY"), nbtTagCompound.getInteger("HomeZ"), nbtTagCompound.getFloat("HomeDistanceMax"));
        }
    }
    
    // ========== Write ==========
    /** Used when saving this mob to a chunk. **/
    @Override
    public void writeEntityToNBT(NBTTagCompound nbtTagCompound) {
    	nbtTagCompound.setBoolean("FirstSpawn", false);
    	nbtTagCompound.setString("SpawnEventType", this.spawnEventType);
    	nbtTagCompound.setInteger("SpawnEventCount", this.spawnEventCount);
    	
    	nbtTagCompound.setFloat("Stealth", this.getStealth());
    	nbtTagCompound.setBoolean("IsMinion", this.isMinion());
    	nbtTagCompound.setBoolean("IsTemporary", this.isTemporary);
    	nbtTagCompound.setInteger("TemporaryDuration", this.temporaryDuration);
        nbtTagCompound.setBoolean("IsBoundPet", this.isBoundPet());
    	nbtTagCompound.setBoolean("ForceNoDespawn", this.forceNoDespawn);
    	nbtTagCompound.setByte("Color", (byte) this.getColor());
        nbtTagCompound.setByte("Subspecies", (byte) this.getSubspeciesIndex());
    	nbtTagCompound.setDouble("Size", this.sizeScale);
    	
    	if(this.hasHome()) {
    		ChunkCoordinates homePos = this.getHomePosition();
    		nbtTagCompound.setInteger("HomeX", homePos.posX);
    		nbtTagCompound.setInteger("HomeY", homePos.posY);
    		nbtTagCompound.setInteger("HomeZ", homePos.posZ);
    		nbtTagCompound.setFloat("HomeDistanceMax", this.getHomeDistanceMax());
    	}
    	
        super.writeEntityToNBT(nbtTagCompound);
        this.inventory.writeToNBT(nbtTagCompound);
        
        NBTTagCompound extTagCompound = new NBTTagCompound();
        this.extraMobBehaviour.writeToNBT(extTagCompound);
        nbtTagCompound.setTag("ExtraBehaviour", extTagCompound);
    }
    
    
    // ==================================================
  	//                       Client
  	// ==================================================
    // ========== Just Attacked Animation ==========
    /** Returns true if this creature should play it's attack animation. **/
    public boolean justAttacked() { return justAttacked > 0; }
    /** Called when this mob has just attacked, triggers the attack animation. **/
    public void setJustAttacked() { this.justAttacked = this.justAttackedTime; }
    
    
    // ==================================================
    //                       Visuals
    // ==================================================
    /** Returns this creature's main texture. Also checks for for subspecies. **/
    public ResourceLocation getTexture() {
        String textureName = this.getTextureName();
        if(this.getSubspecies() != null) {
            textureName += "_" + this.getSubspecies().name;
        }
    	if(AssetManager.getTexture(textureName) == null)
    		AssetManager.addTexture(textureName, this.group, "textures/entity/" + textureName.toLowerCase() + ".png");
    	return AssetManager.getTexture(textureName);
    }

    /** Returns this creature's equipment texture. **/
    public ResourceLocation getEquipmentTexture(String equipmentName) {
    	equipmentName = equipmentName.toLowerCase();
    	String textureName = this.getTextureName();
    	textureName += "_" + equipmentName;
    	if(!this.canEquip())
    		return this.getTexture();
    	if(AssetManager.getTexture(textureName) == null)
    		AssetManager.addTexture(textureName, this.group, "textures/entity/" + textureName.toLowerCase() + ".png");
    	return AssetManager.getTexture(textureName);
    }

    /** Gets the name of this creature's texture, normally links to it's code name but can be overridden by subspecies and alpha creatures. **/
    public String getTextureName() {
    	return this.mobInfo.name;
    }
    
    
    // ========== Coloring ==========
    /**
     * Returns true if this mob can be dyed different colors. Usually for wool and collars.
     * @param player The player to check for when coloring, this is to stop players from dying other players pets. If provided with null it should return if this creature can be dyed in general.
     * @return True if tis entity can be dyed by the player or if the player is null, if it can be dyed at all (null is passed by the renderer).
     */
    public boolean canBeColored(EntityPlayer player) {
    	return false;
    }
    
    /**
     * Gets the color ID of this mob.
     * @return A color ID that is used by the static RenderCreature.colorTable array.
     */
    public int getColor() {
		if(this.dataWatcher == null) return 0;
        return this.dataWatcher.getWatchableObjectByte(WATCHER_ID.COLOR.id) & 15;
    }
    
    /**
     * Sets the color ID of this mob.
     * @param color The color ID to use (see the static RenderCreature.colorTable array).
     */
    public void setColor(int color) {
    	if(this.worldObj != null && !this.worldObj.isRemote)
    		this.dataWatcher.updateObject(WATCHER_ID.COLOR.id, Byte.valueOf((byte)(color & 15)));
    }


    // ========== Boss Health Bar ==========
    public boolean showBossHealthBar() {
        if(this.forceBossHealthBar)
            return true;
        // Rare subspecies health bar:
        if(this.getSubspeciesIndex() >= 3)
            return Subspecies.rareHealthBars;
        return false;
    }
    
    
    // ==================================================
   	//                       Sounds
   	// ==================================================
    /** Returns the volume of this entity. **/
    @Override
    protected float getSoundVolume() {
        if(this.isBoss())
            return 4.0F;
        if(this.getSubspeciesIndex() >= 3)
            return 2.0F;
        return 1.0F;
    }

    // ========== Idle ==========
    /** Get number of ticks, at least during which the living entity will be silent. **/
    @Override
    public int getTalkInterval() {
        return 80;
    }

    /** Returns the sound to play when this creature is making a random ambient roar, grunt, etc. **/
    @Override
    protected String getLivingSound() { return AssetManager.getSound(this.mobInfo.name + "_say"); }

    // ========== Hurt ==========
    /** Returns the sound to play when this creature is damaged. **/
    @Override
    protected String getHurtSound() { return AssetManager.getSound(this.mobInfo.name + "_hurt"); }

    // ========== Death ==========
    /** Returns the sound to play when this creature dies. **/
    @Override
    protected String getDeathSound() { return AssetManager.getSound(this.mobInfo.name + "_death"); }
     
    // ========== Step ==========
    /** Plays an additional footstep sound that this creature makes when moving on the ground (all mobs use the block's stepping sounds by default). **/
    protected void playStepSound(int x, int y, int z, Block block) {
    	 if(this.useFlightNavigator() || !this.hasStepSound)
             return;
    	 this.playSound(AssetManager.getSound(this.mobInfo.name + "_step"), this.getSoundVolume() / 2, 1.0F / (this.getRNG().nextFloat() * 0.4F + 0.8F));
    }

    /** Vanilla Step Method **/
    @Override
    protected void func_145780_a(int x, int y, int z, Block block) {
        this.playStepSound(x, y, z, block);
    }
     
    // ========== Jump ==========
    /** Plays the jump sound when this creature jumps. **/
    public void playJumpSound() {
    	if(!this.hasJumpSound) return;
    	this.playSound(AssetManager.getSound(this.mobInfo.name + "_jump"), this.getSoundVolume(), 1.0F / (this.getRNG().nextFloat() * 0.4F + 0.8F));
    }
     
    // ========== Fly ==========
    /** Plays a flying sound, usually a wing flap, called randomly when flying. **/
    protected void playFlySound() {
    	if(!this.canFly()) return;
      	this.playSound(AssetManager.getSound(this.mobInfo.name + "_fly"), this.getSoundVolume(), 1.0F / (this.getRNG().nextFloat() * 0.4F + 0.8F));
    }

    // ========== Attack ==========
    /** Plays an attack sound, called once this creature has attacked. note that ranged attacks normally rely on the projectiles playing their launched sound instead. **/
    protected void playAttackSound() {
     	if(!this.hasAttackSound) return;
     	this.playSound(AssetManager.getSound(this.mobInfo.name + "_attack"), this.getSoundVolume(), 1.0F / (this.getRNG().nextFloat() * 0.4F + 0.8F));
    }
    
    // ========== Play Sound ==========
    public void playSound(String soundName, float par2, float par3) {
    	if(soundName == null)
    		return;
    	super.playSound(soundName, par2, par3);
    }
}