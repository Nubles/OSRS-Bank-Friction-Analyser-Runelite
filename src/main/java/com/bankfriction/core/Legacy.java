package com.bankfriction.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Reads the history saved by the first version of the plugin, which kept every account's
 * sessions together under one key, so nobody loses what was already gathered.
 */
public final class Legacy
{
	private Legacy()
	{
	}

	/** @return the old history as a journal, or null when there is nothing usable */
	public static Journal read(String saved)
	{
		if (saved == null || saved.trim().isEmpty())
		{
			return null;
		}
		try
		{
			JsonElement root = new JsonParser().parse(saved);
			if (!root.isJsonObject())
			{
				return null;
			}
			JsonObject old = root.getAsJsonObject();
			Journal journal = new Journal();
			if (old.has("itemNames") && old.get("itemNames").isJsonObject())
			{
				for (Map.Entry<String, JsonElement> e : old.getAsJsonObject("itemNames").entrySet())
				{
					try
					{
						journal.getNames().put(Integer.parseInt(e.getKey()), e.getValue().getAsString());
					}
					catch (RuntimeException ignored)
					{
						// A damaged name is not worth losing the rest for.
					}
				}
			}
			List<Visit> visits = new ArrayList<>();
			List<Long> ends = new ArrayList<>();
			for (JsonElement s : array(old, "sessions"))
			{
				JsonObject session = s.getAsJsonObject();
				long start = number(session, "startMillis");
				Visit visit = new Visit(start);
				for (JsonElement i : array(session, "interactions"))
				{
					JsonObject in = i.getAsJsonObject();
					int item = (int) number(in, "itemId");
					int t = (int) Math.max(0, number(in, "timeMillis") - start);
					String type = in.has("type") ? in.get("type").getAsString() : "";
					switch (type)
					{
						case "WITHDRAW":
							visit.add(Action.withdraw(item, t, -1, -1, false, false));
							break;
						case "DEPOSIT":
							visit.add(Action.deposit(item, t));
							break;
						case "DRAG":
							visit.add(Action.rearrange(item, t));
							break;
						case "TAB_SWITCH":
							visit.add(Action.tab(-1, t));
							break;
						default:
							break;
					}
				}
				visit.finish(number(session, "endMillis"));
				visits.add(visit);
				ends.add(Math.max(start, number(session, "endMillis")));
			}
			for (JsonElement s : array(old, "searches"))
			{
				JsonObject search = s.getAsJsonObject();
				long at = number(search, "timeMillis");
				String query = Recorder.normalize(search.has("query") ? search.get("query").getAsString() : "");
				for (int v = 0; v < visits.size() && !query.isEmpty(); v++)
				{
					Visit visit = visits.get(v);
					if (at >= visit.getStart() && at <= ends.get(v) + Recorder.IDLE_MS)
					{
						visit.add(Action.search(query, (int) (at - visit.getStart())));
						break;
					}
				}
			}
			for (Visit visit : visits)
			{
				if (!visit.isEmpty())
				{
					journal.getVisits().add(visit);
				}
			}
			journal.repair();
			return journal.getVisits().isEmpty() ? null : journal;
		}
		catch (RuntimeException e)
		{
			return null;
		}
	}

	private static JsonArray array(JsonObject o, String key)
	{
		return o.has(key) && o.get(key).isJsonArray() ? o.getAsJsonArray(key) : new JsonArray();
	}

	private static long number(JsonObject o, String key)
	{
		return o.has(key) && o.get(key).isJsonPrimitive() ? o.get(key).getAsLong() : 0L;
	}
}
