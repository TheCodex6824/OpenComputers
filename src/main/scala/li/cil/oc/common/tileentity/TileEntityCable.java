package li.cil.oc.common.tileentity;

import li.cil.oc.Constants;
import li.cil.oc.api.Items;
import li.cil.oc.api.Network;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.network.AbstractEnvironment;
import li.cil.oc.common.block.BlockCable;
import li.cil.oc.common.capabilities.CapabilityColored;
import li.cil.oc.common.tileentity.traits.BlockChangeListener;
import li.cil.oc.common.tileentity.traits.ColoredImpl;
import li.cil.oc.common.tileentity.traits.ItemStackSerializable;
import li.cil.oc.common.tileentity.traits.NotAnalyzable;
import li.cil.oc.util.DyeUtils;
import li.cil.oc.util.ItemColorizer;
import li.cil.oc.util.WorldUtils;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;

import javax.annotation.Nullable;

public final class TileEntityCable extends AbstractTileEntityEnvironmentHost implements ItemStackSerializable, BlockChangeListener, ColoredImpl.ColoredHost, NotAnalyzable {
    // ----------------------------------------------------------------------- //
    // Persisted data.

    private final ColoredImpl colored = new ColoredCable(this);
    private final EnvironmentCable environment = new EnvironmentCable(this);

    // ----------------------------------------------------------------------- //
    // Computed data.

    // NBT tag names.
    private static final String TAG_COLORED = "color";

    private static final EnumDyeColor DEFAULT_COLOR = EnumDyeColor.SILVER;

    // ----------------------------------------------------------------------- //

    @Override
    protected Environment getEnvironment() {
        return environment;
    }

    // ----------------------------------------------------------------------- //
    // AbstractTileEntity

    @Override
    protected void readFromNBTCommon(final NBTTagCompound nbt) {
        super.readFromNBTCommon(nbt);
        nbt.setTag(TAG_COLORED, colored.serializeNBT());
    }

    @Override
    protected void writeToNBTCommon(final NBTTagCompound nbt) {
        super.writeToNBTCommon(nbt);
        colored.deserializeNBT((NBTTagInt) nbt.getTag(TAG_COLORED));
    }

    // ----------------------------------------------------------------------- //
    // TileEntity

    @Override
    public boolean hasCapability(final Capability<?> capability, @Nullable final EnumFacing facing) {
        return capability == CapabilityColored.COLORED_CAPABILITY ||
                super.hasCapability(capability, facing);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public <T> T getCapability(final Capability<T> capability, @Nullable final EnumFacing facing) {
        if (capability == CapabilityColored.COLORED_CAPABILITY)
            return (T) colored;
        return super.getCapability(capability, facing);
    }

    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        return BlockCable.computeBounds(getWorld(), getPos()).offset(getPos());
    }

    // ----------------------------------------------------------------------- //
    // BlockChangeListener

    @Override
    public void onBlockChanged(final BlockPos neighborPos) {
        // TODO Implement clever node connections.
        // Keep list of currently connected neighboring nodes, only disconnect old one
        // if it has been replaced by a different one/new one. Only do one connection
        // update per tick (schedule the update via IThreadListener).
        throw new UnsupportedOperationException("NOT YET IMPLEMENTED");
    }

    // ----------------------------------------------------------------------- //
    // ItemStackSerializable

    @Override
    public ItemStack writeItemStack() {
        final ItemStack stack = Items.get(Constants.BlockName.Cable()).createItemStack(1);
        if (colored.getColor() != DyeUtils.rgbFromDye(DEFAULT_COLOR)) {
            ItemColorizer.setColor(stack, colored.getColor());
        }
        return stack;
    }

    @Override
    public void readItemStack(final ItemStack stack) {
        if (ItemColorizer.hasColor(stack)) {
            colored.setColor(ItemColorizer.getColor(stack));
        }
    }

    // ----------------------------------------------------------------------- //
    // ColoredHost

    @Override
    public void onColorChanged() {
        if (hasWorld() && isServer()) {
            Network.joinOrCreateNetwork(this);
            final IBlockState state = getWorld().getBlockState(getPos());
            getWorld().notifyBlockUpdate(getPos(), state, state, WorldUtils.FLAG_REGULAR_UPDATE);
        }
    }

    // ----------------------------------------------------------------------- //

    private static final class ColoredCable extends ColoredImpl {
        ColoredCable(final ColoredHost host) {
            super(host);
            setColor(DyeUtils.rgbFromDye(DEFAULT_COLOR));
        }

        @Override
        public boolean consumesDye() {
            return true;
        }

        @Override
        public boolean controlsConnectivity() {
            return false;
        }
    }

    private static final class EnvironmentCable extends AbstractEnvironment {
        EnvironmentCable(final EnvironmentHost host) {
            super(host);
        }

        @Override
        protected Node createNode() {
            return Network.newNode(this, Visibility.NONE).create();
        }
    }
}