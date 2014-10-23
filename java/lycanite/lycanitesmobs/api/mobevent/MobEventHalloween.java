package lycanite.lycanitesmobs.api.mobevent;

import java.util.Calendar;

import lycanite.lycanitesmobs.api.info.GroupInfo;
import net.minecraft.entity.EntityLiving;
import net.minecraft.world.World;

public class MobEventHalloween extends MobEventBase {


    // ==================================================
    //                     Constructor
    // ==================================================
	public MobEventHalloween(String name, GroupInfo group) {
		super(name, group);
	}


    // ==================================================
    //                       Enabled
    // ==================================================
	@Override
    public boolean isEnabled() {
		Calendar calendar = Calendar.getInstance();
		if(calendar.get(Calendar.DAY_OF_MONTH) != 31 || calendar.get(Calendar.MONTH) != calendar.OCTOBER)
			return false;
        return super.isEnabled();
    }


    // ==================================================
    //                       Start
    // ==================================================
	@Override
    public void onStart(World world) {
        super.onStart(world);
		world.getWorldInfo().setRaining(true);
		world.getWorldInfo().setThundering(true);
    }


    // ==================================================
    //                      Finish
    // ==================================================
	@Override
    public void onFinish() {
        super.onFinish();
    }
	
	
    // ==================================================
    //                   Spawn Effects
    // ==================================================
    @Override
	public void onSpawn(EntityLiving entity) {
		super.onSpawn(entity);
	}
}
