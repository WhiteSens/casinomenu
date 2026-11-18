package com.casino.client;

import java.util.concurrent.ThreadLocalRandom;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;

public class HorseScreen extends BaseCasinoScreen {
	private static final String[] NAMES = {
			"Белый", "Светло-серый", "Серый", "Чёрный", "Коричневый", "Гнедой",
			"Рыжий", "Оранжевый", "Красный", "Золотой", "Жёлтый", "Салатовый",
			"Лаймовый", "Зелёный", "Бирюзовый", "Голубой", "Синий", "Фиолетовый",
			"Розовый"
	};
	private static final int COUNT = NAMES.length; // 19

	private static final float SLOT = 80, PITCH = 86, GRID_Y = 150;
	private static final float GRID_X = 576 - (6 * SLOT + 5 * (PITCH - SLOT)) / 2f;
	private static final float BTN_X = 416, BTN_Y = 538, BTN_W = 320, BTN_H = 48;
	private static final float BACK_X = 318, BACK_Y = 649, BACK_W = 515, BACK_H = 51;
	private static final float SPIN_MS = 4200f;

	private int highlight = -1;
	private boolean spinning;
	private long spinStart;
	private int startIdx, steps, winner;
	private String result;

	public HorseScreen(Screen parent) {
		super("Лучший конь", 0, 1152, 700, 0.84f, parent);
	}

	private static float slotX(int i) {
		return i < 18 ? GRID_X + (i % 6) * PITCH : 576 - SLOT / 2f; // последний конь по центру
	}

	private static float slotY(int i) {
		return GRID_Y + (i < 18 ? i / 6 : 3) * PITCH;
	}

	private void start() {
		if (spinning) {
			return;
		}
		ThreadLocalRandom r = ThreadLocalRandom.current();
		winner = r.nextInt(COUNT); // равновероятно для всех 19 цветов
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
			result = NAMES[winner] + " конь";
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
		tex(ctx, id("horse_panel"), 1152, 632, 0, 0, 1152, 632);
		for (int i = 0; i < COUNT; i++) {
			float x = slotX(i), y = slotY(i);
			tex(ctx, id(i == highlight ? "slot_sel" : "slot"), 40, 40, x, y, SLOT, SLOT);
			tex(ctx, id("horse_" + i), 24, 18, x + 4, y + 13, 72, 54);
		}
		if (result != null) {
			textCentered(ctx, "Выпал: " + result, 576, 500, 1.4f, 0xFFE8C77A, true);
		}
		tex(ctx, id("btn_horse"), 320, 48, BTN_X, BTN_Y, BTN_W, BTN_H);
		hover(ctx, mx, my, BTN_X, BTN_Y, BTN_W, BTN_H);
		tex(ctx, id("btn_back_wide"), 515, 51, BACK_X, BACK_Y, BACK_W, BACK_H);
		hover(ctx, mx, my, BACK_X, BACK_Y, BACK_W, BACK_H);
	}

	@Override
	protected boolean onDesignClick(float x, float y) {
		if (in(x, y, BACK_X, BACK_Y, BACK_W, BACK_H)) {
			click();
			close();
			return true;
		}
		if (in(x, y, BTN_X, BTN_Y, BTN_W, BTN_H)) {
			click();
			start();
			return true;
		}
		return false;
	}
}
