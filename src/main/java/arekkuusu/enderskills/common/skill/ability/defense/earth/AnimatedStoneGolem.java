package arekkuusu.enderskills.common.skill.ability.defense.earth;

import arekkuusu.enderskills.api.capability.AdvancementCapability;
import arekkuusu.enderskills.api.capability.Capabilities;
import arekkuusu.enderskills.api.capability.data.IInfoCooldown;
import arekkuusu.enderskills.api.capability.data.IInfoUpgradeable;
import arekkuusu.enderskills.api.capability.data.SkillData;
import arekkuusu.enderskills.api.capability.data.SkillInfo;
import arekkuusu.enderskills.api.capability.data.nbt.UUIDWatcher;
import arekkuusu.enderskills.api.event.SkillShouldUseEvent;
import arekkuusu.enderskills.api.helper.ExpressionHelper;
import arekkuusu.enderskills.api.helper.NBTHelper;
import arekkuusu.enderskills.api.helper.RayTraceHelper;
import arekkuusu.enderskills.api.registry.Skill;
import arekkuusu.enderskills.client.gui.data.ISkillAdvancement;
import arekkuusu.enderskills.client.util.ResourceLibrary;
import arekkuusu.enderskills.client.util.helper.TextHelper;
import arekkuusu.enderskills.common.CommonConfig;
import arekkuusu.enderskills.common.EnderSkills;
import arekkuusu.enderskills.common.entity.EntityStoneGolem;
import arekkuusu.enderskills.common.lib.LibMod;
import arekkuusu.enderskills.common.lib.LibNames;
import arekkuusu.enderskills.common.skill.ModAbilities;
import arekkuusu.enderskills.common.skill.ModAttributes;
import arekkuusu.enderskills.common.skill.SkillHelper;
import arekkuusu.enderskills.common.skill.ability.AbilityInfo;
import arekkuusu.enderskills.common.skill.ability.BaseAbility;
import arekkuusu.enderskills.common.sound.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.SoundCategory;
import net.minecraft.world.WorldServer;
import net.minecraftforge.client.event.InputUpdateEvent;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class AnimatedStoneGolem extends BaseAbility implements ISkillAdvancement {

    public AnimatedStoneGolem() {
        super(LibNames.ANIMATED_STONE_GOLEM);
        setTexture(ResourceLibrary.ANIMATED_STONE_GOLEM);
    }

    @Override
    public void use(EntityLivingBase user, SkillInfo skillInfo) {
        if (isClientWorld(user) || !shouldUse(user)) return;
        AbilityInfo abilityInfo = (AbilityInfo) skillInfo;

        Capabilities.get(user).ifPresent(capability -> {
            if (!SkillHelper.isActiveOwner(user, this)) {

                RayTraceHelper.getPosLookedAt(user, 5).ifPresent(pos -> {
                    pos = pos.up();
                    if (!((IInfoCooldown) skillInfo).hasCooldown() && canUse(user)) {
                        if (!(user instanceof EntityPlayer) || !((EntityPlayer) user).capabilities.isCreativeMode) {
                            abilityInfo.setCooldown(getCooldown(abilityInfo));
                        }
                        EntityStoneGolem golem = new EntityStoneGolem(user.world);
                        golem.setPosition(pos.getX() + 0.5D, pos.getY() + 0.01D, pos.getZ() + 0.5D);
                        golem.setOwnerId(user.getUniqueID());
                        golem.setMaxHealth(getHealth(abilityInfo));
                        golem.setHealth(getHealth(abilityInfo));
                        golem.setMirrorDamage(getMirror(abilityInfo));
                        golem.setDamage(getDamage(abilityInfo));
                        NBTTagCompound compound = new NBTTagCompound();
                        NBTHelper.setEntity(compound, user, "user");
                        NBTHelper.setEntity(compound, golem, "golem");
                        SkillData data = SkillData.of(this)
                                .with(getTime(abilityInfo))
                                .put(compound, UUIDWatcher.INSTANCE)
                                .overrides(this)
                                .create();
                        golem.setData(data);
                        user.world.spawnEntity(golem);
                        apply(user, data);
                        sync(user, data);
                        sync(user);

                        if (user.world instanceof WorldServer) {
                            ((WorldServer) user.world).playSound(null, user.posX, user.posY, user.posZ, ModSounds.ANIMATED_STONE, SoundCategory.PLAYERS, 5.0F, (1.0F + (user.world.rand.nextFloat() - user.world.rand.nextFloat()) * 0.2F) * 0.7F);
                        }
                    }
                });
            } else {
                SkillHelper.getActiveOwner(user, this, holder -> {
                    Optional.ofNullable(NBTHelper.getEntity(EntityStoneGolem.class, holder.data.nbt, "golem")).ifPresent(Entity::setDead);
                    unapply(user, holder.data);
                    async(user, holder.data);
                });
            }
        });
    }

    @Override
    public void update(EntityLivingBase target, SkillData data, int tick) {
        if (isClientWorld(target) && !(target instanceof EntityPlayer)) return;
        Optional.ofNullable(NBTHelper.getEntity(EntityLivingBase.class, data.nbt, "user")).ifPresent(user -> {
            if (target != user && !(user instanceof EntityStoneGolem)) {
                if (target instanceof EntityLiving) {
                    ((EntityLiving) target).getNavigator().clearPath();
                }
                target.moveStrafing = 0;
                target.moveForward = 0;
                target.motionX = 0;
                target.motionZ = 0;
            }
        });
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void inputListener(InputUpdateEvent event) {
        if (SkillHelper.isActiveNotOwner(event.getEntityLiving(), this)) {
            event.getMovementInput().forwardKeyDown = false;
            event.getMovementInput().rightKeyDown = false;
            event.getMovementInput().backKeyDown = false;
            event.getMovementInput().leftKeyDown = false;
            event.getMovementInput().sneak = false;
            event.getMovementInput().jump = false;
        }
    }

    @SubscribeEvent
    public void onSkillShouldUse(SkillShouldUseEvent event) {
        if (isClientWorld(event.getEntityLiving())) return;
        if (SkillHelper.isActiveNotOwner(event.getEntityLiving(), this)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void mouseListener(MouseEvent event) {
        if (SkillHelper.isActiveNotOwner(Minecraft.getMinecraft().player, this)) {
            Minecraft.getMinecraft().mouseHelper.deltaX = Minecraft.getMinecraft().mouseHelper.deltaY = 0;
        }
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void onMouseClick(InputEvent.MouseInputEvent event) {
        if (SkillHelper.isActiveNotOwner(Minecraft.getMinecraft().player, this)) {
            if (Minecraft.getMinecraft().gameSettings.keyBindAttack.isPressed()) {
                KeyBinding.setKeyBindState(Minecraft.getMinecraft().gameSettings.keyBindAttack.getKeyCode(), false);
            }
        }
    }

    public int getLevel(IInfoUpgradeable info) {
        return info.getLevel();
    }

    @Override
    public int getMaxLevel() {
        return Configuration.getSyncValues().maxLevel;
    }

    public float getDamage(AbilityInfo info) {
        int level = getLevel(info);
        int levelMax = getMaxLevel();
        double func = ExpressionHelper.getExpression(this, Configuration.getSyncValues().extra.damage, level, levelMax);
        double result = (func * CommonConfig.getSyncValues().skill.extra.globalEffectEffectiveness);
        return (float) (result * getEffectiveness());
    }

    public float getMirror(AbilityInfo info) {
        int level = getLevel(info);
        int levelMax = getMaxLevel();
        double func = ExpressionHelper.getExpression(this, Configuration.getSyncValues().extra.mirror, level, levelMax);
        double result = (func * CommonConfig.getSyncValues().skill.extra.globalEffectEffectiveness);
        return (float) (result * getEffectiveness());
    }

    public float getHealth(AbilityInfo info) {
        int level = getLevel(info);
        int levelMax = getMaxLevel();
        double func = ExpressionHelper.getExpression(this, Configuration.getSyncValues().extra.health, level, levelMax);
        double result = (func * CommonConfig.getSyncValues().skill.extra.globalEffectEffectiveness);
        return (float) (result * getEffectiveness());
    }

    public double getStunTime(AbilityInfo info) {
        int level = getLevel(info);
        int levelMax = getMaxLevel();
        double func = ExpressionHelper.getExpression(this, Configuration.getSyncValues().extra.health, level, levelMax);
        double result = (func * CommonConfig.getSyncValues().skill.globalTime);
        return (result * getEffectiveness());
    }

    public double getRange(AbilityInfo info) {
        int level = getLevel(info);
        int levelMax = getMaxLevel();
        double func = ExpressionHelper.getExpression(this, Configuration.getSyncValues().range, level, levelMax);
        double result = (func * CommonConfig.getSyncValues().skill.globalRange);
        return (result * getEffectiveness());
    }

    public int getCooldown(AbilityInfo info) {
        int level = getLevel(info);
        int levelMax = getMaxLevel();
        double func = ExpressionHelper.getExpression(this, Configuration.getSyncValues().cooldown, level, levelMax);
        double result = (func * CommonConfig.getSyncValues().skill.globalCooldown);
        return (int) (result * getEffectiveness());
    }

    public int getTime(AbilityInfo info) {
        int level = getLevel(info);
        int levelMax = getMaxLevel();
        double func = ExpressionHelper.getExpression(this, Configuration.getSyncValues().time, level, levelMax);
        double result = (func * CommonConfig.getSyncValues().skill.globalTime);
        return (int) (result * getEffectiveness());
    }

    public double getEffectiveness() {
        return Configuration.getSyncValues().effectiveness * CommonConfig.getSyncValues().skill.globalEffectiveness;
    }

    /*Advancement Section*/
    @Override
    @SideOnly(Side.CLIENT)
    public void addDescription(List<String> description) {
        Capabilities.get(Minecraft.getMinecraft().player).ifPresent(c -> {
            if (c.owns(this)) {
                if (!GuiScreen.isShiftKeyDown()) {
                    description.add("");
                    description.add("Hold SHIFT for stats.");
                } else {
                    c.get(this).ifPresent(skillInfo -> {
                        AbilityInfo abilityInfo = (AbilityInfo) skillInfo;
                        description.clear();
                        description.add("Endurance Drain: " + ModAttributes.ENDURANCE.getEnduranceDrain(this));
                        description.add("");
                        if (abilityInfo.getLevel() >= getMaxLevel()) {
                            description.add("Max Level:");
                        } else {
                            description.add("Current Level:");
                        }
                        description.add("Cooldown: " + TextHelper.format2FloatPoint(getCooldown(abilityInfo) / 20D) + "s");
                        description.add("Duration: " + TextHelper.format2FloatPoint(getTime(abilityInfo) / 20D) + "s");
                        description.add("Mirror Damage: +" + TextHelper.format2FloatPoint(getMirror(abilityInfo) * 100D) + "%");
                        description.add("Stun Duration: " + TextHelper.format2FloatPoint(getStunTime(abilityInfo) / 20D) + "s");
                        description.add("Golem Health: " + TextHelper.format2FloatPoint(getHealth(abilityInfo) / 2D) + " Hearts");
                        if (abilityInfo.getLevel() < getMaxLevel()) { //Copy info and set a higher level...
                            AbilityInfo infoNew = new AbilityInfo(abilityInfo.serializeNBT());
                            infoNew.setLevel(infoNew.getLevel() + 1);
                            description.add("");
                            description.add("Next Level:");
                            description.add("Cooldown: " + TextHelper.format2FloatPoint(getCooldown(infoNew) / 20D) + "s");
                            description.add("Duration: " + TextHelper.format2FloatPoint(getTime(infoNew) / 20D) + "s");
                            description.add("Mirror Damage: +" + TextHelper.format2FloatPoint(getMirror(infoNew) * 100D) + "%");
                            description.add("Stun Duration: " + TextHelper.format2FloatPoint(getStunTime(infoNew) / 20D) + "s");
                            description.add("Golem Health: " + TextHelper.format2FloatPoint(getHealth(infoNew) / 2D) + " Hearts");
                        }
                    });
                }
            }
        });
    }

    @Override
    public boolean canUpgrade(EntityLivingBase entity) {
        return Capabilities.advancement(entity).map(c -> {
            Requirement requirement = getRequirement(entity);
            int tokens = requirement.getLevels();
            int xp = requirement.getXp();
            return c.level >= tokens && c.getExperienceTotal(entity) >= xp;
        }).orElse(false);
    }

    @Override
    public void onUpgrade(EntityLivingBase entity) {
        Capabilities.advancement(entity).ifPresent(c -> {
            Requirement requirement = getRequirement(entity);
            int tokens = requirement.getLevels();
            int xp = requirement.getXp();
            if (c.level >= tokens && c.getExperienceTotal(entity) >= xp) {
                //c.tokensLevel -= tokens;
                c.consumeExperienceFromTotal(entity, xp);
            }
        });
    }

    @Override
    public Requirement getRequirement(EntityLivingBase entity) {
        AbilityInfo info = (AbilityInfo) Capabilities.get(entity).flatMap(a -> a.get(this)).orElse(null);
        int tokensNeeded = 0;
        int xpNeeded;
        if (info == null) {
            int abilities = Capabilities.get(entity).map(c -> (int) c.getAll().keySet().stream().filter(s -> s instanceof BaseAbility).count()).orElse(0);
            if (abilities > 0) {
                tokensNeeded = abilities + 1;
            } else {
                tokensNeeded = 1;
            }
        }
        xpNeeded = getUpgradeCost(info);
        return new DefaultRequirement(tokensNeeded, getCostIncrement(entity, xpNeeded));
    }

    public int getCostIncrement(EntityLivingBase entity, int total) {
        Optional<AdvancementCapability> optional = Capabilities.advancement(entity);
        if (optional.isPresent()) {
            AdvancementCapability advancement = optional.get();
            List<Skill> skillUnlockOrder = Arrays.asList(advancement.skillUnlockOrder);
            int index = skillUnlockOrder.indexOf(ModAbilities.TAUNT);
            if (index == -1) {
                index = advancement.skillUnlockOrder.length;
            }
            return (int) (total * (1D + index * 0.5D));
        }
        return total;
    }

    public int getUpgradeCost(@Nullable AbilityInfo info) {
        int level = info != null ? getLevel(info) + 1 : 0;
        int levelMax = getMaxLevel();
        double func = ExpressionHelper.getExpression(this, Configuration.getSyncValues().advancement.upgrade, level, levelMax);
        return (int) (func * CommonConfig.getSyncValues().advancement.xp.globalCostMultiplier);
    }
    /*Advancement Section*/

    @Override
    public void initSyncConfig() {
        Configuration.getSyncValues().maxLevel = Configuration.getValues().maxLevel;
        Configuration.getSyncValues().cooldown = Configuration.getValues().cooldown;
        Configuration.getSyncValues().time = Configuration.getValues().time;
        Configuration.getSyncValues().range = Configuration.getValues().range;
        Configuration.getSyncValues().effectiveness = Configuration.getValues().effectiveness;
        Configuration.getSyncValues().extra.mirror = Configuration.getValues().extra.mirror;
        Configuration.getSyncValues().extra.stunTime = Configuration.getValues().extra.stunTime;
        Configuration.getSyncValues().extra.health = Configuration.getValues().extra.health;
        Configuration.getSyncValues().advancement.upgrade = Configuration.getValues().advancement.upgrade;
    }

    @Override
    public void writeSyncConfig(NBTTagCompound compound) {
        compound.setInteger("maxLevel", Configuration.getValues().maxLevel);
        compound.setString("cooldown", Configuration.getValues().cooldown);
        compound.setString("time", Configuration.getValues().time);
        compound.setString("range", Configuration.getValues().range);
        compound.setDouble("effectiveness", Configuration.getValues().effectiveness);
        compound.setString("extra.mirror", Configuration.getValues().extra.mirror);
        compound.setString("extra.stunTime", Configuration.getValues().extra.stunTime);
        compound.setString("extra.health", Configuration.getValues().extra.health);
        compound.setString("advancement.upgrade", Configuration.getValues().advancement.upgrade);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void readSyncConfig(NBTTagCompound compound) {
        Configuration.getSyncValues().maxLevel = compound.getInteger("maxLevel");
        Configuration.getSyncValues().cooldown = compound.getString("cooldown");
        Configuration.getSyncValues().time = compound.getString("time");
        Configuration.getSyncValues().range = compound.getString("range");
        Configuration.getSyncValues().effectiveness = compound.getDouble("effectiveness");
        Configuration.getSyncValues().extra.mirror = compound.getString("extra.mirror");
        Configuration.getSyncValues().extra.stunTime = compound.getString("extra.stunTime");
        Configuration.getSyncValues().extra.health = compound.getString("extra.health");
        Configuration.getSyncValues().advancement.upgrade = compound.getString("advancement.upgrade");
    }

    @Config(modid = LibMod.MOD_ID, name = LibMod.MOD_ID + "/Ability/" + LibNames.ANIMATED_STONE_GOLEM)
    public static class Configuration {

        @Config.Comment("Ability Values")
        @Config.LangKey(LibMod.MOD_ID + ".config." + LibNames.ANIMATED_STONE_GOLEM)
        public static Values CONFIG = new Values();

        public static Values getValues() {
            return CONFIG;
        }

        @Config.Ignore
        protected static Values CONFIG_SYNC = new Values();

        public static Values getSyncValues() {
            return CONFIG_SYNC;
        }

        public static class Values {
            @Config.Comment("Skill specific extra Configuration")
            public final Extra extra = new Extra();
            @Config.Comment("Skill specific Advancement Configuration")
            public final Advancement advancement = new Advancement();

            @Config.Comment("Max level obtainable")
            @Config.RangeInt(min = 0)
            public int maxLevel = 100;

            @Config.Comment("Cooldown Function f(x,y)=? where 'x' is [Current Level] and 'y' is [Max Level]")
            public String cooldown = "(90 * 20) + (30 * 20) * (1 - ((e^(-0.1 * (x / y)) - 1)/((e^-0.1) - 1)))";

            @Config.Comment("Duration Function f(x,y)=? where 'x' is [Current Level] and 'y' is [Max Level]")
            public String time = "45 * 20 + ((e^(-0.1 * (x / y)) - 1)/((e^-0.1) - 1)) * ((90 * 20) - (45 * 20))";

            @Config.Comment("Range Function f(x,y)=? where 'x' is [Current Level] and 'y' is [Max Level]")
            public String range = "UNUSED";

            @Config.Comment("Effectiveness Modifier")
            @Config.RangeDouble
            public double effectiveness = 1D;

            public static class Extra {
                @Config.Comment("Golem Damage Function f(x,y)=? where 'x' is [Current Level] and 'y' is [Max Level]")
                public String damage = "0.75 + ((e^(0.1 * (x / y)) - 1)/((e^0.1) - 1)) * (1.5 - 0.75)";
                @Config.Comment("Golem Damage Multiplier Function f(x,y)=? where 'x' is [Current Level] and 'y' is [Max Level]")
                public String mirror = "0.75 + ((e^(0.1 * (x / y)) - 1)/((e^0.1) - 1)) * (1.5 - 0.75)";
                @Config.Comment("Golem Health Function f(x,y)=? where 'x' is [Current Level] and 'y' is [Max Level]")
                public String stunTime = "3 * 20";
                @Config.Comment("Golem Health Function f(x,y)=? where 'x' is [Current Level] and 'y' is [Max Level]")
                public String health = "100 + (100 * 0.1 * x)";
            }

            public static class Advancement {
                @Config.Comment("Function f(x)=? where 'x' is [Next Level] and 'y' is [Max Level], XP Cost is in units [NOT LEVELS]")
                public String upgrade = "(22070 * (1 - (0 ^ (0 ^ x)))) + 7 * x";
            }
        }
    }
}