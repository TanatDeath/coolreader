package org.coolreader.options;

import java.util.EnumSet;

public class HardwareKeyInfo {
    public int keyCode;
    public String keyName;
    EnumSet<OptionsDialog.KeyActionFlag> keyFlags;
    public int drawableAttrId;
    public int fallbackIconId;
    public int drawableAttrId_long;
    public int fallbackIconId_long;
    public int drawableAttrId_double;
    public int fallbackIconId_double;

    public HardwareKeyInfo(int keyCode, String keyName,
                           EnumSet<OptionsDialog.KeyActionFlag> keyFlags,
                           int drawableAttrId, int fallbackIconId,
                           int drawableAttrId_long, int fallbackIconId_long,
                           int drawableAttrId_double, int fallbackIconId_double) {
        this.keyCode = keyCode;
        this.keyName = keyName;
        this.keyFlags = keyFlags;
        this.drawableAttrId = drawableAttrId;
        this.fallbackIconId = fallbackIconId;
        this.drawableAttrId_long = drawableAttrId_long;
        this.fallbackIconId_long = fallbackIconId_long;
        this.drawableAttrId_double = drawableAttrId_double;
        this.fallbackIconId_double = fallbackIconId_double;
    }

    public HardwareKeyInfo(int keyCode, String keyName,
                           int drawableAttrId, int fallbackIconId,
                           int drawableAttrId_long, int fallbackIconId_long,
                           int drawableAttrId_double, int fallbackIconId_double) {
        this(keyCode, keyName, null,
            drawableAttrId, fallbackIconId,
            drawableAttrId_long, fallbackIconId_long,
            drawableAttrId_double, fallbackIconId_double);
    }

    public HardwareKeyInfo(int keyCode, String keyName, EnumSet<OptionsDialog.KeyActionFlag> keyFlags) {
        this(keyCode, keyName, keyFlags, 0, 0, 0, 0, 0, 0);
    }

    public HardwareKeyInfo(int keyCode, String keyName) {
        this(keyCode, keyName, null);
    }
}
