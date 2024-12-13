package com.eit.abcdframework.projectMaker;

import java.util.ArrayList;
//import java.util.Arrays;
import java.util.List;
import java.util.Map;
//import java.util.Set;

import org.json.JSONArray;
import org.springframework.stereotype.Component;

@Component
public class UtilitiesForService {
	public String formStringArray(ArrayList<String> list, String result) {
		// String result="String[] columnNames = ";

		for (int i = 0; i < list.size(); i++) {
			if (i == 0)
				result += "{";
			result += "\"" + list.get(i) + "\"";
			if (i != list.size() - 1)
				result += ",";
			if (i == list.size() - 1)
				result += "};";
		}
		return result;
	}

	public String formJSONArray(String value, String result) {

		JSONArray valueArray = new JSONArray(value);
		if (valueArray.isEmpty()) {
			return result += "new JSONArray();";
		}
		result += "new JSONArray(\"[";
		for (int i = 0; i < valueArray.length(); i++) {
			result += "\\\"" + valueArray.getString(i) + "\\\"";
			if (i >= 0 && i < valueArray.length() - 1)
				result += ",";
		}
		result += "]\");";
		return result;
	}

//	public String formStringArrayByMapForLang(ArrayList<Map<String,String>> list,String result) {
//		for(int i=0;i<list.size();i++) {
//			Map<String,String> map = list.get(i);
//			if(i==0) result +="{";
//			result+="\"";
//			
//			result+="{" + "\"english\" : " +"\"" +map.get("english") +"\"" + "," + "\"arabic\" : "+"\"" +map.get("arabic") +"\"" +"}";
//			result+="\"";
//			if(i!=list.size()-1) result+=" , ";
//			
//			if(i==list.size()-1) result+="};";
//		}
//		return result;
//	}

	public String formStringArrayByMapForLang2(ArrayList<Map<String, String>> list, String result) {
		result += "{";
		for (int i = 0; i < list.size(); i++) {
			Map<String, String> map = list.get(i);

			List<String> keys = new ArrayList<String>(map.keySet());
			for (int j = 0; j < keys.size(); j++) {
				if (j == 0)
					result += "\"{";

				result += "\\\"" + keys.get(j) + "\\\"";
				result += ":";
				result += "\\\"" + map.get(keys.get(j)) + "\\\"";

				if (j != keys.size() - 1)
					result += ",";
				if (j == keys.size() - 1)
					result += "}\"";
			}
			if (i != list.size() - 1)
				result += ",";

		}
		result += "};";
		return result;
	}

	public String formJSONObject(String value, String result) {
		value = value.replaceAll("\"", "\\\\\"");
		result += "\"" + value + "\");";
		return result;
	}

}
