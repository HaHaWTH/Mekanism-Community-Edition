package mekanism.client.gui;

import java.io.IOException;
import java.util.Arrays;
import mekanism.api.EnumColor;
import mekanism.api.TileNetworkList;
import mekanism.client.gui.element.GuiEnergyInfo;
import mekanism.client.gui.element.GuiPowerBar;
import mekanism.client.gui.element.GuiRedstoneControl;
import mekanism.client.gui.element.GuiSlot;
import mekanism.client.gui.element.GuiSlot.SlotOverlay;
import mekanism.client.gui.element.GuiSlot.SlotType;
import mekanism.client.gui.element.tab.GuiSecurityTab;
import mekanism.client.gui.element.tab.GuiSideConfigurationTab;
import mekanism.client.gui.element.tab.GuiTransporterConfigTab;
import mekanism.client.gui.element.tab.GuiUpgradeTab;
import mekanism.client.sound.SoundHandler;
import mekanism.common.Mekanism;
import mekanism.common.inventory.container.ContainerFormulaicAssemblicator;
import mekanism.common.item.ItemCraftingFormula;
import mekanism.common.network.PacketTileEntity.TileEntityMessage;
import mekanism.common.tile.TileEntityFormulaicAssemblicator;
import mekanism.common.util.LangUtils;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.MekanismUtils.ResourceType;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class GuiFormulaicAssemblicator extends GuiMekanismTile<TileEntityFormulaicAssemblicator> {

    public GuiFormulaicAssemblicator(InventoryPlayer inventory, TileEntityFormulaicAssemblicator tile) {
        super(tile, new ContainerFormulaicAssemblicator(inventory, tile));
        ResourceLocation resource = getGuiLocation();
        addGuiElement(new GuiSecurityTab(this, tileEntity, resource));
        addGuiElement(new GuiUpgradeTab(this, tileEntity, resource));
        addGuiElement(new GuiRedstoneControl(this, tileEntity, resource));
        addGuiElement(new GuiSideConfigurationTab(this, tileEntity, resource));
        addGuiElement(new GuiTransporterConfigTab(this, 34, tileEntity, resource));
        addGuiElement(new GuiPowerBar(this, tileEntity, resource, 159, 15));
        addGuiElement(new GuiEnergyInfo(() -> {
            String multiplier = MekanismUtils.getEnergyDisplay(tileEntity.energyPerTick);
            return Arrays.asList(LangUtils.localize("gui.using") + ": " + multiplier + "/t",
                  LangUtils.localize("gui.needed") + ": " + MekanismUtils.getEnergyDisplay(tileEntity.getMaxEnergy() - tileEntity.getEnergy()));
        }, this, resource));
        addGuiElement(new GuiSlot(SlotType.POWER, this, resource, 151, 75).with(SlotOverlay.POWER));
        ySize += 64;
    }

    private boolean overFillEmpty(int xAxis, int yAxis) {
        return xAxis >= 44 && xAxis <= 60 && yAxis >= 75 && yAxis <= 91;
    }

    private boolean overEncodeFormula(int xAxis, int yAxis) {
        return xAxis >= 7 && xAxis <= 21 && yAxis >= 45 && yAxis <= 59;
    }

    private boolean overCraftSingle(int xAxis, int yAxis) {
        return xAxis >= 71 && xAxis <= 87 && yAxis >= 75 && yAxis <= 91;
    }

    private boolean overCraftAvailable(int xAxis, int yAxis) {
        return xAxis >= 89 && xAxis <= 105 && yAxis >= 75 && yAxis <= 91;
    }

    private boolean overAutoMode(int xAxis, int yAxis) {
        return xAxis >= 107 && xAxis <= 123 && yAxis >= 75 && yAxis <= 91;
    }

    private boolean overStockControl(int xAxis, int yAxis) {
        return xAxis >= 26 && xAxis <= 42 && yAxis >= 75 && yAxis <= 91;
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        fontRenderer.drawString(tileEntity.getName(), (xSize / 2) - (fontRenderer.getStringWidth(tileEntity.getName()) / 2), 6, 0x404040);
        fontRenderer.drawString(LangUtils.localize("container.inventory"), 8, (ySize - 96) + 2, 0x404040);
        int xAxis = mouseX - guiLeft;
        int yAxis = mouseY - guiTop;
        if (overFillEmpty(xAxis, yAxis)) {
            drawHoveringText(LangUtils.localize("gui.fillEmpty"), xAxis, yAxis);
        } else if (overEncodeFormula(xAxis, yAxis)) {
            drawHoveringText(LangUtils.localize("gui.encodeFormula"), xAxis, yAxis);
        } else if (overCraftSingle(xAxis, yAxis)) {
            drawHoveringText(LangUtils.localize("gui.craftSingle"), xAxis, yAxis);
        } else if (overCraftAvailable(xAxis, yAxis)) {
            drawHoveringText(LangUtils.localize("gui.craftAvailable"), xAxis, yAxis);
        } else if (overAutoMode(xAxis, yAxis)) {
            drawHoveringText(LangUtils.localize("gui.autoModeToggle") + ": " + LangUtils.transOnOff(tileEntity.autoMode), xAxis, yAxis);
        } else if (overStockControl(xAxis, yAxis)) {
            drawHoveringText(LangUtils.localize("gui.stockControl") + ": " + LangUtils.transOnOff(tileEntity.stockControl), xAxis, yAxis);
        }
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(int xAxis, int yAxis) {
        if (!tileEntity.autoMode) {
            drawTexturedModalRect(guiLeft + 44, guiTop + 75, 238, overFillEmpty(xAxis, yAxis), 16);
        } else {
            drawTexturedModalRect(guiLeft + 44, guiTop + 75, 238, 32, 16, 16);
        }
        if (!tileEntity.autoMode && tileEntity.isRecipe) {
            if (canEncode()) {
                drawTexturedModalRect(guiLeft + 7, guiTop + 45, 176, overEncodeFormula(xAxis, yAxis), 14);
            } else {
                drawTexturedModalRect(guiLeft + 7, guiTop + 45, 176, 28, 14, 14);
            }
            drawTexturedModalRect(guiLeft + 71, guiTop + 75, 190, overCraftSingle(xAxis, yAxis), 16);
            drawTexturedModalRect(guiLeft + 89, guiTop + 75, 206, overCraftAvailable(xAxis, yAxis), 16);
        } else {
            drawTexturedModalRect(guiLeft + 7, guiTop + 45, 176, 28, 14, 14);
            drawTexturedModalRect(guiLeft + 71, guiTop + 75, 176 + 14, 32, 16, 16);
            drawTexturedModalRect(guiLeft + 89, guiTop + 75, 176 + 30, 32, 16, 16);
        }

        if (tileEntity.formula != null) {
            drawTexturedModalRect(guiLeft + 107, guiTop + 75, 222, overAutoMode(xAxis, yAxis), 16);
            drawTexturedModalRect(guiLeft + 26, guiTop + 75, 238, 48, overStockControl(xAxis, yAxis), 16);
        } else {
            drawTexturedModalRect(guiLeft + 107, guiTop + 75, 176 + 46, 32, 16, 16);
            drawTexturedModalRect(guiLeft + 26, guiTop + 75, 176 + 62, 48 + 32, 16, 16);
        }

        if (tileEntity.operatingTicks > 0) {
            int display = (int) ((double) tileEntity.operatingTicks * 22 / (double) tileEntity.ticksRequired);
            drawTexturedModalRect(guiLeft + 86, guiTop + 43, 176, 48, display, 16);
        }

        mc.renderEngine.bindTexture(MekanismUtils.getResource(ResourceType.GUI_ELEMENT, "GuiSlot.png"));
        drawTexturedModalRect(guiLeft + 90, guiTop + 25, tileEntity.isRecipe ? 2 : 20, 39, 14, 12);

        if (tileEntity.formula != null) {
            for (int i = 0; i < 9; i++) {
                ItemStack stack = tileEntity.formula.input.get(i);
                if (!stack.isEmpty()) {
                    Slot slot = inventorySlots.inventorySlots.get(i + 20);
                    int guiX = guiLeft + slot.xPos;
                    int guiY = guiTop + slot.yPos;
                    if (slot.getStack().isEmpty() || !tileEntity.formula.isIngredientInPos(tileEntity.getWorld(), slot.getStack(), i)) {
                        drawColorIcon(guiX, guiY, EnumColor.DARK_RED, 0.8F);
                    }
                    renderItem(stack, guiX, guiY);
                }
            }
        }
    }

    private boolean canEncode() {
        if (tileEntity.formula != null) {
            return false;
        }
        ItemStack formulaStack = tileEntity.inventory.get(TileEntityFormulaicAssemblicator.SLOT_FORMULA);
        return !formulaStack.isEmpty() && formulaStack.getItem() instanceof ItemCraftingFormula && ((ItemCraftingFormula) formulaStack.getItem()).getInventory(formulaStack) == null;
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) throws IOException {
        super.mouseClicked(mouseX, mouseY, button);
        if (button == 0) {
            int xAxis = mouseX - guiLeft;
            int yAxis = mouseY - guiTop;
            if (!tileEntity.autoMode) {
                if (overFillEmpty(xAxis, yAxis)) {
                    SoundHandler.playSound(SoundEvents.UI_BUTTON_CLICK);
                    TileNetworkList data = TileNetworkList.withContents(4);
                    Mekanism.packetHandler.sendToServer(new TileEntityMessage(tileEntity, data));
                }

                if (tileEntity.isRecipe) {
                    if (canEncode()) {
                        if (overEncodeFormula(xAxis, yAxis)) {
                            SoundHandler.playSound(SoundEvents.UI_BUTTON_CLICK);
                            TileNetworkList data = TileNetworkList.withContents(1);
                            Mekanism.packetHandler.sendToServer(new TileEntityMessage(tileEntity, data));
                        }
                    }

                    if (overCraftSingle(xAxis, yAxis)) {
                        SoundHandler.playSound(SoundEvents.UI_BUTTON_CLICK);
                        TileNetworkList data = TileNetworkList.withContents(2);
                        Mekanism.packetHandler.sendToServer(new TileEntityMessage(tileEntity, data));
                    } else if (overCraftAvailable(xAxis, yAxis)) {
                        SoundHandler.playSound(SoundEvents.UI_BUTTON_CLICK);
                        TileNetworkList data = TileNetworkList.withContents(3);
                        Mekanism.packetHandler.sendToServer(new TileEntityMessage(tileEntity, data));
                    }
                }
            }

            if (tileEntity.formula != null) {
                if (overAutoMode(xAxis, yAxis)) {
                    SoundHandler.playSound(SoundEvents.UI_BUTTON_CLICK);
                    TileNetworkList data = TileNetworkList.withContents(0);
                    Mekanism.packetHandler.sendToServer(new TileEntityMessage(tileEntity, data));
                } else if (overStockControl(xAxis, yAxis)) {
                    SoundHandler.playSound(SoundEvents.UI_BUTTON_CLICK);
                    TileNetworkList data = TileNetworkList.withContents(5);
                    Mekanism.packetHandler.sendToServer(new TileEntityMessage(tileEntity, data));
                }
            }
        }
    }

    @Override
    protected ResourceLocation getGuiLocation() {
        return MekanismUtils.getResource(ResourceType.GUI, "GuiFormulaicAssemblicator.png");
    }
}