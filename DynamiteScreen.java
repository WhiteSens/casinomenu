package com.casino.client;

import java.util.concurrent.ThreadLocalRandom;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;

public class DynamiteScreen extends BaseCasinoScreen {
	private static final float F = 490f / 421f; // масштаб исходной картинки панели (421x452)
	private static final float PANEL_W = 490, PANEL_H = 452 * F;
	private static final float CELL_X0 = 55 * F, CELL_Y0 = 63 * F, CELL_PITCH = 105.5f * F, CELL = 104 * F;
	private static final int COUNT = 9;
	private static final float BEST_Y = PANEL_H + 36, BTN_H = 50, BACK_Y = BEST_Y + BTN_H + 8;
	private static final float SPIN_MS = 3500f;

	private int highlight = -1;
	private boolean spinning;
	private long spinStart;
	private int startIdx, steps, winner;
	private String result;

	public DynamiteScreen(Screen parent) {
		super("Динамит", 0, PANEL_W, BACK_Y + BTN_H, 0.36f, parent);
	}

	private static float cellX(int i) {
		return CELL_X0 + (i % 3) * CELL_PITCH;
	}

	private static float cellY(int i) {
		return CELL_Y0 + (i / 3) * CELL_PITCH;
	}

	private void start() {
		if (spinning) {
			return;
		}
		ThreadLocalRandom r = ThreadLocalRandom.current();
		winner = r.nextInt(COUNT); // равновероятно для всех 9 клеток
		startIdx = highlight < 0 ? r.nextInt(COUNT) : highlight;
		steps = 2 * COUNT + Math.floorMod(winner - startIdx, COUNT);
		spinStart = System.currentTimeMillis();
		spinning = true;
		result = null;
	}

	private void update() {
		if (!spinning) {
			return;
		}
		float u = (System.currentTimeMillis() - spinStart) / SPIN_MS;
		if (u >= 1f) {
			spinning = false;
			highlight = winner;
			result = "Лучший вариант: ряд " + (winner / 3 + 1) + ", колонка " + (winner % 3 + 1);
			if (client != null) {
				client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f));
			}
			return;
		}
		float e = 1f - (1f - u) * (1f - u);
		highlight = (startIdx + (int) (steps * e)) % COUNT;
	}

	@Override
	protected void drawDesign(DrawContext ctx, float mx, float my, float delta) {
		update();
		tex(ctx, id("dynamite_panel"), 421, 452, 0, 0, PANEL_W, PANEL_H);

		if (highlight >= 0) {
			int x = (int) cellX(highlight), y = (int) cellY(highlight), c = (int) CELL, t = 5;
			ctx.fill(x, y, x + c, y + c, 0x35FFE88C);
			ctx.fill(x, y, x + c, y + t, 0xFFFFC428);
			ctx.fill(x, y + c - t, x + c, y + c, 0xFFFFC428);
			ctx.fill(x, y, x + t, y + c, 0xFFFFC428);
			ctx.fill(x + c - t, y, x + c, y + c, 0xFFFFC428);
		}
		if (result != null) {
			textCentered(ctx, result, PANEL_W / 2f, PANEL_H + 8, 1.2f, 0xFFE8C77A, true);
		}
		tex(ctx, id("btn_dynamite_best"), 490, 50, 0, BEST_Y, 490, BTN_H);
		tex(ctx, id("btn_back_wide"), 515, 51, 0, BACK_Y, 490, BTN_H);
		hover(ctx, mx, my, 0, BEST_Y, 490, BTN_H);
		hover(ctx, mx, my, 0, BACK_Y, 490, BTN_H);
	}

	@Override
	protected boolean onDesignClick(float x, float y) {
		if (in(x, y, 0, BACK_Y, 490, BTN_H)) {
			click();
			close();
			return true;
		}
		if (in(x, y, 0, BEST_Y, 490, BTN_H)) {
			click();
			start();
			return true;
		}
		return false;
	}
}
