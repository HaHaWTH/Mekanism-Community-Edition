package mekanism.common;

import com.mojang.authlib.GameProfile;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import mekanism.api.Coord4D;
import mekanism.api.EnumColor;
import mekanism.api.MekanismAPI;
import mekanism.api.MekanismAPI.BoxBlacklistEvent;
import mekanism.api.gas.Gas;
import mekanism.api.gas.GasRegistry;
import mekanism.api.gas.GasStack;
import mekanism.api.gas.OreGas;
import mekanism.api.infuse.InfuseObject;
import mekanism.api.infuse.InfuseRegistry;
import mekanism.api.infuse.InfuseType;
import mekanism.api.transmitters.DynamicNetwork.ClientTickUpdate;
import mekanism.api.transmitters.DynamicNetwork.NetworkClientRequest;
import mekanism.api.transmitters.DynamicNetwork.TransmittersAddedEvent;
import mekanism.api.transmitters.TransmitterNetworkRegistry;
import mekanism.client.ClientTickHandler;
import mekanism.common.base.IModule;
import mekanism.common.block.states.BlockStateMachine.MachineType;
import mekanism.common.block.states.BlockStateTransmitter.TransmitterType;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.chunkloading.ChunkManager;
import mekanism.common.command.CommandMek;
import mekanism.common.config.MekanismConfig;
import mekanism.common.content.boiler.SynchronizedBoilerData;
import mekanism.common.content.entangloporter.InventoryFrequency;
import mekanism.common.content.matrix.SynchronizedMatrixData;
import mekanism.common.content.tank.SynchronizedTankData;
import mekanism.common.content.transporter.PathfinderCache;
import mekanism.common.content.transporter.TransporterManager;
import mekanism.common.entity.EntityBabySkeleton;
import mekanism.common.entity.EntityBalloon;
import mekanism.common.entity.EntityFlame;
import mekanism.common.entity.EntityObsidianTNT;
import mekanism.common.entity.EntityRobit;
import mekanism.common.fixers.MekanismDataFixers;
import mekanism.common.frequency.Frequency;
import mekanism.common.frequency.FrequencyManager;
import mekanism.common.integration.IMCHandler;
import mekanism.common.integration.MekanismHooks;
import mekanism.common.integration.multipart.MultipartMekanism;
import mekanism.common.multiblock.MultiblockManager;
import mekanism.common.network.PacketDataRequest.DataRequestMessage;
import mekanism.common.network.PacketSimpleGui;
import mekanism.common.network.PacketTransmitterUpdate.PacketType;
import mekanism.common.network.PacketTransmitterUpdate.TransmitterUpdateMessage;
import mekanism.common.recipe.BinRecipe;
import mekanism.common.recipe.GasConversionHandler;
import mekanism.common.recipe.RecipeHandler;
import mekanism.common.recipe.RecipeHandler.Recipe;
import mekanism.common.recipe.inputs.ItemStackInput;
import mekanism.common.recipe.machines.SmeltingRecipe;
import mekanism.common.recipe.outputs.ItemStackOutput;
import mekanism.common.security.SecurityFrequency;
import mekanism.common.tier.BaseTier;
import mekanism.common.tile.TileEntityAdvancedBoundingBlock;
import mekanism.common.tile.TileEntityAdvancedFactory;
import mekanism.common.tile.TileEntityAmbientAccumulator;
import mekanism.common.tile.TileEntityBin;
import mekanism.common.tile.TileEntityBoilerCasing;
import mekanism.common.tile.TileEntityBoilerValve;
import mekanism.common.tile.TileEntityBoundingBlock;
import mekanism.common.tile.TileEntityCardboardBox;
import mekanism.common.tile.TileEntityChargepad;
import mekanism.common.tile.TileEntityChemicalCrystallizer;
import mekanism.common.tile.TileEntityChemicalDissolutionChamber;
import mekanism.common.tile.TileEntityChemicalInfuser;
import mekanism.common.tile.TileEntityChemicalInjectionChamber;
import mekanism.common.tile.TileEntityChemicalOxidizer;
import mekanism.common.tile.TileEntityChemicalWasher;
import mekanism.common.tile.TileEntityCombiner;
import mekanism.common.tile.TileEntityCrusher;
import mekanism.common.tile.TileEntityDigitalMiner;
import mekanism.common.tile.TileEntityDynamicTank;
import mekanism.common.tile.TileEntityDynamicValve;
import mekanism.common.tile.TileEntityElectricPump;
import mekanism.common.tile.TileEntityElectrolyticSeparator;
import mekanism.common.tile.TileEntityEliteFactory;
import mekanism.common.tile.TileEntityEnergizedSmelter;
import mekanism.common.tile.TileEntityEnergyCube;
import mekanism.common.tile.TileEntityEnrichmentChamber;
import mekanism.common.tile.TileEntityFactory;
import mekanism.common.tile.TileEntityFluidTank;
import mekanism.common.tile.TileEntityFluidicPlenisher;
import mekanism.common.tile.TileEntityFormulaicAssemblicator;
import mekanism.common.tile.TileEntityFuelwoodHeater;
import mekanism.common.tile.TileEntityGasTank;
import mekanism.common.tile.TileEntityGlowPanel;
import mekanism.common.tile.TileEntityInductionCasing;
import mekanism.common.tile.TileEntityInductionCell;
import mekanism.common.tile.TileEntityInductionPort;
import mekanism.common.tile.TileEntityInductionProvider;
import mekanism.common.tile.TileEntityLaser;
import mekanism.common.tile.TileEntityLaserAmplifier;
import mekanism.common.tile.TileEntityLaserTractorBeam;
import mekanism.common.tile.TileEntityLogisticalSorter;
import mekanism.common.tile.TileEntityMetallurgicInfuser;
import mekanism.common.tile.TileEntityOredictionificator;
import mekanism.common.tile.TileEntityOsmiumCompressor;
import mekanism.common.tile.TileEntityPRC;
import mekanism.common.tile.TileEntityPersonalChest;
import mekanism.common.tile.TileEntityPrecisionSawmill;
import mekanism.common.tile.TileEntityPressureDisperser;
import mekanism.common.tile.TileEntityPurificationChamber;
import mekanism.common.tile.TileEntityQuantumEntangloporter;
import mekanism.common.tile.TileEntityResistiveHeater;
import mekanism.common.tile.TileEntityRotaryCondensentrator;
import mekanism.common.tile.TileEntitySecurityDesk;
import mekanism.common.tile.TileEntitySeismicVibrator;
import mekanism.common.tile.TileEntitySolarNeutronActivator;
import mekanism.common.tile.TileEntityStructuralGlass;
import mekanism.common.tile.TileEntitySuperheatingElement;
import mekanism.common.tile.TileEntityTeleporter;
import mekanism.common.tile.TileEntityThermalEvaporationBlock;
import mekanism.common.tile.TileEntityThermalEvaporationController;
import mekanism.common.tile.TileEntityThermalEvaporationValve;
import mekanism.common.tile.transmitter.TileEntityDiversionTransporter;
import mekanism.common.tile.transmitter.TileEntityLogisticalTransporter;
import mekanism.common.tile.transmitter.TileEntityMechanicalPipe;
import mekanism.common.tile.transmitter.TileEntityPressurizedTube;
import mekanism.common.tile.transmitter.TileEntityRestrictiveTransporter;
import mekanism.common.tile.transmitter.TileEntityThermodynamicConductor;
import mekanism.common.tile.transmitter.TileEntityUniversalCable;
import mekanism.common.transmitters.grid.EnergyNetwork.EnergyTransferEvent;
import mekanism.common.transmitters.grid.FluidNetwork.FluidTransferEvent;
import mekanism.common.transmitters.grid.GasNetwork.GasTransferEvent;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.StackUtils;
import mekanism.common.voice.VoiceServerManager;
import mekanism.common.world.GenHandler;
import net.minecraft.block.Block;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeProvider;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.world.ChunkDataEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLInterModComms;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.OreDictionary;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = Mekanism.MODID, useMetadata = true, guiFactory = "mekanism.client.gui.ConfigGuiFactory", acceptedMinecraftVersions = "[1.12,1.13)", version = "${version}")
@Mod.EventBusSubscriber()
public class Mekanism {

    public static final String MODID = "mekanism";
    public static final String MOD_NAME = "Mekanism";
    public static final String LOG_TAG = '[' + MOD_NAME + ']';
    public static final PlayerState playerState = new PlayerState();
    public static final Set<UUID> freeRunnerOn = new HashSet<>();
    /**
     * Mekanism Packet Pipeline
     */
    public static PacketHandler packetHandler = new PacketHandler();
    /**
     * Mekanism logger instance
     */
    public static Logger logger = LogManager.getLogger(MOD_NAME);
    /**
     * Mekanism proxy instance
     */
    @SidedProxy(clientSide = "mekanism.client.ClientProxy", serverSide = "mekanism.common.CommonProxy")
    public static CommonProxy proxy;
    /**
     * Mekanism mod instance
     */
    @Instance(MODID)
    public static Mekanism instance;
    /**
     * Mekanism hooks instance
     */
    public static MekanismHooks hooks = new MekanismHooks();
    /**
     * Mekanism configuration instances
     */
    public static Configuration configuration;
    public static Configuration configurationce;
    public static Configuration configurationgenerators;
    public static Configuration configurationtools;
    /**
     * Mekanism version number
     */
    public static Version versionNumber = new Version(999, 999, 999);
    /**
     * MultiblockManagers for various structrures
     */
    public static MultiblockManager<SynchronizedTankData> tankManager = new MultiblockManager<>("dynamicTank");
    public static MultiblockManager<SynchronizedMatrixData> matrixManager = new MultiblockManager<>("inductionMatrix");
    public static MultiblockManager<SynchronizedBoilerData> boilerManager = new MultiblockManager<>(
          "thermoelectricBoiler");
    /**
     * FrequencyManagers for various networks
     */
    public static FrequencyManager publicTeleporters = new FrequencyManager(Frequency.class, Frequency.TELEPORTER);
    public static Map<UUID, FrequencyManager> privateTeleporters = new HashMap<>();
    public static FrequencyManager publicEntangloporters = new FrequencyManager(InventoryFrequency.class, InventoryFrequency.ENTANGLOPORTER);
    public static Map<UUID, FrequencyManager> privateEntangloporters = new HashMap<>();
    public static FrequencyManager securityFrequencies = new FrequencyManager(SecurityFrequency.class, SecurityFrequency.SECURITY);
    /**
     * Mekanism creative tab
     */
    public static CreativeTabMekanism tabMekanism = new CreativeTabMekanism();
    /**
     * List of Mekanism modules loaded
     */
    public static List<IModule> modulesLoaded = new ArrayList<>();
    /**
     * The server's world tick handler.
     */
    public static CommonWorldTickHandler worldTickHandler = new CommonWorldTickHandler();
    /**
     * The Mekanism world generation handler.
     */
    public static GenHandler genHandler = new GenHandler();
    /**
     * The version of ore generation in this version of Mekanism. Increment this every time the default ore generation changes.
     */
    public static int baseWorldGenVersion = 0;
    /**
     * The VoiceServer manager for walkie talkies
     */
    public static VoiceServerManager voiceManager;
    /**
     * The GameProfile used by the dummy Mekanism player
     */
    public static GameProfile gameProfile = new GameProfile(UUID.nameUUIDFromBytes("mekanism.common".getBytes()), Mekanism.LOG_TAG);
    public static KeySync keyMap = new KeySync();
    public static Set<Coord4D> activeVibrators = new HashSet<>();

    static {
        MekanismFluids.register();
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        // Register blocks and tile entities
        MekanismBlocks.registerBlocks(event.getRegistry());
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        // Register items and itemBlocks
        MekanismItems.registerItems(event.getRegistry());
        MekanismBlocks.registerItemBlocks(event.getRegistry());
        //Integrate certain OreDictionary recipes
        registerOreDict();
    }

    @SubscribeEvent
    public static void registerEntities(RegistryEvent.Register<EntityEntry> event) {
        EntityRegistry.registerModEntity(new ResourceLocation(MODID, "ObsidianTNT"), EntityObsidianTNT.class, "ObsidianTNT", 0, Mekanism.instance, 64, 5, true);
        EntityRegistry.registerModEntity(new ResourceLocation(MODID, "Robit"), EntityRobit.class, "Robit", 1, Mekanism.instance, 64, 2, true);
        EntityRegistry.registerModEntity(new ResourceLocation(MODID, "Balloon"), EntityBalloon.class, "Balloon", 2, Mekanism.instance, 64, 1, true);
        EntityRegistry.registerModEntity(new ResourceLocation(MODID, "BabySkeleton"), EntityBabySkeleton.class, "BabySkeleton", 3, Mekanism.instance, 64, 5, true, 0xFFFFFF, 0x800080);
        EntityRegistry.registerModEntity(new ResourceLocation(MODID, "Flame"), EntityFlame.class, "Flame", 4, Mekanism.instance, 64, 5, true);
    }

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        // Register models
        proxy.registerBlockRenders();
        proxy.registerItemRenders();
    }

    @SubscribeEvent
    public static void registerSounds(RegistryEvent.Register<SoundEvent> event) {
        MekanismSounds.register(event.getRegistry());
    }

    @SubscribeEvent
    public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {
        event.getRegistry().register(new BinRecipe());
        addRecipes();
        GasConversionHandler.addDefaultGasMappings();
    }

    /**
     * Adds all in-game crafting, smelting and machine recipes.
     */
    public static void addRecipes() {
        //Furnace Recipes
        GameRegistry.addSmelting(new ItemStack(MekanismBlocks.OreBlock, 1, 0), new ItemStack(MekanismItems.Ingot, 1, 1), 1.0F);
        GameRegistry.addSmelting(new ItemStack(MekanismBlocks.OreBlock, 1, 1), new ItemStack(MekanismItems.Ingot, 1, 5), 1.0F);
        GameRegistry.addSmelting(new ItemStack(MekanismBlocks.OreBlock, 1, 2), new ItemStack(MekanismItems.Ingot, 1, 6), 1.0F);
        GameRegistry.addSmelting(new ItemStack(MekanismItems.Dust, 1, Resource.OSMIUM.ordinal()), new ItemStack(MekanismItems.Ingot, 1, 1), 0.0F);
        GameRegistry.addSmelting(new ItemStack(MekanismItems.Dust, 1, Resource.IRON.ordinal()), new ItemStack(Items.IRON_INGOT), 0.0F);
        GameRegistry.addSmelting(new ItemStack(MekanismItems.Dust, 1, Resource.GOLD.ordinal()), new ItemStack(Items.GOLD_INGOT), 0.0F);
        GameRegistry.addSmelting(new ItemStack(MekanismItems.OtherDust, 1, 1), new ItemStack(MekanismItems.Ingot, 1, 4), 0.0F);
        GameRegistry.addSmelting(new ItemStack(MekanismItems.Dust, 1, Resource.COPPER.ordinal()), new ItemStack(MekanismItems.Ingot, 1, 5), 0.0F);
        GameRegistry.addSmelting(new ItemStack(MekanismItems.Dust, 1, Resource.TIN.ordinal()), new ItemStack(MekanismItems.Ingot, 1, 6), 0.0F);

        //Enrichment Chamber Recipes
        if (MekanismConfig.current().general.machinesManager.isEnabled(MachineType.ENRICHMENT_CHAMBER)) {
            RecipeHandler.addEnrichmentChamberRecipe(new ItemStack(Blocks.OBSIDIAN), new ItemStack(MekanismItems.OtherDust, 4, 6));
            RecipeHandler.addEnrichmentChamberRecipe(new ItemStack(Items.COAL, 1, 0), new ItemStack(MekanismItems.CompressedCarbon));
            RecipeHandler.addEnrichmentChamberRecipe(new ItemStack(Items.COAL, 1, 1), new ItemStack(MekanismItems.CompressedCarbon));
            RecipeHandler.addEnrichmentChamberRecipe(new ItemStack(Items.REDSTONE), new ItemStack(MekanismItems.CompressedRedstone));
            RecipeHandler.addEnrichmentChamberRecipe(new ItemStack(Blocks.MOSSY_COBBLESTONE), new ItemStack(Blocks.COBBLESTONE));
            RecipeHandler.addEnrichmentChamberRecipe(new ItemStack(Blocks.STONE), new ItemStack(Blocks.STONEBRICK, 1, 2));
            RecipeHandler.addEnrichmentChamberRecipe(new ItemStack(Blocks.SAND), new ItemStack(Blocks.GRAVEL));
            RecipeHandler.addEnrichmentChamberRecipe(new ItemStack(Blocks.GRAVEL), new ItemStack(Blocks.COBBLESTONE));
            RecipeHandler.addEnrichmentChamberRecipe(new ItemStack(Items.GUNPOWDER), new ItemStack(Items.FLINT));
            RecipeHandler.addEnrichmentChamberRecipe(new ItemStack(Blocks.STONEBRICK, 1, 2), new ItemStack(Blocks.STONEBRICK, 1, 0));
            RecipeHandler.addEnrichmentChamberRecipe(new ItemStack(Blocks.STONEBRICK, 1, 0), new ItemStack(Blocks.STONEBRICK, 1, 3));
            RecipeHandler.addEnrichmentChamberRecipe(new ItemStack(Blocks.STONEBRICK, 1, 1), new ItemStack(Blocks.STONEBRICK, 1, 0));
            RecipeHandler.addEnrichmentChamberRecipe(new ItemStack(Blocks.GLOWSTONE), new ItemStack(Items.GLOWSTONE_DUST, 4));
            RecipeHandler.addEnrichmentChamberRecipe(new ItemStack(Blocks.CLAY), new ItemStack(Items.CLAY_BALL, 4));
            RecipeHandler.addEnrichmentChamberRecipe(new ItemStack(MekanismBlocks.SaltBlock), new ItemStack(MekanismItems.Salt, 4));
            RecipeHandler.addEnrichmentChamberRecipe(new ItemStack(Items.DIAMOND), new ItemStack(MekanismItems.CompressedDiamond));
            RecipeHandler.addEnrichmentChamberRecipe(new ItemStack(MekanismItems.Polyethene, 3, 0), new ItemStack(MekanismItems.Polyethene, 1, 2));

            for (int i = 0; i < EnumColor.DYES.length; i++) {
                RecipeHandler.addEnrichmentChamberRecipe(new ItemStack(MekanismBlocks.PlasticBlock, 1, i), new ItemStack(MekanismBlocks.SlickPlasticBlock, 1, i));
            }
        }

        //Combiner recipes
        if (MekanismConfig.current().general.machinesManager.isEnabled(MachineType.COMBINER)) {
            RecipeHandler.addCombinerRecipe(new ItemStack(Items.FLINT), new ItemStack(Blocks.COBBLESTONE), new ItemStack(Blocks.GRAVEL));
            RecipeHandler.addCombinerRecipe(new ItemStack(Items.COAL, 3), new ItemStack(Blocks.COBBLESTONE), new ItemStack(Blocks.COAL_ORE));
        }

        //Osmium Compressor Recipes
        if (MekanismConfig.current().general.machinesManager.isEnabled(MachineType.OSMIUM_COMPRESSOR)) {
            RecipeHandler.addOsmiumCompressorRecipe(new ItemStack(Items.GLOWSTONE_DUST), new ItemStack(MekanismItems.Ingot, 1, 3));
        }

        //Crusher Recipes
        if (MekanismConfig.current().general.machinesManager.isEnabled(MachineType.CRUSHER)) {
            RecipeHandler.addCrusherRecipe(new ItemStack(Items.IRON_INGOT), new ItemStack(MekanismItems.Dust, 1, Resource.IRON.ordinal()));
            RecipeHandler.addCrusherRecipe(new ItemStack(Items.GOLD_INGOT), new ItemStack(MekanismItems.Dust, 1, Resource.GOLD.ordinal()));
            RecipeHandler.addCrusherRecipe(new ItemStack(Blocks.GRAVEL), new ItemStack(Blocks.SAND));
            RecipeHandler.addCrusherRecipe(new ItemStack(Blocks.STONE), new ItemStack(Blocks.COBBLESTONE));
            RecipeHandler.addCrusherRecipe(new ItemStack(Blocks.COBBLESTONE), new ItemStack(Blocks.GRAVEL));
            RecipeHandler.addCrusherRecipe(new ItemStack(Blocks.STONEBRICK, 1, 2), new ItemStack(Blocks.STONE));
            RecipeHandler.addCrusherRecipe(new ItemStack(Blocks.STONEBRICK, 1, 0), new ItemStack(Blocks.STONEBRICK, 1, 2));
            RecipeHandler.addCrusherRecipe(new ItemStack(Blocks.STONEBRICK, 1, 3), new ItemStack(Blocks.STONEBRICK, 1, 0));
            RecipeHandler.addCrusherRecipe(new ItemStack(Items.FLINT), new ItemStack(Items.GUNPOWDER));
            RecipeHandler.addCrusherRecipe(new ItemStack(Blocks.SANDSTONE), new ItemStack(Blocks.SAND, 2));
            RecipeHandler.addCrusherRecipe(new ItemStack(Blocks.RED_SANDSTONE), new ItemStack(Blocks.SAND, 2, 1));

            for (int i = 0; i < 16; i++) {
                RecipeHandler.addCrusherRecipe(new ItemStack(Blocks.WOOL, 1, i), new ItemStack(Items.STRING, 4));
            }

            //BioFuel Crusher Recipes
            RecipeHandler.addCrusherRecipe(new ItemStack(Blocks.TALLGRASS), new ItemStack(MekanismItems.BioFuel, 4));
            RecipeHandler.addCrusherRecipe(new ItemStack(Items.REEDS), new ItemStack(MekanismItems.BioFuel, 2));
            RecipeHandler.addCrusherRecipe(new ItemStack(Items.WHEAT_SEEDS), new ItemStack(MekanismItems.BioFuel, 2));
            RecipeHandler.addCrusherRecipe(new ItemStack(Items.WHEAT), new ItemStack(MekanismItems.BioFuel, 4));
            RecipeHandler.addCrusherRecipe(new ItemStack(Items.PUMPKIN_SEEDS), new ItemStack(MekanismItems.BioFuel, 2));
            RecipeHandler.addCrusherRecipe(new ItemStack(Items.MELON_SEEDS), new ItemStack(MekanismItems.BioFuel, 2));
            RecipeHandler.addCrusherRecipe(new ItemStack(Items.APPLE), new ItemStack(MekanismItems.BioFuel, 4));
            RecipeHandler.addCrusherRecipe(new ItemStack(Items.BREAD), new ItemStack(MekanismItems.BioFuel, 4));
            RecipeHandler.addCrusherRecipe(new ItemStack(Items.POTATO), new ItemStack(MekanismItems.BioFuel, 4));
            RecipeHandler.addCrusherRecipe(new ItemStack(Items.CARROT), new ItemStack(MekanismItems.BioFuel, 4));
            RecipeHandler.addCrusherRecipe(new ItemStack(Items.ROTTEN_FLESH), new ItemStack(MekanismItems.BioFuel, 2));
            RecipeHandler.addCrusherRecipe(new ItemStack(Items.MELON), new ItemStack(MekanismItems.BioFuel, 4));
            RecipeHandler.addCrusherRecipe(new ItemStack(Blocks.PUMPKIN), new ItemStack(MekanismItems.BioFuel, 6));
            RecipeHandler.addCrusherRecipe(new ItemStack(Items.BAKED_POTATO), new ItemStack(MekanismItems.BioFuel, 4));
            RecipeHandler.addCrusherRecipe(new ItemStack(Items.POISONOUS_POTATO), new ItemStack(MekanismItems.BioFuel, 4));
            RecipeHandler.addCrusherRecipe(new ItemStack(Items.BEETROOT), new ItemStack(MekanismItems.BioFuel, 4));
            RecipeHandler.addCrusherRecipe(new ItemStack(Items.BEETROOT_SEEDS), new ItemStack(MekanismItems.BioFuel, 2));
            RecipeHandler.addCrusherRecipe(new ItemStack(Blocks.CACTUS), new ItemStack(MekanismItems.BioFuel, 2));
        }

        //Purification Chamber Recipes
        if (MekanismConfig.current().general.machinesManager.isEnabled(MachineType.PURIFICATION_CHAMBER)) {
            RecipeHandler.addPurificationChamberRecipe(new ItemStack(Blocks.GRAVEL), new ItemStack(Items.FLINT));
        }

        //Chemical Injection Chamber Recipes
        if (MekanismConfig.current().general.machinesManager.isEnabled(MachineType.CHEMICAL_INJECTION_CHAMBER)) {
            RecipeHandler.addChemicalInjectionChamberRecipe(new ItemStack(Blocks.DIRT), MekanismFluids.Water, new ItemStack(Blocks.CLAY));
            RecipeHandler.addChemicalInjectionChamberRecipe(new ItemStack(Blocks.HARDENED_CLAY), MekanismFluids.Water, new ItemStack(Blocks.CLAY));
            RecipeHandler.addChemicalInjectionChamberRecipe(new ItemStack(Items.BRICK), MekanismFluids.Water, new ItemStack(Items.CLAY_BALL));
            RecipeHandler.addChemicalInjectionChamberRecipe(new ItemStack(Items.GUNPOWDER), MekanismFluids.HydrogenChloride, new ItemStack(MekanismItems.OtherDust, 1, 3));
        }

        //Precision Sawmill Recipes
        if (MekanismConfig.current().general.machinesManager.isEnabled(MachineType.PRECISION_SAWMILL)) {
            RecipeHandler.addPrecisionSawmillRecipe(new ItemStack(Blocks.LADDER, 3), new ItemStack(Items.STICK, 7));
            RecipeHandler.addPrecisionSawmillRecipe(new ItemStack(Blocks.TORCH, 4), new ItemStack(Items.STICK), new ItemStack(Items.COAL), 1);
            RecipeHandler.addPrecisionSawmillRecipe(new ItemStack(Blocks.JUKEBOX), new ItemStack(Blocks.PLANKS, 8), new ItemStack(Items.DIAMOND), 1);
            RecipeHandler.addPrecisionSawmillRecipe(new ItemStack(Blocks.BOOKSHELF), new ItemStack(Blocks.PLANKS, 6), new ItemStack(Items.BOOK, 3), 1);
            RecipeHandler.addPrecisionSawmillRecipe(new ItemStack(Blocks.WOODEN_PRESSURE_PLATE), new ItemStack(Blocks.PLANKS, 2));
            RecipeHandler.addPrecisionSawmillRecipe(new ItemStack(Blocks.NOTEBLOCK), new ItemStack(Blocks.PLANKS, 8), new ItemStack(Items.REDSTONE), 1);
            RecipeHandler.addPrecisionSawmillRecipe(new ItemStack(Blocks.REDSTONE_TORCH), new ItemStack(Items.STICK), new ItemStack(Items.REDSTONE), 1);
            RecipeHandler.addPrecisionSawmillRecipe(new ItemStack(Blocks.CRAFTING_TABLE), new ItemStack(Blocks.PLANKS, 4));
            RecipeHandler.addPrecisionSawmillRecipe(new ItemStack(Blocks.CHEST), new ItemStack(Blocks.PLANKS, 8));
            RecipeHandler.addPrecisionSawmillRecipe(new ItemStack(Blocks.TRAPPED_CHEST), new ItemStack(Blocks.PLANKS, 8), new ItemStack(Blocks.TRIPWIRE_HOOK), 0.75);
            //Boats
            RecipeHandler.addPrecisionSawmillRecipe(new ItemStack(Items.BOAT), new ItemStack(Blocks.PLANKS, 5));
            RecipeHandler.addPrecisionSawmillRecipe(new ItemStack(Items.SPRUCE_BOAT), new ItemStack(Blocks.PLANKS, 5, 1));
            RecipeHandler.addPrecisionSawmillRecipe(new ItemStack(Items.BIRCH_BOAT), new ItemStack(Blocks.PLANKS, 5, 2));
            RecipeHandler.addPrecisionSawmillRecipe(new ItemStack(Items.JUNGLE_BOAT), new ItemStack(Blocks.PLANKS, 5, 3));
            RecipeHandler.addPrecisionSawmillRecipe(new ItemStack(Items.ACACIA_BOAT), new ItemStack(Blocks.PLANKS, 5, 4));
            RecipeHandler.addPrecisionSawmillRecipe(new ItemStack(Items.DARK_OAK_BOAT), new ItemStack(Blocks.PLANKS, 5, 5));
            //Beds
            for (int i = 0; i < 16; i++) {
                RecipeHandler.addPrecisionSawmillRecipe(new ItemStack(Items.BED, 1, i), new ItemStack(Blocks.PLANKS, 3), new ItemStack(Blocks.WOOL, 3, i), 1);
            }
            //Doors
            RecipeHandler.addPrecisionSawmillRecipe(new ItemStack(Blocks.TRAPDOOR), new ItemStack(Blocks.PLANKS, 1));
            RecipeHandler.addPrecisionSawmillRecipe(new ItemStack(Items.OAK_DOOR), new ItemStack(Blocks.PLANKS, 2, 0));
            RecipeHandler.addPrecisionSawmillRecipe(new ItemStack(Items.SPRUCE_DOOR), new ItemStack(Blocks.PLANKS, 2, 1));
            RecipeHandler.addPrecisionSawmillRecipe(new ItemStack(Items.BIRCH_DOOR), new ItemStack(Blocks.PLANKS, 2, 2));
            RecipeHandler.addPrecisionSawmillRecipe(new ItemStack(Items.JUNGLE_DOOR), new ItemStack(Blocks.PLANKS, 2, 3));
            RecipeHandler.addPrecisionSawmillRecipe(new ItemStack(Items.ACACIA_DOOR), new ItemStack(Blocks.PLANKS, 2, 4));
            RecipeHandler.addPrecisionSawmillRecipe(new ItemStack(Items.DARK_OAK_DOOR), new ItemStack(Blocks.PLANKS, 2, 5));
            //Fences
            RecipeHandler.addPrecisionSawmillRecipe(new ItemStack(Blocks.OAK_FENCE), new ItemStack(Items.STICK, 3));
            RecipeHandler.addPrecisionSawmillRecipe(new ItemStack(Blocks.SPRUCE_FENCE), new ItemStack(Items.STICK, 3));
            RecipeHandler.addPrecisionSawmillRecipe(new ItemStack(Blocks.BIRCH_FENCE), new ItemStack(Items.STICK, 3));
            RecipeHandler.addPrecisionSawmillRecipe(new ItemStack(Blocks.JUNGLE_FENCE), new ItemStack(Items.STICK, 3));
            RecipeHandler.addPrecisionSawmillRecipe(new ItemStack(Blocks.ACACIA_FENCE), new ItemStack(Items.STICK, 3));
            RecipeHandler.addPrecisionSawmillRecipe(new ItemStack(Blocks.DARK_OAK_FENCE), new ItemStack(Items.STICK, 3));
            //Fence Gates
            RecipeHandler.addPrecisionSawmillRecipe(new ItemStack(Blocks.OAK_FENCE_GATE), new ItemStack(Blocks.PLANKS, 2, 0), new ItemStack(Items.STICK, 4), 1);
            RecipeHandler.addPrecisionSawmillRecipe(new ItemStack(Blocks.SPRUCE_FENCE_GATE), new ItemStack(Blocks.PLANKS, 2, 1), new ItemStack(Items.STICK, 4), 1);
            RecipeHandler.addPrecisionSawmillRecipe(new ItemStack(Blocks.BIRCH_FENCE_GATE), new ItemStack(Blocks.PLANKS, 2, 2), new ItemStack(Items.STICK, 4), 1);
            RecipeHandler.addPrecisionSawmillRecipe(new ItemStack(Blocks.JUNGLE_FENCE_GATE), new ItemStack(Blocks.PLANKS, 2, 3), new ItemStack(Items.STICK, 4), 1);
            RecipeHandler.addPrecisionSawmillRecipe(new ItemStack(Blocks.ACACIA_FENCE_GATE), new ItemStack(Blocks.PLANKS, 2, 4), new ItemStack(Items.STICK, 4), 1);
            RecipeHandler.addPrecisionSawmillRecipe(new ItemStack(Blocks.DARK_OAK_FENCE_GATE), new ItemStack(Blocks.PLANKS, 2, 5), new ItemStack(Items.STICK, 4), 1);
        }

        if (MekanismConfig.current().general.machinesManager.isEnabled(MachineType.METALLURGIC_INFUSER)) {
            InfuseType carbon = InfuseRegistry.get("CARBON");
            InfuseType bio = InfuseRegistry.get("BIO");
            InfuseType redstone = InfuseRegistry.get("REDSTONE");
            InfuseType fungi = InfuseRegistry.get("FUNGI");
            InfuseType diamond = InfuseRegistry.get("DIAMOND");
            InfuseType obsidian = InfuseRegistry.get("OBSIDIAN");

            //Infuse objects
            InfuseRegistry.registerInfuseObject(new ItemStack(MekanismItems.BioFuel), new InfuseObject(bio, 5));
            InfuseRegistry.registerInfuseObject(new ItemStack(Items.COAL, 1, 0), new InfuseObject(carbon, 10));
            InfuseRegistry.registerInfuseObject(new ItemStack(Items.COAL, 1, 1), new InfuseObject(carbon, 20));
            InfuseRegistry.registerInfuseObject(new ItemStack(Blocks.COAL_BLOCK, 1, 0), new InfuseObject(carbon, 90));
            InfuseRegistry.registerInfuseObject(new ItemStack(MekanismBlocks.BasicBlock, 1, 3), new InfuseObject(carbon, 180));
            InfuseRegistry.registerInfuseObject(new ItemStack(MekanismItems.CompressedCarbon), new InfuseObject(carbon, 80));
            InfuseRegistry.registerInfuseObject(new ItemStack(Items.REDSTONE), new InfuseObject(redstone, 10));
            InfuseRegistry.registerInfuseObject(new ItemStack(Blocks.REDSTONE_BLOCK), new InfuseObject(redstone, 90));
            InfuseRegistry.registerInfuseObject(new ItemStack(MekanismItems.CompressedRedstone), new InfuseObject(redstone, 80));
            InfuseRegistry.registerInfuseObject(new ItemStack(Blocks.RED_MUSHROOM), new InfuseObject(fungi, 10));
            InfuseRegistry.registerInfuseObject(new ItemStack(Blocks.BROWN_MUSHROOM), new InfuseObject(fungi, 10));
            InfuseRegistry.registerInfuseObject(new ItemStack(MekanismItems.CompressedDiamond), new InfuseObject(diamond, 80));
            InfuseRegistry.registerInfuseObject(new ItemStack(MekanismItems.CompressedObsidian), new InfuseObject(obsidian, 80));

            //Metallurgic Infuser Recipes
            RecipeHandler.addMetallurgicInfuserRecipe(carbon, 10, new ItemStack(Items.IRON_INGOT), new ItemStack(MekanismItems.EnrichedIron));
            RecipeHandler.addMetallurgicInfuserRecipe(carbon, 10, new ItemStack(MekanismItems.EnrichedIron), new ItemStack(MekanismItems.OtherDust, 1, 1));
            for (ItemStack steel : OreDictionary.getOres("ingotSteel", false)) {
                RecipeHandler.addMetallurgicInfuserRecipe(redstone, 10, StackUtils.size(steel, 1), new ItemStack(MekanismItems.EnrichedAlloy));
            }
            RecipeHandler.addMetallurgicInfuserRecipe(fungi, 10, new ItemStack(Blocks.DIRT), new ItemStack(Blocks.MYCELIUM));
            RecipeHandler.addMetallurgicInfuserRecipe(bio, 10, new ItemStack(Blocks.COBBLESTONE), new ItemStack(Blocks.MOSSY_COBBLESTONE));
            RecipeHandler.addMetallurgicInfuserRecipe(bio, 10, new ItemStack(Blocks.STONEBRICK, 1, 0), new ItemStack(Blocks.STONEBRICK, 1, 1));
            RecipeHandler.addMetallurgicInfuserRecipe(bio, 10, new ItemStack(Blocks.SAND), new ItemStack(Blocks.DIRT));
            RecipeHandler.addMetallurgicInfuserRecipe(bio, 10, new ItemStack(Blocks.DIRT), new ItemStack(Blocks.DIRT, 1, 2));
            RecipeHandler.addMetallurgicInfuserRecipe(diamond, 10, new ItemStack(MekanismItems.EnrichedAlloy), new ItemStack(MekanismItems.ReinforcedAlloy));
            RecipeHandler.addMetallurgicInfuserRecipe(obsidian, 10, new ItemStack(MekanismItems.ReinforcedAlloy), new ItemStack(MekanismItems.AtomicAlloy));
        }

        //Chemical Infuser Recipes
        if (MekanismConfig.current().general.machinesManager.isEnabled(MachineType.CHEMICAL_INFUSER)) {
            RecipeHandler.addChemicalInfuserRecipe(new GasStack(MekanismFluids.Oxygen, 1), new GasStack(MekanismFluids.SulfurDioxide, 2), new GasStack(MekanismFluids.SulfurTrioxide, 2));
            RecipeHandler.addChemicalInfuserRecipe(new GasStack(MekanismFluids.SulfurTrioxide, 1), new GasStack(MekanismFluids.Water, 1), new GasStack(MekanismFluids.SulfuricAcid, 1));
            RecipeHandler.addChemicalInfuserRecipe(new GasStack(MekanismFluids.Hydrogen, 1), new GasStack(MekanismFluids.Chlorine, 1), new GasStack(MekanismFluids.HydrogenChloride, 1));
            RecipeHandler.addChemicalInfuserRecipe(new GasStack(MekanismFluids.Deuterium, 1), new GasStack(MekanismFluids.Tritium, 1), new GasStack(MekanismFluids.FusionFuel, 2));
        }

        //Electrolytic Separator Recipes
        if (MekanismConfig.current().general.machinesManager.isEnabled(MachineType.ELECTROLYTIC_SEPARATOR)) {
            RecipeHandler.addElectrolyticSeparatorRecipe(FluidRegistry.getFluidStack("water", 2), 2 * MekanismConfig.current().general.FROM_H2.val(),
                  new GasStack(MekanismFluids.Hydrogen, 2), new GasStack(MekanismFluids.Oxygen, 1));
            RecipeHandler.addElectrolyticSeparatorRecipe(FluidRegistry.getFluidStack("brine", 10), 2 * MekanismConfig.current().general.FROM_H2.val(),
                  new GasStack(MekanismFluids.Sodium, 1), new GasStack(MekanismFluids.Chlorine, 1));
            RecipeHandler.addElectrolyticSeparatorRecipe(FluidRegistry.getFluidStack("heavywater", 2), MekanismConfig.current().usage.heavyWaterElectrolysis.val(),
                  new GasStack(MekanismFluids.Deuterium, 2), new GasStack(MekanismFluids.Oxygen, 1));
        }

        //Thermal Evaporation Plant Recipes
        RecipeHandler.addThermalEvaporationRecipe(FluidRegistry.getFluidStack("water", 10), FluidRegistry.getFluidStack("brine", 1));
        RecipeHandler.addThermalEvaporationRecipe(FluidRegistry.getFluidStack("brine", 10), FluidRegistry.getFluidStack("liquidlithium", 1));

        //Chemical Crystallizer Recipes
        if (MekanismConfig.current().general.machinesManager.isEnabled(MachineType.CHEMICAL_CRYSTALLIZER)) {
            RecipeHandler.addChemicalCrystallizerRecipe(new GasStack(MekanismFluids.Lithium, 100), new ItemStack(MekanismItems.OtherDust, 1, 4));
            RecipeHandler.addChemicalCrystallizerRecipe(new GasStack(MekanismFluids.Brine, 15), new ItemStack(MekanismItems.Salt));
        }

        //T4 Processing Recipes
        for (Gas gas : GasRegistry.getRegisteredGasses()) {
            if (gas instanceof OreGas && !((OreGas) gas).isClean()) {
                OreGas oreGas = (OreGas) gas;
                if (MekanismConfig.current().general.machinesManager.isEnabled(MachineType.CHEMICAL_WASHER)) {
                    RecipeHandler.addChemicalWasherRecipe(new GasStack(oreGas, 1), new GasStack(oreGas.getCleanGas(), 1));
                }

                //do the crystallizer only if it's one of ours!
                Resource gasResource = Resource.getFromName(oreGas.getName());
                if (gasResource != null && MekanismConfig.current().general.machinesManager.isEnabled(MachineType.CHEMICAL_CRYSTALLIZER)) {
                    RecipeHandler.addChemicalCrystallizerRecipe(new GasStack(oreGas.getCleanGas(), 200), new ItemStack(MekanismItems.Crystal, 1, gasResource.ordinal()));
                }
            }
        }

        //Pressurized Reaction Chamber Recipes
        if (MekanismConfig.current().general.machinesManager.isEnabled(MachineType.PRESSURIZED_REACTION_CHAMBER)) {
            RecipeHandler.addPRCRecipe(new ItemStack(MekanismItems.BioFuel, 2), new FluidStack(FluidRegistry.WATER, 10), new GasStack(MekanismFluids.Hydrogen, 100),
                  new ItemStack(MekanismItems.Substrate), new GasStack(MekanismFluids.Ethene, 100), 0, 100);
            RecipeHandler.addPRCRecipe(new ItemStack(MekanismItems.Substrate), new FluidStack(MekanismFluids.Ethene.getFluid(), 50),
                  new GasStack(MekanismFluids.Oxygen, 10), new ItemStack(MekanismItems.Polyethene), new GasStack(MekanismFluids.Oxygen, 5), 1000, 60);
            RecipeHandler.addPRCRecipe(new ItemStack(MekanismItems.Substrate), new FluidStack(FluidRegistry.WATER, 200), new GasStack(MekanismFluids.Ethene, 100),
                  new ItemStack(MekanismItems.Substrate, 8), new GasStack(MekanismFluids.Oxygen, 10), 200, 400);
            RecipeHandler.addPRCRecipe(new ItemStack(Items.COAL, 1, OreDictionary.WILDCARD_VALUE), new FluidStack(FluidRegistry.WATER, 100), new GasStack(MekanismFluids.Oxygen, 100),
                  new ItemStack(MekanismItems.OtherDust, 1, 3), new GasStack(MekanismFluids.Hydrogen, 100), 0, 100);
        }

        //Solar Neutron Activator Recipes
        if (MekanismConfig.current().general.machinesManager.isEnabled(MachineType.SOLAR_NEUTRON_ACTIVATOR)) {
            RecipeHandler.addSolarNeutronRecipe(new GasStack(MekanismFluids.Lithium, 1), new GasStack(MekanismFluids.Tritium, 1));
        }

        //Fuel Gases
        FuelHandler.addGas(MekanismFluids.Hydrogen, 1, MekanismConfig.current().general.FROM_H2.val());
    }

    /**
     * Registers specified items with the Ore Dictionary.
     */
    public static void registerOreDict() {
        //Add specific items to ore dictionary for recipe usage in other mods.
        OreDictionary.registerOre("universalCable", MekanismUtils.getTransmitter(TransmitterType.UNIVERSAL_CABLE, BaseTier.BASIC, 1));
        OreDictionary.registerOre("battery", MekanismItems.EnergyTablet.getUnchargedItem());
        OreDictionary.registerOre("pulpWood", MekanismItems.Sawdust);
        OreDictionary.registerOre("dustWood", MekanismItems.Sawdust);
        OreDictionary.registerOre("blockSalt", MekanismBlocks.SaltBlock);

        //Alloys!
        OreDictionary.registerOre("alloyBasic", new ItemStack(Items.REDSTONE));
        OreDictionary.registerOre("alloyAdvanced", new ItemStack(MekanismItems.EnrichedAlloy));
        OreDictionary.registerOre("alloyElite", new ItemStack(MekanismItems.ReinforcedAlloy));
        OreDictionary.registerOre("alloyUltimate", new ItemStack(MekanismItems.AtomicAlloy));

        //GregoriousT?
        OreDictionary.registerOre("itemSalt", MekanismItems.Salt);
        OreDictionary.registerOre("dustSalt", MekanismItems.Salt);
        OreDictionary.registerOre("foodSalt", MekanismItems.Salt);

        OreDictionary.registerOre("dustDiamond", new ItemStack(MekanismItems.OtherDust, 1, 0));
        OreDictionary.registerOre("dustSteel", new ItemStack(MekanismItems.OtherDust, 1, 1));
        //Lead was once here
        OreDictionary.registerOre("dustSulfur", new ItemStack(MekanismItems.OtherDust, 1, 3));
        OreDictionary.registerOre("dustLithium", new ItemStack(MekanismItems.OtherDust, 1, 4));
        OreDictionary.registerOre("dustRefinedObsidian", new ItemStack(MekanismItems.OtherDust, 1, 5));
        OreDictionary.registerOre("dustObsidian", new ItemStack(MekanismItems.OtherDust, 1, 6));

        OreDictionary.registerOre("ingotRefinedObsidian", new ItemStack(MekanismItems.Ingot, 1, 0));
        OreDictionary.registerOre("ingotOsmium", new ItemStack(MekanismItems.Ingot, 1, 1));
        OreDictionary.registerOre("ingotBronze", new ItemStack(MekanismItems.Ingot, 1, 2));
        OreDictionary.registerOre("ingotRefinedGlowstone", new ItemStack(MekanismItems.Ingot, 1, 3));
        OreDictionary.registerOre("ingotSteel", new ItemStack(MekanismItems.Ingot, 1, 4));
        OreDictionary.registerOre("ingotCopper", new ItemStack(MekanismItems.Ingot, 1, 5));
        OreDictionary.registerOre("ingotTin", new ItemStack(MekanismItems.Ingot, 1, 6));

        OreDictionary.registerOre("nuggetRefinedObsidian", new ItemStack(MekanismItems.Nugget, 1, 0));
        OreDictionary.registerOre("nuggetOsmium", new ItemStack(MekanismItems.Nugget, 1, 1));
        OreDictionary.registerOre("nuggetBronze", new ItemStack(MekanismItems.Nugget, 1, 2));
        OreDictionary.registerOre("nuggetRefinedGlowstone", new ItemStack(MekanismItems.Nugget, 1, 3));
        OreDictionary.registerOre("nuggetSteel", new ItemStack(MekanismItems.Nugget, 1, 4));
        OreDictionary.registerOre("nuggetCopper", new ItemStack(MekanismItems.Nugget, 1, 5));
        OreDictionary.registerOre("nuggetTin", new ItemStack(MekanismItems.Nugget, 1, 6));

        OreDictionary.registerOre("blockOsmium", new ItemStack(MekanismBlocks.BasicBlock, 1, 0));
        OreDictionary.registerOre("blockBronze", new ItemStack(MekanismBlocks.BasicBlock, 1, 1));
        OreDictionary.registerOre("blockRefinedObsidian", new ItemStack(MekanismBlocks.BasicBlock, 1, 2));
        OreDictionary.registerOre("blockCharcoal", new ItemStack(MekanismBlocks.BasicBlock, 1, 3));
        OreDictionary.registerOre("blockRefinedGlowstone", new ItemStack(MekanismBlocks.BasicBlock, 1, 4));
        OreDictionary.registerOre("blockSteel", new ItemStack(MekanismBlocks.BasicBlock, 1, 5));
        OreDictionary.registerOre("blockCopper", new ItemStack(MekanismBlocks.BasicBlock, 1, 12));
        OreDictionary.registerOre("blockTin", new ItemStack(MekanismBlocks.BasicBlock, 1, 13));

        for (Resource resource : Resource.values()) {
            OreDictionary.registerOre("dust" + resource.getName(), new ItemStack(MekanismItems.Dust, 1, resource.ordinal()));
            OreDictionary.registerOre("dustDirty" + resource.getName(), new ItemStack(MekanismItems.DirtyDust, 1, resource.ordinal()));
            OreDictionary.registerOre("clump" + resource.getName(), new ItemStack(MekanismItems.Clump, 1, resource.ordinal()));
            OreDictionary.registerOre("shard" + resource.getName(), new ItemStack(MekanismItems.Shard, 1, resource.ordinal()));
            OreDictionary.registerOre("crystal" + resource.getName(), new ItemStack(MekanismItems.Crystal, 1, resource.ordinal()));
        }

        OreDictionary.registerOre("oreOsmium", new ItemStack(MekanismBlocks.OreBlock, 1, 0));
        OreDictionary.registerOre("oreCopper", new ItemStack(MekanismBlocks.OreBlock, 1, 1));
        OreDictionary.registerOre("oreTin", new ItemStack(MekanismBlocks.OreBlock, 1, 2));

        if (MekanismConfig.current().general.controlCircuitOreDict.val()) {
            OreDictionary.registerOre("circuitBasic", new ItemStack(MekanismItems.ControlCircuit, 1, 0));
            OreDictionary.registerOre("circuitAdvanced", new ItemStack(MekanismItems.ControlCircuit, 1, 1));
            OreDictionary.registerOre("circuitElite", new ItemStack(MekanismItems.ControlCircuit, 1, 2));
            OreDictionary.registerOre("circuitUltimate", new ItemStack(MekanismItems.ControlCircuit, 1, 3));
        }

        OreDictionary.registerOre("itemCompressedCarbon", new ItemStack(MekanismItems.CompressedCarbon));
        OreDictionary.registerOre("itemCompressedRedstone", new ItemStack(MekanismItems.CompressedRedstone));
        OreDictionary.registerOre("itemCompressedDiamond", new ItemStack(MekanismItems.CompressedDiamond));
        OreDictionary.registerOre("itemCompressedObsidian", new ItemStack(MekanismItems.CompressedObsidian));

        OreDictionary.registerOre("itemEnrichedAlloy", new ItemStack(MekanismItems.EnrichedAlloy));
        OreDictionary.registerOre("itemBioFuel", new ItemStack(MekanismItems.BioFuel));
    }

    private static void registerTileEntity(Class<? extends TileEntity> clazz, String name) {
        GameRegistry.registerTileEntity(clazz, new ResourceLocation(MODID, name));
    }

    /**
     * Adds and registers all tile entities.
     */
    private void registerTileEntities() {
        //Tile entities
        registerTileEntity(TileEntityAdvancedBoundingBlock.class, "advanced_bounding_block");
        registerTileEntity(TileEntityAdvancedFactory.class, "advanced_smelting_factory");
        registerTileEntity(TileEntityAmbientAccumulator.class, "ambient_accumulator");
        registerTileEntity(TileEntityBin.class, "bin");
        registerTileEntity(TileEntityBoilerCasing.class, "boiler_casing");
        registerTileEntity(TileEntityBoilerValve.class, "boiler_valve");
        registerTileEntity(TileEntityBoundingBlock.class, "bounding_block");
        registerTileEntity(TileEntityCardboardBox.class, "cardboard_box");
        registerTileEntity(TileEntityChargepad.class, "chargepad");
        registerTileEntity(TileEntityChemicalCrystallizer.class, "chemical_crystallizer");
        registerTileEntity(TileEntityChemicalDissolutionChamber.class, "chemical_dissolution_chamber");
        registerTileEntity(TileEntityChemicalInfuser.class, "chemical_infuser");
        registerTileEntity(TileEntityChemicalInjectionChamber.class, "chemical_injection_chamber");
        registerTileEntity(TileEntityChemicalOxidizer.class, "chemical_oxidizer");
        registerTileEntity(TileEntityChemicalWasher.class, "chemical_washer");
        registerTileEntity(TileEntityCombiner.class, "combiner");
        registerTileEntity(TileEntityCrusher.class, "crusher");
        registerTileEntity(TileEntityDigitalMiner.class, "digital_miner");
        registerTileEntity(TileEntityDiversionTransporter.class, "diversion_transporter");
        registerTileEntity(TileEntityDynamicTank.class, "dynamic_tank");
        registerTileEntity(TileEntityDynamicValve.class, "dynamic_valve");
        registerTileEntity(TileEntityElectricPump.class, "electric_pump");
        registerTileEntity(TileEntityElectrolyticSeparator.class, "electrolytic_separator");
        registerTileEntity(TileEntityEliteFactory.class, "ultimate_smelting_factory");
        registerTileEntity(TileEntityEnergizedSmelter.class, "energized_smelter");
        registerTileEntity(TileEntityEnergyCube.class, "energy_cube");
        registerTileEntity(TileEntityEnrichmentChamber.class, "enrichment_chamber");
        registerTileEntity(TileEntityFactory.class, "smelting_factory");
        registerTileEntity(TileEntityFluidTank.class, "fluid_tank");
        registerTileEntity(TileEntityFluidicPlenisher.class, "fluidic_plenisher");
        registerTileEntity(TileEntityFormulaicAssemblicator.class, "formulaic_assemblicator");
        registerTileEntity(TileEntityFuelwoodHeater.class, "fuelwood_heater");
        registerTileEntity(TileEntityGasTank.class, "gas_tank");
        registerTileEntity(TileEntityGlowPanel.class, "glow_panel");
        registerTileEntity(TileEntityInductionCasing.class, "induction_casing");
        registerTileEntity(TileEntityInductionCell.class, "induction_cell");
        registerTileEntity(TileEntityInductionPort.class, "induction_port");
        registerTileEntity(TileEntityInductionProvider.class, "induction_provider");
        registerTileEntity(TileEntityLaser.class, "laser");
        registerTileEntity(TileEntityLaserAmplifier.class, "laser_amplifier");
        registerTileEntity(TileEntityLaserTractorBeam.class, "laser_tractor_beam");
        registerTileEntity(TileEntityLogisticalSorter.class, "logistical_sorter");
        registerTileEntity(TileEntityLogisticalTransporter.class, "logistical_transporter");
        registerTileEntity(TileEntityMechanicalPipe.class, "mechanical_pipe");
        registerTileEntity(TileEntityMetallurgicInfuser.class, "metallurgic_infuser");
        registerTileEntity(TileEntityOredictionificator.class, "oredictionificator");
        registerTileEntity(TileEntityOsmiumCompressor.class, "osmium_compressor");
        registerTileEntity(TileEntityPRC.class, "pressurized_reaction_chamber");
        registerTileEntity(TileEntityPersonalChest.class, "personal_chest");
        registerTileEntity(TileEntityPrecisionSawmill.class, "precision_sawmill");
        registerTileEntity(TileEntityPressureDisperser.class, "pressure_disperser");
        registerTileEntity(TileEntityPressurizedTube.class, "pressurized_tube");
        registerTileEntity(TileEntityPurificationChamber.class, "purification_chamber");
        registerTileEntity(TileEntityQuantumEntangloporter.class, "quantum_entangloporter");
        registerTileEntity(TileEntityResistiveHeater.class, "resistive_heater");
        registerTileEntity(TileEntityRestrictiveTransporter.class, "restrictive_transporter");
        registerTileEntity(TileEntityRotaryCondensentrator.class, "rotary_condensentrator");
        registerTileEntity(TileEntitySecurityDesk.class, "security_desk");
        registerTileEntity(TileEntitySeismicVibrator.class, "seismic_vibrator");
        registerTileEntity(TileEntitySolarNeutronActivator.class, "solar_neutron_activator");
        registerTileEntity(TileEntityStructuralGlass.class, "structural_glass");
        registerTileEntity(TileEntitySuperheatingElement.class, "superheating_element");
        registerTileEntity(TileEntityTeleporter.class, "mekanism_teleporter");
        registerTileEntity(TileEntityThermalEvaporationBlock.class, "thermal_evaporation_block");
        registerTileEntity(TileEntityThermalEvaporationController.class, "thermal_evaporation_controller");
        registerTileEntity(TileEntityThermalEvaporationValve.class, "thermal_evaporation_valve");
        registerTileEntity(TileEntityThermodynamicConductor.class, "thermodynamic_conductor");
        registerTileEntity(TileEntityUniversalCable.class, "universal_cable");

        //Register the TESRs
        proxy.registerTESRs();

        MekanismDataFixers.register();
    }

    @EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        if (MekanismConfig.current().general.voiceServerEnabled.val()) {
            voiceManager.start();
        }
        CommandMek.register(event);
    }

    @EventHandler
    public void serverStopping(FMLServerStoppingEvent event) {
        if (MekanismConfig.current().general.voiceServerEnabled.val()) {
            voiceManager.stop();
        }

        //Clear all cache data
        playerState.clear();
        activeVibrators.clear();
        worldTickHandler.resetRegenChunks();
        privateTeleporters.clear();
        privateEntangloporters.clear();
        freeRunnerOn.clear();

        //Reset consistent managers
        MultiblockManager.reset();
        FrequencyManager.reset();
        TransporterManager.reset();
        PathfinderCache.reset();
        TransmitterNetworkRegistry.reset();
    }

    @EventHandler
    public void loadComplete(FMLInterModComms.IMCEvent event) {
        new IMCHandler().onIMCEvent(event.getMessages());
    }

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        //sanity check the api location if not deobf
        if (!((Boolean) Launch.blackboard.get("fml.deobfuscatedEnvironment"))) {
            String apiLocation = MekanismAPI.class.getProtectionDomain().getCodeSource().getLocation().toString();
            if (apiLocation.toLowerCase(Locale.ROOT).contains("-api.jar")) {
                proxy.throwApiPresentException();
            }
        }

        //Set the mod's configuration
        configuration = new Configuration(new File("config/mekanism/Mekanism.cfg"));
        configurationce = new Configuration(new File("config/mekanism/MekanismCE.cfg"));
        configurationgenerators = new Configuration(new File("config/mekanism/MekanismGenerators.cfg"));
        configurationtools = new Configuration(new File("config/mekanism/MekanismTools.cfg"));

        //Load configuration
        proxy.loadConfiguration();
        proxy.onConfigSync(false);

        MinecraftForge.EVENT_BUS.register(MekanismItems.GasMask);
        MinecraftForge.EVENT_BUS.register(MekanismItems.FreeRunners);

        if (Loader.isModLoaded("mcmultipart")) {
            //Set up multiparts
            new MultipartMekanism();
        } else {
            logger.info("Didn't detect MCMP, ignoring compatibility package");
        }

        Mekanism.proxy.preInit();

        //Register infuses
        InfuseRegistry.registerInfuseType(new InfuseType("CARBON", new ResourceLocation(Mekanism.MODID, "blocks/infuse/Carbon")).setTranslationKey("carbon"));
        InfuseRegistry.registerInfuseType(new InfuseType("TIN", new ResourceLocation(Mekanism.MODID, "blocks/infuse/Tin")).setTranslationKey("tin"));
        InfuseRegistry.registerInfuseType(new InfuseType("DIAMOND", new ResourceLocation(Mekanism.MODID, "blocks/infuse/Diamond")).setTranslationKey("diamond"));
        InfuseRegistry.registerInfuseType(new InfuseType("REDSTONE", new ResourceLocation(Mekanism.MODID, "blocks/infuse/Redstone")).setTranslationKey("redstone"));
        InfuseRegistry.registerInfuseType(new InfuseType("FUNGI", new ResourceLocation(Mekanism.MODID, "blocks/infuse/Fungi")).setTranslationKey("fungi"));
        InfuseRegistry.registerInfuseType(new InfuseType("BIO", new ResourceLocation(Mekanism.MODID, "blocks/infuse/Bio")).setTranslationKey("bio"));
        InfuseRegistry.registerInfuseType(new InfuseType("OBSIDIAN", new ResourceLocation(Mekanism.MODID, "blocks/infuse/Obsidian")).setTranslationKey("obsidian"));

        Capabilities.registerCapabilities();

        hooks.hookPreInit();
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        //Register the mod's world generators
        GameRegistry.registerWorldGenerator(genHandler, 1);

        //Register the mod's GUI handler
        NetworkRegistry.INSTANCE.registerGuiHandler(this, new CoreGuiHandler());

        //Register player tracker
        MinecraftForge.EVENT_BUS.register(new CommonPlayerTracker());
        MinecraftForge.EVENT_BUS.register(new CommonPlayerTickHandler());

        //Initialization notification
        logger.info("Version " + versionNumber + " initializing...");

        //Register with ForgeChunkManager
        ForgeChunkManager.setForcedChunkLoadingCallback(this, new ChunkManager());

        //Register to receive subscribed events
        MinecraftForge.EVENT_BUS.register(this);

        //Register this module's GUI handler in the simple packet protocol
        PacketSimpleGui.handlers.add(0, proxy);

        //Set up VoiceServerManager
        if (MekanismConfig.current().general.voiceServerEnabled.val()) {
            voiceManager = new VoiceServerManager();
        }

        //Register with TransmitterNetworkRegistry
        TransmitterNetworkRegistry.initiate();

        //Add baby skeleton spawner
        if (MekanismConfig.current().general.spawnBabySkeletons.val()) {
            for (Biome biome : BiomeProvider.allowedBiomes) {
                if (biome.getSpawnableList(EnumCreatureType.MONSTER) != null && biome.getSpawnableList(EnumCreatureType.MONSTER).size() > 0) {
                    EntityRegistry.addSpawn(EntityBabySkeleton.class, 40, 1, 3, EnumCreatureType.MONSTER, biome);
                }
            }
        }

        //Load this module
        registerTileEntities();

        hooks.hookInit();

        //Packet registrations
        packetHandler.initialize();

        //Load proxy
        proxy.init();

        //Completion notification
        logger.info("Loading complete.");

        //Success message
        logger.info("Mod loaded.");
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        logger.info("Fake player readout: UUID = " + gameProfile.getId().toString() + ", name = " + gameProfile.getName());

        // Add all furnace recipes to the energized smelter
        // Must happen after CraftTweaker for vanilla stuff has run.
        for (Entry<ItemStack, ItemStack> entry : FurnaceRecipes.instance().getSmeltingList().entrySet()) {
            SmeltingRecipe recipe = new SmeltingRecipe(new ItemStackInput(entry.getKey()), new ItemStackOutput(entry.getValue()));
            Recipe.ENERGIZED_SMELTER.put(recipe);
        }

        hooks.hookPostInit();

        MinecraftForge.EVENT_BUS.post(new BoxBlacklistEvent());

        logger.info("Hooking complete.");
    }

    @SubscribeEvent
    public void onEnergyTransferred(EnergyTransferEvent event) {
        try {
            packetHandler.sendToReceivers(new TransmitterUpdateMessage(PacketType.ENERGY, event.energyNetwork.firstTransmitter().coord(), event.power),
                  event.energyNetwork.getPacketRange());
        } catch (Exception ignored) {
        }
    }

    @SubscribeEvent
    public void onGasTransferred(GasTransferEvent event) {
        try {
            packetHandler.sendToReceivers(new TransmitterUpdateMessage(PacketType.GAS, event.gasNetwork.firstTransmitter().coord(), event.transferType, event.didTransfer),
                  event.gasNetwork.getPacketRange());
        } catch (Exception ignored) {
        }
    }

    @SubscribeEvent
    public void onLiquidTransferred(FluidTransferEvent event) {
        try {
            packetHandler.sendToReceivers(new TransmitterUpdateMessage(PacketType.FLUID, event.fluidNetwork.firstTransmitter().coord(), event.fluidType, event.didTransfer),
                  event.fluidNetwork.getPacketRange());
        } catch (Exception ignored) {
        }
    }

    @SubscribeEvent
    public void onTransmittersAddedEvent(TransmittersAddedEvent event) {
        try {
            packetHandler.sendToReceivers(new TransmitterUpdateMessage(PacketType.UPDATE, event.network.firstTransmitter().coord(), event.newNetwork, event.newTransmitters),
                  event.network.getPacketRange());
        } catch (Exception ignored) {
        }
    }

    @SubscribeEvent
    public void onNetworkClientRequest(NetworkClientRequest event) {
        try {
            packetHandler.sendToServer(new DataRequestMessage(Coord4D.get(event.tileEntity)));
        } catch (Exception ignored) {
        }
    }

    @SubscribeEvent
    public void onClientTickUpdate(ClientTickUpdate event) {
        try {
            if (event.operation == 0) {
                ClientTickHandler.tickingSet.remove(event.network);
            } else {
                ClientTickHandler.tickingSet.add(event.network);
            }
        } catch (Exception ignored) {
        }
    }

    @SubscribeEvent
    public void onBlacklistUpdate(BoxBlacklistEvent event) {
        event.blacklistWildcard(MekanismBlocks.CardboardBox);

        // Mekanism multiblock structures
        event.blacklistWildcard(MekanismBlocks.BoundingBlock);
        event.blacklist(MekanismBlocks.BasicBlock2, 9);   // Security Desk
        event.blacklist(MekanismBlocks.MachineBlock, 4);  // Digital Miner
        event.blacklist(MekanismBlocks.MachineBlock2, 9); // Seismic Vibrator
        event.blacklist(MekanismBlocks.MachineBlock3, 1); // Solar Neutron Activator

        // Minecraft unobtainable
        event.blacklist(Blocks.BEDROCK, 0);
        event.blacklistWildcard(Blocks.PORTAL);
        event.blacklistWildcard(Blocks.END_PORTAL);
        event.blacklistWildcard(Blocks.END_PORTAL_FRAME);

        // Minecraft multiblock structures
        event.blacklistWildcard(Blocks.BED);
        event.blacklistWildcard(Blocks.OAK_DOOR);
        event.blacklistWildcard(Blocks.SPRUCE_DOOR);
        event.blacklistWildcard(Blocks.BIRCH_DOOR);
        event.blacklistWildcard(Blocks.JUNGLE_DOOR);
        event.blacklistWildcard(Blocks.ACACIA_DOOR);
        event.blacklistWildcard(Blocks.DARK_OAK_DOOR);
        event.blacklistWildcard(Blocks.IRON_DOOR);

        //Extra Utils 2
        event.blacklistWildcard(new ResourceLocation("extrautils2", "machine"));

        //ImmEng multiblocks
        event.blacklistWildcard(new ResourceLocation("immersiveengineering", "metal_device0"));
        event.blacklistWildcard(new ResourceLocation("immersiveengineering", "metal_device1"));
        event.blacklistWildcard(new ResourceLocation("immersiveengineering", "wooden_device0"));
        event.blacklistWildcard(new ResourceLocation("immersiveengineering", "wooden_device1"));
        event.blacklistWildcard(new ResourceLocation("immersiveengineering", "connector"));
        event.blacklistWildcard(new ResourceLocation("immersiveengineering", "metal_multiblock"));

        //IC2
        event.blacklistWildcard(new ResourceLocation("ic2", "te"));

        event.blacklistMod("storagedrawers");//without packing tape, you're gonna have a bad time
        event.blacklistMod("colossalchests");

        BoxBlacklistParser.load();
    }

    @SubscribeEvent
    public void chunkSave(ChunkDataEvent.Save event) {
        if (!event.getWorld().isRemote) {
            NBTTagCompound nbtTags = event.getData();

            nbtTags.setInteger("MekanismWorldGen", baseWorldGenVersion);
            nbtTags.setInteger("MekanismUserWorldGen", MekanismConfig.current().general.userWorldGenVersion.val());
        }
    }

    @SubscribeEvent
    public synchronized void onChunkDataLoad(ChunkDataEvent.Load event) {
        if (!event.getWorld().isRemote) {
            if (MekanismConfig.current().general.enableWorldRegeneration.val()) {
                NBTTagCompound loadData = event.getData();
                if (loadData.getInteger("MekanismWorldGen") == baseWorldGenVersion &&
                    loadData.getInteger("MekanismUserWorldGen") == MekanismConfig.current().general.userWorldGenVersion.val()) {
                    return;
                }
                ChunkPos coordPair = event.getChunk().getPos();
                worldTickHandler.addRegenChunk(event.getWorld().provider.getDimension(), coordPair);
            }
        }
    }

    @SubscribeEvent
    public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (event.getModID().equals(Mekanism.MODID)) {
            proxy.loadConfiguration();
            proxy.onConfigSync(false);
        }
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        playerState.init(event.getWorld());
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        // Make sure the global fake player drops its reference to the World
        // when the server shuts down
        if (event.getWorld() instanceof WorldServer) {
            MekFakePlayer.releaseInstance(event.getWorld());
        }
    }
}
