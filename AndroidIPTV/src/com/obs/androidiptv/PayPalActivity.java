package com.obs.androidiptv;

import java.math.BigDecimal;

import org.json.JSONException;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.paypal.android.sdk.payments.PayPalConfiguration;
import com.paypal.android.sdk.payments.PayPalPayment;
import com.paypal.android.sdk.payments.PayPalService;
import com.paypal.android.sdk.payments.PaymentActivity;
import com.paypal.android.sdk.payments.PaymentConfirmation;

public class PayPalActivity extends Activity {

	/** PayPal configurations */
	private static final String CONFIG_ENVIRONMENT = PayPalConfiguration.ENVIRONMENT_SANDBOX;
    // note that these credentials will differ between live & sandbox environments.
	private static final String CONFIG_CLIENT_ID = "AVqMmxDlwKnNin9LPyx1La7OW58dqm87gznLqRCOv1uSkiQLehhvHrYxi4de";
	private static final int REQUEST_CODE_PAYMENT = 1;
	private static final int REQUEST_CODE_FUTURE_PAYMENT = 2;
	private static PayPalConfiguration config = new PayPalConfiguration()
    .environment(CONFIG_ENVIRONMENT)
    .clientId(CONFIG_CLIENT_ID)
    // The following are only used in PayPalFuturePaymentActivity.
    .merchantName("Android IPTV")
    .merchantPrivacyPolicyUri(Uri.parse("https://www.example.com/privacy"))
    .merchantUserAgreementUri(Uri.parse("https://www.example.com/legal"));
	/*  Paypal configurations*/  
	
	
	   @Override
	    protected void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);
	       // setContentView(R.layout.activity_paypal);

	        Intent intent = new Intent(this, PayPalService.class);
	        intent.putExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION, config);
	        startService(intent);
	    }
	   public void onBuyPressed(View pressed) {
	        /* 
	         * PAYMENT_INTENT_SALE will cause the payment to complete immediately.
	         * Change PAYMENT_INTENT_SALE to PAYMENT_INTENT_AUTHORIZE to only authorize payment and 
	         * capture funds later.
	         * 
	         * Also, to include additional payment details and an item list, see getStuffToBuy() below.
	         */
	        PayPalPayment thingToBuy = getThingToBuy(PayPalPayment.PAYMENT_INTENT_SALE);

	        Intent intent = new Intent(PayPalActivity.this, PaymentActivity.class);

	        intent.putExtra(PaymentActivity.EXTRA_PAYMENT, thingToBuy);

	        startActivityForResult(intent, REQUEST_CODE_PAYMENT);
	    }
	 
	 @Override
	 protected void onActivityResult (int requestCode, int resultCode, Intent data) {
	     if (resultCode == Activity.RESULT_OK) {
	         PaymentConfirmation confirm = data.getParcelableExtra(PaymentActivity.EXTRA_RESULT_CONFIRMATION);
	         if (confirm != null) {
	             try {
	                 Log.i("paymentExample", confirm.toJSONObject().toString(4));

	                 // TODO: send 'confirm' to your server for verification.
	                 // see https://developer.paypal.com/webapps/developer/docs/integration/mobile/verify-mobile-payment/
	                 // for more details.

	             } catch (JSONException e) {
	                 Log.e("paymentExample", "an extremely unlikely failure occurred: ", e);
	             }
	         }
	     }
	     else if (resultCode == Activity.RESULT_CANCELED) {
	         Log.i("paymentExample", "The user canceled.");
	     }
	     else if (resultCode == PaymentActivity.RESULT_EXTRAS_INVALID) {
	         Log.i("paymentExample", "An invalid Payment or PayPalConfiguration was submitted. Please see the docs.");
	     }
	 }
	 
	 private PayPalPayment getThingToBuy(String environment) {
	        return new PayPalPayment(new BigDecimal("1.75"), "USD", "hipster jeans",
	                environment);
	    }

	 @Override
	 public void onDestroy() {
	     stopService(new Intent(this, PayPalService.class));
	     super.onDestroy();
	 }
}
