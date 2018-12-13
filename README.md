# android-traditional-chinese-ime
https://code.google.com/archive/p/android-traditional-chinese-ime/

适用于台湾的注音输入法！

### 注音输入法
修改后使之支持TV遥控器操作。

<img src="https://github.com/tracyliu1/android-traditional-chinese-ime/blob/master/screenshots/chinese_tradition_IME.png" width = "600"/>

<img src="https://github.com/tracyliu1/android-traditional-chinese-ime/blob/master/screenshots/code.png" />

重写onDraw方法绘制选中状态
```
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
```
