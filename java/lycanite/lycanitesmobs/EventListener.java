package lycanite.lycanitesmobs;

import java.util.Calendar;

import lycanite.lycanitesmobs.api.entity.EntityCreatureBase;
import lycanite.lycanitesmobs.api.entity.EntityCreatureRideable;
import lycanite.lycanitesmobs.api.entity.EntityItemCustom;
import lycanite.lycanitesmobs.api.entity.MinionEntityDamageSource;
import lycanite.lycanitesmobs.api.info.ItemInfo;
import lycanite.lycanitesmobs.api.item.ItemBase;
import lycanite.lycanitesmobs.api.item.ItemScepter;
import lycanite.lycanitesmobs.api.item.ItemSwordBase;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IEntityOwnable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.EntityEvent.EntityConstructing;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingSetAttackTargetEvent;
import net.minecraftforge.event.entity.player.EntityInteractEvent;
import net.minecraftforge.event.entity.player.FillBucketEvent;
import net.minecraftforge.event.world.WorldEvent;
import cpw.mods.fml.common.eventhandler.Event.Result;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public class EventListener {
	
    // ==================================================
    //                     Constructor
    // ==================================================
	public EventListener() {}
	
	
    // ==================================================
    //                    World Load
    // ==================================================
	@SubscribeEvent
	public void onWorldLoading(WorldEvent.Load event) {
		if(event.world == null)
			return;
		
		// ========== Extended World ==========
		ExtendedWorld.getForWorld(event.world);
	}
	
	
	// ==================================================
    //                Entity Constructing
    // ==================================================
	@SubscribeEvent
	public void onEntityConstructing(EntityConstructing event) {
		if(event.entity == null)
			return;
        if(event.entity.worldObj == null || !event.entity.worldObj.isRemote)
            return;

        // ========== Force Remove Entity ==========
        if(!(event.entity instanceof EntityLivingBase)) {
            if(ExtendedEntity.FORCE_REMOVE_ENTITY_IDS != null && ExtendedEntity.FORCE_REMOVE_ENTITY_IDS.length > 0) {
                LycanitesMobs.printDebug("ForceRemoveEntity", "Forced entity removal, checking: " + event.entity.getCommandSenderName());
                for(String forceRemoveID : ExtendedEntity.FORCE_REMOVE_ENTITY_IDS) {
                    if(forceRemoveID.equalsIgnoreCase(event.entity.getCommandSenderName())) {
                        event.entity.setDead();
                        break;
                    }
                }
            }
        }
		
		// ========== Extended Entity ==========
		if(event.entity instanceof EntityLivingBase)
			ExtendedEntity.getForEntity(event.entity);
		
		// ========== Extended Player ==========
		if(event.entity instanceof EntityPlayer) {
			EntityPlayer player = (EntityPlayer)event.entity;
			ExtendedPlayer.getForPlayer(player);
		}
	}
	
	
	// ==================================================
    //                  Entity Join World
    // ==================================================
	@SubscribeEvent
	public void onEntityJoinWorld(EntityJoinWorldEvent event) {
		// ========== Extended Player ==========
		if(event.entity instanceof EntityPlayer) {
			EntityPlayer player = (EntityPlayer)event.entity;
			ExtendedPlayer playerExtended = ExtendedPlayer.getForPlayer(player);
			playerExtended.onJoinWorld();
		}
	}
	
	
	// ==================================================
    //                 Living Death Event
    // ==================================================
	@SubscribeEvent
	public void onLivingDeathEvent(LivingDeathEvent event) {
		EntityLivingBase entity = event.entityLiving;
		if(entity == null) return;
		
		// ========== Extended Entity ==========
		ExtendedEntity extendedEntity = ExtendedEntity.getForEntity(entity);
		if(extendedEntity != null)
			extendedEntity.onDeath();
		
		// ========== Extended Player ==========
		if(entity instanceof EntityPlayer) {
			EntityPlayer player = (EntityPlayer)entity;
			ExtendedPlayer.getForPlayer(player).onDeath();
		}

        // ========== Minion Kills ==========
        // TODO: If damage is minion/pet damage set the entity to the minion's owner instead so they are credited for the kill?
	}
	
	
	// ==================================================
	//                    Entity Update
	// ==================================================
	@SubscribeEvent
	public void onEntityUpdate(LivingUpdateEvent event) {
		EntityLivingBase entity = event.entityLiving;
		if(entity == null) return;

		// ========== Extended Entity ==========
		ExtendedEntity extendedEntity = ExtendedEntity.getForEntity(entity);
		if(extendedEntity != null)
			extendedEntity.onUpdate();

		// ========== Extended Player ==========
		if(entity instanceof EntityPlayer) {
			EntityPlayer player = (EntityPlayer)entity;
			
			ExtendedPlayer playerExt = ExtendedPlayer.getForPlayer(player);
			if(playerExt != null)
				playerExt.onUpdate();
		}

        // ========== Item Early Update ==========
        if(event.entityLiving instanceof EntityPlayer) {
            EntityPlayer entityPlayer = (EntityPlayer)event.entityLiving;
            if(entityPlayer.getCurrentEquippedItem() != null) {
                ItemStack equippedItemStack = entityPlayer.getCurrentEquippedItem();
                if(equippedItemStack.getItem() instanceof ItemSwordBase) {
                    ((ItemSwordBase)equippedItemStack.getItem()).onEarlyUpdate(equippedItemStack, event.entityLiving);
                }
            }
        }
	}
	
	
    // ==================================================
    //               Entity Interact Event
    // ==================================================
	@SubscribeEvent
	public void onEntityInteract(EntityInteractEvent event) {
		EntityPlayer player = event.entityPlayer;
		Entity entity = event.target;
		if(player == null || entity == null)
			return;
		
		//record which mob the player clicked
		if (entity instanceof EntityLivingBase)
			LycanitesMobs.debugEntity = (EntityLivingBase)entity;
		
		// Item onItemRightClickOnEntity():
		if(player.getHeldItem() != null) {
			ItemStack itemStack = player.getHeldItem();
			Item item = itemStack.getItem();
			if(item instanceof ItemBase)
				if(((ItemBase)item).onItemRightClickOnEntity(player, entity, itemStack)) {
					if(event.isCancelable())
						event.setCanceled(true);
				}
			if(item instanceof ItemSwordBase)
				if(((ItemSwordBase)item).onItemRightClickOnEntity(player, entity, itemStack)) {
					if(event.isCancelable())
						event.setCanceled(true);
				}
		}
	}
	
    // ==================================================
    //                 Attack Target Event
    // ==================================================
	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onAttackTarget(LivingSetAttackTargetEvent event) {
		// Better Invisibility:
		if(event.entityLiving != null) {
			if(!event.entityLiving.isPotionActive(Potion.nightVision) && event.target != null) {
				if(event.target.isInvisible())
					event.entityLiving.setRevengeTarget(null);
			}
		}
	}
	
	
    // ==================================================
    //                 Living Hurt Event
    // ==================================================
	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onLivingHurt(LivingHurtEvent event) {
		if(event.isCanceled())
	      return;
		
		if(event.source == null || event.entityLiving == null)
			return;

        EntityLivingBase damagedEntity = event.entityLiving;

//        EntityDamageSource entityDamageSource = null;
//        if(event.source instanceof EntityDamageSource)
//            entityDamageSource = (EntityDamageSource)event.source;

//        Entity damagingEntity = null;
//        if(entityDamageSource != null)
//            damagingEntity = entityDamageSource.getSourceOfDamage();

		// ========== Mounted Protection ==========
		if(damagedEntity.ridingEntity != null) {
			if(damagedEntity.ridingEntity instanceof EntityCreatureRideable) {

				// Prevent Mounted Entities from Suffocating:
				if("inWall".equals(event.source.damageType)) {
					event.ammount = 0;
					event.setCanceled(true);
					return;
				}

				// Copy Mount Immunities to Rider:
				EntityCreatureRideable creatureRideable = (EntityCreatureRideable)event.entityLiving.ridingEntity;
				if(!creatureRideable.isDamageTypeApplicable(event.source.damageType)) {
					event.ammount = 0;
					event.setCanceled(true);
					return;
				}
			}
		}
	}
	
	
	// ==================================================
    //                 Living Drops Event
    // ==================================================
	@SubscribeEvent
    public void onLivingDrops(LivingDropsEvent event) {
		World world = event.entityLiving.worldObj;
		
		// Seasonal Items:
        if(ItemInfo.seasonalItemDropChance > 0
            && (Utilities.isHalloween() || Utilities.isYuletide() || Utilities.isNewYear())) {
            boolean noSeaonalDrop = false;
            boolean alwaysDrop = false;
            if(event.entityLiving instanceof EntityCreatureBase) {
                if (((EntityCreatureBase) event.entityLiving).isMinion())
                    noSeaonalDrop = true;
                if (((EntityCreatureBase) event.entityLiving).getSubspecies() != null)
                    alwaysDrop = true;
            }

            Item seasonalItem = null;
            if(Utilities.isHalloween())
                seasonalItem = ObjectManager.getItem("halloweentreat");
            if(Utilities.isYuletide()) {
                seasonalItem = ObjectManager.getItem("wintergift");
                if(Utilities.isYuletideDay() && world.rand.nextBoolean())
                    seasonalItem = ObjectManager.getItem("wintergiftlarge");
            }

            if(seasonalItem != null && !noSeaonalDrop && (alwaysDrop || event.entityLiving.getRNG().nextFloat() < ItemInfo.seasonalItemDropChance)) {
                ItemStack dropStack = new ItemStack(seasonalItem, 1);
                EntityItemCustom entityItem = new EntityItemCustom(world, event.entityLiving.posX, event.entityLiving.posY, event.entityLiving.posZ, dropStack);
                entityItem.delayBeforeCanPickup = 10;
                world.spawnEntityInWorld(entityItem);
            }
        }
	}
	
	
    // ==================================================
    //                 Bucket Fill Event
    // ==================================================
	@SubscribeEvent
    public void onBucketFill(FillBucketEvent event) {
        World world = event.world;
        MovingObjectPosition pos = event.target;
        Block block = world.getBlock(pos.blockX, pos.blockY, pos.blockZ);
        Item bucket = ObjectManager.buckets.get(block);
        ItemStack result = null;
        if(bucket != null && world.getBlockMetadata(pos.blockX, pos.blockY, pos.blockZ) == 0) {
            world.setBlockToAir(pos.blockX, pos.blockY, pos.blockZ);
            result = new ItemStack(bucket);
        }
        
        if(result == null)
        	return;

        event.result = result;
        event.setResult(Result.ALLOW);
    }
}
