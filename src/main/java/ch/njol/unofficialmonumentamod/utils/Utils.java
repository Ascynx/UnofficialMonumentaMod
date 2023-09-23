package ch.njol.unofficialmonumentamod.utils;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

public abstract class Utils {

	private Utils() {
	}

	/**
	 * Gets the plain display name of an items. This is used by Monumenta to distinguish items.
	 *
	 * @param itemStack An item stack
	 * @return The plain display name of the item, i.e. the value of NBT node plain.display.Name.
	 */
	public static String getPlainDisplayName(ItemStack itemStack) {
		return itemStack.getNbt() == null ? null : itemStack.getNbt().getCompound("plain").getCompound("display").getString("Name");
	}

	public static float smoothStep(float f) {
		if (f <= 0) {
			return 0;
		}
		if (f >= 1) {
			return 1;
		}
		return f * f * (3 - 2 * f);
	}

	public static float ease(float currentValue, float oldValue, float speedFactor, float minSpeed) {
		if (Math.abs(currentValue - oldValue) <= minSpeed) {
			return currentValue;
		} else {
			float speed = (currentValue - oldValue) * speedFactor;
			if (speed > 0 && speed < minSpeed) {
				speed = minSpeed;
			} else if (speed < 0 && speed > -minSpeed) {
				speed = -minSpeed;
			}
			return oldValue + speed;
		}
	}

	public static int clamp(int lowerBound, int value, int upperBound) {
		return Math.max(lowerBound, Math.min(value, upperBound));
	}

	public static float clamp(float lowerBound, float value, float upperBound) {
		return Math.max(lowerBound, Math.min(value, upperBound));
	}

	public static List<Text> getTooltip(ItemStack stack) {
		return stack.getTooltip(MinecraftClient.getInstance().player, TooltipContext.BASIC);
	}
	
	public static class Lerp {
		//copied from https://github.com/NotEnoughUpdates/NotEnoughUpdates/blob/master/src/main/java/io/github/moulberry/notenoughupdates/core/util/lerp/LerpingFloat.java under the LGPL 3.0 license
		
		private int timeSpent;
		private long lastMillis;
		private final int targetTime;
		
		private float targetValue;
		private float lerpValue;
		
		public Lerp(float initValue, int targetTime) {
			this.targetValue = this.lerpValue = initValue;
			this.targetTime = targetTime;
		}
		
		public Lerp(int initValue) {
			this(initValue, 200);
		}
		
		public void tick() {
			int lastTimeSpent = timeSpent;
			this.timeSpent += (int) (System.currentTimeMillis() - lastMillis);
			
			float lastDistPercentToTarget = lastTimeSpent / (float) targetTime;
			float distPercentToTarget = timeSpent / (float) targetTime;
			float fac = (1 - lastDistPercentToTarget) / lastDistPercentToTarget;
			
			float startValue = lerpValue - (targetValue - lerpValue) / fac;
			
			float dist = targetValue - startValue;
			if (dist == 0) return;
			
			float oldLerpValue = lerpValue;
			if (distPercentToTarget >= 1) {
				lerpValue = targetValue;
			} else {
				lerpValue = startValue + dist * distPercentToTarget;
			}
			
			if (lerpValue == oldLerpValue) {
				timeSpent = lastTimeSpent;
			} else {
				this.lastMillis = System.currentTimeMillis();
			}
		}
		
		public void resetTimer() {
			this.timeSpent = 0;
			this.lastMillis = System.currentTimeMillis();
		}
		
		public void setTarget(float targetValue) {
			this.targetValue = targetValue;
		}
		
		public void setValue(float value) {
			this.targetValue = this.lerpValue = value;
		}
		
		public float getValue() {
			return lerpValue;
		}
		
		public float getTarget() {
			return targetValue;
		}
	}

	private static final Pattern IDENTIFIER_SANITATION_PATTERN = Pattern.compile("[^a-zA-Z0-9/._-]");

	public static String sanitizeForIdentifier(String string) {
		return IDENTIFIER_SANITATION_PATTERN.matcher(string).replaceAll("_").toLowerCase(Locale.ROOT);
	}

	public static List<Formatter> getFormatters(String searchString) throws ParseException {
		List<Formatter> formatterList = new ArrayList<>();

		int lastOpenBracketIndex = -1;
		boolean ignoreNextChar = false;
		for (int i = 0; i < searchString.length(); i++) {
			char c = searchString.charAt(i);

			if (i == searchString.length()-1 && lastOpenBracketIndex != -1 && c != '}') {
				//end of string and there is still an open bracket
				throw new ParseException("Found '" + c + "'" + " but expected '}'", i);
			}

			if (ignoreNextChar) {
				ignoreNextChar = false;
				continue;
			}

			if (c == '{' && lastOpenBracketIndex == -1) {
				lastOpenBracketIndex = i;
				continue;
			} else if (c == '{') {
				//if the last open bracket is left open.
				int nextSpaceIndex = searchString.substring(lastOpenBracketIndex).indexOf(" ");
				throw new ParseException("Found " + (nextSpaceIndex == -1 ? "end of line" : "' '") + " but expected '}'", nextSpaceIndex != -1 ? nextSpaceIndex : i);
			}

			if (c == '}' && lastOpenBracketIndex != -1) {
				//get characters between curr index and last open bracket index.
				String content = searchString.substring(lastOpenBracketIndex+1, i);
				formatterList.add(new Formatter(content));
				//reset open bracket index
				lastOpenBracketIndex = -1;
				continue;
			} else if (c == '}') {
				//there was no open bracket
				throw new ParseException("Found '}' but expected '{'", i);
			}

			if (c == '\\') {
				ignoreNextChar = true;
			}
		}

		return formatterList;
	}

	public static class Formatter {
		public String match;

		Formatter(String match) {
			this.match = match;
		}

		public String replaceIn(String string, String replacedValue) {
			return string.replace("{" + match + "}", replacedValue);
		}
	}
}
