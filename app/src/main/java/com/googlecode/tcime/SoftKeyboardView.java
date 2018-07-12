/*
 * Copyright 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.googlecode.tcime;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.Keyboard.Key;
import android.inputmethodservice.KeyboardView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Shows a soft keyboard, rendering keys and detecting key presses.
 */
public class SoftKeyboardView extends KeyboardView {

    private static final String TAG = "SoftKeyboardView";
    private static final int FULL_WIDTH_OFFSET = 0xFEE0;

    private SoftKeyboard currentKeyboard;
    private boolean capsLock;
    private boolean cangjieSimplified;

    public int mCurrFocusIndex = 0;

    private int mCurKeyboardKeyNum;
    private List<Key> mKeys;
    /**
     * The keyboard is get the focus
     */
    private boolean focusFlag = false;


    private static Method invalidateKeyMethod;

    static {
        try {
            invalidateKeyMethod = KeyboardView.class.getMethod(
                    "invalidateKey", new Class[]{int.class});
        } catch (NoSuchMethodException nsme) {
        }
    }


    public static boolean canRedrawKey() {
        return invalidateKeyMethod != null;
    }

    public SoftKeyboardView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SoftKeyboardView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

    }

    private boolean canCapsLock() {
        // Caps-lock can only be toggled on English keyboard.
        return (currentKeyboard != null) && currentKeyboard.isEnglish();
    }

    public boolean toggleCapsLock() {
        if (canCapsLock()) {
            capsLock = !isShifted();
            setShifted(capsLock);
            return true;
        }
        return false;
    }

    public void updateCursorCaps(int caps) {
        if (canCapsLock()) {
            setShifted(capsLock || (caps != 0));
        }
    }

    private boolean canCangjieSimplified() {
        // Simplified-cangjie can only be toggled on Cangjie keyboard.
        return (currentKeyboard != null) && currentKeyboard.isCangjie();
    }

    public boolean toggleCangjieSimplified() {
        if (canCangjieSimplified()) {
            cangjieSimplified = !isShifted();
            setShifted(cangjieSimplified);
            return true;
        }
        return false;
    }

    public boolean isCangjieSimplified() {
        return cangjieSimplified;
    }

    public boolean hasEscape() {
        return (currentKeyboard != null) && currentKeyboard.hasEscape();
    }

    public void setEscape(boolean escape) {
        if ((currentKeyboard != null) && currentKeyboard.setEscape(escape)) {
            invalidateEscapeKey();
        }
    }

    private void invalidateEscapeKey() {
        // invalidateKey method is only supported since 1.6.
        if (invalidateKeyMethod != null) {
            try {
                invalidateKeyMethod.invoke(this, currentKeyboard.getEscapeKeyIndex());
            } catch (IllegalArgumentException e) {
                Log.e("SoftKeyboardView", "exception: ", e);
            } catch (IllegalAccessException e) {
                Log.e("SoftKeyboardView", "exception: ", e);
            } catch (InvocationTargetException e) {
                Log.e("SoftKeyboardView", "exception: ", e);
            }
        }
    }

    @Override
    public void setKeyboard(Keyboard keyboard) {
        if (keyboard instanceof SoftKeyboard) {
            boolean escape = hasEscape();
            currentKeyboard = (SoftKeyboard) keyboard;
            currentKeyboard.updateStickyKeys();
            currentKeyboard.setEscape(escape);

            mCurKeyboardKeyNum = currentKeyboard.getKeys().size();
        }
        super.setKeyboard(keyboard);
    }

    @Override
    protected boolean onLongPress(Key key) {
        // 0xFF01~0xFF5E map to the full-width forms of the characters from
        // 0x21~0x7E. Make the long press as producing corresponding full-width
        // forms for these characters by adding the offset (0xff01 - 0x21).
        if (currentKeyboard != null && currentKeyboard.isSymbols() &&
                key.popupResId == 0 && key.codes[0] >= 0x21 && key.codes[0] <= 0x7E) {
            getOnKeyboardActionListener().onKey(
                    key.codes[0] + FULL_WIDTH_OFFSET, null);
            return true;
        } else if (key.codes[0] == SoftKeyboard.KEYCODE_MODE_CHANGE_LETTER) {
            getOnKeyboardActionListener().onKey(SoftKeyboard.KEYCODE_OPTIONS, null);
            return true;
        } else {
            return super.onLongPress(key);
        }
    }


    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        currentKeyboard = (SoftKeyboard) this.getKeyboard();

        mKeys = currentKeyboard.getKeys();
        Paint p = new Paint();
        p.setColor(getResources().getColor(R.color.select_key));
        //p.setStyle(Paint.Style.STROKE);
        //p.setStrokeWidth(4.0f);
        if (mCurrFocusIndex > mKeys.size()) {
            mCurrFocusIndex = mKeys.size() - 1;
        }
        p.setAntiAlias(true);
        p.setAlpha(90);

        Key focusedKey = mKeys.get(mCurrFocusIndex);

        RectF rect = new RectF(focusedKey.x, focusedKey.y + 4, focusedKey.x + focusedKey.width, focusedKey.y + focusedKey.height + 2);
        if (this.isFocusFlag()) {
            canvas.drawRoundRect(rect, 5, 5, p);
        }

    }


    public boolean isFocusFlag() {
        return focusFlag;
    }


    public void setFocusFlag(boolean focusFlag) {
        this.focusFlag = focusFlag;
    }


    public void processFunctionKey(int keyCode) {

        if (isFocusFlag()) {

            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_CENTER:

                    int currentKeyCode = mKeys.get(mCurrFocusIndex).codes[0];
                    getOnKeyboardActionListener().onKey(currentKeyCode, null);
                    break;

                case KeyEvent.KEYCODE_DPAD_RIGHT:

                    if (mCurrFocusIndex >= mCurKeyboardKeyNum - 1) {
                        mCurrFocusIndex = 0;
                    } else {
                        mCurrFocusIndex++;
                    }

                    break;
                case KeyEvent.KEYCODE_DPAD_LEFT:

                    if (mCurrFocusIndex <= 0) {
                        mCurrFocusIndex = mCurKeyboardKeyNum - 1;
                    } else {
                        mCurrFocusIndex--;
                    }

                    break;
                case KeyEvent.KEYCODE_DPAD_DOWN:

                    if (mCurrFocusIndex >= mCurKeyboardKeyNum - 1) {
                        mCurrFocusIndex = 0;
                    } else {
                        int[] nearestKeys = currentKeyboard.getNearestKeys(mKeys.get(mCurrFocusIndex).x, mKeys.get(mCurrFocusIndex).y);

                        for (int i = 0; i < nearestKeys.length; i++) {
                            int nearIndex = nearestKeys[i];
                            Key nearKey = mKeys.get(nearIndex);

                            if (mCurrFocusIndex < nearIndex) {
                                Key lastKey = mKeys.get(mCurrFocusIndex);

                                if (((lastKey.x >= nearKey.x) && (lastKey.x < (nearKey.x + nearKey.width)))
                                        || (((lastKey.x + lastKey.width) > nearKey.x) && ((lastKey.x + lastKey.width) <= (nearKey.x + nearKey.width)))) {
                                    mCurrFocusIndex = nearIndex;
                                    break;
                                }
                            }
                        }

                    }
                    break;

                case KeyEvent.KEYCODE_DPAD_UP:

                    if (mCurrFocusIndex <= 0) {
                        mCurrFocusIndex = mCurKeyboardKeyNum - 1;
                    } else {

                        int[] nearestKeys = currentKeyboard.getNearestKeys(mKeys.get(mCurrFocusIndex).x, mKeys.get(mCurrFocusIndex).y);
                        for (int i = nearestKeys.length - 1; i >= 0; i--) {

                            int nearIndex = nearestKeys[i];
                            Key nearKey = mKeys.get(nearIndex);

                            if (mCurrFocusIndex > nearIndex) {
                                Key nextNearKey = mKeys.get(nearIndex + 1);
                                Key lastKey = mKeys.get(mCurrFocusIndex);

                                if ((lastKey.x >= nearKey.x) && (lastKey.x < (nearKey.x + nearKey.width))
                                        && (((lastKey.x + lastKey.width) <= (nextNearKey.x + nextNearKey.width)) || ((lastKey.x + lastKey.width) > nextNearKey.x))
                                        ) {

                                    mCurrFocusIndex = nearIndex;
                                    break;
                                }

                            }
                        }
                        break;
                    }
                default:
                    break;
            }

        }
    }


}
