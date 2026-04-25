package eu.pb4.trinkets.impl.client;

import eu.pb4.trinkets.impl.TrinketInventoryMenu;
import eu.pb4.trinkets.impl.TrinketsConfig;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.function.Consumer;
import java.util.function.IntConsumer;

public class TrinketsConfigScreen extends Screen {
    private final Screen lastScreen;
    public final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
    private LinearLayout list;

    public TrinketsConfigScreen(Screen previousScreen) {
        super(Component.translatable("screen.trinkets.config"));
        this.lastScreen = previousScreen;
    }

    protected void init() {
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
        this.list = LinearLayout.vertical().spacing(4);
        list.defaultCellSetting().alignHorizontallyCenter().alignVerticallyMiddle();
        this.createButtons(list::addChild);
        this.layout.addToContents(list);
        list.arrangeElements();
    }

    private void createButtons(Consumer<LayoutElement> consumer) {
        {
            var buttons = LinearLayout.horizontal().spacing(4);

            buttons.addChild(
                    CycleButton.onOffBuilder(TrinketsConfig.instance.renderFirstPersonHand)
                            .create(Component.translatable("config.trinkets.render_first_person_hand"), (_, v) -> TrinketsConfig.instance.renderFirstPersonHand = v)
            );

            buttons.addChild(
                    CycleButton.onOffBuilder(TrinketsConfig.instance.showSlotsIndicator)
                            .create(Component.translatable("config.trinkets.show_slots_indicator"), (_, v) -> TrinketsConfig.instance.showSlotsIndicator = v)
            );

            consumer.accept(buttons);
        }

        {
            var buttons = LinearLayout.horizontal().spacing(4);

            buttons.addChild(
                    CycleButton.onOffBuilder(TrinketsConfig.instance.sidebarTrinketsSlots)
                            .create(Component.translatable("config.trinkets.sidebar_slots"), (_, v) -> TrinketsConfig.instance.sidebarTrinketsSlots = v)
            );

            buttons.addChild(
                    new IntSlider(Component.translatable("config.trinkets.sidebar_heigth"), 3, 8, TrinketsConfig.instance.sidebarHeight, (v) -> TrinketsConfig.instance.sidebarHeight = v)
            );

            consumer.accept(buttons);
        }
    }

    protected void addFooter() {
        this.layout.addToFooter(Button.builder(CommonComponents.GUI_DONE, (_) -> this.onClose()).width(200).build());
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

        public IntSlider(Component message, int min, int max, int val, IntConsumer consumer) {
            this.min = min;
            this.max = max;
            this.length = (max - min);
            this.consumer = consumer;
            this.initialMessage = message;
            super(0, 0, 150, 20, CommonComponents.optionNameValue(message, Component.literal(String.valueOf(val))), (val - min) / (double) (max - min));
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
