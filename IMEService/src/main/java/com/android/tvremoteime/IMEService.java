package com.android.tvremoteime;

import android.content.Intent;
import android.inputmethodservice.InputMethodService;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.tvremoteime.adb.AdbHelper;
import com.android.tvremoteime.server.RemoteServer;
import com.android.tvremoteime.server.RemoteServerFileManager;

import java.io.IOException;


public class IMEService extends InputMethodService implements View.OnClickListener{
	public static String TAG = "TVRemoteIME";
	public static String ACTION = "com.android.tvremoteime";

	private boolean capsOn = false;
	private ImageButton btnCaps = null;
	private View focusedView = null;
	private RelativeLayout mInputView = null;
	private boolean hideWindowByKey = false;

	private View helpDialog = null;
	private ImageView qrCodeImage = null;
	private TextView  addressView = null;

	private RemoteServer mServer = null;
	private LinearLayout numLine = null;
	private LinearLayout qweLine = null;
	private LinearLayout asdLine = null;
	private LinearLayout zxcLine = null;

	private static final int SERVER_START_ERROR = 901;
	private static final int ERROR = 999;
	private static final int TOAST_MESSAGE = 1000;

	public static final int KEY_ACTION_PRESSED = 0;
	public static final int KEY_ACTION_DOWN = 1;
	public static final int KEY_ACTION_UP = 2;

	final Handler handler = new Handler();

	private ClipboardHelper clipboardHelper;
	private EditText editText;
	private int length = 0;

	@Override
	public void onCreate() {
		super.onCreate();
		//android.os.Debug.waitForDebugger();
		Environment.initToastHandler();
		clipboardHelper = new ClipboardHelper(this);
		RemoteServerFileManager.resetBaseDir(this);
		startRemoteServer();
//		DLNAUtils.startDLNAService(this.getApplicationContext());
		new AutoUpdateManager(this, this.handler);
//		xllib.DownloadManager.instance().init(this);
		FtpUtils.startFtpService(this.getApplicationContext());
	}

	@Override
    public View onCreateInputView()  {
		if(Environment.needDebug){
			Environment.debug(TAG, "onCreateInputView.");
		}
    	mInputView = (RelativeLayout)getLayoutInflater().inflate(R.layout.keyboard, null);

		capsOn = true;
		btnCaps = mInputView.findViewById(R.id.btnCaps);
		numLine = mInputView.findViewById(R.id.numLine);
		qweLine = mInputView.findViewById(R.id.qweLine);
		asdLine = mInputView.findViewById(R.id.asdLine);
		zxcLine = mInputView.findViewById(R.id.zxcLine);
		editText = mInputView.findViewById(R.id.copyText);
		editText.setFocusable(true);
		editText.setFocusableInTouchMode(true);
		editText.requestFocus();
		editText.setCursorVisible(true);
		editText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(View view, boolean b) {
				editText.setSelection(editText.getText().length());
			}
		});
		helpDialog = mInputView.findViewById(R.id.helpDialog);
		qrCodeImage = helpDialog.findViewById(R.id.ivQRCode);
		addressView = helpDialog.findViewById(R.id.tvAddress);
		toggleCapsState(true);

        return mInputView; 
    }

	@Override
	public View onCreateCandidatesView() {
		if(Environment.needDebug){
			Environment.debug(TAG, "onCreateCandidatesView.");
		}
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if(Environment.needDebug){
			Environment.debug(TAG, "onStartCommand.");
		}

		onStart(intent, startId);
		return START_STICKY;
	}

	@Override
	public void onStartInput(EditorInfo attribute, boolean restarting) {
		if(Environment.needDebug){
			Environment.debug(TAG, "onStartInput " + " inputType: "
					+ String.valueOf(attribute.inputType) + " Restarting:"
					+ String.valueOf(restarting));
		}
		super.onStartInput(attribute, restarting);
	}

	@Override
	public void onFinishInputView(boolean finishingInput) {
		if (Environment.needDebug) {
			Environment.debug(TAG, "onFinishInputView." + " finishingInput: "
					+ String.valueOf(finishingInput));
		}
		super.onFinishInputView(finishingInput);
	}

	@Override
	public void onFinishCandidatesView(boolean finishingInput) {
		if (Environment.needDebug) {
			Environment.debug(TAG, "onFinishCandidatesView." + " finishingInput: "
					+ String.valueOf(finishingInput));
		}
		super.onFinishCandidatesView(finishingInput);
	}

	@Override
	public boolean onEvaluateInputViewShown() {
		if (Environment.needDebug) {
			Environment.debug(TAG, "onEvaluateInputViewShown.");
		}
		super.onEvaluateInputViewShown();
		if(hideWindowByKey){
			hideWindowByKey = false;
			return hideWindowByKey;
		}
		EditorInfo editorInfo = getCurrentInputEditorInfo();
		return !(editorInfo == null || editorInfo.inputType == EditorInfo.TYPE_NULL);
	}

	@Override
	public boolean onEvaluateFullscreenMode() {
		if (Environment.needDebug) {
			Environment.debug(TAG, "onEvaluateFullscreenMode.");
		}
		return false;
	}

	private boolean isSendToAdbService(Object data){
		if(AdbHelper.initService(getApplicationContext())){
			AdbHelper.getInstance().sendData(data);
			return true;
		}
		return false;
	}

	private void startRemoteServer(){
		do {
			mServer = new RemoteServer(RemoteServer.serverPort, this);
			mServer.setDataReceiver(new RemoteServer.DataReceiver() {
				@Override
				public void onKeyEventReceived(String keyCode, final int keyAction) {
					if(keyCode != null) {
						if("cls".equalsIgnoreCase(keyCode)){
							InputConnection ic = getCurrentInputConnection();
							if(ic != null) {
								ic.deleteSurroundingText(Integer.MAX_VALUE,Integer.MAX_VALUE);
								//ic.performContextMenuAction(android.R.id.selectAll);
								//ic.commitText("", 1);
							}
							editText.setText("");
							editText.setSelection(0);
						} else {
							final int kc = KeyEvent.keyCodeFromString(keyCode);
							if(kc != KeyEvent.KEYCODE_UNKNOWN){
								if(mInputView != null && KeyEventUtils.isKeyboardFocusEvent(kc) && mInputView.isShown()){
									if(keyAction == KEY_ACTION_PRESSED || keyAction == KEY_ACTION_DOWN) {
										handler.post(new Runnable() {
											@Override
											public void run() {
												if (!handleKeyboardFocusEvent(kc)) {
													if(!isSendToAdbService(kc)) sendKeyCode(kc);
												}
											}
										});
									}
								}
								else{
									long eventTime = SystemClock.uptimeMillis();
									InputConnection ic = getCurrentInputConnection();
									switch (keyAction) {
										case KEY_ACTION_PRESSED:
											if(!isSendToAdbService(kc)) sendKeyCode(kc);
											break;
										case KEY_ACTION_DOWN:
											if(!isSendToAdbService(kc) && ic != null) {
												ic.sendKeyEvent(new KeyEvent(eventTime, eventTime,
														KeyEvent.ACTION_DOWN, kc, 0, 0, KeyCharacterMap.VIRTUAL_KEYBOARD, 0,
														KeyEvent.FLAG_SOFT_KEYBOARD | KeyEvent.FLAG_KEEP_TOUCH_MODE));
											}
											break;
										case KEY_ACTION_UP:
											if(ic != null) {
												ic.sendKeyEvent(new KeyEvent(eventTime, eventTime,
													KeyEvent.ACTION_UP, kc, 0, 0, KeyCharacterMap.VIRTUAL_KEYBOARD, 0,
													KeyEvent.FLAG_SOFT_KEYBOARD | KeyEvent.FLAG_KEEP_TOUCH_MODE));
											}
											break;
									}
								}
							}
						}
					}
				}

				@Override
				public void onTextReceived(String text) {
					if (text != null) {
						if(!isSendToAdbService(text)) {
							commitText(text);
							String msg = editText.getText().toString().substring(0,length) + text + editText.getText().toString().substring(length);
							length=length + text.length();
							handler.postDelayed(new Runnable() {
								@Override
								public void run() {
									editText.setText(msg);
								}
							},200);
						}
					}
				}

				@Override
				public String onClipboardRecived(String code, String text) {
					if (code.equals("copy")) {
						clipboardHelper.copyText(text);
						return text;
					}
					else if(code.equals("paste")) {
						text = clipboardHelper.getCopiedText();
						return text;
					}
					return "";
				}
			});
			try {
				mServer.start();
				Environment.toastInHandler(this, getString(R.string.app_name)  + "远程服务已启动");
				Log.i(TAG, "远程服务创建成功！port=" + RemoteServer.serverPort);
				break;
			}catch (IOException ex){
				Log.e(TAG, "建立输入HTTP服务时出错", ex);
				RemoteServer.serverPort ++;
				mServer.stop();
			}
		}while (RemoteServer.serverPort < 9999);
	}

	private boolean commitText(String text){
		InputConnection ic = getCurrentInputConnection();
		boolean flag = false;
		if (ic != null){
			if(Environment.needDebug) {
				Environment.debug(TAG, "commitText:" + text);
			}
			if(text.length() > 1 && ic.beginBatchEdit()){
				flag = ic.commitText(text, 1);
				ic.endBatchEdit();
			}else{
				flag = ic.commitText(text, 1);
			}
		}
		return flag;
	}
	private void sendKeyCode(int keyCode){
		if(Environment.needDebug) {
			Environment.debug(TAG, "send-key-code:" + keyCode);
		}
		if(keyCode == KeyEvent.KEYCODE_HOME){
			//拦截HOME键
			Intent i = new Intent(Intent.ACTION_MAIN);
			i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			i.addCategory(Intent.CATEGORY_HOME);
			this.startActivity(i);
		} else if(keyCode == KeyEvent.KEYCODE_ENTER){
			sendDefaultEditorAction(true);
		}else {
			sendDownUpKeyEvents(keyCode);
		}
	}
    
    public void onDestroy() {
		if (mServer != null && mServer.isStarting()){
            Log.i(TAG, "远程输入服务已停止！");
			mServer.stop();
		}
//		DLNAUtils.stopDLNAService();
		AdbHelper.stopService();
		FtpUtils.stopFtpService();
		Environment.toastInHandler(this, getString(R.string.app_name)  + "服务已停止");
    	super.onDestroy();    	
    }

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if(handleKeyboardFocusEvent(keyCode)) return true;
		if (Environment.needDebug) {
			Environment.debug(TAG, "keydown-event:" + keyCode);
		}
		//同步软键盘状态处理代码：不处理以下按键事件则有可能物理键盘字符输入与软键盘的大小写状态不同步
		if(keyCode == KeyEvent.KEYCODE_CAPS_LOCK) capsOn = !capsOn;
		if ((keyCode >= KeyEvent.KEYCODE_0 && keyCode <= KeyEvent.KEYCODE_9)) {
			if(commitText(String.valueOf(keyCode - KeyEvent.KEYCODE_0))) return true;
		} else if (keyCode >= KeyEvent.KEYCODE_A && keyCode <= KeyEvent.KEYCODE_Z) {
			if (commitText(String.valueOf((char) ((capsOn ? 65 : 97) + keyCode - KeyEvent.KEYCODE_A))))
				return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	private boolean handleKeyboardFocusEvent(int keyCode){
		if(mInputView != null) {
			if (Environment.needDebug) {
				Environment.debug(TAG, "handleKeyboardFocusEvent:" + keyCode);
			}
			switch (keyCode) {
				case KeyEvent.KEYCODE_DPAD_UP:
				case KeyEvent.KEYCODE_DPAD_DOWN:
				case KeyEvent.KEYCODE_DPAD_LEFT:
				case KeyEvent.KEYCODE_DPAD_RIGHT:
					if(mInputView.isShown()) {
						requestNextButtonFocus(keyCode);
						return true;
					}
					break;
				case KeyEvent.KEYCODE_ENTER:
				case KeyEvent.KEYCODE_DPAD_CENTER:
					if (mInputView.isShown() && focusedView != null) {
						clickButtonByKey(focusedView);
						return true;
					}
					break;
				case KeyEvent.KEYCODE_CAPS_LOCK:
					toggleCapsState(true);
					return true;
				case KeyEvent.KEYCODE_ESCAPE:
				case KeyEvent.KEYCODE_BACK:
					if (mInputView.isShown()){
						if(helpDialog != null && helpDialog.isShown()){
							helpDialog.setVisibility(View.GONE);
						}else {
							this.finishInput();
						}
						return true;
					}
					break;
			}
		}
		return false;
	}

	private void requestNextButtonFocus(int keyCode){
		if(focusedView == null){
			focusedView =  ((LinearLayout)mInputView.getChildAt(0)).getChildAt(0);
		}else {
			LinearLayout container = (LinearLayout)focusedView.getParent();
			int rootInde = mInputView.indexOfChild(container);
			int index = container.indexOfChild(focusedView);
			boolean isLasted = container.getChildCount() == (index + 1);
			switch (keyCode) {
				case KeyEvent.KEYCODE_DPAD_UP:
					rootInde --;
					if(rootInde < 0) rootInde = mInputView.getChildCount() - 2;
					container = (LinearLayout)mInputView.getChildAt(rootInde);
					if(index >= container.getChildCount()) index = isLasted ? container.getChildCount() - 1 : 0;
					break;
				case KeyEvent.KEYCODE_DPAD_DOWN:
					rootInde ++;
					if(rootInde >= (mInputView.getChildCount() - 1)) rootInde = 0;
					container = (LinearLayout)mInputView.getChildAt(rootInde);
					if(index >= container.getChildCount()) index = isLasted ? container.getChildCount() - 1 :  0;
					break;
				case KeyEvent.KEYCODE_DPAD_LEFT:
					index --;
					if(index < 0){
						rootInde --;
						if(rootInde < 0) rootInde = mInputView.getChildCount() - 2;
						container = (LinearLayout)mInputView.getChildAt(rootInde);
						index = container.getChildCount() - 1;
					}
					break;
				case KeyEvent.KEYCODE_DPAD_RIGHT:
					index ++;
					if(index >= container.getChildCount()){
						rootInde ++;
						if(rootInde >= (mInputView.getChildCount() - 1)) rootInde = 0;
						container = (LinearLayout)mInputView.getChildAt(rootInde);
						index = 0;
					}
					break;
			}
			focusedView = container.getChildAt(index);
		}

		focusedView.requestFocus();
		focusedView.requestFocusFromTouch();
	}
	private void finishInput(){
		this.onFinishInput();
		this.hideWindow();
		//this.onFinishInputView(true);
		//this.onFinishCandidatesView(true);
	}
	private void clickButtonByKey(final View v){
		switch (v.getId()) {
			case R.id.btnCaps:
				v.setBackgroundResource(capsOn ? R.drawable.key_pressed_on : R.drawable.key_pressed_off);
				break;
			case R.id.btnClose:
				this.hideWindowByKey = true;
				this.finishInput();
				return;
			default:
				v.setBackgroundResource(R.drawable.key_pressed);
				break;
		}
		clickButton(v, false);
		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				if(v == btnCaps){
					v.setBackgroundResource(capsOn ? R.drawable.key_on : R.drawable.key_off);
				}else{
					v.setBackgroundResource(R.drawable.key);
				}
				v.requestFocus();
			}
		}, 200);
	}
	private void clickButton(View v, boolean resetCapsButtonState){
		if(v instanceof Button){
			if(v.getId() == R.id.btnClose){
				this.finishInput();
			}else {
				commitText(((Button) v).getText().toString());
				String text = ((Button) v).getText().toString();
				String msg = editText.getText().toString().substring(0,length) + text + editText.getText().toString().substring(length);
				length=length + text.length();
				editText.setText(msg);
			}
		}else if(v instanceof ImageButton){
			String msg = "";
			switch (v.getId()){
				case R.id.btnEnter:
					sendKeyCode(KeyEvent.KEYCODE_ENTER);
					break;
				case R.id.btnSpace:
					msg = editText.getText().toString().substring(0,length) + " " + editText.getText().toString().substring(length);
					length++;
					editText.setText(msg);
					sendKeyCode(KeyEvent.KEYCODE_SPACE);
					break;
				case R.id.btnDelete:
					if (length>0)
						msg = editText.getText().toString().substring(0,length-1) + "" + editText.getText().toString().substring(length);
					length--;
					length = length < 0?0:length;
					editText.setText(msg);
					sendKeyCode(KeyEvent.KEYCODE_DEL);
					break;
				case R.id.btnForward:
					length++;
					length = length<editText.getText().length()?length:editText.getText().length();
					sendKeyCode(KeyEvent.KEYCODE_DPAD_RIGHT);
					break;
				case R.id.btnBack:
					length--;
					length = length < 0?0:length;
					sendKeyCode(KeyEvent.KEYCODE_DPAD_LEFT);
					break;
				case R.id.btnCopy:
					clipboardHelper.copyText(editText.getText().toString());
					Toast.makeText(this, "复制成功", Toast.LENGTH_SHORT).show();
					break;
				case R.id.btnPaste:
					String text = clipboardHelper.getCopiedText();
					commitText(text);
					msg = editText.getText().toString().substring(0,length) + text + editText.getText().toString().substring(length);
					length=length + text.length();
					editText.setText(msg);
					Toast.makeText(this, "黏贴成功", Toast.LENGTH_SHORT).show();
					break;
				case R.id.btnTab:
					sendKeyCode(KeyEvent.KEYCODE_TAB);
					break;
				case R.id.btnCaps:
					toggleCapsState(resetCapsButtonState);
					break;
				case R.id.btnHelp:
					showHelpDialog();
					break;
			}
		}
		editText.setFocusable(true);
		editText.setFocusableInTouchMode(true);
		editText.requestFocus();
		editText.setCursorVisible(true);
		editText.setSelection(length);
	}
	@Override
	public void onClick(View v) {
		clickButton(v, true);
		if(v.getId() != R.id.btnClose) {
			v.requestFocusFromTouch();
			focusedView = v;
		}
	}

	private void toggleCapsState(boolean resetCapsButtonState){
		capsOn = !capsOn;
		if(resetCapsButtonState)
			btnCaps.setBackgroundResource(capsOn ? R.drawable.key_on : R.drawable.key_off);
		resetButtonChar(numLine);
		resetButtonChar(qweLine);
		resetButtonChar(asdLine);
		resetButtonChar(zxcLine);
	}
	private void resetButtonChar(LinearLayout layout){
		for(int i =0; i<layout.getChildCount(); i++){
			View v = layout.getChildAt(i);
			if(v instanceof Button){
				Button b = (Button)v;
				if (b.getText().toString().matches("[a-zA-Z]")) {
					if (capsOn) {
						b.setText(b.getText().toString().toUpperCase());
					} else {
						b.setText(b.getText().toString().toLowerCase());
					}
				} else if (b.getText().toString().matches("[\\d+·\\-=~!@#$%^&*()_+]")) {
					String nums = "`1234567890-=?\"";
					String marks = "~!@#$%^&*()_+/\'";
					if (capsOn) {
						b.setText(String.valueOf(nums.charAt(i)));
					} else {
						b.setText(String.valueOf(marks.charAt(i)));
					}
				}
			}
		}
	}

	private void showHelpDialog(){
		if(mServer == null) return;

        if(addressView.getText().length() == 0) {
            String version = AppPackagesHelper.getCurrentPackageVersion(this);
            TextView title = helpDialog.findViewById(R.id.title);
            title.setText(title.getText() + " " + version);
            String address = mServer.getServerAddress();
            addressView.setText(address);
            qrCodeImage.setImageBitmap(QRCodeGen.generateBitmap(address, 300, 300));
        }

		helpDialog.setVisibility(View.VISIBLE);
	}

}
