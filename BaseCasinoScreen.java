package com.casino.client;

import java.util.List;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Базовый экран: всё рисуется в "дизайнерских" координатах (пиксели исходных картинок),
 * которые масштабируются под размер окна.
 */
public abstract class BaseCasinoScreen extends Screen {
	protected final Screen parent;
	protected final float areaX0, areaW, areaH, widthFrac;
	protected float s = 1f, ox, oy;

	protected BaseCasinoScreen(String title, float areaX0, float areaW, float areaH, float widthFrac, Screen parent) {
		super(Text.literal(title));
		this.parent = parent;
		this.areaX0 = areaX0;
		this.areaW = areaW;
		this.areaH = areaH;
		this.widthFrac = widthFrac;
	}

	protected static Identifier id(String name) {
		return Identifier.of(CasinoMod.MOD_ID, "textures/gui/" + name + ".png");
	}

	protected void layout() {
		s = Math.min(widthFrac * width / areaW, 0.95f * height / areaH);
		ox = (width - areaW * s) / 2f - areaX0 * s;
		oy = (height - areaH * s) / 2f;
	}

	protected abstract void drawDesign(DrawContext ctx, float mx, float my, float delta);

	protected void drawOverlay(DrawContext ctx, int mouseX, int mouseY, float delta) {
	}

	protected abstract boolean onDesignClick(float x, float y);

	@Override
	public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
		super.render(ctx, mouseX, mouseY, delta);
		layout();
		MatrixStack m = ctx.getMatrices();
		m.push();
		m.translate(ox, oy, 0f);
		m.scale(s, s, 1f);
		drawDesign(ctx, (mouseX - ox) / s, (mouseY - oy) / s, delta);
		m.pop();
		drawOverlay(ctx, mouseX, mouseY, delta);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0) {
			float dx = (float) ((mouseX - ox) / s);
			float dy = (float) ((mouseY - oy) / s);
			if (onDesignClick(dx, dy)) {
				return true;
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public void close() {
		if (client != null) {
			client.setScreen(parent);
		}
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	// ---------- helpers ----------

	protected static boolean in(float px, float py, float x, float y, float w, float h) {
		return px >= x && px < x + w && py >= y && py < y + h;
	}

	protected void click() {
		if (client != null) {
			client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0f));
		}
	}

	protected void tex(DrawContext ctx, Identifier id, int tw, int th, float x, float y, float w, float h) {
		MatrixStack m = ctx.getMatrices();
		m.push();
		m.translate(x, y, 0f);
		m.scale(w / tw, h / th, 1f);
		ctx.drawTexture(RenderLayer::getGuiTextured, id, 0, 0, 0f, 0f, tw, th, tw, th);
		m.pop();
	}

	protected void hover(DrawContext ctx, float mx, float my, float x, float y, float w, float h) {
		if (in(mx, my, x, y, w, h)) {
			ctx.fill((int) x, (int) y, (int) (x + w), (int) (y + h), 0x30FFFFFF);
		}
	}

	/** Текст с размером mult (1.0 = обычный размер шрифта Minecraft на экране), позиция в дизайн-координатах. */
	protected void text(DrawContext ctx, String str, float x, float y, float mult, int color, boolean shadow) {
		MatrixStack m = ctx.getMatrices();
		float k = mult / s;
		m.push();
		m.translate(x, y, 0f);
		m.scale(k, k, 1f);
		ctx.drawText(textRenderer, str, 0, 0, color, shadow);
		m.pop();
	}

	protected float textWidth(String str, float mult) {
		return textRenderer.getWidth(str) * mult / s;
	}

	protected void textCentered(DrawContext ctx, String str, float cx, float y, float mult, int color, boolean shadow) {
		text(ctx, str, cx - textWidth(str, mult) / 2f, y, mult, color, shadow);
	}

	/** Многострочный текст по центру прямоугольника; размер подбирается, чтобы всё влезло. */
	protected void textWrapped(DrawContext ctx, String str, float x, float y, float w, float h,
							   float maxMult, float minMult, int color, boolean shadow) {
		float mult = maxMult;
		List<OrderedText> lines;
		while (true) {
			int wrapW = Math.max(10, (int) (w * s / mult));
			lines = textRenderer.wrapLines(Text.literal(str), wrapW);
			float total = lines.size() * 10f * mult / s;
			if (total <= h || mult <= minMult) {
				break;
			}
			mult -= 0.1f;
		}
		float lh = 10f * mult / s;
		float total = lines.size() * lh;
		float cy = y + (h - total) / 2f;
		MatrixStack m = ctx.getMatrices();
		float k = mult / s;
		for (OrderedText line : lines) {
			float lw = textRenderer.getWidth(line) * k;
			m.push();
			m.translate(x + (w - lw) / 2f, cy, 0f);
			m.scale(k, k, 1f);
			ctx.drawText(textRenderer, line, 0, 0, color, shadow);
			m.pop();
			cy += lh;
		}
	}
}
