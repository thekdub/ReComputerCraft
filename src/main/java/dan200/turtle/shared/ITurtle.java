package dan200.turtle.shared;

public interface ITurtle {
	int issueCommand(int var1);

	int getSelectedSlot();

	int getSlotItemCount(int var1);

	int getSlotSpaceRemaining(int var1);

	void addListener(ITurtleListener var1);
}
