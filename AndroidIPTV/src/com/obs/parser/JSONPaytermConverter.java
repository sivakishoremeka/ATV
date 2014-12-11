package com.obs.parser;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import retrofit.converter.ConversionException;
import retrofit.converter.Converter;
import retrofit.mime.TypedInput;
import retrofit.mime.TypedOutput;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.obs.androidiptv.MyApplication;
import com.obs.data.Paytermdatum;
import com.obs.data.Subscriptiondatum;

public class JSONPaytermConverter implements Converter {

	public static String TAG = JSONPaytermConverter.class.getName();

	@Override
	public List<Paytermdatum> fromBody(TypedInput typedInput, Type type)
			throws ConversionException {
		List<Paytermdatum> list = null;

		try {
			String json = MyApplication.getJSONfromInputStream(typedInput.in());
			list = parseJsonToClient(json);
		} catch (IOException e) {
			Log.i(TAG, e.getMessage());
		}
		return list;
	}

	@Override
	public TypedOutput toBody(Object o) {
		return null;
	}

	public static List<Paytermdatum> parseJsonToClient(String json) {
		List<Paytermdatum> paytermList = null;
		List<Subscriptiondatum> subscriptionList = null;
		try {

			JSONObject jsonObj = new JSONObject(json);
			JSONArray jArrPay = jsonObj.getJSONArray("paytermdata");
			paytermList = new Gson().fromJson(jArrPay.toString(),
					new TypeToken<List<Paytermdatum>>() {
					}.getType());

			JSONArray jArrSubscrip = jsonObj.getJSONArray("subscriptiondata");
			subscriptionList = new Gson().fromJson(jArrSubscrip.toString(),
					new TypeToken<List<Subscriptiondatum>>() {
					}.getType());

			if (paytermList != null && subscriptionList != null) {

				for (Paytermdatum data : paytermList) {

					for (Subscriptiondatum subdata : subscriptionList) {

						if (data.getDuration().equalsIgnoreCase(
								subdata.getContractdata())) {
							data.setId(subdata.getId());
						}
					}
				}
			}

		} catch (Exception e) {
			Log.i(TAG, e.getMessage());
		}

		return paytermList;
	}
}