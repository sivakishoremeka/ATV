package com.obs.androidiptv;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;


public  class ForgetPwdDialogFragment extends DialogFragment {

	interface PwdSubmitClickListener{
		public void onPwdSubmitClickListener(String mailId);
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {

		final AlertDialog alertDialog = new AlertDialog.Builder(
				getActivity()).setView(
				getActivity().getLayoutInflater().inflate(
						R.layout.dialog_forget_pwd, null)).create();
		alertDialog.show();
		((Button) alertDialog.findViewById(R.id.a_reg_fp_dialog_btn_cancel))
				.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						alertDialog.cancel();
					}
				});
		((Button) alertDialog.findViewById(R.id.a_reg_fp_dialog_btn_submit))
				.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
					// call reset pwd
						String mailId = ((EditText) alertDialog
								.findViewById(R.id.a_reg_fp_dialog_et_login_email_id))
								.getText().toString();
						if (mailId != null && mailId.length() != 0) {

							((PwdSubmitClickListener)getActivity()).onPwdSubmitClickListener(mailId);
						} else {
							// do nothing
						}
						alertDialog.cancel();
					}
				});
		return alertDialog;
	}

}