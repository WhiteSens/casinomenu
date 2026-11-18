package com.casino.client;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.RotationAxis;

public class RouletteScreen extends BaseCasinoScreen {
	// координаты относительно roulette_panel.png (1152x632)
	private static final float WCX = 320, WCY = 354, WR = 193;
	private static final float SLOT_X = 792, SLOT_W = 293, SLOT_H = 55, SLOT_TOP = 169, SLOT_PITCH = 66.5f;
	private static final float START_X = 594, START_Y = 286, START_W = 166, START_H = 54;
	private static final float RESET_X = 594, RESET_Y = 387, RESET_W = 166, RESET_H = 52;
	private static final float ADD_X = 792, ADD_Y = 515, ADD_W = 294, ADD_H = 59;
	private static final float BACK_X = 318, BACK_Y = 649, BACK_W = 515, BACK_H = 51;
	private static final int VISIBLE = 5, MAX_OPTIONS = 24;
	private static final float SPIN_MS = 5500f;

	private final List<String> options = new ArrayList<>();
	private final TextFieldWidget[] fields = new TextFieldWidget[VISIBLE];
	private int scroll = 0;
	private boolean syncing;

	private float rot = 0f;
	private boolean spinning;
	private long spinStart, msgUntil;
	private float rotStart, rotEnd;
	private String pending, result;
	private List<String> wheelLabels = new ArrayList<>();

	public RouletteScreen(Screen parent) {
		super("Рулетка", 0, 1152, 700, 0.84f, parent);
		resetOptions();
	}

	private void resetOptions() {
		options.clear();
		options.add("ВАРИАНТ 1");
		options.add("ВАРИАНТ 2");
		options.add("ВАРИАНТ 3");
		options.add("");
		options.add("");
	}

	private List<String> activeOptions() {
		List<String> res = new ArrayList<>();
		for (String o : options) {
			String t = o.trim();
			if (!t.isEmpty()) {
				res.add(t);
			}
		}
		return res;
	}

	@Override
	protected void init() {
		layout();
		int pw = textRenderer.getWidth("24.") + 6;
		for (int k = 0; k < VISIBLE; k++) {
			final int kk = k;
			float top = SLOT_TOP + SLOT_PITCH * k;
			int fx = (int) (ox + (SLOT_X + 14) * s) + pw;
			int fw = (int) (ox + (SLOT_X + SLOT_W - 14) * s) - fx;
			TextFieldWidget f = new TextFieldWidget(textRenderer, fx, (int) (oy + top * s), fw, (int) (SLOT_H * s),
					Text.literal("Вариант"));
			f.setDrawsBackground(false);
			f.setMaxLength(40);
			f.setPlaceholder(Text.literal("..."));
			f.setChangedListener(t -> {
				if (!syncing) {
					int idx = scroll + kk;
					if (idx < options.size()) {
						options.set(idx, t);
					}
				}
			});
			fields[k] = f;
			addSelectableChild(f);
		}
		syncFields();
	}

	private void syncFields() {
		syncing = true;
		for (int k = 0; k < VISIBLE; k++) {
			int idx = scroll + k;
			if (fields[k] == null) {
				continue;
			}
			if (idx < options.size()) {
				fields[k].visible = true;
				fields[k].setText(options.get(idx));
			} else {
				fields[k].visible = false;
				fields[k].setFocused(false);
			}
		}
		syncing = false;
	}

	private void addOption() {
		if (options.size() >= MAX_OPTIONS) {
			return;
		}
		options.add("");
		scroll = Math.max(0, options.size() - VISIBLE);
		syncFields();
		int k = options.size() - 1 - scroll;
		if (k >= 0 && k < VISIBLE) {
			setFocused(fields[k]);
			fields[k].setFocused(true);
		}
	}

	private void reset() {
		resetOptions();
		scroll = 0;
		rot = 0f;
		spinning = false;
		result = null;
		pending = null;
		syncFields();
	}

	private void start() {
		if (spinning) {
			return;
		}
		List<String> act = activeOptions();
		if (act.size() < 2) {
			msgUntil = System.currentTimeMillis() + 2500;
			return;
		}
		ThreadLocalRandom r = ThreadLocalRandom.current();
		int n = act.size();
		wheelLabels = new ArrayList<>(act);
		int w = r.nextInt(n); // равновероятно для каждого варианта
		float sector = 360f / n;
		float mid = (w + 0.5f) * sector + (r.nextFloat() - 0.5f) * sector * 0.8f;
		float base = ((rot % 360f) + 360f) % 360f;
		float delta = (((-mid - base) % 360f) + 360f) % 360f;
		rotStart = rot;
		rotEnd = rot + 360f * (5 + r.nextInt(3)) + delta;
		pending = act.get(w);
		result = null;
		spinning = true;
		spinStart = System.currentTimeMillis();
	}

	private void updateSpin() {
		if (!spinning) {
			return;
		}
		float t = (System.currentTimeMillis() - spinStart) / SPIN_MS;
		if (t >= 1f) {
			spinning = false;
			rot = rotEnd % 360f;
			result = pending;
			if (client != null) {
				client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f));
			}
			return;
		}
		float e = 1f - (float) Math.pow(1f - t, 4);
		rot = rotStart + (rotEnd - rotStart) * e;
	}

	@Override
	protected void drawDesign(DrawContext ctx, float mx, float my, float delta) {
		updateSpin();
		tex(ctx, id("roulette_panel"), 1152, 632, 0, 0, 1152, 632);

		// номера в слотах списка
		for (int k = 0; k < VISIBLE; k++) {
			int idx = scroll + k;
			if (idx < options.size()) {
				float top = SLOT_TOP + SLOT_PITCH * k;
				text(ctx, (idx + 1) + ".", SLOT_X + 16, top + (SLOT_H - 9f / s) / 2f, 1.0f, 0xFFE8C77A, false);
			}
		}
		// полоса прокрутки
		if (options.size() > VISIBLE) {
			float tx = 1094, ty = 158, tw = 14, th = 416;
			ctx.fill((int) tx, (int) ty, (int) (tx + tw), (int) (ty + th), 0xFF2A2A28);
			float ratio = (float) VISIBLE / options.size();
			float thumbH = Math.max(30f, th * ratio);
			float maxScroll = options.size() - VISIBLE;
			float thumbY = ty + (th - thumbH) * (scroll / maxScroll);
			ctx.fill((int) tx + 1, (int) thumbY, (int) (tx + tw) - 1, (int) (thumbY + thumbH), 0xFF9A9A9A);
		}

		drawWheel(ctx);

		hover(ctx, mx, my, START_X, START_Y, START_W, START_H);
		hover(ctx, mx, my, RESET_X, RESET_Y, RESET_W, RESET_H);
		hover(ctx, mx, my, ADD_X, ADD_Y, ADD_W, ADD_H);

		if (System.currentTimeMillis() < msgUntil) {
			textWrapped(ctx, "Нужно минимум 2 варианта!", 588, 458, 178, 60, 1.0f, 0.6f, 0xFFFF6B5E, false);
		} else if (result != null) {
			textCentered(ctx, "Выпало:", START_X + START_W / 2f, 454, 0.9f, 0xFFE8C77A, false);
			textWrapped(ctx, result, 588, 474, 178, 48, 1.3f, 0.6f, 0xFFFFFFFF, true);
		}

		tex(ctx, id("btn_back_wide"), 515, 51, BACK_X, BACK_Y, BACK_W, BACK_H);
		hover(ctx, mx, my, BACK_X, BACK_Y, BACK_W, BACK_H);
	}

	private void drawWheel(DrawContext ctx) {
		List<String> shown;
		if (spinning) {
			shown = wheelLabels;
		} else {
			shown = activeOptions();
			wheelLabels = shown;
		}
		if (shown.size() < 2) {
			shown = List.of("?", "?");
		}
		int n = Math.min(shown.size(), MAX_OPTIONS);
		MatrixStack m = ctx.getMatrices();

		// диск с секторами
		m.push();
		m.translate(WCX, WCY, 0f);
		m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rot));
		float k = (2 * WR) / 512f;
		m.scale(k, k, 1f);
		ctx.drawTexture(RenderLayer::getGuiTextured, id("wheel_" + n), -256, -256, 0f, 0f, 512, 512, 512, 512);
		m.pop();

		// подписи вдоль радиуса
		float sector = 360f / n;
		float chord = (float) (2 * 125 * Math.sin(Math.PI / n));
		float fs = Math.min(3.0f, Math.max(0.55f, chord * 0.62f / 9f)); // дизайн-пикселей на пиксель шрифта
		int avail = (int) (118 / fs);
		for (int i = 0; i < n; i++) {
			String label = shown.get(i);
			if (textRenderer.getWidth(label) > avail) {
				label = textRenderer.trimToWidth(label, Math.max(4, avail - textRenderer.getWidth("…"))) + "…";
			}
			float tw = textRenderer.getWidth(label) * fs;
			float mid = (i + 0.5f) * sector;
			m.push();
			m.translate(WCX, WCY, 0f);
			m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rot + mid - 90f));
			m.translate(WR - 16 - tw, -4.5f * fs, 0f);
			m.scale(fs, fs, 1f);
			ctx.drawText(textRenderer, label, 0, 0, 0xFF000000, false);
			m.pop();
		}

		// центр и стрелка
		tex(ctx, id("hub"), 160, 160, WCX - 68, WCY - 68, 136, 136);
		tex(ctx, id("pointer"), 60, 64, WCX - 30, 124, 60, 64);
	}

	@Override
	protected void drawOverlay(DrawContext ctx, int mouseX, int mouseY, float delta) {
		for (TextFieldWidget f : fields) {
			if (f != null) {
				f.render(ctx, mouseX, mouseY, delta);
			}
		}
	}

	@Override
	protected boolean onDesignClick(float x, float y) {
		if (in(x, y, BACK_X, BACK_Y, BACK_W, BACK_H)) {
			click();
			close();
			return true;
		}
		if (in(x, y, START_X, START_Y, START_W, START_H)) {
			click();
			start();
			return true;
		}
		if (in(x, y, RESET_X, RESET_Y, RESET_W, RESET_H)) {
			click();
			reset();
			return true;
		}
		if (in(x, y, ADD_X, ADD_Y, ADD_W, ADD_H)) {
			click();
			addOption();
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		float dx = (float) ((mouseX - ox) / s);
		float dy = (float) ((mouseY - oy) / s);
		if (options.size() > VISIBLE && in(dx, dy, SLOT_X, SLOT_TOP, SLOT_W + 30, 360)) {
			int max = options.size() - VISIBLE;
			scroll = Math.max(0, Math.min(max, scroll - (int) Math.signum(verticalAmount)));
			syncFields();
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
	}
}
