package com.eit.abcdframework.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.eit.abcdframework.http.caller.Httpclientcaller;

@Service
public class Location {

	@Autowired
	Httpclientcaller dataTransmit;

	@Value("${applicationurl}")
	private String pgresturl;

	private static final Logger LOGGER = LoggerFactory.getLogger(Location.class);

	public static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
		final double R = 6371.0; // Radius of the Earth in km

		double dLat = toRadians(lat2 - lat1);
		double dLon = toRadians(lon2 - lon1);

		double a = Math.pow(Math.sin(dLat / 2), 2)
				+ Math.cos(toRadians(lat1)) * Math.cos(toRadians(lat2)) * Math.pow(Math.sin(dLon / 2), 2);
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

		return R * c; // Distance in km
	}

	// Helper function to convert degrees to radians
	public static double toRadians(double degrees) {
		return degrees * (Math.PI / 180);
	}

	// Function to filter locations within a given radius from a center point
	public static List<String> filterLocations(List<String> locations, double centerLat, double centerLon,
			double radiusInKm) {
		List<String> filteredLocations = new ArrayList<>();

		for (String loc : locations) {
			double distance = calculateDistance(centerLat, centerLon, Double.parseDouble(loc.split("#")[0]),
					Double.parseDouble(loc.split("#")[1]));
			if (distance <= radiusInKm) {
				filteredLocations.add(loc);
			}
		}

		return filteredLocations;
	}

	public String filteredLocationswithradius(double centerLat, double centerLon, double radiusInKm) {
		JSONObject returnMessage=new JSONObject();
		try {
			String url = pgresturl + "property?select=location_detail";
			JSONObject jsonObject = new JSONObject(dataTransmit.transmitDataspgrest(url).get(0).toString());
			Iterator<String> keys = jsonObject.keys();
			List<String> locations = new ArrayList<>();
			while (keys.hasNext()) {
				String value = String.valueOf(jsonObject.get(keys.next()));
				if (!locations.contains(value))
					locations.add(value);
			}
			List<String> filteredLocations = filterLocations(locations, centerLat, centerLon, radiusInKm);

			LOGGER.warn("Locations within " + radiusInKm + " km of (" + centerLat + ", " + centerLon + "):");
			JSONArray filteredLocationsArray=new JSONArray();
			for (String loc : filteredLocations) {
				filteredLocationsArray.put(loc);
			}
			returnMessage.put("datavalues", filteredLocationsArray);
		} catch (Exception e) {
			LOGGER.error(Thread.currentThread().getStackTrace()[0].getMethodName(), e);
		}
		return returnMessage.toString();

	}
}
