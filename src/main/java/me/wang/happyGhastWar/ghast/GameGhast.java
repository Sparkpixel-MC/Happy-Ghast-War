package me.wang.happyGhastWar.ghast;

import me.wang.happyGhastWar.ghast.armor.GhastArmorManager;
import me.wang.happyGhastWar.ghast.skill.GhastSkillManager;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.HappyGhast;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;

public class GameGhast {
    private final GhastSkillManager skillManager;
    private final GhastArmorManager armorManager;
    private final HappyGhast happyGhast;
    private final BlockDisplay blockDisplay;
    private final TextDisplay textDisplay;
    private final Interaction interaction;

    private int healthLevel = 0;
    private int armorLevel = 0;

    private int snowAmount = 0;

    public GameGhast(HappyGhast happyGhast, BlockDisplay blockDisplay, TextDisplay textDisplay,
                     Interaction interaction, GhastSkillManager skillManager) {
        this(happyGhast, blockDisplay, textDisplay, interaction, skillManager, null);
    }

    public GameGhast(HappyGhast happyGhast, BlockDisplay blockDisplay, TextDisplay textDisplay,
                     Interaction interaction, GhastSkillManager skillManager, GhastArmorManager armorManager) {
        this.blockDisplay = blockDisplay;
        this.interaction = interaction;
        this.happyGhast = happyGhast;
        this.textDisplay = textDisplay;
        this.skillManager = skillManager;
        this.armorManager = armorManager;
    }

    public int getSnowAmount() {
        return snowAmount;
    }

    public void setSnowAmount(int snowAmount) {
        this.snowAmount = snowAmount;
    }

    public HappyGhast getHappyGhast() {
        return happyGhast;
    }

    public BlockDisplay getBlockDisplay() {
        return blockDisplay;
    }

    public Interaction getInteraction() {
        return interaction;
    }

    public TextDisplay getTextDisplay() {
        return textDisplay;
    }

    public GhastSkillManager getSkillManager() {
        return skillManager;
    }

    public GhastArmorManager getArmorManager() {
        return armorManager;
    }

    public int getArmorLevel() {
        return armorLevel;
    }

    public int getHealthLevel() {
        return healthLevel;
    }

    public void setArmorLevel(int armorLevel) {
        this.armorLevel = armorLevel;
    }

    public void setHealthLevel(int healthLevel) {
        this.healthLevel = healthLevel;
    }

    public void addMaxHealth(double h){
        if (happyGhast == null) return;
        double old = happyGhast.getMaxHealth();
        happyGhast.setMaxHealth(old+h);
    }

    public void addHealth(double h){
        if (happyGhast == null) return;
        double old = happyGhast.getHealth();
        happyGhast.setHealth(old+h);
    }

    public void useSkill(Player rider) {
        if (skillManager != null) {
            skillManager.useSkill(this, rider);
        }
    }

    public void upgradeArmor(Player rider) {
        if (armorManager != null) {
            armorManager.upgradeGhastArmor(this, rider);
        } else if (rider != null) {
            me.wang.happyGhastWar.util.Text.send(rider, "<red>乐魂护甲系统未就绪");
        }
    }

    public String getSkillCooldownInfo() {
        if (skillManager != null) {
            return skillManager.getCooldownInfo(this);
        }
        return "未注册";
    }

    public String getArmorUpgradeInfo() {
        if (armorManager != null) {
            return armorManager.getArmorUpgradeInfo(this);
        }
        return "未注册";
    }

    public void unregister(){
        if (happyGhast != null && !happyGhast.isDead()){
            happyGhast.remove();
        }
        if (blockDisplay != null){
            blockDisplay.remove();
        }
        if (textDisplay != null){
            textDisplay.remove();
        }
        if (interaction != null){
            interaction.remove();
        }
    }
}
