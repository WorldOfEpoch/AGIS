package atavism.agis.events;

import atavism.server.engine.*;
import atavism.server.network.*;
import atavism.agis.objects.*;

/**
 * this message means that a player has just completed a quest
 * currently generated by the quest state object
 */
public class QuestCompleted extends AgisEvent {
    public QuestCompleted() {
	super();
    }

    public QuestCompleted(AOByteBuffer buf, ClientConnection con) {
	super(buf,con);
    }

    public QuestCompleted(AgisMob user, AgisQuest quest) {
	super();
	setObject(user);
        setQuestId(quest.getOid());
    }

    public String getName() {
	return "QuestCompleted";
    }

    public void setQuestId(OID id) {
        this.questId = id;
    }
    public OID getQuestId() {
        return questId;
    }
    OID questId = null;

    public AOByteBuffer toBytes() {
	int msgId = Engine.getEventServer().getEventID(this.getClass());

	AOByteBuffer buf = new AOByteBuffer(20);
	buf.putOID(getObjectOid()); 
	buf.putInt(msgId);
	buf.putOID(getQuestId());
	buf.flip();
	return buf;
    }

    public void parseBytes(AOByteBuffer buf) {
	buf.rewind();
	setObjectOid(buf.getOID());
	/* int msgId = */ buf.getInt();
        setQuestId(buf.getOID());
    }
}