package com.eit.abcdframework.serverbo;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class WhereFormation {

	private static final Logger LOGGER = LoggerFactory.getLogger(WhereFormation.class);

	public String whereCheck(String where, boolean isFunction, String displayname, JSONObject condition) {
		JSONObject whereConfig = null;
		String whereCondition = "";
		try {
			JSONObject dispObj = new JSONObject(DisplaySingleton.memoryDispObjs2.get(displayname).toString());
			whereConfig = new JSONObject(dispObj.get("whereconfig").toString());

			if (isFunction) {
				whereCondition = MapppingWhere(condition,
						whereConfig.getJSONObject("FormationOfWhere").getJSONObject("isFuction"));
			} else {

			}
			whereCondition = changeTheFormatOfQuery(where, isFunction, condition);
		} catch (Exception e) {
			LOGGER.error("Execption at {}", Thread.currentThread().getStackTrace()[0].getMethodName(), e);
		}
		return whereCondition;

	}

	private String MapppingWhere(JSONObject condition, JSONObject formation) {
		JSONObject changedWhere = new JSONObject();
		try {
			for (String data : formation.toMap().keySet()) {
				System.err.println(data);
				for (String valueofwhere : condition.toMap().keySet()) {
					System.err.println(valueofwhere);
					if (condition.getString(valueofwhere).contains(data)) {
						changedWhere.put(valueofwhere,
								condition.getString(valueofwhere).replace(data, formation.getString(data)));
					}

				}
			}
			System.err.println(condition);
		} catch (Exception e) {
			LOGGER.error("Execption at {}", Thread.currentThread().getStackTrace()[0].getMethodName(), e);
		}
		return condition.toString();

	}

	private String changeTheFormatOfQuery(String whereCondition, boolean isFunction, JSONObject condition) {
		try {
			if (isFunction) {
				if (whereCondition.contains("Add"))
					whereCondition.replace("Add", "and");
				if (whereCondition.contains("Sub"))
					whereCondition.replace("Add", "or");

				for (String data : condition.toMap().keySet()) {
					whereCondition.replace(data, condition.getString(data));
				}

			}
			System.err.println(whereCondition);
		} catch (Exception e) {
			LOGGER.error("Execption at {}", Thread.currentThread().getStackTrace()[0].getMethodName(), e);
		}
		return whereCondition;

	}
}
