package atavism.agis.objects;

import atavism.server.objects.*;
import atavism.server.engine.*;
import atavism.server.util.*;
import atavism.agis.core.*;
import java.util.*;
import java.io.*;

public class AgisMob extends AgisObject {
    public AgisMob() {
        super();
        init();
    }
    
    public AgisMob(OID oid) {
        super(oid);
        init();
    }

    public AgisMob(String name) {
        super();
        init();
        setName(name);        
    }

    public AgisMob(String name, Map<String, Serializable> propMap) {
        super();
        setName(name);
        setPropertyMap(propMap);
        init();
    }
    
    protected void init() {
        setType(ObjectTypes.mob);
        if (Log.loggingDebug)
            Log.debug("AgisMob.init: name=" + getName() + ", perceiver=" + perceiver());
        if (perceiver() == null) {
            if (Log.loggingDebug)
                Log.debug("AgisMob.init: generating perceiver");
            
            Log.debug("QuadTree AgisMob.init: generating perceiver radius :"+ World.perceiverRadius);
            MobilePerceiver<WMWorldNode> p = new MobilePerceiver<WMWorldNode>((WMWorldNode) worldNode(), World.perceiverRadius);
		     p.setFilter(new BasicPerceiverFilter());
            p.setRadius(World.perceiverRadius);
            perceiver(p);
            if (Log.loggingDebug)
                Log.debug("AgisMob.init: generated perceiver=" + p + ", func=" + perceiver());
        }
    }

    public void worldNode(WorldNode worldNode) {
        super.worldNode(worldNode);
        MobilePerceiver<WMWorldNode> p = perceiver();
        if (p != null) {
            ((WMWorldNode) worldNode).setPerceiver(p);
            p.setElement((WMWorldNode) worldNode);
        }
    }

    public static AgisMob convert(Entity obj) {
        if (!(obj instanceof AgisMob)) {
            throw new AORuntimeException("AgisMob.convert: obj is not a agismob: "
                    + obj);
        }
        return (AgisMob) obj;
    }

    // public AgisBehavior getAgisBehavior() {
    // return (AgisBehavior) getBehavior();
    // }

    /**
     * returns the item occupying the slot
     */
    public AgisItem getItemBySlot(AgisEquipSlot slot) {
        lock.lock();
        try {
            return equipMap.get(slot);
        } finally {
            lock.unlock();
        }
    }

    /**
     * returns what slot the item is occupying
     */
    public AgisEquipSlot getSlotByItem(AgisItem item) {
        lock.lock();
        try {
            for (Map.Entry<AgisEquipSlot, AgisItem> entry : equipMap.entrySet()) {
                AgisEquipSlot slot = entry.getKey();
                AgisItem curItem = entry.getValue();
                if (AOObject.equals(item, curItem)) {
                    return slot;
                }
            }
            return null;
        } finally {
            lock.unlock();
        }
    }

    /**
     * sets up what slots are equippable
     */
    public void setEquipInfo(AgisEquipInfo equipInfo) {
        this.equipInfo = equipInfo;
        if (Log.loggingDebug)
            log.debug("setEquipInfo: mob=" + this + ", equipInfo="
                      + equipInfo);
    }

    public AgisEquipInfo getEquipInfo() {
        return equipInfo;
    }

    public List<AgisEquipSlot> getEquippableSlots() {
        if (equipInfo == null) {
            throw new AORuntimeException("AgisMob.getEquippableSlots: equipinfo is null for mob " + this);
        }
        return equipInfo.getEquippableSlots();
    }

    AgisEquipInfo equipInfo = null;

    /**
     * returns all equipped items
     */
    public Set<AgisItem> getEquippedItems() {
        lock.lock();
        try {
            return new HashSet<AgisItem>(equipMap.values());
        } finally {
            lock.unlock();
        }
    }

    /**
     * places the item into the passed in equipment slot this is just a property
     * setting method and does not send any messages or perform any checks.
     */
    public void putItemIntoSlot(AgisEquipSlot slot, AgisItem item) {
        lock.lock();
        try {
            if (Log.loggingDebug)
                Log.debug("AgisObject: putting item " + item + " into equip slot "
                          + slot.getName() + " for obj " + this);
            if (!getEquippableSlots().contains(slot)) {
				log.error("mob " + this.getName() + ", item=" + item + ", mob does not have this slot " + slot);
			     throw new AORuntimeException("mob does not have this slot");
            }
            equipMap.put(slot, item);
        } finally {
            lock.unlock();
        }
    }

    /**
     * removes the slot mapping in the map - does not send out any messages
     * returns the item which was in the slot or null if none was there
     */
    public AgisItem clearSlot(AgisEquipSlot slot) {
        lock.lock();
        try {
            return equipMap.remove(slot);
        } finally {
            lock.unlock();
        }
    }

    public void setEquipMap(Map<AgisEquipSlot, AgisItem> equipMap) {
        lock.lock();
        try {
            if (Log.loggingDebug)
                log.debug("setEquipMap: thismob=" + getName()
                          + ", new equipMap size=" + equipMap.size());
            if (equipMap == null) {
                throw new RuntimeException("equipMap is null");
            }
            this.equipMap = new HashMap<AgisEquipSlot, AgisItem>(equipMap);
        } finally {
            lock.unlock();
        }
    }

    public Map<AgisEquipSlot, AgisItem> getEquipMap() {
        lock.lock();
        try {
            return new HashMap<AgisEquipSlot, AgisItem>(equipMap);
        } finally {
            lock.unlock();
        }
    }

    protected Map<AgisEquipSlot, AgisItem> equipMap = new HashMap<AgisEquipSlot, AgisItem>();

    public int getOCV() {
        return (Math.round((float) getDexterity() / 3));
    }

    public int getDCV() {
        return (Math.round((float) getDexterity() / 3));
    }

    public int getCV() {
        return (Math.round((float) getDexterity() / 3));
    }

    public AgisObject getAutoAttackTarget() {
        return autoAttackTarget;
    }

    AgisObject autoAttackTarget = null;

    public long getLastRecTime() {
        lock.lock(); // long
        try {
            return lastRecTime;
        } finally {
            lock.unlock();
        }
    }

    public void setLastRecTime(long time) {
        lock.lock(); // long
        try {
            lastRecTime = time;
        } finally {
            lock.unlock();
        }
    }

    long lastRecTime = 0;

    public long getLastAttackTime() {
        lock.lock(); // long
        try {
            return lastAttackTime;
        } finally {
            lock.unlock();
        }
    }

    // sets time to now
    public void setLastAttackTime() {
        lock.lock();
        try {
            lastAttackTime = System.currentTimeMillis();
        } finally {
            lock.unlock();
        }
    }

    public long timeSinceLastAttack() {
        return System.currentTimeMillis() - getLastAttackTime();
    }

    long lastAttackTime = 0;

    public void setStrength(int str) {
        this.strength = str;
    }

    public int getStrength() {
        return strength;
    }

    public void modifyStrength(int delta) {
        lock.lock();
        try {
            int strength = getStrength();
            setStrength(strength + delta);
        } finally {
            lock.unlock();
        }
    }

    int strength = 0;

    public void setIntelligence(int intelligence) {
        this.intelligence = intelligence;
    }

    public int getIntelligence() {
        return intelligence;
    }

    public void modifyIntelligence(int delta) {
        lock.lock();
        try {
            int intelligence = getIntelligence();
            setIntelligence(intelligence + delta);
        } finally {
            lock.unlock();
        }
    }

    int intelligence = 0;

    public void setEgo(int ego) {
        this.ego = ego;
    }

    public int getEgo() {
        return ego;
    }

    public void modifyEgo(int delta) {
        lock.lock();
        try {
            int ego = getEgo();
            setEgo(ego + delta);
        } finally {
            lock.unlock();
        }
    }

    int ego = 0;

    public void setPresence(int pre) {
        this.presence = pre;
    }

    public int getPresence() {
        return presence;
    }

    public void modifyPresence(int delta) {
        lock.lock();
        try {
            int presence = getPresence();
            setPresence(presence + delta);
        } finally {
            lock.unlock();
        }
    }

    int presence = 0;

    public void setComeliness(int comeliness) {
        this.comeliness = comeliness;
    }

    public int getComeliness() {
        return comeliness;
    }

    public void modifyComeliness(int delta) {
        lock.lock();
        try {
            int comeliness = getComeliness();
            setComeliness(comeliness + delta);
        } finally {
            lock.unlock();
        }
    }

    int comeliness = 0;

    public void setDexterity(int dex) {
        this.dexterity = dex;
    }

    public int getDexterity() {
        return dexterity;
    }

    public void modifyDexterity(int delta) {
        lock.lock();
        try {
            int dexterity = getDexterity();
            setDexterity(dexterity + delta);
        } finally {
            lock.unlock();
        }
    }

    int dexterity = 0;

    public int getBaseRecovery() {
        return (getStrength() + getConstitution()) / 5;
    }

    public int getConstitution() {
        return constitution;
    }

    public void setConstitution(int con) {
        this.constitution = con;
    }

    public void modifyConstitution(int delta) {
        lock.lock();
        try {
            int constitution = getConstitution();
            setConstitution(constitution + delta);
        } finally {
            lock.unlock();
        }
    }

    int constitution = 0;

    public void setEndurance(int end) {
        this.endurance = end;
    }

    public int getEndurance() {
        return endurance;
    }

    public void modifyEndurance(int delta) {
        lock.lock();
        try {
            int endurace = getEndurance();
            setEndurance(endurace + delta);
        } finally {
            lock.unlock();
        }
    }

    int endurance = 0;

    public void setCurrentEndurance(int end) {
        this.currentEndurance = end;
    }

    public void modifyCurrentEndurance(int delta) {
        lock.lock();
        try {
            int end = getCurrentEndurance();
            setCurrentEndurance(end + delta);
        } finally {
            lock.unlock();
        }
    }

    public int getCurrentEndurance() {
        return currentEndurance;
    }

    int currentEndurance = 0;

    public void setPDBonus(int bonus) {
        pdBonus = bonus;
    }

    public int getPDBonus() {
        return pdBonus;
    }

    public void modifyPDBonus(int delta) {
        lock.lock();
        try {
            int pdBonus = getPDBonus();
            setPDBonus(pdBonus + delta);
        } finally {
            lock.unlock();
        }
    }

    int pdBonus = 0;

    public int getPD() {
        int rv = (Math.round((float) getStrength() / 5));
        lock.lock();
        try {
            return rv + getPDBonus();
        } finally {
            lock.unlock();
        }
    }

    public void setSpeedBonus(int bonus) {
        speedBonus = bonus;
    }

    public int getSpeedBonus() {
        return speedBonus;
    }

    public void modifySpeedBonus(int delta) {
        lock.lock();
        try {
            int speedBonus = getSpeedBonus();
            setSpeedBonus(speedBonus + delta);
        } finally {
            lock.unlock();
        }
    }

    int speedBonus = 0;

    public int getSpeed() {
        int rv = 10 + getDexterity();
        lock.lock();
        try {
            int bonus = getSpeedBonus();
            return rv + bonus;
        } finally {
            lock.unlock();
        }
    }

    public void setResistantPD(int pd) {
        resistPD = pd;
    }

    public int getResistantPD() {
        return resistPD;
    }

    public void modifyResistantPD(int delta) {
        lock.lock();
        try {
            int resistantPD = getResistantPD();
            setResistantPD(resistantPD + delta);
        } finally {
            lock.unlock();
        }
    }

    int resistPD = 0;

    public void setMaxMoveSpeed(int speed) {
        maxMoveSpeed = speed;
    }

    public int getMaxMoveSpeed() {
        return maxMoveSpeed;
    }

    private int maxMoveSpeed = 0;

    /**
     * this mob will give out the passed in quest to users this is not used for
     * storing which quests the player is doing
     */
    public void addQuestPrototype(AgisQuest quest) {
        lock.lock();
        try {
            questSet.add(quest);
        } finally {
            lock.unlock();
        }
    }

    /**
     * this mob is able to conclude the passed in quest for completion by the
     * player.
     */
    public void addConcludeQuest(AgisQuest quest) {
        lock.lock();
        try {
            if (quest == null) {
                throw new RuntimeException("quest is null");
            }
            concludeSet.add(quest);
        } finally {
            lock.unlock();
        }
    }

    // /**
    // * does this mob have a quest for the passed in user
    // */
    // public boolean hasQuestFor(AOObject obj) {
    // lock.lock();
    // try {
    // return (! questSet.isEmpty());
    // }
    // finally {
    // lock.unlock();
    // }
    // }

    /**
     * for now, they are ordered in their dependency copies the actual list, but
     * the references are the original this is for the quests this object is
     * GIVING out - not doing
     */
    public LinkedList<AgisQuest> getQuestPrototypes() {
        lock.lock();
        try {
            return new LinkedList<AgisQuest>(questSet);
        } finally {
            lock.unlock();
        }
    }

    /**
     * returns the set of quests that can be 'turned in' to this mob
     */
    public Set<AgisQuest> getConcludableQuests() {
        lock.lock();
        try {
            return new HashSet<AgisQuest>(concludeSet);
        } finally {
            lock.unlock();
        }
    }

    // collection of quest PROTOTYPES
    // for now they are ordered in their dependency
    Collection<AgisQuest> questSet = new LinkedList<AgisQuest>();

    // quests which this mob can act as the conclude npc
    // (who you go to when you are turning in the quest)
    Set<AgisQuest> concludeSet = new HashSet<AgisQuest>();


    //
    // skills
    //

    /**
     * adds the skill to the characters list of learned skills. it is added with
     * 0 xp
     */
    public void addSkill(AgisSkill skill) {
        lock.lock();
        try {
            skillMap.put(skill, 0);
        } finally {
            lock.unlock();
        }
    }

    public boolean hasSkill(AgisSkill skill) {
        lock.lock();
        try {
            // Iterator<AgisSkill> iter = skillMap.keyValues();
            // while (iter.hasNext()) {
            // AgisSkill s = iter.next();
            // if (s.equals(skill)) {
            // return true;
            // }
            // }
            // Log.debug("AgisMob.hasSkill: could not find matching skill");
            return skillMap.containsKey(skill);
        } finally {
            lock.unlock();
        }
    }

    public void setSkillMap(Map<AgisSkill, Integer> skillMap) {
        this.skillMap = new HashMap<AgisSkill, Integer>(skillMap);
    }

    /**
     * returns the amount of xp you have in the passed in skill
     */
    public int getXPforSkill(AgisSkill skill) {
        lock.lock();
        try {
            Integer xp = skillMap.get(skill);
            return (xp == null) ? 0 : xp;
        } finally {
            lock.unlock();
        }
    }

    public void addSkillXP(AgisSkill skill, int newXp) {
        lock.lock();
        try {
            Integer curXp = skillMap.get(skill);
            if (curXp == null) {
                log.warn("AgisMob.addSKillXp: mob " + this.getName()
                        + " does not have skill " + skill.getName());
                return;
            }
            skillMap.put(skill, (curXp + newXp));
        } finally {
            lock.unlock();
        }
    }

    public Map<AgisSkill, Integer> getSkillMap() {
        lock.lock();
        try {
            return new HashMap<AgisSkill, Integer>(skillMap);
        } finally {
            lock.unlock();
        }
    }

    // skill -> xp map
    private Map<AgisSkill, Integer> skillMap = new HashMap<AgisSkill, Integer>();

    /**
     * returns all the mobs that have done damage to this mob
     */
    public Set<AgisMob> getAttackers() {
        lock.lock();
        try {
            return new HashSet<AgisMob>(dmgTable.keySet());
        } finally {
            lock.unlock();
        }
    }

    /**
     * returns the skills an attacker used on this mob
     */
    public Set<AgisSkill> getAttackerSkills(AgisMob attacker) {
        lock.lock();
        try {
            Map<AgisSkill, Integer> attackerDmgMap = dmgTable.get(attacker);
            return new HashSet<AgisSkill>(attackerDmgMap.keySet());
        } finally {
            lock.unlock();
        }
    }

    /**
     * returns the amount of dmg the attacker has done using skill 'skill'
     */
    public int getDmgForSkill(AgisMob attacker, AgisSkill skill) {
        lock.lock();
        try {
            Map<AgisSkill, Integer> attackerDmgMap = dmgTable.get(attacker);
            if (attackerDmgMap == null) {
                return 0;
            }
            Integer dmg = attackerDmgMap.get(skill);
            if (dmg == null) {
                return 0;
            }
            return dmg;
        } finally {
            lock.unlock();
        }
    }

    /**
     * record that a some other mob has done damage to this mob, so that when
     * this mob dies, the appropriate xp is rewarded
     */
    public void addDamage(AgisMob attacker, AgisSkill skill, int dmg) {
        lock.lock();
        try {
            // get the map of how much damage this particular
            // attacker has done with his various skills
            Map<AgisSkill, Integer> attackerDmgMap = dmgTable.get(attacker);
            if (attackerDmgMap == null) {
                attackerDmgMap = new HashMap<AgisSkill, Integer>();
            }

            // find out how much dmg attacker has done with the skill so far
            Integer curDmg = attackerDmgMap.get(skill);
            if (curDmg == null) {
                curDmg = 0;
            }
            attackerDmgMap.put(skill, (curDmg + dmg));
            dmgTable.put(attacker, attackerDmgMap);

            if (Log.loggingDebug)
                log.debug("addDamage: attacker=" + attacker.getName() + ", skill="
                          + skill.getName() + ", prevDmg=" + curDmg + ", newDmg="
                          + dmg + ", newTotal=" + (curDmg + dmg));

            totalDmgTaken += dmg;
        } finally {
            lock.unlock();
        }
    }

    public int getDamageTaken() {
        return totalDmgTaken;
    }

    // dmg that others have done on this mob
    // attacker -> skilltype -> dmg
    Map<AgisMob, Map<AgisSkill, Integer>> dmgTable = new HashMap<AgisMob, Map<AgisSkill, Integer>>();

    private int totalDmgTaken = 0;

    public AgisAbilityState getCurrentAbility() {
        return currentAbility;
    }

    public void setCurrentAbility(AgisAbilityState state) {
        currentAbility = state;
    }

    protected AgisAbilityState currentAbility = null;

    public Set<AgisAbilityState> getActiveAbilities() {
        return new HashSet<AgisAbilityState>(activeAbilities);
    }

    protected void setActiveAbilities(Set<AgisAbilityState> abilities) {
        activeAbilities = new HashSet<AgisAbilityState>(abilities);
    }

    public void addActiveAbility(AgisAbilityState state) {
        activeAbilities.add(state);
    }

    public void removeActiveAbility(AgisAbilityState state) {
        activeAbilities.remove(state);
    }

    protected Set<AgisAbilityState> activeAbilities = new HashSet<AgisAbilityState>();

    private static final long serialVersionUID = 1L;
}
