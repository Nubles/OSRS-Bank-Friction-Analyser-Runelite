package com.bankfriction;

public class BankFrictionInteraction
{
	private BankFrictionInteractionType type;
	private int itemId;
	private String itemName;
	private long timeMillis;

	public BankFrictionInteraction()
	{
		this(BankFrictionInteractionType.WITHDRAW, -1, "", 0L);
	}

	BankFrictionInteraction(BankFrictionInteractionType type, int itemId, String itemName, long timeMillis)
	{
		this.type = type;
		this.itemId = itemId;
		this.itemName = itemName == null ? "" : itemName;
		this.timeMillis = timeMillis;
	}

	public BankFrictionInteractionType getType()
	{
		return type;
	}

	public int getItemId()
	{
		return itemId;
	}

	public String getItemName()
	{
		return itemName;
	}

	public long getTimeMillis()
	{
		return timeMillis;
	}
}