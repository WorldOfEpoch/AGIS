package atavism.agis.effects;

import atavism.server.engine.*;
import atavism.server.objects.Template;
import atavism.server.plugins.InventoryClient;
import atavism.server.plugins.ObjectManagerClient;
import atavism.server.plugins.ObjectManagerPlugin;
import atavism.server.util.Log;
import atavism.agis.objects.*;
import atavism.agis.core.*;
import atavism.agis.plugins.*;
import atavism.agis.util.*;

import java.io.Serializable;
import java.util.*;

/**
 * Effect child class that applies physical damage over time reducing a value
 * stat of the target (e.g. Health or Mana). This effect should be used for
 * physical based damage over time abilities such as bleeds. The total damage is
 * calculated upon applying the effect, then the damage is distributed over the
 * ticks.
 * 
 * @author: Andrew Harrison
 */
public class AttackDotEffect extends AgisEffect {
	static Random random = new Random();

	public AttackDotEffect(int id, String name) {
		super(id, name);
	}

	// add the effect to the object
	public void apply(EffectState state) {
		super.apply(state);
		Map<String, Serializable> params = state.getParams();
		hitRoll = (int) params.get("hitRoll");

		int claimID = -1;
		int objectID = -1;
		if (params.containsKey("claimID"))
			claimID = (int) params.get("claimID");
		if (params.containsKey("objectID"))
			objectID = (int) params.get("objectID");

		String abilityEvent = EventMessageHelper.COMBAT_DEBUFF_GAINED;

		CombatInfo target = state.getTarget();
		CombatInfo source = state.getSource();
		
		// FIXME Stacking
		// Stacking handling
		int stackCase = stackCheck();
		Log.debug("AttackDotEffect.apply stackCase:" + stackCase);
		if (stackCase == 0) {
			// Do not apply this effect
			return;
		}
		if (target == null && claimID == -1 && objectID == -1) {
			Log.error("AttackDotEffect.apply: target is null " + state);
			return;
		}
		int stackLevel = 1;
		boolean hasThisEffect = false;
		boolean fromThisCaster = false;
		int damageFromEffects = 0;
		int damageFromEffectsSame = 0;

		// AgisEffect effect = state.getEffect();
		EffectState similarEffect = null;
		EffectState sameCasterEffect = null;
		if (target != null) {
			if(target.dead()) {
				Log.debug("AttackDotEffect: target is dead");
				state.wasApplied(false);
				return;
			}
			
			
			for (EffectState existingState : target.getCurrentEffects()) {
				if (existingState.getEffect().getID() == getID() && !state.equals(existingState)) {
					AttackDotEffect pde = (AttackDotEffect) existingState.getEffect();
					damageFromEffects += pde.getPulseDamage();
					hasThisEffect = true;
					similarEffect = existingState;
					if (source.getOwnerOid().equals(similarEffect.getStackCaster())) {
						damageFromEffectsSame += pde.getPulseDamage();
						fromThisCaster = true;
						sameCasterEffect = similarEffect;
					}
				}
			}
		}

		int dmg = minDmg;
		if (maxDmg > minDmg) {
			dmg += random.nextInt(maxDmg - minDmg);
		}
		if (useWeaponDamage) {
			int dmg_base = source.statGetCurrentValue("dmg-base");
			int dmg_max = source.statGetCurrentValue("dmg-max");
			if (dmg_max <= dmg_base) {
				dmg += dmg_base;
			} else {
				dmg += dmg_base + random.nextInt(dmg_max - dmg_base);
			}

		}

		
		if (Log.loggingDebug)
			Log.debug("AttackDotEffect.apply dmg:" + dmg);
		// dmg*=stackLevel;
		if (Log.loggingDebug)
			Log.debug("AttackDotEffect.apply dmg:" + dmg + " multiple stack");

		int ammoId = 0;
		int ammoDamage = 0;
		Integer ammoType = 0;
		Boolean ammoMatch = false;
		try {
			ammoId = (Integer) source.getProperty(CombatInfo.COMBAT_AMMO_LOADED);
			ammoDamage = (Integer) source.getProperty(CombatInfo.COMBAT_AMMO_DAMAGE);
			if (ammoId > 0) {
				Template itemTemplate = ObjectManagerClient.getTemplate(ammoId, ObjectManagerPlugin.ITEM_TEMPLATE);
				if (itemTemplate == null)
					ammoType = (Integer) itemTemplate.get(InventoryClient.ITEM_NAMESPACE, AgisItem.AMMO_TYPE);
			}
		} catch (Exception e) {
			Log.debug("AttackDotEffect: ammo error " + e);
		}
		if (ammoId > 0) {
			List <AgisEquipSlot> slots = AgisEquipSlot.getSlotByType("Weapon");
			for(AgisEquipSlot aes : slots) {
				Integer weaponAmmoType = AgisInventoryClient.findItemAmmoType(source.getOid(),aes);
				if (weaponAmmoType != null && weaponAmmoType == ammoType) {
					ammoMatch = true;
				}	
			}
			/*Integer primAmmoType = AgisInventoryClient.findItemAmmoType(source.getOid(), AgisEquipSlot.PRIMARYWEAPON);
			Integer secAmmoType = AgisInventoryClient.findItemAmmoType(source.getOid(), AgisEquipSlot.SECONDARYWEAPON);
			if (primAmmoType != null && primAmmoType == ammoType) {
				ammoMatch = true;
			}
			if (secAmmoType != null && secAmmoType == ammoType) {
				ammoMatch = true;
			}*/
		}
		if (ammoMatch)
			dmg += ammoDamage;

		dmg = CombatHelper.CalcDamage(target, source, dmg, damageType, skillEffectMod.get(0), effectSkillType, hitRoll, false, params);
		if (DamageMod != 1.0) {
			Log.debug("AttackDotEffect: DamageMod: " + DamageMod + " Damage: " + dmg);
			float dmgF = (float) dmg * DamageMod;
			Log.debug("AttackDotEffect: DamageFloat: " + dmgF);
			dmg = Math.round(dmgF);
			Log.debug("AttackDotEffect: Damage: " + dmg);
		}

		dmg = CombatHelper.modifyDamage(source, target, dmg);

		Log.debug("AttackDotEffect.apply: target has this effect: " + hasThisEffect + "; from this caster: " + fromThisCaster + " with stackCase: " + stackCase);
		if (stackCase == 1) {
			// If the target already has the same effect type from the same caster, remove
			// the old one
			if (fromThisCaster) {
				Log.debug("AttackDotEffect.apply remove  caster effect sameCasterEffect:" + sameCasterEffect);
				AgisEffect.removeEffect(sameCasterEffect);
			}
		} else if (stackCase == 2) {
			// If the target already has the same effect type from the same caster,
			// increment the stack and pulse damage
			if (fromThisCaster) {
				stackLevel = sameCasterEffect.getCurrentStack();
				// AgisEffect.removeEffect(sameCasterEffect);
				if (stackLevel < this.stackLimit)
					stackLevel++;

				sameCasterEffect.setTimeUntilEnd(getDuration());
				sameCasterEffect.setNextPulse(0);
				sameCasterEffect.reschedule(getPulseTime());
				sameCasterEffect.setCurrentStack(stackLevel);
				sameCasterEffect.getTarget().updateEffectsProperty();

				int pulseDamage = dmg / this.numPulses;
				if (pulseDamage == 0)
					pulseDamage = 1;
				AttackDotEffect se = (AttackDotEffect) sameCasterEffect.getEffect();
				se.setPulseDamage(pulseDamage * stackLevel);

				state.wasApplied(false);
				return;

			}
		} else if (stackCase == 3) {
			// If the target already has the same effect type, remove the old one
			if (hasThisEffect) {
				Log.debug("AttackDotEffect.apply remove effect similarEffect:" + similarEffect);
				AgisEffect.removeEffect(similarEffect);
			}
		} else if (stackCase == 4) {
			// If the target already has the same effect type, increment the stack and pulse
			// damage
			if (hasThisEffect) {
				stackLevel = similarEffect.getCurrentStack();
				Log.debug("AttackDotEffect.apply remove effect similarEffect:" + similarEffect + " hasThisEffect:" + hasThisEffect);
				// AgisEffect.removeEffect(similarEffect);
				if (stackLevel < this.stackLimit)
					stackLevel++;

				similarEffect.setTimeUntilEnd(getDuration());
				similarEffect.setNextPulse(0);
				similarEffect.reschedule(getPulseTime());
				similarEffect.setCurrentStack(stackLevel);
				similarEffect.getTarget().updateEffectsProperty();
				int pulseDamage = dmg / this.numPulses;
				if (pulseDamage == 0)
					pulseDamage = 1;
				AttackDotEffect se = (AttackDotEffect) similarEffect.getEffect();
				se.setPulseDamage(pulseDamage * stackLevel);
				state.wasApplied(false);
				return;
			}
		} else if (stackCase == 5) {
			// Time stacking
			if (fromThisCaster) {
				stackLevel = sameCasterEffect.getCurrentStack();
				if (stackLevel < this.stackLimit)
					stackLevel++;
				sameCasterEffect.setCurrentStack(stackLevel);
				sameCasterEffect.getTarget().updateEffectsProperty();
				state.wasApplied(false);
				return;
			}
		} else if (stackCase == 6) {
			// Time stacking
			if (hasThisEffect) {
				stackLevel = similarEffect.getCurrentStack();
				if (stackLevel < this.stackLimit)
					stackLevel++;
				similarEffect.setCurrentStack(stackLevel);
				similarEffect.getTarget().updateEffectsProperty();
				state.wasApplied(false);
				return;
			}
		}
		// Log.debug("AttackDotEffect.apply stackLevel:"+stackLevel);
		if (source.isUser() && target != null && target.isUser() && !source.getOid().equals(target.getOid()) && CombatPlugin.PVP_DAMAGE_REDUCTION_USE) {
			dmg = (int) (dmg * CombatPlugin.PVP_DAMAGE_REDUCTION_PERCENT);
			Log.debug("COMBAT: AttackDotEffect  caster:" + source + " target:" + target + " dmg * " + CombatPlugin.PVP_DAMAGE_REDUCTION_PERCENT);
		}
		// Divide the total damage by the number of pulses
		pulseDamage = dmg / this.numPulses;
		if (pulseDamage == 0)
			pulseDamage = 1;

		// pulseDamage += damageFromEffects;

		state.setStackCaster(source.getOwnerOid());
		state.setCurrentStack(stackLevel);
		if (target != null) {
			EventMessageHelper.SendCombatEvent(source.getOwnerOid(), target.getOwnerOid(), abilityEvent, state.getAbilityID(), getID(), -1, -1,"",damageType);
		}
	}

	// perform the next periodic pulse for this effect on the object
	public void pulse(EffectState state) {
		super.pulse(state);
		Map<String, Serializable> params = state.getParams();
		int result = (int) params.get("result");
		int claimID = -1;
		int objectID = -1;
		if (params.containsKey("claimID"))
			claimID = (int) params.get("claimID");
		if (params.containsKey("objectID"))
			objectID = (int) params.get("objectID");
		Log.debug("PULSE: running pulse for AttackDotEffect dmg=" + pulseDamage + " result=" + result + " claimID=" + claimID + " objectID=" + objectID);
		String abilityEvent = EventMessageHelper.COMBAT_DAMAGE;

		CombatInfo target = state.getTarget();
		CombatInfo source = state.getSource();
		if (target == null && claimID == -1 && objectID == -1) {
			Log.error("AttackDotEffect.pulse: target is null " + state);
			return;
		}
		if (target != null) {
			if (target.dead()) {
				Log.debug("AttackDotEffect: target is dead");
				return;
			}
		}
		// Check if the attack was duel based

		int dmg = pulseDamage;
		Set<EffectState> effects = null;
		if (target != null)
			effects = state.getTarget().getCurrentEffects() != null ? new HashSet<EffectState>(state.getTarget().getCurrentEffects()) : new HashSet<EffectState>();
		Set<EffectState> ceffects = null;
		if (source != null)
			ceffects = state.getSource().getCurrentEffects() != null ? new HashSet<EffectState>(state.getSource().getCurrentEffects()) : new HashSet<EffectState>();
		// Triggers Calculations
		Log.debug("AttackDotEffect.pulse: Check Triggetson caster");
		if(ceffects!=null)
		for (EffectState ef : ceffects) {
			if (ef.getEffect().getClass().equals(TriggerEffect.class)) {
				TriggerEffect se = (TriggerEffect) ef.getEffect();
				dmg = se.Calculate(TriggerProfile.Type.DAMAGE, tags, dmg, source, target, ef);
			}
		}
		Log.debug("AttackDotEffect.pulse: Check Triggetson target");
		if (effects != null && !state.getSourceOid().equals(state.getTargetOid())) {
			for (EffectState ef : effects) {
				if (ef.getEffect().getClass().equals(TriggerEffect.class)) {
					TriggerEffect se = (TriggerEffect) ef.getEffect();
					dmg = se.Calculate(TriggerProfile.Type.DAMAGE, tags, dmg, source, target, ef);
				}
			}
		}
		CombatClient.sendCombatEvent(state.getSourceOid(), false, TriggerProfile.Type.DAMAGE);
		CombatClient.sendCombatEvent(state.getTargetOid(), true, TriggerProfile.Type.DAMAGE);
		if (result == AgisAbility.RESULT_CRITICAL) {
			if(ceffects!=null)
				for (EffectState ef : ceffects) {
				if (ef.getEffect().getClass().equals(TriggerEffect.class)) {
					TriggerEffect se = (TriggerEffect) ef.getEffect();
					dmg = se.Calculate(TriggerProfile.Type.CRITICAL, tags, dmg, source, target, ef);
				}
			}
			Log.debug("AttackDotEffect.pulse: Check Triggetson target");
			if (effects != null && !state.getSourceOid().equals(state.getTargetOid())) {
				for (EffectState ef : effects) {
					if (ef.getEffect().getClass().equals(TriggerEffect.class)) {
						TriggerEffect se = (TriggerEffect) ef.getEffect();
						dmg = se.Calculate(TriggerProfile.Type.CRITICAL, tags, dmg, source, target, ef);
					}
				}
			}
			CombatClient.sendCombatEvent(state.getSourceOid(),false, TriggerProfile.Type.CRITICAL);
			CombatClient.sendCombatEvent(state.getTargetOid(),true, TriggerProfile.Type.CRITICAL);
			
		}
		// Ability Triggers Calculations
		if(state.getAbilityID() > 0) {
			AgisAbility aa = Agis.AbilityManager.get(state.getAbilityID());
			if(aa!=null) {
				long time = (long) params.get("powerUp");
				dmg = aa.getPowerUpDefinition(time).CalculateTriggers(TriggerProfile.Type.DAMAGE, tags, dmg, source, target,aa);
			if (result == AgisAbility.RESULT_CRITICAL) {
				dmg = aa.getPowerUpDefinition(time).CalculateTriggers(TriggerProfile.Type.CRITICAL, tags, dmg, source, target,aa);
			}
			}
		}
		if (effects != null)
			for (EffectState ef : effects) {
				if (ef.getEffect().getClass().equals(ShieldEffect.class)) {
					ShieldEffect se = (ShieldEffect) ef.getEffect();
					dmg = se.Calculate(source, target, pulseDamage, getDamageProperty(), tags, ef);
				}
			}
		if (target != null) {
			int targetHealth = target.statGetCurrentValue(getDamageProperty());

			if (dmg >= targetHealth) {
				// Check if this will cause a duel defeat
				if (checkDuelDefeat(target, source, getDamageProperty())) {
					dmg = targetHealth - 1;
				} else {
					if(ceffects!=null)
							for (EffectState ef : ceffects) {
						if (ef.getEffect().getClass().equals(TriggerEffect.class)) {
							TriggerEffect se = (TriggerEffect) ef.getEffect();
							dmg = se.Calculate(TriggerProfile.Type.KILL, tags, dmg, source, target, ef);
						}
					}
					Log.debug("AttackDotEffect.pulse: Check Triggetson target");
					for (EffectState ef : effects) {
						if (ef.getEffect().getClass().equals(TriggerEffect.class)) {
							TriggerEffect se = (TriggerEffect) ef.getEffect();
							dmg = se.Calculate(TriggerProfile.Type.KILL, tags, dmg, source, target, ef);
						}
					}
					// Ability Triggers Calculations
					if(state.getAbilityID() > 0) {
						AgisAbility aa = Agis.AbilityManager.get(state.getAbilityID());
						if(aa!=null) {
							long time = (long) params.get("powerUp");
							dmg = aa.getPowerUpDefinition(time).CalculateTriggers(TriggerProfile.Type.KILL, tags, dmg, source, target,aa);
						}
					}
					CombatClient.sendCombatEvent(state.getSourceOid(), false, TriggerProfile.Type.KILL);
					CombatClient.sendCombatEvent(state.getTargetOid(), true, TriggerProfile.Type.KILL);
						}
				// Check for arena kill
				if (target.isUser()) {
					Integer arenaID = (Integer) target.getProperty(CombatInfo.COMBAT_PROP_ARENA_ID);
					if (arenaID != null && arenaID > -1) {
						ArenaClient.arenaDeath(source.getOwnerOid(), target.getOwnerOid());
					}
				}
				AgisEffect.removeEffect(state);
			}
		}
		int threat = dmg;
		if (threat == 0)
			threat = 1;
		if (claimID > 0 && objectID > 0) {
			if(source!=null)
				VoxelClient.sendBuildingDamage(state.getSourceOid(), claimID, objectID, dmg);
		}
		if (target != null) {
			Engine.getAgent().sendBroadcast(new CombatClient.DamageMessage(target.getOwnerOid(), state.getSourceOid(), dmg, damageType, threat));

			if (dmg > 0) {
				// Engine.getAgent().sendBroadcast(new
				// CombatClient.DamageMessage(target.getOwnerOid(), source.getOwnerOid(), dmg,
				// this.damageType));
				target.statModifyBaseValue(getDamageProperty(), -dmg);
			
				// Send interrupt to target
				if ((interruption_chance == 0 && interruption_chance_max > 0) || interruption_chance > 0 || forceInterruption) {
					CombatClient.interruptAbilityMessage interruptMsg = new CombatClient.interruptAbilityMessage(target.getOwnerOid());
					interruptMsg.setForce(forceInterruption);
					float chance = interruption_chance;
					if (interruption_chance < interruption_chance_max) {
						Random rand = new Random();
						float f = rand.nextFloat();
						chance = interruption_chance + (interruption_chance_max - interruption_chance) * f;
					}
					interruptMsg.setChance(chance);
					Engine.getAgent().sendBroadcast(interruptMsg);
				}
				// Dragonsan Remove Sleep because apply damage
				// Set<EffectState> effects = target.getCurrentEffects();
				List<Integer> listToRemove = new ArrayList<Integer>();
				for (EffectState ef : effects) {
					Log.debug("AttackDotEffect Damage: Check is SleepEffect " + ef.getEffectName());
					if (ef.getEffect().getClass().equals(SleepEffect.class)) {
						Log.debug("AttackDotEffect Damage: add to remove SleepEffect " + ef.getEffectName());
						if (!listToRemove.contains(ef.getEffect().getID()))
							listToRemove.add(ef.getEffect().getID());
					}

				}
				Log.debug("AttackDotEffect.pulse Removing Sleep Effects");
				for (Integer id : listToRemove) {
					AgisEffect.removeEffectByID(target, id);
				}
				Log.debug("AttackDotEffect.pulse Removed Sleep Effects");

				target.sendStatusUpdate();
			}
		}
		if (source != null) {
			double healD = dmg * transferModifier;
			int heal = (int) healD;

			source.statModifyBaseValue(getDamageProperty(), heal);
			source.sendStatusUpdate();
		}
		// If there is a pulse coord effect, run it now
		if (pulseCoordEffect != null && !pulseCoordEffect.isEmpty()) {
			CoordinatedEffect cE = new CoordinatedEffect(pulseCoordEffect);
			cE.sendSourceOid(true);
			cE.sendTargetOid(true);
			cE.putArgument("claimID", claimID);
			cE.putArgument("claimObjID", objectID);
			if (claimID > 0 && objectID > 0) {
				state.getCoordinatedEffectStates().add(cE.invoke(state.getSourceOid(), state.getSourceOid()));
			} else {
				state.getCoordinatedEffectStates().add(cE.invoke(target.getOwnerOid(), target.getOwnerOid()));
			}
		}
		if (target != null) {
			EventMessageHelper.SendCombatEvent(state.getSourceOid(), target.getOwnerOid(), abilityEvent, state.getAbilityID(), getID(), dmg, -1,"",damageType);
		} else {
			EventMessageHelper.SendCombatEvent(state.getSourceOid(), (target != null ? target.getOwnerOid() : new OID()), abilityEvent, state.getAbilityID(), getID(), dmg, claimID, objectID + "", damageType);
		}
	}

	public int getMinDamage() {
		return minDmg;
	}

	public void setMinDamage(int value) {
		minDmg = value;
	}

	protected int minDmg = 0;

	public int getMaxDamage() {
		return maxDmg;
	}

	public void setMaxDamage(int value) {
		maxDmg = value;
	}

	protected int maxDmg = 0;

	public int getPulseDamage() {
		return pulseDamage;
	}

	public void setPulseDamage(int value) {
		pulseDamage = value;
	}

	protected int pulseDamage = 0;

	public String getPulseCoordEffect() {
		return pulseCoordEffect;
	}

	public void setPulseCoordEffect(String coordEffect) {
		pulseCoordEffect = coordEffect;
	}

	protected String pulseCoordEffect;

	public String getDamageProperty() {
		return damageProperty;
	}

	public void setDamageProperty(String property) {
		damageProperty = property;
	}

	protected String damageProperty = CombatPlugin.HEALTH_STAT;

	public float getDamageMod() {
		return DamageMod;
	}

	public void setDamageMod(float hps) {
		DamageMod = hps;
	}

	protected float DamageMod = 1.0f;

	public double getTransferModifier() {
		return transferModifier;
	}

	public void setTransferModifier(double modifier) {
		transferModifier = modifier;
	}

	protected double transferModifier = 1.0;

	// Effect Value that needs to be removed upon effect removal
	public void setEffectVal(int effect) {
		effectVal = effect;
	}

	public int GetEffectVal() {
		return effectVal;
	}

	public int effectVal = 0;

	public void setEffectName(String eName) {
		effectName = eName;
	}

	public String getEffectName() {
		return effectName;
	}

	protected String effectName = "";

	public void setEffectType(int type) {
		effectType = type;
	}

	public int GetEffectType() {
		return effectType;
	}

	public int effectType = 0;

	public void setEffectSkillType(int type) {
		effectSkillType = type;
	}

	public int GetEffectSkillType() {
		return effectSkillType;
	}

	public int effectSkillType = 0;

	public void setHitRoll(int roll) {
		hitRoll = roll;
	}

	public int GetHitRoll() {
		return hitRoll;
	}

	public int hitRoll = 0;
	
	public void setUseWeaponDamage(boolean useWeaponDamage) {
		this.useWeaponDamage = useWeaponDamage;
	}

	public boolean getUseWeaponDamage() {
		return useWeaponDamage;
	}

	public boolean useWeaponDamage = false;

	private static final long serialVersionUID = 1L;
}
