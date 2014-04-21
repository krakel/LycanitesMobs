package lycanite.lycanitesmobs.api.info;

import lycanite.lycanitesmobs.ExtendedPlayer;
import lycanite.lycanitesmobs.ObjectManager;
import net.minecraft.nbt.NBTTagCompound;

public class SummonSet {
	public ExtendedPlayer playerExt;
	
	// Summoning Details:
	public String summonType = "";
	public boolean sitting = false;
	public boolean following = true;
	public boolean passive = false;
	public boolean aggressive = false;
	public boolean pvp = true;
	
	// Behaviour Bits:
	/** A list bytes for each behaviour, used when syncing network packets. **/
	public static enum BEHAVIOUR_ID {
		SITTING((byte)1), FOLLOWING((byte)2), PASSIVE((byte)4), STANCE((byte)8), PVP((byte)16);
		public byte id;
		private BEHAVIOUR_ID(byte i) { id = i; }
	}
	
	// ==================================================
    //                   Static Methods
    // ==================================================
	public static boolean isSummonableCreature(String creatureName) {
		return MobInfo.summonableCreatures.contains(creatureName);
	}
	
	
	// ==================================================
    //                    Constructor
    // ==================================================
	public SummonSet(ExtendedPlayer playerExt) {
		this.playerExt = playerExt;
	}
	
	
	// ==================================================
    //                      Behaviour
    // ==================================================
	public void setSummonType(String summonType) {
		if(!this.playerExt.beastiary.hasFullKnowledge(this.summonType) && isSummonableCreature(this.summonType))
			this.summonType = "";
		this.summonType = summonType;
	}
	
	public boolean getSitting() {
		return this.sitting;
	}

	public boolean getFollowing() {
		return this.following;
	}

	public boolean getPassive() {
		return this.passive;
	}

	public boolean getAggressive() {
		return this.aggressive;
	}

	public boolean getPVP() {
		return this.pvp;
	}
	
	
	// ==================================================
    //                        Info
    // ==================================================
	/** Returns true if this summon set has a valid mob to summon and can be used by staves, etc. **/
	public boolean isUseable() {
		if(this.summonType == null || "".equals(this.summonType) || ObjectManager.getMob(this.summonType) == null || !isSummonableCreature(this.summonType))
			return false;
		return true;
	}
	
	/** Returns the class of the creature to summon. **/
	public Class getCreatureClass() {
		return ObjectManager.getMob(this.summonType);
	}
	
	
	// ==================================================
    //                        Sync
    // ==================================================
	public void readFromPacket(String summonType, byte behaviour) {
		if(this.playerExt.beastiary == null)
			return;
		this.setSummonType(summonType);
		this.sitting = (behaviour & BEHAVIOUR_ID.SITTING.id) > 0;
		this.following = (behaviour & BEHAVIOUR_ID.FOLLOWING.id) > 0;
		this.passive = (behaviour & BEHAVIOUR_ID.PASSIVE.id) > 0;
		this.aggressive = (behaviour & BEHAVIOUR_ID.STANCE.id) > 0;
		this.pvp = (behaviour & BEHAVIOUR_ID.PVP.id) > 0;
	}

	public byte getBehaviourBytes() {
		byte behaviour = 0;
		if(this.getSitting()) behaviour += BEHAVIOUR_ID.SITTING.id;
		if(this.getFollowing()) behaviour += BEHAVIOUR_ID.FOLLOWING.id;
		if(this.getPassive()) behaviour += BEHAVIOUR_ID.PASSIVE.id;
		if(this.getAggressive()) behaviour += BEHAVIOUR_ID.STANCE.id;
		if(this.getPVP()) behaviour += BEHAVIOUR_ID.PVP.id;
		return behaviour;
	}
	
	
	// ==================================================
    //                        NBT
    // ==================================================
   	// ========== Read ===========
    /** Reads a list of Creature Knowledge from a player's NBTTag. **/
    public void readFromNBT(NBTTagCompound nbtTagCompound) {
		if(this.playerExt.beastiary == null)
			return;
		
    	if(nbtTagCompound.hasKey("SummonType"))
    		this.setSummonType(nbtTagCompound.getString("SummonType"));
    	
    	if(nbtTagCompound.hasKey("Sitting"))
    		this.sitting = nbtTagCompound.getBoolean("Sitting");
    	
    	if(nbtTagCompound.hasKey("Following"))
    		this.following = nbtTagCompound.getBoolean("Following");
    	
    	if(nbtTagCompound.hasKey("Passive"))
    		this.passive = nbtTagCompound.getBoolean("Passive");
    	
    	if(nbtTagCompound.hasKey("Aggressive"))
    		this.aggressive = nbtTagCompound.getBoolean("Aggressive");
    	
    	if(nbtTagCompound.hasKey("PVP"))
    		this.pvp = nbtTagCompound.getBoolean("PVP");
    }
    
    // ========== Write ==========
    /** Writes a list of Creature Knowledge to a player's NBTTag. **/
    public void writeToNBT(NBTTagCompound nbtTagCompound) {
		if(this.playerExt.beastiary == null)
			return;
		
    	nbtTagCompound.setString("SummonType", this.summonType);
    	
    	nbtTagCompound.setBoolean("Sitting", this.sitting);
    	
    	nbtTagCompound.setBoolean("Following", this.following);
    	
    	nbtTagCompound.setBoolean("Passive", this.passive);
    	
    	nbtTagCompound.setBoolean("Aggressive", this.aggressive);
    	
    	nbtTagCompound.setBoolean("PVP", this.pvp);
    }
}
