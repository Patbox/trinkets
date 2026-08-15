package eu.pb4.trinkets.impl.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/**
 * Represents a separator element.
 *
 * @author LambdAurora
 */
public class SpruceSeparatorWidget extends AbstractWidget {
	public static final int TEXT_COLOR = 0xffe0e0e0;
	private final Font renderer;
	private Component title;
	private Component tooltip;
	private int tooltipTicks;
	private long lastTick;

	public SpruceSeparatorWidget(@Nullable Component title, Font renderer) {
		super(0, 0, 20, 9, title == null ? Component.empty() : title);
		this.height = 9;
		this.title = title;
		this.renderer = renderer;
	}

	/**
	 * Gets the title of this separator widget.
	 *
	 * @return the title
	 */
	public Optional<Component> getTitle() {
		return Optional.ofNullable(this.title);
	}

	/**
	 * Sets the title of this separator widget.
	 *
	 * @param title the title
	 */
	public void setTitle(@Nullable Component title) {
		this.title = title;
	}

	/* Rendering */

	@Override
	public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		if (this.title != null) {
			int titleWidth = this.renderer.width(this.title);
			int titleX = this.getX() + (this.getWidth() / 2 - titleWidth / 2);
			if (this.width > titleWidth) {
				graphics.fill(this.getX(), this.getY() + 4, titleX - 5, this.getY() + 6, TEXT_COLOR);
				graphics.fill(titleX + titleWidth + 5, this.getY() + 4, this.getX() + this.getWidth(), this.getY() + 6, TEXT_COLOR);
			}
			graphics.text(this.renderer, this.title, titleX, this.getY(), 0xFFFFFFFF);
		} else {
			graphics.fill(this.getX(), this.getY() + 4, this.getX() + this.getWidth(), this.getY() + 6, TEXT_COLOR);
		}

		//Tooltip.queueFor(this, mouseX, mouseY, this.tooltipTicks, i -> this.tooltipTicks = i, this.lastTick, i -> this.lastTick = i);
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput builder) {
		builder.add(NarratedElementType.TITLE, this.title);
	}
}