package com.casino.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class MenuScreen extends BaseCasinoScreen {
	// координаты относительно левого верхнего угла menu_panel.png (490x602)
	private static final float BX = 47, BW = 388, BH = 88;
	private static final float[] BY = {87, 200, 313, 427};
	private static final float BOT_Y = 647, BOT_W = 241, BOT_H = 50;
	private static final float BACK_X = -14, CLOSE_X = 259;

	public MenuScreen() {
		super("Казино", -14, 514, 697, 0.36f, null);
	}

	@Override
	protected void drawDesign(DrawContext ctx, float mx, float my, float delta) {
		tex(ctx, id("menu_panel"), 490, 602, 0, 0, 490, 602);
		for (float y : BY) {
			hover(ctx, mx, my, BX, y, BW, BH);
		}
		tex(ctx, id("btn_back_small"), 241, 50, BACK_X, BOT_Y, BOT_W, BOT_H);
		tex(ctx, id("btn_close_small"), 241, 50, CLOSE_X, BOT_Y, BOT_W, BOT_H);
		hover(ctx, mx, my, BACK_X, BOT_Y, BOT_W, BOT_H);
		hover(ctx, mx, my, CLOSE_X, BOT_Y, BOT_W, BOT_H);
	}

	@Override
	protected boolean onDesignClick(float x, float y) {
		if (in(x, y, BACK_X, BOT_Y, BOT_W, BOT_H) || in(x, y, CLOSE_X, BOT_Y, BOT_W, BOT_H)) {
			click();
			close();
			return true;
		}
		for (int i = 0; i < BY.length; i++) {
			if (in(x, y, BX, BY[i], BW, BH)) {
				click();
				open(i);
				return true;
			}
		}
		return false;
	}

	private void open(int i) {
		Screen next = null;
		switch (i) {
			case 0 -> next = new RouletteScreen(this);
			case 1 -> next = new FortuneScreen(this);
			case 2 -> next = new HorseScreen(this);
			default -> {
				client.setScreen(null);
				if (client.player != null) {
					client.player.sendMessage(Text.literal("Динамит: этот раздел пока в разработке."), true);
				}
				return;
			}
		}
		client.setScreen(next);
	}
}
