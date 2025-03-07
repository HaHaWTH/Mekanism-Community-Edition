package mekanism.common.tile.transmitter;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nonnull;
import mcmultipart.api.multipart.IMultipart;
import mekanism.api.Coord4D;
import mekanism.api.EnumColor;
import mekanism.api.IConfigurable;
import mekanism.api.TileNetworkList;
import mekanism.api.transmitters.IBlockableConnection;
import mekanism.api.transmitters.ITransmitter;
import mekanism.api.transmitters.TransmissionType;
import mekanism.common.Mekanism;
import mekanism.common.base.ITileNetwork;
import mekanism.common.block.BlockTransmitter;
import mekanism.common.block.property.PropertyConnection;
import mekanism.common.block.states.BlockStateTransmitter.TransmitterType;
import mekanism.common.block.states.BlockStateTransmitter.TransmitterType.Size;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.integration.multipart.MultipartMekanism;
import mekanism.common.integration.multipart.MultipartTileNetworkJoiner;
import mekanism.common.tier.BaseTier;
import mekanism.common.util.CapabilityUtils;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.MultipartUtils;
import mekanism.common.util.MultipartUtils.AdvancedRayTraceResult;
import mekanism.common.util.TextComponentGroup;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.client.model.obj.OBJModel.OBJProperty;
import net.minecraftforge.client.model.obj.OBJModel.OBJState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.fml.common.FMLCommonHandler;
import org.apache.commons.lang3.tuple.Pair;

public abstract class TileEntitySidedPipe extends TileEntity implements ITileNetwork, IBlockableConnection, IConfigurable, ITransmitter, ITickable {

    public int delayTicks;

    public byte currentAcceptorConnections = 0x00;
    public byte currentTransmitterConnections = 0x00;

    public boolean sendDesc = false;
    private boolean redstonePowered = false;

    private boolean redstoneReactive = false;

    public boolean forceUpdate = true;

    private boolean redstoneSet = false;

    public ConnectionType[] connectionTypes = {ConnectionType.NORMAL, ConnectionType.NORMAL, ConnectionType.NORMAL,
                                               ConnectionType.NORMAL, ConnectionType.NORMAL, ConnectionType.NORMAL};
    public TileEntity[] cachedAcceptors = new TileEntity[6];

    public static boolean connectionMapContainsSide(byte connections, EnumFacing side) {
        byte tester = (byte) (1 << side.ordinal());
        return (connections & tester) > 0;
    }

    public static byte setConnectionBit(byte connections, boolean toSet, EnumFacing side) {
        return (byte) ((connections & ~(byte) (1 << side.ordinal())) | (byte) ((toSet ? 1 : 0) << side.ordinal()));
    }

    public static ConnectionType getConnectionType(EnumFacing side, byte allConnections, byte transmitterConnections, ConnectionType[] types) {
        if (!connectionMapContainsSide(allConnections, side)) {
            return ConnectionType.NONE;
        } else if (connectionMapContainsSide(transmitterConnections, side)) {
            return ConnectionType.NORMAL;
        }
        return types[side.ordinal()];
    }

    @Override
    public void update() {
        if (getWorld().isRemote) {
            if (delayTicks == 5) {
                delayTicks = 6; /* don't refresh again */
                refreshConnections();
            } else if (delayTicks < 5) {
                delayTicks++;
            }
        } else {
            if (forceUpdate) {
                refreshConnections();
                forceUpdate = false;
            }
            if (sendDesc) {
                Mekanism.packetHandler.sendUpdatePacket(this);
                sendDesc = false;
            }
        }
    }

    public BaseTier getBaseTier() {
        return BaseTier.BASIC;
    }

    public void setBaseTier(BaseTier baseTier) {
    }

    public boolean handlesRedstone() {
        return true;
    }

    public boolean renderCenter() {
        return false;
    }

    public byte getPossibleTransmitterConnections() {
        byte connections = 0x00;
        if (handlesRedstone() && redstoneReactive && redstonePowered) {
            return connections;
        }
        for (EnumFacing side : EnumFacing.VALUES) {
            if (canConnectMutual(side)) {
                TileEntity tileEntity = MekanismUtils.getTileEntity(world, getPos().offset(side));
                if (CapabilityUtils.hasCapability(tileEntity, Capabilities.GRID_TRANSMITTER_CAPABILITY, side.getOpposite())
                    && TransmissionType.checkTransmissionType(CapabilityUtils.getCapability(tileEntity, Capabilities.GRID_TRANSMITTER_CAPABILITY, side.getOpposite()),
                      getTransmitterType().getTransmission()) && isValidTransmitter(tileEntity)) {
                    connections |= 1 << side.ordinal();
                }
            }
        }
        return connections;
    }

    public boolean getPossibleAcceptorConnection(EnumFacing side) {
        if (handlesRedstone() && redstoneReactive && redstonePowered) {
            return false;
        }
        if (canConnectMutual(side)) {
            TileEntity tileEntity = MekanismUtils.getTileEntity(world, getPos().offset(side));
            if (isValidAcceptor(tileEntity, side)) {
                if (cachedAcceptors[side.ordinal()] != tileEntity) {
                    cachedAcceptors[side.ordinal()] = tileEntity;
                    markDirtyAcceptor(side);
                }
                return true;
            }
        }
        if (cachedAcceptors[side.ordinal()] != null) {
            cachedAcceptors[side.ordinal()] = null;
            markDirtyAcceptor(side);
        }
        return false;
    }

    public boolean getPossibleTransmitterConnection(EnumFacing side) {
        if (handlesRedstone() && redstoneReactive && redstonePowered) {
            return false;
        }
        if (canConnectMutual(side)) {
            TileEntity tileEntity = MekanismUtils.getTileEntity(world, getPos().offset(side));
            return CapabilityUtils.hasCapability(tileEntity, Capabilities.GRID_TRANSMITTER_CAPABILITY, side.getOpposite())
                   && TransmissionType.checkTransmissionType(CapabilityUtils.getCapability(tileEntity, Capabilities.GRID_TRANSMITTER_CAPABILITY, side.getOpposite()),
                  getTransmitterType().getTransmission()) && isValidTransmitter(tileEntity);
        }
        return false;
    }

    public byte getPossibleAcceptorConnections() {
        byte connections = 0x00;

        if (handlesRedstone() && redstoneReactive && redstonePowered) {
            return connections;
        }

        for (EnumFacing side : EnumFacing.VALUES) {
            if (canConnectMutual(side)) {
                Coord4D coord = new Coord4D(getPos(), getWorld()).offset(side);
                if (!getWorld().isRemote && !coord.exists(getWorld())) {
                    forceUpdate = true;
                    continue;
                }

                TileEntity tileEntity = coord.getTileEntity(getWorld());
                if (isValidAcceptor(tileEntity, side)) {
                    if (cachedAcceptors[side.ordinal()] != tileEntity) {
                        cachedAcceptors[side.ordinal()] = tileEntity;
                        markDirtyAcceptor(side);
                    }
                    connections |= 1 << side.ordinal();
                    continue;
                }
            }
            if (cachedAcceptors[side.ordinal()] != null) {
                cachedAcceptors[side.ordinal()] = null;
                markDirtyAcceptor(side);
            }
        }
        return connections;
    }

    public byte getAllCurrentConnections() {
        return (byte) (currentTransmitterConnections | currentAcceptorConnections);
    }

    public boolean isValidTransmitter(TileEntity tileEntity) {
        return true;
    }

    public List<AxisAlignedBB> getCollisionBoxes() {
        List<AxisAlignedBB> list = new ArrayList<>();
        for (EnumFacing side : EnumFacing.VALUES) {
            int ord = side.ordinal();
            byte connections = getAllCurrentConnections();
            if (connectionMapContainsSide(connections, side)) {
                list.add(getTransmitterType().getSize() == Size.SMALL ? BlockTransmitter.smallSides[ord] : BlockTransmitter.largeSides[ord]);
            }
        }
        list.add(getTransmitterType().getSize() == Size.SMALL ? BlockTransmitter.smallSides[6] : BlockTransmitter.largeSides[6]);
        return list;
    }

    public abstract TransmitterType getTransmitterType();

    public List<AxisAlignedBB> getCollisionBoxes(AxisAlignedBB entityBox) {
        List<AxisAlignedBB> list = new ArrayList<>();
        for (EnumFacing side : EnumFacing.VALUES) {
            int ord = side.ordinal();
            byte connections = getAllCurrentConnections();
            if (connectionMapContainsSide(connections, side)) {
                AxisAlignedBB box = getTransmitterType().getSize() == Size.SMALL ? BlockTransmitter.smallSides[ord] : BlockTransmitter.largeSides[ord];
                if (box.intersects(entityBox)) {
                    list.add(box);
                }
            }
        }
        AxisAlignedBB box = getTransmitterType().getSize() == Size.SMALL ? BlockTransmitter.smallSides[6] : BlockTransmitter.largeSides[6];
        if (box.intersects(entityBox)) {
            list.add(box);
        }
        return list;
    }

    public abstract boolean isValidAcceptor(TileEntity tile, EnumFacing side);

    @Override
    public boolean canConnectMutual(EnumFacing side) {
        if (!canConnect(side)) {
            return false;
        }
        final BlockPos testPos = getPos().offset(side);
        final TileEntity tile = MekanismUtils.getTileEntity(world, testPos);
        if (!CapabilityUtils.hasCapability(tile, Capabilities.BLOCKABLE_CONNECTION_CAPABILITY, side.getOpposite())) {
            return true;
        }
        if (Capabilities.BLOCKABLE_CONNECTION_CAPABILITY == null) {
            return false;
        }
        return CapabilityUtils.getCapability(tile, Capabilities.BLOCKABLE_CONNECTION_CAPABILITY, side.getOpposite()).canConnect(side.getOpposite());
    }

    @Override
    public boolean canConnect(EnumFacing side) {
        if (connectionTypes[side.ordinal()] == ConnectionType.NONE) {
            return false;
        }
        if (handlesRedstone()) {
            if (!redstoneSet) {
                if (redstoneReactive) {
                    redstonePowered = MekanismUtils.isGettingPowered(getWorld(), new Coord4D(getPos(), getWorld()));
                } else {
                    redstonePowered = false;
                }
                redstoneSet = true;
            }
            if (redstoneReactive && redstonePowered) {
                return false;
            }
        }
        if (Mekanism.hooks.MCMPLoaded) {
            return MultipartMekanism.hasConnectionWith(this, side);
        }
        return true;
    }

    @Override
    public void handlePacketData(ByteBuf dataStream) throws Exception {
        if (FMLCommonHandler.instance().getEffectiveSide().isClient()) {
            currentTransmitterConnections = dataStream.readByte();
            currentAcceptorConnections = dataStream.readByte();
            for (int i = 0; i < 6; i++) {
                connectionTypes[i] = ConnectionType.values()[dataStream.readInt()];
            }
            markDirty();
            MekanismUtils.updateBlock(world, pos);
        }
    }

    @Override
    public TileNetworkList getNetworkedData(TileNetworkList data) {
        if (Mekanism.hooks.MCMPLoaded) {
            MultipartTileNetworkJoiner.addMultipartHeader(this, data, null);
        }
        data.add(currentTransmitterConnections);
        data.add(currentAcceptorConnections);
        for (int i = 0; i < 6; i++) {
            data.add(connectionTypes[i].ordinal());
        }
        return data;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbtTags) {
        super.readFromNBT(nbtTags);
        redstoneReactive = nbtTags.getBoolean("redstoneReactive");
        for (int i = 0; i < 6; i++) {
            connectionTypes[i] = ConnectionType.values()[nbtTags.getInteger("connection" + i)];
        }
    }

    @Nonnull
    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbtTags) {
        super.writeToNBT(nbtTags);
        nbtTags.setBoolean("redstoneReactive", redstoneReactive);
        for (int i = 0; i < 6; i++) {
            nbtTags.setInteger("connection" + i, connectionTypes[i].ordinal());
        }
        return nbtTags;
    }

    @Nonnull
    @Override
    public NBTTagCompound getUpdateTag() {
        NBTTagCompound nbtTags = super.getUpdateTag();
        nbtTags.setInteger("tier", getBaseTier().ordinal());
        return nbtTags;
    }

    protected void onRefresh() {
    }

    public void refreshConnections() {
        if (handlesRedstone()) {
            boolean previouslyPowered = redstonePowered;
            if (redstoneReactive) {
                redstonePowered = MekanismUtils.isGettingPowered(getWorld(), new Coord4D(getPos(), getWorld()));
            } else {
                redstonePowered = false;
            }
            //If the redstone mode changed properly update the connection to other transmitters/networks
            if (previouslyPowered != redstonePowered) {
                //Has to be markDirtyTransmitters instead of notify tile change
                // or it will not properly tell the neighboring connections that
                // it is no longer valid
                markDirtyTransmitters();
            }
            redstoneSet = true;
        }

        if (!getWorld().isRemote) {
            byte possibleTransmitters = getPossibleTransmitterConnections();
            byte possibleAcceptors = getPossibleAcceptorConnections();
            byte newlyEnabledTransmitters = 0;

            if ((possibleTransmitters | possibleAcceptors) != getAllCurrentConnections()) {
                sendDesc = true;
                if (possibleTransmitters != currentTransmitterConnections) {
                    //If they don't match get the difference
                    newlyEnabledTransmitters = (byte) (possibleTransmitters ^ currentTransmitterConnections);
                    //Now remove all bits that already where enabled so we only have the
                    // ones that are newly enabled. There is no need to recheck for a
                    // network merge on two transmitters if one is no longer accessible
                    newlyEnabledTransmitters &= ~currentTransmitterConnections;
                }
            }

            currentTransmitterConnections = possibleTransmitters;
            currentAcceptorConnections = possibleAcceptors;
            if (newlyEnabledTransmitters != 0) {
                //If any sides are now valid transmitters that were not before recheck the connection
                recheckConnections(newlyEnabledTransmitters);
            }
        }
    }

    public void refreshConnections(EnumFacing side) {
        if (!getWorld().isRemote) {
            boolean possibleTransmitter = getPossibleTransmitterConnection(side);
            boolean possibleAcceptor = getPossibleAcceptorConnection(side);
            boolean transmitterChanged = false;

            if ((possibleTransmitter || possibleAcceptor) != connectionMapContainsSide(getAllCurrentConnections(), side)) {
                sendDesc = true;
                if (possibleTransmitter != connectionMapContainsSide(currentTransmitterConnections, side)) {
                    //If it doesn't match check if it is now enabled, as we don't care about it changing to disabled
                    transmitterChanged = possibleTransmitter;
                }
            }

            currentTransmitterConnections = setConnectionBit(currentTransmitterConnections, possibleTransmitter, side);
            currentAcceptorConnections = setConnectionBit(currentAcceptorConnections, possibleAcceptor, side);
            if (transmitterChanged) {
                //If this side is now a valid transmitter and it wasn't before recheck the connection
                recheckConnection(side);
            }
        }
    }

    /**
     * Only call this from server side
     *
     * @param newlyEnabledTransmitters The transmitters that are now enabled and were not before.
     */
    protected void recheckConnections(byte newlyEnabledTransmitters) {
        //If our connectivity changed on a side and it is also a sided pipe, inform it to recheck its connections
        //This fixes pipes not reconnecting cross chunk
        for (EnumFacing side : EnumFacing.VALUES) {
            if (connectionMapContainsSide(newlyEnabledTransmitters, side)) {
                TileEntity tileEntity = MekanismUtils.getTileEntity(world, getPos().offset(side));
                if (tileEntity instanceof TileEntitySidedPipe) {
                    ((TileEntitySidedPipe) tileEntity).refreshConnections();
                }
            }
        }
    }

    /**
     * Only call this from server side
     *
     * @param side The side that a transmitter is now enabled on after having been disabled.
     */
    protected void recheckConnection(EnumFacing side) {
    }

    protected void onModeChange(EnumFacing side) {
        markDirtyAcceptor(side);
        if (getPossibleTransmitterConnections() != currentTransmitterConnections) {
            markDirtyTransmitters();
        }
        markDirty();
    }

    protected void markDirtyTransmitters() {
        notifyTileChange();
    }

    protected void markDirtyAcceptor(EnumFacing side) {
    }

    public abstract void onWorldJoin();

    public abstract void onWorldSeparate();

    @Override
    public void invalidate() {
        onWorldSeparate();
        super.invalidate();
    }

    @Override
    public void validate() {
        onWorldJoin();
        super.validate();
    }

    @Override
    public void onChunkUnload() {
        onWorldSeparate();
        super.onChunkUnload();
    }

    public void onAdded() {
        onWorldJoin();
        refreshConnections();
    }

    @Override
    public void onLoad() {
        onWorldJoin();
        if (getPossibleTransmitterConnections() != currentTransmitterConnections) {
            //Mark the transmitters as invalidated if they do not match what we have stored/calculated
            refreshConnections();
        }
        super.onLoad();
    }

    public void onNeighborTileChange(EnumFacing side) {
        refreshConnections(side);
    }

    public void onNeighborBlockChange(EnumFacing side) {
        refreshConnections();
    }

    public void onPartChanged(IMultipart part) {
        byte transmittersBefore = currentTransmitterConnections;
        refreshConnections();
        if (transmittersBefore != currentTransmitterConnections) {
            markDirtyTransmitters();
        }
    }

    public ConnectionType getConnectionType(EnumFacing side) {
        return getConnectionType(side, getAllCurrentConnections(), currentTransmitterConnections, connectionTypes);
    }

    public List<EnumFacing> getConnections(ConnectionType type) {
        List<EnumFacing> sides = new ArrayList<>();
        for (EnumFacing side : EnumFacing.VALUES) {
            if (getConnectionType(side) == type) {
                sides.add(side);
            }
        }
        return sides;
    }

    @Override
    public EnumActionResult onSneakRightClick(EntityPlayer player, EnumFacing side) {
        if (!getWorld().isRemote) {
            RayTraceResult hit = reTrace(getWorld(), getPos(), player);
            if (hit == null) {
                return EnumActionResult.PASS;
            } else {
                EnumFacing hitSide = sideHit(hit.subHit + 1);
                if (hitSide == null) {
                    if (connectionTypes[side.ordinal()] != ConnectionType.NONE && onConfigure(player, 6, side) == EnumActionResult.SUCCESS) {
                        return EnumActionResult.SUCCESS;
                    }
                    hitSide = side;
                }

                if (hitSide != null) {
                    connectionTypes[hitSide.ordinal()] = connectionTypes[hitSide.ordinal()].next();
                    sendDesc = true;
                    onModeChange(EnumFacing.byIndex(hitSide.ordinal()));

                    refreshConnections();
                    notifyTileChange();
                    player.sendMessage(new TextComponentGroup().translation("tooltip.configurator.modeChange").string(" ").translation(connectionTypes[hitSide.ordinal()].translationKey()));
                    return EnumActionResult.SUCCESS;
                } else {
                    return EnumActionResult.PASS;
                }
            }
        }
        return EnumActionResult.SUCCESS;
    }

    private RayTraceResult reTrace(World world, BlockPos pos, EntityPlayer player) {
        Pair<Vec3d, Vec3d> vecs = MultipartUtils.getRayTraceVectors(player);
        AdvancedRayTraceResult result = MultipartUtils.collisionRayTrace(getPos(), vecs.getLeft(), vecs.getRight(), getCollisionBoxes());
        return result == null ? null : result.hit;
    }

    protected EnumFacing sideHit(int boxIndex) {
        List<EnumFacing> list = new ArrayList<>();
        for (EnumFacing side : EnumFacing.VALUES) {
            byte connections = getAllCurrentConnections();
            if (connectionMapContainsSide(connections, side)) {
                list.add(side);
            }
        }
        if (boxIndex < list.size()) {
            return list.get(boxIndex);
        }
        return null;
    }

    protected EnumActionResult onConfigure(EntityPlayer player, int part, EnumFacing side) {
        return EnumActionResult.PASS;
    }

    public EnumColor getRenderColor() {
        return null;
    }

    @Override
    public EnumActionResult onRightClick(EntityPlayer player, EnumFacing side) {
        if (!getWorld().isRemote && handlesRedstone()) {
            redstoneReactive ^= true;
            refreshConnections();
            notifyTileChange();
            player.sendMessage(new TextComponentString(EnumColor.DARK_BLUE + Mekanism.LOG_TAG + EnumColor.GREY + " Redstone sensitivity turned " + EnumColor.INDIGO
                                                       + (redstoneReactive ? "on." : "off.")));
        }
        return EnumActionResult.SUCCESS;
    }

    public List<String> getVisibleGroups() {
        List<String> visible = new ArrayList<>();
        for (EnumFacing side : EnumFacing.VALUES) {
            visible.add(side.getName() + getConnectionType(side).getName().toUpperCase());
        }
        return visible;
    }

    public IBlockState getExtendedState(IBlockState state) {
        PropertyConnection connectionProp = new PropertyConnection(getAllCurrentConnections(), currentTransmitterConnections, connectionTypes, renderCenter());
        return ((IExtendedBlockState) state).withProperty(OBJProperty.INSTANCE, new OBJState(getVisibleGroups(), true)).withProperty(PropertyConnection.INSTANCE, connectionProp);
    }

    public void notifyTileChange() {
        MekanismUtils.notifyLoadedNeighborsOfTileChange(getWorld(), new Coord4D(getPos(), getWorld()));
    }

    @Override
    public boolean canRenderBreaking() {
        return false;
    }

    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability, EnumFacing facing) {
        return capability == Capabilities.CONFIGURABLE_CAPABILITY || capability == Capabilities.TILE_NETWORK_CAPABILITY
               || capability == Capabilities.BLOCKABLE_CONNECTION_CAPABILITY || super.hasCapability(capability, facing);
    }

    @Override
    public <T> T getCapability(@Nonnull Capability<T> capability, EnumFacing facing) {
        if (capability == Capabilities.CONFIGURABLE_CAPABILITY || capability == Capabilities.TILE_NETWORK_CAPABILITY
            || capability == Capabilities.BLOCKABLE_CONNECTION_CAPABILITY) {
            return (T) this;
        }
        return super.getCapability(capability, facing);
    }

    public enum ConnectionType implements IStringSerializable {
        NORMAL,
        PUSH,
        PULL,
        NONE;

        public ConnectionType next() {
            if (ordinal() == values().length - 1) {
                return NORMAL;
            }
            return values()[ordinal() + 1];
        }

        @Override
        public String getName() {
            return name().toLowerCase(Locale.ROOT);
        }

        public String translationKey() {
            return "mekanism.pipe.connectiontype." + getName();
        }
    }
}
