/*
 * Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * 
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.iot.agent;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;

import org.wso2.iot.agent.utils.CommonDialogUtils;
import org.wso2.iot.agent.utils.Constants;
import org.wso2.iot.agent.utils.Preference;

/**
 * Activity which handles PIN code setting/editing.
 */
public class PinCodeActivity extends Activity {
	private TextView txtPin;
	private EditText evPin;
	private EditText evReTypePin;
	private EditText evOldPin;
	private Button btnPin;
	private String username;
	private String registrationId;
	private static final int TAG_BTN_SET_PIN = 0;
	private static final int PIN_MIN_LENGTH = 4;
	private String fromActivity;
	private Context context;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_pin_code);
		context = PinCodeActivity.this;
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			if (extras.containsKey(getResources().getString(R.string.intent_extra_username))) {
				username =
						extras.getString(getResources().getString(R.string.intent_extra_username));
			}

			if (extras.containsKey(getResources().getString(R.string.intent_extra_regid))) {
				registrationId =
						extras.getString(getResources().getString(R.string.intent_extra_regid));
			}

			if (extras.containsKey(getResources().getString(R.string.intent_extra_from_activity))) {
				fromActivity =
						extras.getString(
								getResources().getString(R.string.intent_extra_from_activity));
			}
		}

		txtPin = (TextView) findViewById(R.id.lblPin);
		evPin = (EditText) findViewById(R.id.txtPinCode);
		evReTypePin = (EditText) findViewById(R.id.txtRetypePinCode);
		evOldPin = (EditText) findViewById(R.id.txtOldPinCode);
		btnPin = (Button) findViewById(R.id.btnSetPin);
		btnPin.setTag(TAG_BTN_SET_PIN);
		btnPin.setOnClickListener(onClickListenerButtonClicked);
		btnPin.setEnabled(false);
		btnPin.setBackground(getResources().getDrawable(R.drawable.btn_grey));
		btnPin.setTextColor(getResources().getColor(R.color.black));

		if (Constants.DEFAULT_OWNERSHIP == Constants.OWNERSHIP_COSU) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				startLockTask();
			}
		}

		if (AlreadyRegisteredActivity.class.getSimpleName().equals(fromActivity)) {
			txtPin.setVisibility(View.GONE);
			evOldPin.setVisibility(View.VISIBLE);
			evOldPin.requestFocus();
			evPin.setHint(getResources().getString(R.string.hint_new_pin));
			evReTypePin.setHint(getResources().getString(R.string.hint_retype_pin));
		}

		evPin.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				enableSubmitIfReady();
			}

			@Override
			public void afterTextChanged(Editable s) {
				enableSubmitIfReady();
			}
		});

		evReTypePin.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				enableSubmitIfReady();
			}

			@Override
			public void afterTextChanged(Editable s) {
				enableSubmitIfReady();
			}
		});
	}

	private OnClickListener onClickListenerButtonClicked = new OnClickListener() {

		@Override
		public void onClick(View view) {
			int viewTag = (Integer) view.getTag();
			switch (viewTag) {
				case TAG_BTN_SET_PIN:
					savePin();
					break;
				default:
					break;
			}
		}
	};

	private void savePin() {
		if (AlreadyRegisteredActivity.class.getSimpleName().equals(fromActivity)) {
			String pin = Preference.getString(context, getResources().getString(R.string.shared_pref_pin));
			if (!evOldPin.getText().toString().trim().equals(pin.trim())) {
				Toast.makeText(getApplicationContext(),
						getResources().getString(R.string.toast_message_pin_change_failed),
						Toast.LENGTH_SHORT).show();
				evOldPin.requestFocus();
				evOldPin.setText("");
				return;
			}
			Toast.makeText(getApplicationContext(),
			               getResources().getString(R.string.toast_message_pin_change_success),
			               Toast.LENGTH_SHORT).show();
			Intent intent = new Intent(PinCodeActivity.this, AlreadyRegisteredActivity.class);
			intent.putExtra(getResources().getString(R.string.intent_extra_from_activity),
			                PinCodeActivity.class.getSimpleName());
			intent.putExtra(getResources().getString(R.string.intent_extra_regid), registrationId);
			startActivity(intent);
		} else {
			Intent intent = new Intent(PinCodeActivity.this, RegistrationActivity.class);
			intent.putExtra(getResources().getString(R.string.intent_extra_regid), registrationId);
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			intent.putExtra(getResources().getString(R.string.intent_extra_username), username);
			startActivity(intent);
		}
		Preference.putString(context, getResources().getString(R.string.shared_pref_pin),
				evPin.getText().toString().trim());
		finish();
	}

	private void enableSubmitIfReady() {

		boolean isReady = false;

		if (evPin.getText().toString().length() >= PIN_MIN_LENGTH
				&& evPin.getText().toString().equals(evReTypePin.getText().toString())) {
			isReady = true;
		}

		if (isReady) {
			btnPin.setBackground(getResources().getDrawable(R.drawable.btn_orange));
			btnPin.setTextColor(getResources().getColor(R.color.white));
			btnPin.setEnabled(true);
		} else {
			btnPin.setBackground(getResources().getDrawable(R.drawable.btn_grey));
			btnPin.setTextColor(getResources().getColor(R.color.black));
			btnPin.setEnabled(false);
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK &&
				AlreadyRegisteredActivity.class.getSimpleName().equals(fromActivity)) {
			Intent intent = new Intent(PinCodeActivity.this, AlreadyRegisteredActivity.class);
			intent.putExtra(getResources().getString(R.string.intent_extra_from_activity),
			                PinCodeActivity.class.getSimpleName());
			intent.putExtra(getResources().getString(R.string.intent_extra_regid), registrationId);
			startActivity(intent);
			finish();
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_BACK) {
			Intent i = new Intent();
			i.setAction(Intent.ACTION_MAIN);
			i.addCategory(Intent.CATEGORY_HOME);
			startActivity(i);
			finish();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return true;
	}

}
