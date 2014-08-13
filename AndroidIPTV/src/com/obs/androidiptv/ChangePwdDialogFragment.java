package com.obs.androidiptv;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.obs.data.ResetPwdDatum;

public class ChangePwdDialogFragment extends DialogFragment {

	interface ChgPwdSubmitClickListener {
		public void onChgPwdSubmitClickListener(ResetPwdDatum data);
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {

		final AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
				.setView(
						getActivity().getLayoutInflater().inflate(
								R.layout.dialog_change_pwd, null)).create();
		alertDialog.show();
		((Button) alertDialog
				.findViewById(R.id.f_my_profile_cp_dialog_btn_cancel))
				.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						alertDialog.cancel();
					}
				});
		((Button) alertDialog
				.findViewById(R.id.f_my_profile_cp_dialog_btn_submit))
				.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						// call reset pwd
						String mailId = ((EditText) alertDialog
								.findViewById(R.id.f_my_profile_cp_dialog_et_email_id))
								.getText().toString();
						String pwd = ((EditText) alertDialog
								.findViewById(R.id.f_my_profile_cp_dialog_et_new_pwd))
								.getText().toString();
						if (mailId != null && mailId.length() != 0
								&& pwd != null && pwd.length() != 0) {
							((ChgPwdSubmitClickListener)getFragmentManager().findFragmentByTag(MyAccountActivity.FRAG_TAG))
									.onChgPwdSubmitClickListener(new ResetPwdDatum(
											mailId, pwd));

						} else {
							Toast.makeText(getActivity(),
									"Please enter valid mailId or password.",
									Toast.LENGTH_LONG).show();
						}
						alertDialog.cancel();
					}
				});
		return alertDialog;
	}

}