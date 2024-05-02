package atavism.agis.objects;

import atavism.agis.behaviors.CombatBehavior;
import atavism.agis.util.ExtendedCombatMessages;
import atavism.server.objects.*;
import atavism.server.plugins.*;
import atavism.server.plugins.WorldManagerClient.TargetedExtensionMessage;
import atavism.server.util.*;
import atavism.server.engine.*;
import atavism.server.math.Point;
import atavism.server.messages.*;
import atavism.agis.behaviors.BaseBehavior;
import atavism.agis.behaviors.CombatPetBehavior;
import atavism.agis.plugins.*;
import atavism.msgsys.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class CombatPet extends Pet implements Serializable, MessageDispatch
{
    public CombatPet() {
    }

    public CombatPet(int mobTemplateID, OID ownerOid, Long duration, int passiveEffect) {
    	super("CombatPet_"+ownerOid);
    	this.mobTemplateID = mobTemplateID;
    	this.ownerOid = ownerOid;
    	this.passiveEffect = passiveEffect;
    	this.despawnTime = duration;
    	summonPet();
    }
    
    /**
     * Spawns a copy of the pet at the owners location.
     */
    public void summonPet() {
    	// Do we need to make sure the template has been registered?
    	if (Log.loggingTrace)log.trace("PET: summon pet hit");
    	if (isSpawned == true) {
    		log.debug("PET: pet is already spawned");
    		boolean wasSpawned = despawnPet();
    		/*if (wasSpawned) {
    			return;
    		}*/
    	}
		Template tmpl = ObjectManagerClient.getTemplate(mobTemplateID, ObjectManagerPlugin.MOB_TEMPLATE);
		String meshName = "";
		LinkedList<String> displays = (LinkedList) tmpl.get(WorldManagerClient.NAMESPACE, "displays");
		if (displays.size() > 0) {
			meshName = displays.get(0);
			log.debug("MOB: got display: " + meshName);
		}
		DisplayContext dc = new DisplayContext(meshName, true);
		dc.addSubmesh(new DisplayContext.Submesh("", ""));
		tmpl.put(WorldManagerClient.NAMESPACE, WorldManagerClient.TEMPL_DISPLAY_CONTEXT, dc);
		tmpl.put(WorldManagerClient.NAMESPACE, "model", meshName);
		tmpl.put(InventoryClient.NAMESPACE, "pet", true);
		ObjectManagerClient.registerTemplate(tmpl);

		// log.debug("PET: summoning pet with props: " + propMapWM);
		MobFactory mobFactory = new MobFactory(mobTemplateID);
		mobFactory.addBehav(new BaseBehavior());
		CombatPetBehavior ncpBehav = new CombatPetBehavior();
		try {
			int movement_speed = (Integer) EnginePlugin.getObjectProperty(ownerOid, WorldManagerClient.NAMESPACE, AgisWorldManagerPlugin.PROP_MOVEMENT_SPEED);
			ncpBehav.setMovementSpeed((int) (movement_speed * 1.2f));
		} catch (Exception e) {
			log.debug("owner no movement speed");
		}
		ncpBehav.setOwnerOid(ownerOid);
		mobFactory.addBehav(ncpBehav);

		//if(Log.loggingDebug)
	//		Log.debug("BEHAV: about to add combat behaviour to mob: " + templateID);
		HashMap<Integer, MobLootTable> lootTables = (HashMap<Integer, MobLootTable>) tmpl.get(InventoryClient.NAMESPACE, "lootTables");

		CombatBehavior cBehav = new CombatBehavior();
		//cBehav.setLootTables(lootTables);
		//cBehav.setCenterLoc(sd.getLoc());
		cBehav.setAggroRange( (int)tmpl.get(Namespace.FACTION, FactionStateInfo.AGGRO_RADIUS));
		//cBehav.setSendLinkedAggro( (boolean)tmpl.get(CombatClient.NAMESPACE, Behavior.LINKED_AGGRO_SEND));
		//cBehav.setLinkedAggroRadius( (int)tmpl.get(CombatClient.NAMESPACE, Behavior.LINKED_AGGRO_RADIUS));
		//cBehav.setReciveLinkedAggro( (boolean)tmpl.get(CombatClient.NAMESPACE, Behavior.LINKED_AGGRO_GET));
		cBehav.setchaseDistance( (int)tmpl.get(CombatClient.NAMESPACE, Behavior.LINKED_CHASING_DISTANCE));

		String tags = (String)tmpl.get(CombatClient.NAMESPACE, "tags");
		cBehav.setTags(tags);
		AgisMobPlugin.SetCombatBehavior(cBehav,tmpl);

		mobFactory.addBehav(cBehav);

		BasicWorldNode bwNode = WorldManagerClient.getWorldNode(ownerOid);
		SpawnData spawnData = new SpawnData();
		spawnData.setTemplateID(mobTemplateID);
		spawnData.setSpawnRadius(1);

		if (Log.loggingTrace)
			log.debug("SummonPet: spawnData:" + spawnData);
		ObjectStub obj = null;
		obj = mobFactory.makeObject(spawnData, bwNode.getInstanceOid(), bwNode.getLoc());
		obj.spawn();
		InterpolatedWorldNode iwNode = obj.getWorldNode();
		if (Log.loggingTrace)
			log.debug("PET: pet " + mobName + " spawned at: " + iwNode.getLoc() + " in instance: " + iwNode.getInstanceOid());
		if (Log.loggingTrace)
			log.debug("PET: owner is at: " + bwNode.getLoc() + " in instance: " + bwNode.getInstanceOid());
		// Update the display - this really needs fixed...

		// tmpl.get(WorldManagerClient.NAMESPACE, "displays")
		String gender = "Male";
        /*(String) tmpl.get(WorldManagerClient.NAMESPACE, "genderOptions");
        log.error("CombatPet: gender "+gender);
    	if (gender.equals("Either")) {
    		Random random = new Random();
    		if (random.nextInt(2) == 0)
    			gender = "Male";
    		else
    			gender = "Female";
    	}*/
		isSpawned = true;
		mobObj = obj.getOid();
		if (Log.loggingTrace)
			log.debug("Pet OID " + mobObj);
		AgisMobPlugin.setDisplay(mobObj, gender);
		boolean activated = activate();
		
		EnginePlugin.setObjectProperty(ownerOid, WorldManagerClient.NAMESPACE, "activePet", this.getOid());
		EnginePlugin.setObjectProperty(ownerOid, WorldManagerClient.NAMESPACE, "aP", mobObj);
		// Copy the owners factions to the pet
		Serializable test = EnginePlugin.getObjectProperty(ownerOid, Namespace.FACTION, FactionStateInfo.FACTION_PROP);
		Integer faction = (Integer) EnginePlugin.getObjectProperty(ownerOid, Namespace.FACTION, FactionStateInfo.FACTION_PROP);
		String tempFaction = (String) EnginePlugin.getObjectProperty(ownerOid, Namespace.FACTION, FactionStateInfo.TEMPORARY_FACTION_PROP);
		EnginePlugin.setObjectProperty(mobObj, Namespace.FACTION, FactionStateInfo.FACTION_PROP, faction);
		EnginePlugin.setObjectProperty(mobObj, Namespace.FACTION, FactionStateInfo.TEMPORARY_FACTION_PROP, tempFaction);
		EnginePlugin.setObjectProperty(mobObj, WorldManagerClient.NAMESPACE, "petOwner", ownerOid);
		EnginePlugin.setObjectProperty(mobObj, MobManagerClient.NAMESPACE, "petOwner", ownerOid);
		EnginePlugin.setObjectProperty(mobObj, WorldManagerClient.NAMESPACE, "pet", true);
		EnginePlugin.setObjectProperty(ownerOid, WorldManagerClient.NAMESPACE, "hasPet", true);
		log.debug("Create CombatPet Set hasPet true ownerOid="+ownerOid+" mobObj="+mobObj);
		AgisMobClient.petCommandUpdate(mobObj, attitude, null);
		AgisMobClient.petCommandUpdate(mobObj, currentCommand, null);
	 
		updatePetCombatStats();
		applyPassiveEffect();
		if (Log.loggingTrace)
			log.debug("CobatPet summonPet Done " + this.despawnTime);
		if (this.despawnTime > 0) {
			if (Log.loggingTrace)
				log.trace("CobatPet summonPet Done");
			DespawnPet despawnPet = new DespawnPet();
			scheduledExecutioner = Engine.getExecutor().schedule(despawnPet, this.despawnTime, TimeUnit.MILLISECONDS);

		}
		log.debug("Run distance check");
		DistancePet distancePet = new DistancePet();
		scheduledDistanceExecutioner = Engine.getExecutor().scheduleAtFixedRate(distancePet, 10, 10, TimeUnit.SECONDS);
		log.debug("Run props updater");
		sendProps sp = new sendProps();
		Engine.getExecutor().schedule(sp, 1, TimeUnit.SECONDS);
	}
    
	class sendProps implements Runnable, Serializable {
		public void run() {
			try {
				Map<String, Serializable> props = new HashMap<String, Serializable>();
				props.put("ext_msg_subtype", "ao.pet_stats");
				int health = 100;
				int health_max = 100;
				try {
					health = (Integer) EnginePlugin.getObjectProperty(mobObj, WorldManagerClient.NAMESPACE, CombatPlugin.HEALTH_STAT);
					health_max = (Integer) EnginePlugin.getObjectProperty(mobObj, WorldManagerClient.NAMESPACE, CombatPlugin.HEALTH_MAX_STAT);
				} catch (Exception e) {
					
				}
				props.put("health", health);
				props.put("health_max", health_max);
				log.debug("send props "+props);
				if (props.size() > 1) {
					TargetedExtensionMessage _msg = new TargetedExtensionMessage(WorldManagerClient.MSG_TYPE_EXTENSION, ownerOid, ownerOid, false, props);
					Engine.getAgent().sendBroadcast(_msg);
				}
			} catch (Exception e) {
				log.error("sendProps Exception "+e.getMessage()+" "+e.getLocalizedMessage());
			}
		}
	}

    /**
     * Applies the passive Effect tied to this pet to the spawned pet mob.
     */
	private void applyPassiveEffect() {
		if (passiveEffect != -1) {
			CombatClient.applyEffect(mobObj, passiveEffect);
		}
	}

	/**
	 * Sends out a message to update the combat stats of the spawned pet mob.
	 */
	private void updatePetCombatStats() {
		AgisMobClient.updatePetStats(ownerOid, mobObj, currentLevel, 20);
	}

	/**
	 * Despawns the pet.
	 */
	public boolean despawnPet() {
		boolean wasSpawned = false;

		if (Log.loggingDebug)
			log.debug("PET: despawn hit with isSpawned: " + isSpawned);
		if (isSpawned) {
			if (scheduledExecutioner != null) {
				if (Log.loggingDebug)
					log.debug("CombatPet despawn Cancel despawn timer mobObj=" + mobObj);
				scheduledExecutioner.cancel(true);
				scheduledExecutioner = null;
			}
			if (scheduledDistanceExecutioner != null) {
				if (Log.loggingDebug)
					log.debug("CombatPet despawn Cancel distance check mobObj=" + mobObj);
				scheduledDistanceExecutioner.cancel(true);
				scheduledDistanceExecutioner = null;
			}
			try {
				// Reset Pet Action
				updateCommand(-3, null);
				if (Log.loggingDebug)
					log.debug("PET: despawning pet: " + mobObj);
				WorldManagerClient.despawn(mobObj);
				if (Log.loggingDebug)
					log.debug("PET: despawned pet: " + mobObj);
			} catch (NoRecipientsException e) {
				log.debug("PET: no recipients found for despawn pet.");
			}
			isSpawned = false;
			boolean deactivated = deactivate();
			EnginePlugin.setObjectProperty(ownerOid, WorldManagerClient.NAMESPACE, "activePet", null);
			EnginePlugin.setObjectProperty(ownerOid, WorldManagerClient.NAMESPACE, "aP", null);
			EnginePlugin.setObjectProperty(ownerOid, WorldManagerClient.NAMESPACE, "hasPet", false);
			if (Log.loggingDebug)
				log.debug("Despawn Pet Set hasPet false ownerOid=" + ownerOid + " mobObj=" + mobObj);
			wasSpawned = true;
		}
		return wasSpawned;
	}

    /**
     * Activates the message subscriptions so this pet object can pick up relevant messages.
     * @return
     */
    public boolean activate() {
    	if (Log.loggingDebug)
    		log.debug("PET: in activate: this " + this);
		// subscribe for some messages
        SubscriptionManager.get().subscribe(this, mobObj, PropertyMessage.MSG_TYPE_PROPERTY, AgisMobClient.MSG_TYPE_PET_TARGET_LOST);
		
        SubscriptionManager.get().subscribe(this, ownerOid, PropertyMessage.MSG_TYPE_PROPERTY, AgisMobClient.MSG_TYPE_SEND_PET_COMMAND,
                LogoutMessage.MSG_TYPE_LOGOUT, WorldManagerClient.MSG_TYPE_DESPAWNED);
		if (Log.loggingDebug)
			log.debug("PET: set up subscription for pet owner: " + ownerOid);
		return true;
    }
    
    /**
     * Deactivates the message subscriptions so this pet object will no longer pick up any messages.
     * @return
     */
    public boolean deactivate() {
		log.debug("PET: deactivating");
		SubscriptionManager.get().unsubscribe(this);
        return true;
    }
    
    /**
     * process network messages
     */
    public void handleMessage(Message msg, int flags) {
    	//log.debug("PET: got message with type: " + msg.getMsgType());
		if (msg instanceof PropertyMessage) {
			PropertyMessage propMsg = (PropertyMessage) msg;
			OID subject = propMsg.getSubject();
			if (subject.equals(mobObj)) {
				handlePetPropertyUpdate(propMsg);
			} else if (subject.equals(ownerOid)) {
				handleOwnerPropertyUpdate(propMsg);
			}
		} else if (msg.getMsgType() == LogoutMessage.MSG_TYPE_LOGOUT) {
			LogoutMessage logoutMsg = (LogoutMessage) msg;
			OID playerOid = logoutMsg.getSubject();
			if (playerOid.equals(ownerOid)) {
				if (Log.loggingDebug)
					log.debug("CombatPet owner:" + ownerOid + " is logout");
				DespawnPet despawnPet = new DespawnPet();
				Engine.getExecutor().schedule(despawnPet, 500, TimeUnit.MILLISECONDS);
			}
		} else if (msg.getMsgType() == WorldManagerClient.MSG_TYPE_DESPAWNED) {
			WorldManagerClient.DespawnedMessage logoutMsg = (WorldManagerClient.DespawnedMessage) msg;
			OID playerOid = logoutMsg.getSubject();
			if (playerOid.equals(ownerOid)) {
				if (Log.loggingDebug)
					log.debug("CombatPet owner:" + ownerOid + " left instance despawn pet");
				DespawnPet despawnPet = new DespawnPet();
				Engine.getExecutor().schedule(despawnPet, 500, TimeUnit.MILLISECONDS);
			}
		} else if (msg.getMsgType() == AgisMobClient.MSG_TYPE_SEND_PET_COMMAND) {
			log.debug("PET: got send pet command message");
			AgisMobClient.sendPetCommandMessage spcMsg = (AgisMobClient.sendPetCommandMessage) msg;
			String command = spcMsg.getCommand();
			OID targetOid = spcMsg.getTargetOid();
			handleCommand(command, targetOid);
		} else if (msg.getMsgType() == AgisMobClient.MSG_TYPE_PET_TARGET_LOST) {
			log.debug("PET: got send pet command message");
			AgisMobClient.petTargetLostMessage spcMsg = (AgisMobClient.petTargetLostMessage) msg;
			handleTargetLost();
		} else {
			log.error("PET: unknown msg: " + msg);
		}
        //return true;
    }
    
    /**
     * Processes commands, which will either update the pets attitude, the current command, or activate
     * an ability that the pet has.
     * @param command
     */
    public void handleCommand(String command, OID targetOid) {
		if (!isSpawned) {
			log.debug("PET: command issued to pet that is not spawned");
			return;
		}
		if (Log.loggingDebug)
			log.debug("PET: issuing pet command: " + command);
		if (command.equals("passive")) {
			updateAttitude(1);
		} else if (command.equals("defensive")) {
			updateAttitude(2);
		} else if (command.equals("aggressive")) {
			updateAttitude(3);
		} else if (command.equals("stay")) {
			updateCommand(-1, targetOid);
		} else if (command.equals("follow")) {
			updateCommand(-2, targetOid);
		} else if (command.equals("attack")) {
			if (targetOid.equals(ownerOid) || targetOid.equals(mobObj)) {
				ExtendedCombatMessages.sendErrorMessage(ownerOid, "Your pet cannot attack that target");
			} else {
				updateCommand(-3, targetOid);
			}
		} else if (command.equals("despawn")) {
			// if ( targetOid.equals(mobObj)) {
			log.debug("Pet: despawn command");
			DespawnPet despawnPet = new DespawnPet();
			Engine.getExecutor().schedule(despawnPet, 200, TimeUnit.MILLISECONDS);
			// }

		}
    }
    
    /**
     * Sets the pet to follow the owner if its current command was set to attack.
     */
    private void handleTargetLost() {
    	if (Log.loggingDebug)
    		log.debug("PET: pet has lost target, checking current command: " + currentCommand);
    	if (currentCommand == -3) {
    		updateCommand(-2, null);
    	}
    }
    
    /**
     * Deals with the different property updates that have occurred for the pet.
     * @param propMsg
     */
    protected void handlePetPropertyUpdate(PropertyMessage propMsg) {
		Boolean dead = (Boolean) propMsg.getProperty(CombatInfo.COMBAT_PROP_DEADSTATE);
		if (dead != null && dead) {
			log.debug("PET: got pet death, despawning");
			DespawnPet despawnPet = new DespawnPet();
			Engine.getExecutor().schedule(despawnPet, 2000, TimeUnit.MILLISECONDS);
		}

		Integer health = (Integer) propMsg.getProperty(CombatPlugin.HEALTH_STAT);
		Integer health_max = null;
		if (propMsg.keySet().contains(CombatPlugin.HEALTH_MAX_STAT))
			health_max = (Integer) propMsg.getProperty(CombatPlugin.HEALTH_MAX_STAT);

		HashMap props = new HashMap();
		props.put("ext_msg_subtype", "ao.pet_stats");
		props.put("oi",mobObj);
		props.put("oii",getOid());
			if (health != null) {
			props.put("health", health);

		}
		if (health_max != null) {
			props.put("health_max", health_max);

		}
		if (props.size() > 1) {
			TargetedExtensionMessage _msg = new TargetedExtensionMessage(WorldManagerClient.MSG_TYPE_EXTENSION, ownerOid, ownerOid, false, props);
			Engine.getAgent().sendBroadcast(_msg);
		}

    }
    
    /**
     * Deals with the different property updates that have occurred for the owner of the pet.
     * @param propMsg
     */
    protected void handleOwnerPropertyUpdate(PropertyMessage propMsg) {
    	if (Log.loggingDebug)
    		log.debug("COMBATPET: handleOwnerPropertyUpdate "+propMsg.getPropertyMapRef());
    	Boolean dead = (Boolean)propMsg.getProperty(CombatInfo.COMBAT_PROP_DEADSTATE);
	    if (dead != null && dead) {
	    	log.debug("PET: got owner death, despawning");
	    	//despawnPet();
	    	DespawnPet despawnPet = new DespawnPet();
	    	Engine.getExecutor().schedule(despawnPet, 2000, TimeUnit.MILLISECONDS);
	    	return;
	    }
    }
    
    /**
     * A runnable class that will despawn the spawned pet mob when run.
     * @author Andrew
     *
     */
	class DespawnPet implements Runnable, Serializable {
		public void run() {
			if (Log.loggingDebug)
				log.debug("PET: Timer despawning pet: " + mobObj);
			if (isSpawned) {
				if (scheduledExecutioner != null) {
					if (Log.loggingDebug)
						log.debug("CombatPet Timer despawn Cancel despawn timer mobObj=" + mobObj);
					scheduledExecutioner.cancel(true);
					scheduledExecutioner = null;
				}
				if (scheduledDistanceExecutioner != null) {
					if (Log.loggingDebug)
						log.debug("CombatPet Timer despawn Cancel distance check mobObj=" + mobObj);
					scheduledDistanceExecutioner.cancel(true);
					scheduledDistanceExecutioner = null;
				}
				updateCommand(-3, null);
				try {
					if (Log.loggingDebug)
						log.debug("PET: Timer despawning pet: " + mobObj);
					WorldManagerClient.despawn(mobObj);
					if (Log.loggingDebug)
						log.debug("PET: Timer despawned pet: " + mobObj);
				} catch (NoRecipientsException e) {
					log.debug("PET: Timer no recipients found for despawn pet.");
				}
				isSpawned = false;
				boolean deactivated = deactivate();
				OID activePetOid = (OID) EnginePlugin.getObjectProperty(ownerOid, WorldManagerClient.NAMESPACE, "activePet");
				if (!mobObj.equals(activePetOid))
					log.error("PET: Timer despawning mobObj:" + mobObj + " is not equals activePetOid:" + activePetOid);
				EnginePlugin.setObjectProperty(ownerOid, WorldManagerClient.NAMESPACE, "activePet", null);
				EnginePlugin.setObjectProperty(ownerOid, WorldManagerClient.NAMESPACE, "aP", null);
				EnginePlugin.setObjectProperty(ownerOid, WorldManagerClient.NAMESPACE, "hasPet", false);
				if (Log.loggingDebug)
					log.debug("Timer Despawn Pet run Set hasPet false ownerOid=" + ownerOid + " mobObj=" + mobObj);
			}
		}

		private static final long serialVersionUID = 1L;
	}

    
	class DistancePet implements Runnable, Serializable {
		public void run() {
			if (Log.loggingDebug)
				log.debug("PET: Timer check pet:" + mobObj + " distance from owner:" + ownerOid);
			BasicWorldNode bwNodeOwner = WorldManagerClient.getWorldNode(ownerOid);
			BasicWorldNode bwNodePet = WorldManagerClient.getWorldNode(mobObj);
			Point owPoint = bwNodeOwner.getLoc();
			Point petPoint = bwNodePet.getLoc();
			float dist = Point.distanceTo(owPoint, petPoint);
			if (Log.loggingDebug)
				log.debug("PET: Timer check pet:" + mobObj + " distance from owner:" + ownerOid + " distance:" + dist + " PET_DISTANCE_DESPAWN=" + CombatPlugin.PET_DISTANCE_DESPAWN);
			if (dist > CombatPlugin.PET_DISTANCE_DESPAWN) {
				if (scheduledDistanceExecutioner != null)
					scheduledDistanceExecutioner.cancel(true);
				updateCommand(-3, null);
				try {
					if (Log.loggingDebug)
						log.debug("PET: Timer distance try despawning pet: " + mobObj);
					WorldManagerClient.despawn(mobObj);
					if (Log.loggingDebug)
						log.debug("PET: Timer distance despawned pet: " + mobObj);
				} catch (NoRecipientsException e) {
					log.debug("PET: no recipients found for distance despawn pet.");
				}
				boolean deactivated = deactivate();
				OID activePetOid = (OID) EnginePlugin.getObjectProperty(ownerOid, WorldManagerClient.NAMESPACE, "activePet");
				if (!mobObj.equals(activePetOid)) {
					if (Log.loggingDebug)
						log.debug("PET: Timer distance mobObj:" + mobObj + " is not equals activePetOid:" + activePetOid);
				}
				EnginePlugin.setObjectProperty(ownerOid, WorldManagerClient.NAMESPACE, "activePet", null);
				EnginePlugin.setObjectProperty(ownerOid, WorldManagerClient.NAMESPACE, "aP", null);
				EnginePlugin.setObjectProperty(ownerOid, WorldManagerClient.NAMESPACE, "hasPet", false);
				if (Log.loggingDebug)
					log.debug("Disctance Pet Set hasPet false ownerOid=" + ownerOid + " mobObj=" + mobObj);
				isSpawned = false;
			}
		}

		private static final long serialVersionUID = 1L;
	}
    
    static final Logger log = new Logger("CombatPet");

    /**
     * Sends out a command update message so the behavior for this pet will act in the specified manner.
     * @param attitude
     */
    public void updateAttitude(int attitude) {
    	this.attitude = attitude;
    	AgisMobClient.petCommandUpdate(mobObj, attitude, null);
    }
    /**
     * Sends out a command update message so the behavior for this pet will perform the requested command.
     * @param attitude
     */
    public void updateCommand(int command, OID target) {
    	this.currentCommand = command;
    	AgisMobClient.petCommandUpdate(mobObj, currentCommand, target);
    }
    
	public String getMobName() {
		return mobName;
	}

	public void setMobName(String mobName) {
		this.mobName = mobName;
	}

	public OID getMobObj() {
		return mobObj;
	}

	public void setMobObj(OID mobObj) {
		this.mobObj = mobObj;
	}

	public boolean getSpawned() {
		return isSpawned;
	}

	public void setSpawned(boolean isSpawned) {
		this.isSpawned = isSpawned;
	}

	public OID getOwnerOid() {
		return ownerOid;
	}

	public void setOwnerOid(OID ownerOid) {
		this.ownerOid = ownerOid;
	}

	public int getAttitude() {
		return attitude;
	}

	public void setAttitude(int attitude) {
		this.attitude = attitude;
	}

	public int getCurrentCommand() {
		return currentCommand;
	}

	public void setCurrentCommand(int currentCommand) {
		this.currentCommand = currentCommand;
	}

	public int getCurrentLevel() {
		return currentLevel;
	}

	public void setCurrentLevel(int currentLevel) {
		this.currentLevel = currentLevel;
	}

	public Long getDespawnTime() {
		return despawnTime;
	}

	public void setDespawnTime(Long despawnTime) {
		this.despawnTime = despawnTime;
	}

	public int getPassiveEffect() {
		return passiveEffect;
	}

	public void setPassiveEffect(int passiveEffect) {
		this.passiveEffect = passiveEffect;
	}

    private String mobName;
    //private Template mobTemplate = null;
    private OID mobObj;
    private boolean isSpawned;
    private OID ownerOid;
    private int attitude = 2; // 1: Passive, 2: Defensive, 3: Aggressive
    private int currentCommand = -2; // -1: Stay, -2: Follow, -3: Attack
    private int currentLevel = 1;
    private Long despawnTime;
    private int duration = 0;//
    int passiveEffect = -1;
    protected ScheduledFuture<?> scheduledExecutioner;
    protected ScheduledFuture<?> scheduledDistanceExecutioner;
      private static final long serialVersionUID = 1L;
}
