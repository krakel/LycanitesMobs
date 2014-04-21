package lycanite.lycanitesmobs;

import lycanite.lycanitesmobs.api.entity.EntityCreatureRideable;
import lycanite.lycanitesmobs.api.item.ItemBase;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.potion.Potion;
import net.minecraftforge.event.ForgeSubscribe;
import net.minecraftforge.event.entity.EntityEvent.EntityConstructing;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingSetAttackTargetEvent;
import net.minecraftforge.event.entity.player.EntityInteractEvent;

public class EventListener {
	
    // ==================================================
    //                     Constructor
    // ==================================================
	public EventListener() {}
	
	
	// ==================================================
    //                Entity Constructing
    // ==================================================
	@ForgeSubscribe
	public void onEntityConstructing(EntityConstructing event) {
		// ========== Extended Player ==========
		if(event.entity instanceof EntityPlayer)
			ExtendedPlayer.getForPlayer((EntityPlayer)event.entity);
	}
	
	
	// ==================================================
    //                  Entity Join World
    // ==================================================
	@ForgeSubscribe
	public void onEntityJoinWorld(EntityJoinWorldEvent event) {
		// ========== Extended Player ==========
		if(event.entity instanceof EntityPlayer)
			ExtendedPlayer.getForPlayer((EntityPlayer)event.entity);
	}
	
	
	// ==================================================
    //                 Living Death Event
    // ==================================================
	@ForgeSubscribe
	public void onLivingDeathEvent(LivingDeathEvent event) {
		// ========== Extended Player Data Backup ==========
		if(event.entity instanceof EntityPlayer)
			ExtendedPlayer.getForPlayer((EntityPlayer)event.entity).onDeath();
	}
	
	
	// ==================================================
	//                    Entity Update
	// ==================================================
	@ForgeSubscribe
	public void onEntityUpdate(LivingUpdateEvent event) {
		EntityLivingBase entity = event.entityLiving;
		if(entity == null)
			return;
		
		if(entity instanceof EntityPlayer) {
			EntityPlayer player = (EntityPlayer)entity;
			ExtendedPlayer playerExt = ExtendedPlayer.getForPlayer(player);
			if(playerExt != null)
				playerExt.onUpdate();
		}
	}
	
	
    // ==================================================
    //                Entity Interact Event
    // ==================================================
	@ForgeSubscribe
	public void onEntityInteract(EntityInteractEvent event) {
		EntityPlayer player = event.entityPlayer;
		Entity entity = event.target;
		if(player == null || entity == null)
			return;
		
		if(player.getHeldItem() != null) {
			Item item = player.getHeldItem().getItem();
			if(item instanceof ItemBase)
				if(((ItemBase)item).onItemRightClickOnEntity(player, entity)) {
					if(event.isCancelable())
						event.setCanceled(true);
				}
		}
	}
	
	
    // ==================================================
    //                 Attack Target Event
    // ==================================================
	@ForgeSubscribe
	public void onAttackTarget(LivingSetAttackTargetEvent event) {
		if(event.isCancelable() && event.isCanceled())
	      return;
		
		// Better Invisibility:
		if(event.entityLiving != null) {
			if(event.entityLiving.isPotionActive(Potion.nightVision))
				return;
			if(event.target != null) {
				if(event.target.isInvisible())
					if(event.isCancelable())
						event.setCanceled(true);
			}
		}
	}
	
	
    // ==================================================
    //                 Living Hurt Event
    // ==================================================
	@ForgeSubscribe
	public void onLivingHurt(LivingHurtEvent event) {
		if(event.isCancelable() && event.isCanceled())
	      return;
		
		if(event.entityLiving == null || event.source == null)
			return;
		
		// ========== Mounted Protection ==========
		if(event.entityLiving.ridingEntity != null) {
			if(event.entityLiving.ridingEntity instanceof EntityCreatureRideable) {
				// Prevent Mounted Entities from Suffocating:
				if("inWall".equals(event.source.damageType)) {
					event.setCanceled(true);
					return;
				}
				
				// Copy Mount Immunities to Rider:
				EntityCreatureRideable creatureRideable = (EntityCreatureRideable)event.entityLiving.ridingEntity;
				if(!creatureRideable.isDamageTypeApplicable(event.source.damageType)) {
					event.setCanceled(true);
					return;
				}
			}
		}
	}
}
