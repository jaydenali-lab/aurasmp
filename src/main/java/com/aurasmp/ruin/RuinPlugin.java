package com.aurasmp.ruin;

import com.aurasmp.ruin.ability.AbilityManager;
import com.aurasmp.ruin.card.CardManager;
import com.aurasmp.ruin.card.TalentAura;
import com.aurasmp.ruin.command.RuinCommand;
import com.aurasmp.ruin.data.DataStore;
import com.aurasmp.ruin.data.PlayerData;
import com.aurasmp.ruin.gui.SelectionGui;
import com.aurasmp.ruin.hud.ActionBarHud;
import com.aurasmp.ruin.hud.XpBossBar;
import com.aurasmp.ruin.item.RuinItems;
import com.aurasmp.ruin.listener.CombatListener;
import com.aurasmp.ruin.listener.GuiListener;
import com.aurasmp.ruin.listener.PlayerListener;
import com.aurasmp.ruin.progression.Progression;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/** Entry point. Owns the managers and wires up listeners + commands. */
public final class RuinPlugin extends JavaPlugin {

    private DataStore data;
    private CardManager cards;
    private AbilityManager abilities;
    private Progression progression;
    private SelectionGui gui;
    private RuinItems items;
    private ActionBarHud hud;
    private XpBossBar xpBar;
    private TalentAura talentAura;

    @Override
    public void onEnable() {
        this.data = new DataStore(this);
        this.cards = new CardManager(this);
        this.abilities = new AbilityManager(this);
        this.progression = new Progression(this);
        this.gui = new SelectionGui(this);
        this.items = new RuinItems(this);
        this.hud = new ActionBarHud(this);
        this.xpBar = new XpBossBar(this);
        this.talentAura = new TalentAura(this);

        items.registerRecipes();
        hud.start();
        talentAura.start();

        getServer().getPluginManager().registerEvents(new CombatListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new GuiListener(this), this);

        PluginCommand command = getCommand("ruin");
        if (command != null) {
            RuinCommand executor = new RuinCommand(this);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }

        // Players already online during a /reload need their state (re)loaded.
        for (Player player : getServer().getOnlinePlayers()) {
            cards.recalc(player, data.get(player.getUniqueId()));
        }

        getLogger().info("Ruin enabled — 40 talents, 20 manifestations, max level " + PlayerData.MAX_LEVEL + ".");
    }

    @Override
    public void onDisable() {
        if (hud != null) hud.stop();
        if (talentAura != null) talentAura.stop();
        if (xpBar != null) xpBar.cleanupAll();
        if (data != null) data.saveAll();
    }

    /** Full progression wipe: clears data, strips attribute modifiers, removes the Catalyst. */
    public void resetPlayer(Player player) {
        PlayerData playerData = data.get(player.getUniqueId());
        playerData.reset();
        cards.recalc(player, playerData);
        abilities.cooldowns().clear(player.getUniqueId());
        abilities.clearActive(player.getUniqueId());
        gui.clear(player.getUniqueId());
        gui.refreshCatalyst(player, playerData); // no abilities -> removes any Catalyst
        data.save(player.getUniqueId(), playerData);
    }

    public DataStore data() { return data; }
    public CardManager cards() { return cards; }
    public AbilityManager abilities() { return abilities; }
    public Progression progression() { return progression; }
    public SelectionGui gui() { return gui; }
    public RuinItems items() { return items; }
    public XpBossBar xpBar() { return xpBar; }
}
