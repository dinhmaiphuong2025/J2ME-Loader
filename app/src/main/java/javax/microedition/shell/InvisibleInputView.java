/*
 * Copyright 2026 Lixin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package javax.microedition.shell;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import androidx.appcompat.widget.AppCompatEditText;

public class InvisibleInputView extends AppCompatEditText {
	private Runnable onBackListener;
	private Runnable onCommitListener;

	public InvisibleInputView(Context context) {
		super(context);
	}

	public InvisibleInputView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public InvisibleInputView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	public void setOnBackListener(Runnable onBackListener) {
		this.onBackListener = onBackListener;
	}

	public void setOnCommitListener(Runnable onCommitListener) {
		this.onCommitListener = onCommitListener;
	}

	@Override
	public boolean onKeyPreIme(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
			if (onBackListener != null) {
				onBackListener.run();
				return true;
			}
		}
		return super.onKeyPreIme(keyCode, event);
	}

	@Override
	public void onEditorAction(int actionCode) {
		super.onEditorAction(actionCode);
		if (actionCode == EditorInfo.IME_ACTION_DONE
				|| actionCode == EditorInfo.IME_ACTION_GO
				|| actionCode == EditorInfo.IME_ACTION_SEND) {
			if (onCommitListener != null) {
				onCommitListener.run();
			}
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_ENTER) {
			if (onCommitListener != null) {
				onCommitListener.run();
				return true;
			}
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_ENTER) {
			return true;
		}
		return super.onKeyUp(keyCode, event);
	}
}
