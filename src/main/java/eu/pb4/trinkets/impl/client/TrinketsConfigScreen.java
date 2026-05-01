package eu.pb4.trinkets.impl.client;

import eu.pb4.trinkets.impl.TrinketInventoryMenu;
import eu.pb4.trinkets.impl.TrinketsConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.layouts.SpacerElement;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import java.util.function.Consumer;
import java.util.function.IntConsumer;

public class TrinketsConfigScreen extends Screen {
    private static final Identifier SCREEN_BACKGROUND = Identifier.withDefaultNamespace("textures/gui/inworld_menu_list_background.png");
    private final Screen lastScreen;
    private HeaderAndFooterLayout layout;

    public TrinketsConfigScreen(Screen previousScreen) {
        super(Component.translatable("screen.trinkets.config"));
        this.lastScreen = previousScreen;
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        this.rebuildWidgets();
    }

    protected void init() {
        this.layout = new HeaderAndFooterLayout(this);
        this.addTitle();
        this.addContents();
        this.addFooter();
        this.layout.visitWidgets(this::addRenderableWidget);
        this.repositionElements();
    }

    protected void addTitle() {
        this.layout.addTitleHeader(this.title, this.font);
    }

    protected void addContents() {
        var list = LinearLayout.vertical().spacing(4);
        list.addChild(new SpacerElement(0, 0));
        this.createButtons(list::addChild);
        list.addChild(new SpacerElement(0, 0));

        list.arrangeElements();

        var scrl = new ScrollableLayout(minecraft, list, this.layout.getContentHeight());
        list.arrangeElements();
        this.layout.addToContents(scrl);
    }

    private void createButtons(Consumer<LayoutElement> consumer) {
        {
            var buttons = LinearLayout.horizontal().spacing(4);

            buttons.addChild(
                    CycleButton.onOffBuilder(TrinketsConfig.instance.renderFirstPersonHand)
                            .withTooltip(_ -> Tooltip.create(Component.translatable("config.trinkets.render_first_person_hand.desc")))
                            .create(Component.translatable("config.trinkets.render_first_person_hand"), (_, v) -> TrinketsConfig.instance.renderFirstPersonHand = v)
            );

            buttons.addChild(
                    CycleButton.onOffBuilder(TrinketsConfig.instance.showSlotsIndicator)
                            .withTooltip(_ -> Tooltip.create(Component.translatable("config.trinkets.show_slots_indicator.desc")))
                            .create(Component.translatable("config.trinkets.show_slots_indicator"), (_, v) -> TrinketsConfig.instance.showSlotsIndicator = v)
            );

            consumer.accept(buttons);
        }

        {
            var buttons = LinearLayout.horizontal().spacing(4);

            buttons.addChild(
                    CycleButton.onOffBuilder(TrinketsConfig.instance.sidebarTrinketsSlots)
                            .withTooltip(_ -> Tooltip.create(Component.translatable("config.trinkets.sidebar_slots.desc")))
                            .create(Component.translatable("config.trinkets.sidebar_slots"), (_, v) -> TrinketsConfig.instance.sidebarTrinketsSlots = v)
            );

            buttons.addChild(
                    new IntSlider(Component.translatable("config.trinkets.sidebar_heigth"), 3, 8, TrinketsConfig.instance.sidebarHeight, (v) -> TrinketsConfig.instance.sidebarHeight = v,
                            Tooltip.create(Component.translatable("config.trinkets.sidebar_heigth.desc")))
            );

            consumer.accept(buttons);
        }

        {
            var buttons = LinearLayout.horizontal().spacing(4);

            buttons.addChild(
                    CycleButton.onOffBuilder(TrinketsConfig.instance.showSlotTooltip)
                            .withTooltip(_ -> Tooltip.create(Component.translatable("config.trinkets.show_slot_tooltip.desc")))
                            .create(Component.translatable("config.trinkets.show_slot_tooltip"), (_, v) -> TrinketsConfig.instance.showSlotTooltip = v)
            );

            buttons.addChild(
                    CycleButton.onOffBuilder(TrinketsConfig.instance.showItemTooltip)
                            .withTooltip(_ -> Tooltip.create(Component.translatable("config.trinkets.show_item_tooltip.desc")))
                            .create(Component.translatable("config.trinkets.show_item_tooltip"), (_, v) -> TrinketsConfig.instance.showItemTooltip = v)
            );

            consumer.accept(buttons);
        }
    }

    protected void addFooter() {
        this.layout.addToFooter(Button.builder(CommonComponents.GUI_DONE, (_) -> this.onClose()).width(200).build());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);

        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                SCREEN_BACKGROUND,
                0,
                this.layout.getHeaderHeight(),
                this.width, 0,
                width,
                this.layout.getContentHeight(),
                32,
                32
        );

        graphics.blit(RenderPipelines.GUI_TEXTURED, Screen.HEADER_SEPARATOR, 0, this.layout.getHeaderHeight() - 2, 0.0F, 0.0F, width, 2, 32, 2);
        graphics.blit(RenderPipelines.GUI_TEXTURED, Screen.FOOTER_SEPARATOR, 0, this.layout.getHeaderHeight() + this.layout.getContentHeight(), 0.0F, 0.0F, width, 2, 32, 2);
    }

    protected void repositionElements() {
        this.layout.arrangeElements();
    }

    public void removed() {
        this.minecraft.options.save();
    }

    public void onClose() {
        TrinketsConfig.save();
        this.minecraft.setScreen(this.lastScreen);
        if (this.minecraft.player != null) {
            ((TrinketInventoryMenu) this.minecraft.player.inventoryMenu).trinkets$updateTrinketSlots(false);
        }
    }

    private static class IntSlider extends AbstractSliderButton {
        private final int min;
        private final int max;
        private final int length;
        private final IntConsumer consumer;
        private final Component initialMessage;

        public IntSlider(Component message, int min, int max, int val, IntConsumer consumer, Tooltip tooltip) {
            this.min = min;
            this.max = max;
            this.length = (max - min);
            this.consumer = consumer;
            this.initialMessage = message;
            super(0, 0, 150, 20, CommonComponents.optionNameValue(message, Component.literal(String.valueOf(val))), (val - min) / (double) (max - min));
            this.setTooltip(tooltip);
        }

        @Override
        protected void updateMessage() {
            this.message = CommonComponents.optionNameValue(this.initialMessage, Component.literal(String.valueOf(calcValue())));
        }

        @Override
        protected void applyValue() {
            this.consumer.accept(calcValue());
        }

        private int calcValue() {
            return Mth.clamp((int) (this.min + Math.round(this.length * this.value)), this.min, this.max);
        }

        @Override
        public void onRelease(MouseButtonEvent event) {
            this.value = Math.round(this.length * this.value) / (double) this.length;
        }
    }
}
