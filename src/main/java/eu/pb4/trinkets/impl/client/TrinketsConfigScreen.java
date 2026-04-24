package eu.pb4.trinkets.impl.client;

import eu.pb4.trinkets.impl.TrinketsConfig;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

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
    }
}
