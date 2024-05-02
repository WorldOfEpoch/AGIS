package atavism.agis.objects;

import java.util.LinkedList;
import java.io.*;

import atavism.server.engine.Behavior;
import atavism.server.engine.OID;
import atavism.server.math.Point;
import atavism.server.objects.ObjectFactory;
import atavism.server.objects.ObjectStub;
import atavism.server.objects.SpawnData;
import atavism.server.util.Log;

public class MobFactory extends ObjectFactory implements Serializable {
	public MobFactory(int templateID) {
		super(templateID);
	}
		
	public ObjectStub makeObject(SpawnData spawnData, OID instanceOid, Point loc) {
		ObjectStub obj = super.makeObject(spawnData, instanceOid, loc);
	        
		Log.debug("MOBFACTORY: makeObject; adding behavs: " + behavs);
		for (Behavior behav: behavs) {
			if (!obj.getBehaviors().contains(behav)) {
				obj.addBehavior(behav);
				Log.debug("MOBFACTORY: makeObject; adding behav: " + behav);
			}
		}
	    //obj.addBehavior(new BaseBehavior());
	    //behav = WhisperResponseBehavior();
	    //behav.addChatResponse("hi", "A good hello to you sir/madame");
	    //obj.addBehavior(behav);
	    return obj;
	}
		
	public void addBehav(Behavior behav) {
		behavs.add(behav);
	}
	public void setBehavs(LinkedList<Behavior> behavs) {
		this.behavs = behavs;
	}
	public LinkedList<Behavior> getBehavs() {
		return behavs;
	}
	private LinkedList<Behavior> behavs = new LinkedList<Behavior>();
		
	private static final long serialVersionUID = 1L;
}
