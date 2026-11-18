package com.casino.client;

import java.util.concurrent.ThreadLocalRandom;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class FortuneScreen extends BaseCasinoScreen {
	private static final String[] ANSWERS = {
			"Да",
			"Нет",
			"Может быть",
			"Иди нахуй, заебал уже",
			"У тебя маленький хуй",
			"100%",
			"50 на 50",
			"Меньше знаешь - лучше спишь"
	};

	private enum State { IDLE, THINKING, ANSWER }

	// координаты относительно fortune_*.png (1152x632)
	private static final float BTN_X = 588, BTN_Y = 329, BTN_W = 166, BTN_H = 99;
	private static final float BACK_X = 318, BACK_Y = 649, BACK_W = 515, BACK_H = 51;
	private static final float BOX_X = 797, BOX_Y = 173, BOX_W = 300, BOX_H = 112;

	private State state = State.IDLE;
	private long thinkEnd;
	private long hintUntil;
	private String answer = "";
	private String savedQuestion = "";
	private TextFieldWidget question;

	public FortuneScreen(Screen parent) {
		super("Гадалка", 0, 1152, 700, 0.84f, parent);
	}

	@Override
	protected void init() {
		layout();
		question = new TextFieldWidget(textRenderer,
				(int) (ox + (BOX_X + 10) * s), (int) (oy + BOX_Y * s),
				(int) ((BOX_W - 20) * s), (int) (BOX_H * s), Text.literal("Вопрос"));
		question.setDrawsBackground(false);
		question.setMaxLength(120);
		question.setPlaceholder(Text.literal("Введите ваш вопрос здесь..."));
		question.setText(savedQuestion);
		question.setChangedListener(t -> savedQuestion = t);
		question.setEditable(state != State.THINKING);
		addSelectableChild(question);
		setInitialFocus(question);
	}

	private void ask() {
		if (state == State.THINKING) {
			return;
		}
		if (question.getText().trim().isEmpty()) {
			hintUntil = System.currentTimeMillis() + 2000;
			return;
		}
		state = State.THINKING;
		question.setEditable(false);
		thinkEnd = System.currentTimeMillis() + 1000 + ThreadLocalRandom.current().nextInt(1000); // 1–2 сек
	}

	private void updateState() {
		if (state == State.THINKING && System.currentTimeMillis() >= thinkEnd) {
			answer = ANSWERS[ThreadLocalRandom.current().nextInt(ANSWERS.length)];
			state = State.ANSWER;
			question.setEditable(true);
			if (client != null) {
				client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f));
			}
		}
	}

	@Override
	protected void drawDesign(DrawContext ctx, float mx, float my, float delta) {
		updateState();
		String panel = switch (state) {
			case IDLE -> "fortune_idle";
			case THINKING -> "fortune_think";
			case ANSWER -> "fortune_answer";
		};
		tex(ctx, id(panel), 1152, 632, 0, 0, 1152, 632);

		if (state == State.ANSWER) {
			textWrapped(ctx, "* " + answer + " *", 413, 175, 331, 104, 1.2f, 0.6f, 0xFFFFFFFF, false);
		}
		if (state != State.THINKING) {
			hover(ctx, mx, my, BTN_X, BTN_Y, BTN_W, BTN_H);
		}
		if (System.currentTimeMillis() < hintUntil) {
			textCentered(ctx, "Сначала введи вопрос!", BOX_X + BOX_W / 2f, 296, 0.9f, 0xFFFF6B5E, false);
		}
		tex(ctx, id("btn_back_wide"), 515, 51, BACK_X, BACK_Y, BACK_W, BACK_H);
		hover(ctx, mx, my, BACK_X, BACK_Y, BACK_W, BACK_H);
	}

	@Override
	protected void drawOverlay(DrawContext ctx, int mouseX, int mouseY, float delta) {
		question.render(ctx, mouseX, mouseY, delta);
	}

	@Override
	protected boolean onDesignClick(float x, float y) {
		if (in(x, y, BACK_X, BACK_Y, BACK_W, BACK_H)) {
			click();
			close();
			return true;
		}
		if (in(x, y, BTN_X, BTN_Y, BTN_W, BTN_H)) {
			if (state != State.THINKING) {
				click();
				ask();
			}
			return true;
		}
		return false;
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if ((keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) && question.isFocused()) {
			ask();
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}
}
