package atavism.agis.objects;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import atavism.agis.plugins.AgisWorldManagerClient;
import atavism.agis.plugins.CombatClient;
import atavism.agis.plugins.QuestClient;
import atavism.agis.util.ExtendedCombatMessages;
import atavism.msgsys.Message;
import atavism.msgsys.MessageDispatch;
import atavism.msgsys.SubjectFilter;
import atavism.server.engine.BasicWorldNode;
import atavism.server.engine.Engine;
import atavism.server.engine.EnginePlugin;
import atavism.server.engine.InterpolatedWorldNode;
import atavism.server.engine.Namespace;
import atavism.server.engine.OID;
import atavism.server.math.AOVector;
import atavism.server.math.Point;
import atavism.server.math.Quaternion;
import atavism.server.objects.DisplayContext;
import atavism.server.objects.Entity;
import atavism.server.objects.EntityManager;
import atavism.server.objects.EntityWithWorldNode;
import atavism.server.objects.ObjectStub;
import atavism.server.objects.ObjectTracker;
import atavism.server.objects.ObjectTypes;
import atavism.server.objects.Template;
import atavism.server.plugins.MobManagerPlugin;
import atavism.server.plugins.ObjectManagerClient;
import atavism.server.plugins.WorldManagerClient;
import atavism.server.plugins.WorldManagerClient.TargetedExtensionMessage;
import atavism.server.util.Log;

/**
 * An Interactive Object is an object players can interact with. The Object can have different interaction types, along with
 * requirements to be able to interact with it.
 * @author Andrew Harrison
 *
 */
public class InteractiveObject implements Serializable, MessageDispatch, Runnable {
    public InteractiveObject() {
    }
    
    public InteractiveObject(int id, AOVector loc,  OID instanceOID) {
    	this.id = id;
    	this.loc = loc;
    	this.instanceOID = instanceOID;
    }
    
    /**
     * Subscribes the instance to receive certain relevant messages that are sent to the world object 
     * created by this instance.
     */
    public void activate() {
    	SubjectFilter filter = new SubjectFilter(objectOID);
        filter.addType(ObjectTracker.MSG_TYPE_NOTIFY_REACTION_RADIUS);
        eventSub = Engine.getAgent().createSubscription(filter, this);
        // Set the reaction radius tracker to alert the object if a player has entered its draw radius
        MobManagerPlugin.getTracker(instanceOID).addReactionRadius(objectOID, 100);
        active = true;
        Log.debug("INTERACTIVE: node with oid: " + objectOID + " id:"+id+" instanceOID: "+instanceOID+" activated ");
    }
    
    /**
     * Deals with the messages the instance has picked up.
     */
    public void handleMessage(Message msg, int flags) {
    	if (active == false) {
    	    return;
    	}
    	if (msg.getMsgType() == ObjectTracker.MSG_TYPE_NOTIFY_REACTION_RADIUS) {
    	    ObjectTracker.NotifyReactionRadiusMessage nMsg = (ObjectTracker.NotifyReactionRadiusMessage)msg;
     	    Log.debug("INTERACTIVE: myOid=" + objectOID + " objOid=" + nMsg.getSubject()
     		      + " inRadius=" + nMsg.getInRadius() + " wasInRadius=" + nMsg.getWasInRadius());
    	    if (nMsg.getInRadius()) {
    	    	addPlayer(nMsg.getSubject());
    	    } else {
    	    	// Remove subject from targets in range
    	    	removePlayer(nMsg.getSubject());
    	    }
    	} else if (msg instanceof CombatClient.interruptAbilityMessage) {
            interruptInteractTask();
        }
    }
    
    @Override
	public void run() {
    	active = true;
    	
    	if (interactionType.equals("Chest")) {
    		
    	}

		// Loop through players in range and send them the update
		for (OID playerOid : playersInRange) {
			sendState(playerOid);
		}
	}
    
    /**
     * An external call to spawn a world object for the claim.
     * @param instanceOID
     */
    public void spawn(OID instanceOID) {
    	this.instanceOID = instanceOID;
    	spawn();
    }
    
    /**
     * Spawn a world object for the claim.
     */
    public void spawn() {
    	Template markerTemplate = new Template();
    	markerTemplate.put(Namespace.WORLD_MANAGER, WorldManagerClient.TEMPL_NAME, "_ign_interactive_" + id);
    	markerTemplate.put(Namespace.WORLD_MANAGER, WorldManagerClient.TEMPL_OBJECT_TYPE, ObjectTypes.mob);
    	markerTemplate.put(WorldManagerClient.NAMESPACE, WorldManagerClient.TEMPL_PERCEPTION_RADIUS, 75);
    	markerTemplate.put(Namespace.WORLD_MANAGER, WorldManagerClient.TEMPL_INSTANCE, instanceOID);
    	markerTemplate.put(Namespace.WORLD_MANAGER, WorldManagerClient.TEMPL_LOC, new Point(loc));
    	//markerTemplate.put(Namespace.WORLD_MANAGER, WorldManagerClient.TEMPL_ORIENT, orientation);
    	DisplayContext dc = new DisplayContext(gameObject, true);
		dc.addSubmesh(new DisplayContext.Submesh("", ""));
		markerTemplate.put(Namespace.WORLD_MANAGER, WorldManagerClient.TEMPL_DISPLAY_CONTEXT, dc);
		markerTemplate.put(Namespace.WORLD_MANAGER, "model", gameObject); 
    	// Put in any additional props
    	if (props != null) {
    		for (String propName : props.keySet()) {
    			markerTemplate.put(Namespace.WORLD_MANAGER, propName, props.get(propName));
    		}
    	}
    	// Create the object
    	objectOID = ObjectManagerClient.generateObject(ObjectManagerClient.BASE_TEMPLATE_ID,
                ObjectManagerClient.BASE_TEMPLATE, markerTemplate);
    	
    	if (objectOID != null) {
    		// Need to create an interpolated world node to add a tracker/reaction radius to the claim world object
    		BasicWorldNode bwNode = WorldManagerClient.getWorldNode(objectOID);
    		InterpolatedWorldNode iwNode = new InterpolatedWorldNode(bwNode);
    		resourceNodeEntity = new InteractiveObjectEntity(objectOID, iwNode);
    		EntityManager.registerEntityByNamespace(resourceNodeEntity, Namespace.MOB);
    		MobManagerPlugin.getTracker(instanceOID).addLocalObject(objectOID, 100);
    		
            WorldManagerClient.spawn(objectOID);
            Log.debug("INTERACTIVE: spawned resource at : " + loc);
            activate();
        }
    }
    
    /**
     * Add a player to the update list for this ResourceNode. The player will receive data about the node and any updates
     * that occur.
     * @param playerOID
     */
    public void addPlayer(OID playerOid) {
    	Log.debug("INTERACTIVE: added player: " + playerOid);
    	// Send down the state to the player
    	
			
		if (!playersInRange.contains(playerOid)) {
	    	playersInRange.add(playerOid);
	    	sendState(playerOid);
	    }
    }
    
    /**
     * Removes a player from the ResourceNode. They will no longer receive updates.
     * @param playerOID
     * @param removeLastID
     */
    public void removePlayer(OID playerOid) {
    	if (playersInRange.contains(playerOid))
    		playersInRange.remove(playerOid);
    }
    
    /**
     * Checks whether the player can gather items from this resource. Checks their skill level
     * and weapon.
     * @param playerOID
     * @return
     */
    boolean playerCanUse(OID playerOid, boolean checkSkillAndWeapon) {
    	// No one else is currently gathering are they?
   	 	if (task != null && !interactionType.equals("InstancePortal") && !interactionType.equals("LeaveInstance")) {
   	 		//FIXME Disable Check Enter or Exit Instance
   	 	Log.debug("INTERACTIVE: task="+task+" interactionType="+interactionType);
    		ExtendedCombatMessages.sendErrorMessage(playerOid, "The object is currently being used");
    		return false;
    	}
    	// location check
    	Point p = WorldManagerClient.getObjectInfo(playerOid).loc;
    	// Player must be within 4 meters of the node (16 for squared)
    	if(Log.loggingDebug)
    		Log.debug("INTERACTIVE: Ply Loc="+p+" Obj Loc="+loc+" distanceToSquared="+Point.distanceToSquared(p, new Point(loc))+" limit=81");
    	if (Point.distanceToSquared(p, new Point(loc)) > 81) {
    		ExtendedCombatMessages.sendErrorMessage(playerOid, "You are too far away from the object to use it");
    		return false;
    	}
    	
    	if (questIDReq > 0) {
    		boolean onQuest = false;
    		HashMap<Integer, QuestState> activeQuests = QuestClient.getActiveQuests(playerOid);
    		for (int key : activeQuests.keySet()) {
        		if (key == questIDReq) {
        			onQuest = true;
        		}
    		}
    		
    		if (!onQuest) {
    			return false;
    		}
    	}
    	
    	if (checkSkillAndWeapon) {
    		// skill check
    	}
    	
    	return true;
    }
    
    public void tryUseObject(OID playerOid, String state) {
    	Log.debug("INTERACTIVE: got player "+playerOid+" trying to interact with object with state: " + state);
    	if (!playerCanUse(playerOid, true)) {
    		return;
    	}
	    
	    task = new InteractTask();
	    task.StartInteractTask(loc, Quaternion.Identity, playerOid, this, state);
	    
	    if (harvestTimeReq > 0) {
    		Engine.getExecutor().schedule(task, (long) harvestTimeReq * 1000, TimeUnit.MILLISECONDS);
    		task.sendStartInteractTask(harvestTimeReq);
    		// Register for player movement to interrupt the gathering
    		SubjectFilter filter = new SubjectFilter(playerOid);
	        filter.addType(CombatClient.MSG_TYPE_INTERRUPT_ABILITY);
	        sub = Engine.getAgent().createSubscription(filter, this);
    	} else {
    		task.run();
    	}
    }
    
    void interruptInteractTask() {
    	if (task != null) {
    		task.interrupt();
    		task = null;
    		if (sub != null)
                Engine.getAgent().removeSubscription(sub);
    	}
    }
    
	void interactComplete(InteractTask task) {
		Log.debug("INTERACTIVE: interaction complete interactionType="+interactionType);
		if (interactionType.equals("ApplyEffect")) {
			CombatClient.applyEffect(task.playerOid, interactionID);
		} else if (interactionType.equals("Ability")) {
			CombatClient.startAbility(interactionID, task.playerOid, task.playerOid, null, new Point(loc));
		} else if (interactionType.equals("CompleteTask")) {
			QuestClient.TaskUpdateMessage msg = new QuestClient.TaskUpdateMessage(task.playerOid, interactionID, 1);
			Engine.getAgent().sendBroadcast(msg);
		} else if (interactionType.equals("InstancePortal")) {
			AgisWorldManagerClient.sendChangeInstance(task.playerOid, interactionID, new Point(Float.parseFloat(interactionData1), Float.parseFloat(interactionData2), Float.parseFloat(interactionData3)));
		} else if (interactionType.equals("LeaveInstance")) {
			AgisWorldManagerClient.returnToLastInstance(task.playerOid);
		} else if (interactionType.equals("StartQuest")) {
			LinkedList<Integer> quests = new LinkedList<Integer>();
			quests.add(interactionID);
			QuestClient.offerQuestToPlayer(task.playerOid, objectOID, quests, false);
		} else if (interactionType.equals("Chest")) {

		} else if (task.state != null && !task.state.equals("")) {
			Log.debug("INTERACTIVE: sending down state: " + state+" task.state="+task.state);
			// Send down state
			state = task.state;
			for (OID playerOid : playersInRange) {
				sendState(playerOid);
			}
		}
    	
    	if (respawnTime > 0) {
    		despawnResource();
    	}
    }
    
    public void despawnResource() {
    	Log.debug("INTERACTIVE: despawning resource");
    	active = false;

		// Loop through players in range and send them the update
		for (OID playerOid : playersInRange) {
			sendState(playerOid);
		}
		
		// Schedule the respawn
		Engine.getExecutor().schedule(this, respawnTime, TimeUnit.SECONDS);
    }
    
    void sendState(OID playerOid) {
    	Map<String, Serializable> props = new HashMap<String, Serializable>();
		props.put("ext_msg_subtype", "interactive_object_state");
		props.put("nodeID", id);
		props.put("active", active);
		props.put("state", state);
		Log.debug("INTERACTIVE:send state "+props+" to "+playerOid);
		TargetedExtensionMessage msg = new TargetedExtensionMessage( WorldManagerClient.MSG_TYPE_EXTENSION, playerOid, playerOid, false, props);
		Engine.getAgent().sendBroadcast(msg);
    }

	public int getID() { return id; }
    public void setID(int id) {
    	this.id = id;
    }
    
    public String getName() { return name; }
    public void setName(String name) {
    	this.name = name;
    }
    
    public String getGameObject() { return gameObject; }
    public void setGameObject(String gameObject) {
    	this.gameObject = gameObject;
    }
    
    public String getCoordEffect() { return coordinatedEffect; }
    public void setCoordEffect(String coordinatedEffect) {
    	this.coordinatedEffect = coordinatedEffect;
    }
    
    public AOVector getLoc() { return loc; }
    public void setLoc(AOVector loc) {
    	this.loc = loc;
    }
    
    public HashMap<String, Serializable> getProps() { return props; }
    public void setProps(HashMap<String, Serializable> props) {
    	this.props = props;
    }
    
    public OID getInstanceOID() { return instanceOID; }
    public void setInstanceOID(OID instanceOID) {
    	this.instanceOID = instanceOID;
    }
    
    public OID getObjectOID() { return objectOID; }
    public void setObjectOID(OID objectOID) {
    	this.objectOID = objectOID;
    }
    
    public int getQuestIDReq() { return questIDReq; }
    public void setQuestIDReq(int questIDReq) {
    	this.questIDReq = questIDReq;
    }
    
    public String getInteractionType() { return interactionType; }
    public void setInteractionType(String interactionType) {
    	this.interactionType = interactionType;
    }
    
    public int getInteractionID() { return interactionID; }
    public void setInteractionID(int interactionID) {
    	this.interactionID = interactionID;
    }
    
    public String getInteractionData1() { return interactionData1; }
    public void setInteractionData1(String interactionData1) {
    	this.interactionData1 = interactionData1;
    }
    
    public String getInteractionData2() { return interactionData2; }
    public void setInteractionData2(String interactionData2) {
    	this.interactionData2 = interactionData2;
    }
    
    public String getInteractionData3() { return interactionData3; }
    public void setInteractionData3(String interactionData3) {
    	this.interactionData3 = interactionData3;
    }

    public boolean getActive() { return active; }
    public void setActive(boolean active) {
    	this.active = active;
    }
    
    public int getRespawnTime() { return respawnTime; }
    public void setRespawnTime(int respawnTime) {
    	this.respawnTime = respawnTime;
    }
    
    public float getHarvestTimeReq() { return harvestTimeReq; }
    public void setHarvestTimeReq(float harvestTimeReq) {
    	this.harvestTimeReq = harvestTimeReq;
    }

    int id;
    String name;
    int questIDReq;
    String interactionType;
    int interactionID;
    String interactionData1;
    String interactionData2;
    String interactionData3;
    String gameObject;
    String coordinatedEffect;
    String state;
    AOVector loc;
    int respawnTime;
    OID instanceOID;
    OID objectOID;
    HashMap<String, Serializable> props;
    float harvestTimeReq = 0;
    boolean active;
    Long eventSub = null;
    LinkedList<OID> playersInRange = new LinkedList<OID>();
    
    InteractTask task;
    Long sub = null;
    InteractiveObjectEntity resourceNodeEntity;
    
    /**
     * A Runnable class that adds an object to the claim when it is run. 
     * @author Andrew Harrison
     *
     */
    public class InteractTask implements Runnable {
    	
    	protected AOVector loc;
    	protected Quaternion orient;
    	protected OID playerOid;
    	protected int playerSkillLevel;
    	protected InteractiveObject obj;
    	protected String state;
    	protected boolean interrupted;
    	protected CoordinatedEffectState coordinatedEffectState;
    	public InteractTask() {
    		
    	}
    	
    	public void StartInteractTask(AOVector loc, Quaternion orient, OID playerOid, InteractiveObject obj, String state) {
    		Log.debug("INTERACTIVE: creating new interactive task");
    		this.loc = loc;
    		this.orient = orient;
    		this.playerOid = playerOid;
    		this.obj = obj;
    		this.state = state;
    	}
    	
    	public void sendStartInteractTask(float length) {
			Log.debug("INTERACTIVE: sending start interactive task");
			Map<String, Serializable> props = new HashMap<String, Serializable>();
			props.put("ext_msg_subtype", "start_interactive_task");
			props.put("intObjId", obj.getID());
			props.put("length", length);
			TargetedExtensionMessage msg = new TargetedExtensionMessage(WorldManagerClient.MSG_TYPE_EXTENSION, playerOid, playerOid, false, props);
			Engine.getAgent().sendBroadcast(msg);

			// Send animation
			CoordinatedEffect cE = new CoordinatedEffect(obj.coordinatedEffect);
			cE.sendSourceOid(true);
			cE.sendTargetOid(true);
			cE.putArgument("interObjId", obj.getID());
			cE.putArgument("length", length);
			coordinatedEffectState = cE.invoke(playerOid, playerOid);
		}
  	
		@Override
		public void run() {
			if (obj.sub != null)
                Engine.getAgent().removeSubscription(obj.sub);
			
			if (interrupted) {
				Log.debug("INTERACTIVE: task was interrupted, not completing run");
				obj.task = null;
				return;
			}
			
			obj.interactComplete(this);
			obj.task = null;
			coordinatedEffectState = null;
		}
		
		public void interrupt() {
			interrupted = true;
		 	EnginePlugin.setObjectPropertyNoResponse(playerOid, WorldManagerClient.NAMESPACE, "castingParam", -1);
			if(coordinatedEffectState != null)
				coordinatedEffectState.invokeCancel();
			Map<String, Serializable> props = new HashMap<String, Serializable>();
        	props.put("ext_msg_subtype", "interactive_task_interrupted");
			TargetedExtensionMessage msg = new TargetedExtensionMessage(WorldManagerClient.MSG_TYPE_EXTENSION, playerOid, playerOid, false, props);
	  		Engine.getAgent().sendBroadcast(msg);
		}
    }
    

    /**
     * Sub-class needed for the interpolated world node so a perceiver can be created.
     * @author Andrew
     *
     */
	public class InteractiveObjectEntity extends ObjectStub implements EntityWithWorldNode
	{

		public InteractiveObjectEntity(OID oid, InterpolatedWorldNode node) {
	    	setWorldNode(node);
	    	setOid(oid);
	    }
		
		public InterpolatedWorldNode getWorldNode() { return node; }
	    public void setWorldNode(InterpolatedWorldNode node) { this.node = node; }
	    InterpolatedWorldNode node;

		@Override
		public void setDirLocOrient(BasicWorldNode bnode) {
			if (node != null)
	            node.setDirLocOrient(bnode);
		}

		@Override
		public Entity getEntity() {
			return (Entity)this;
		}
		
		private static final long serialVersionUID = 1L;
	}
	
	private static final long serialVersionUID = 1L;

	
}
