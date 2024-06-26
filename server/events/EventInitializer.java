package atavism.agis.server.events;

import atavism.server.engine.*;

public class EventInitializer {

    public static void init() {
        EventServer evtSvr = Engine.getEventServer();
        // register the id numbers to event mapping
        evtSvr.registerEventId(1, "atavism.server.events.LoginEvent");
        // evtSvr.registerEventId(2, "atavism.server.events.DirLocEvent");
        evtSvr.registerEventId(3, "atavism.server.events.ComEvent");
        evtSvr.registerEventId(4, "atavism.server.events.LoginResponseEvent");
        evtSvr.registerEventId(5, "atavism.server.events.LogoutEvent");
        evtSvr.registerEventId(6, "atavism.server.events.TerrainEvent");
        evtSvr.registerEventId(7, "atavism.server.events.SkyboxEvent");
        evtSvr.registerEventId(8, "atavism.server.events.NewObjectEvent");
        // evtSvr.registerEventId(9, "atavism.server.events.OrientEvent");
        evtSvr.registerEventId(10, "atavism.server.events.NotifyFreeObjectEvent");
        evtSvr.registerEventId(11, "atavism.server.events.AcquireEvent");
        evtSvr.registerEventId(12, "atavism.server.events.AcquireResponseEvent");
        evtSvr.registerEventId(13, "atavism.server.events.CommandEvent");
        evtSvr.registerEventId(14, "atavism.agis.events.EquipEvent");
        evtSvr.registerEventId(15, "atavism.agis.events.AgisEquipResponseEvent");
        evtSvr.registerEventId(16, "atavism.agis.events.AgisUnequipEvent");
        // evtSvr.registerEventId(17, "atavism.server.events.UnequipResponseEvent");
        evtSvr.registerEventId(18, "atavism.server.events.AttachEvent");
        evtSvr.registerEventId(19, "atavism.server.events.DetachEvent");
        evtSvr.registerEventId(20, "atavism.agis.events.CombatEvent");
        evtSvr.registerEventId(21, "atavism.server.events.AutoAttackEvent");
        evtSvr.registerEventId(22, "atavism.agis.events.StatusUpdateEvent");
        evtSvr.registerEventId(23, "atavism.agis.events.AgisDamageEvent");
        evtSvr.registerEventId(24, "atavism.server.events.DropEvent");
        evtSvr.registerEventId(25, "atavism.agis.events.DropResponseEvent");
        evtSvr.registerEventId(26, "atavism.server.events.NotifyPlayAnimationEvent");
        // evtSvr.registerEventId(27, "atavism.server.events.NotifyPlaySoundEvent");
        // evtSvr.registerEventId(28, "atavism.server.events.AmbientSoundEvent");
        // evtSvr.registerEventId(29, "atavism.server.events.FollowTerrainEvent");
        evtSvr.registerEventId(30, "atavism.server.events.PortalEvent");
        evtSvr.registerEventId(31, "atavism.server.events.AmbientLightEvent");
        evtSvr.registerEventId(32, "atavism.server.events.NewLightEvent");
        // evtSvr.registerEventId(33, "atavism.agis.events.TradeStartReqEvent");
        // evtSvr.registerEventId(34, "atavism.agis.events.TradeStartEvent");
        // evtSvr.registerEventId(35, "atavism.agis.events.TradeOfferReqEvent");
        // evtSvr.registerEventId(36, "atavism.agis.events.TradeCompleteEvent");
        // evtSvr.registerEventId(37, "atavism.agis.events.TradeOfferUpdateEvent");
        evtSvr.registerEventId(38, "atavism.agis.events.AgisStateEvent");
        evtSvr.registerEventId(39, "atavism.agis.events.RequestQuestInfo");
        evtSvr.registerEventId(40, "atavism.agis.events.QuestInfo");
        evtSvr.registerEventId(41, "atavism.agis.events.QuestResponse");
        evtSvr.registerEventId(42, "atavism.server.events.RegionConfiguration");
        // evtSvr.registerEventId(43, "atavism.agis.events.InventoryUpdate");
        evtSvr.registerEventId(44, "atavism.agis.events.QuestLogInfo");
        // evtSvr.registerEventId(45, "atavism.agis.events.QuestStateInfo");
        evtSvr.registerEventId(47, "atavism.agis.events.RemoveQuestResponse");
        evtSvr.registerEventId(49, "atavism.agis.events.ConcludeQuest");
        evtSvr.registerEventId(50, "atavism.server.events.UITheme");
        // evtSvr.registerEventId(52, "atavism.server.events.ModelInfoEvent");
        evtSvr.registerEventId(53, "atavism.server.events.FragmentedMessage");
        evtSvr.registerEventId(54, "atavism.server.events.RoadEvent");
        // evtSvr.registerEventId(55, "atavism.server.events.FogEvent");
        evtSvr.registerEventId(55, "atavism.server.plugins.WorldManagerClient$FogMessage");
        evtSvr.registerEventId(56, "atavism.agis.events.AbilityUpdateEvent");
        evtSvr.registerEventId(57, "atavism.agis.events.AbilityInfoEvent");
        evtSvr.registerEventId(58, "atavism.agis.events.CooldownEvent");
        evtSvr.registerEventId(59, "atavism.agis.events.AbilityActivateEvent");
        evtSvr.registerEventId(60, "atavism.agis.events.AbilityProgressEvent");
        evtSvr.registerEventId(72, "atavism.server.events.ActivateItemEvent");
        evtSvr.registerEventId(75, "atavism.server.events.NewTerrainDecalEvent");
        evtSvr.registerEventId(76, "atavism.server.events.FreeTerrainDecalEvent");
        evtSvr.registerEventId(77, "atavism.server.events.ModelInfoEvent");
        evtSvr.registerEventId(79, "atavism.server.events.DirLocOrientEvent");
        evtSvr.registerEventId(80, "atavism.server.events.AuthorizedLoginEvent");
        evtSvr.registerEventId(81, "atavism.server.events.AuthorizedLoginResponseEvent");
        evtSvr.registerEventId(82, "atavism.server.events.LoadingStateEvent");
        evtSvr.registerEventId(83, "atavism.server.events.ExtensionMessageEvent");
        evtSvr.registerEventId(84, "atavism.agis.events.AbilityStatusEvent");
        evtSvr.registerEventId(85, "atavism.server.events.WorldFileEvent");
        evtSvr.registerEventId(87, "atavism.server.events.SceneLoadedEvent");
        evtSvr.registerEventId(88, "atavism.server.events.PlayerHaEvent");

        // evtSvr.registerEventId(1024, "atavism.server.events.ServerEvent")
        evtSvr.registerEventId(1025, "atavism.server.events.RegisterEntityEvent");
        evtSvr.registerEventId(1026, "atavism.server.events.RegisterEntityResponseEvent");
        evtSvr.registerEventId(1027, "atavism.server.events.ConResetEvent");
        evtSvr.registerEventId(1028, "atavism.server.events.NotifyNewObjectEvent");
        evtSvr.registerEventId(1029, "atavism.server.events.ScriptEvent");
        evtSvr.registerEventId(1030, "atavism.server.events.DirectedEvent");
        evtSvr.registerEventId(1031, "atavism.server.events.SaveEvent");
        evtSvr.registerEventId(1032, "atavism.agis.events.QuestAvailableEvent");
        evtSvr.registerEventId(1033, "atavism.agis.events.NewQuestStateEvent");
        // evtSvr.registerEventId(1034, "atavism.agis.events.ServerRequestQuestInfo");
        evtSvr.registerEventId(1035, "atavism.agis.events.QuestCompleted");
        evtSvr.registerEventId(1036, "atavism.server.events.UnregisterEntityEvent");
        evtSvr.registerEventId(1037, "atavism.server.events.UnregisterEntityResponseEvent");

    }
}
