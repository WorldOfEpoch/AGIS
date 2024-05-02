package atavism.agis.core;

import atavism.server.engine.*;
import atavism.agis.objects.AgisQuest;
import atavism.agis.objects.Currency;
import atavism.agis.objects.Dialogue;
import atavism.agis.objects.Faction;
import atavism.agis.objects.LootTable;
import atavism.agis.objects.MerchantTable;
import atavism.agis.objects.SkillProfileTemplate;
import atavism.agis.objects.SkillTemplate;

public class Agis {
	public static Manager<AgisAbility> AbilityManager = new Manager<AgisAbility>("AbilityManager");

	public static Manager<AgisEffect> EffectManager = new Manager<AgisEffect>("EffectManager");

	public static Manager<SkillTemplate> SkillManager = new Manager<SkillTemplate>("SkillManager");

	public static Manager<SkillProfileTemplate> SkillProfileManager = new Manager<SkillProfileTemplate>("SkillProfileManager");

	public static Manager<Faction> FactionManager = new Manager<Faction>("FactionManager");

	public static Manager<Currency> CurrencyManager = new Manager<Currency>("CurrencyManager");

	public static Manager<LootTable> LootTableManager = new Manager<LootTable>("LootTableManager");

	public static Manager<AgisQuest> QuestManager = new Manager<AgisQuest>("QuestManager");

	public static Manager<Dialogue> DialogueManager = new Manager<Dialogue>("DialogueManager");

	public static Manager<MerchantTable> MerchantTableManager = new Manager<MerchantTable>("MerchantTableManager");

    public static int getDefaultCorpseTimeout() {
	return defaultCorpseTimeout;
    }
    public static void setDefaultCorpseTimeout(int timeout) {
	defaultCorpseTimeout = timeout;
    }
    private static int defaultCorpseTimeout = 60000;
}