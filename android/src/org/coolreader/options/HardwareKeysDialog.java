package org.coolreader.options;

import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import org.coolreader.CoolReader;
import org.coolreader.R;
import org.coolreader.crengine.BackgroundThread;
import org.coolreader.crengine.BaseActivity;
import org.coolreader.crengine.BaseDialog;
import org.coolreader.crengine.Properties;
import org.coolreader.crengine.Settings;
import org.coolreader.layouts.FlowLayout;
import org.coolreader.utils.StrUtils;
import org.coolreader.utils.Utils;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HardwareKeysDialog extends BaseDialog {

    private final View mGrid;
    private final LayoutInflater mInflater;
    private final CoolReader mCoolReader;
    private final Properties mProperties;

    private final TextView tvKeyScancode;
    private final EditText edtKeydown;

    private final Button btnAddKey;
    private final FlowLayout mFlKeys;
    private KeyEvent lastEvent;

    private ArrayList<String> keys = new ArrayList<>();
    private ArrayList<View> keyViews = new ArrayList<>();

    private String getKeyDef(KeyEvent event) {
        String evt = event.toString();
        Pattern pattern = Pattern.compile("keyCode=.*?,");
        Matcher matcher = pattern.matcher(evt);
        if(matcher.find()) {
            String s = matcher.group(0).replace("keyCode=", "");
            return s.substring(0, s.length()-1);
        }
        return "KEYCODE_" + event.getKeyCode();
    }

    private void removeKey(String mnemo) {
        for (View buttonView: keyViews) {
            TextView tvText = buttonView.findViewById(R.id.tag_flow_item_text);
            if (tvText.getText().equals(mnemo)) {
                mFlKeys.removeView(buttonView);
                keyViews.remove(buttonView);
                break;
            }
        }
        for (String s: keys) {
            if (s.equals(mnemo)) {
                keys.remove(s);
                break;
            }
        }
    }

    private void fillExistingKeys() {
        String res = "";
        for (String s: keys) {
            if (res.equals("")) res = s;
            else res = res + "|" + s;
        }
        String keys = StrUtils.getNonEmptyStr(mProperties.getProperty(Settings.PROP_APP_HARDWARE_KEYS), true);
        for (String k: keys.split("\\|"))
            if (!StrUtils.isEmptyStr(k))
                addKeyView(k);
    }

    public HardwareKeysDialog(String dlgName, BaseActivity activity, Properties properties,
                              String title, View grid, boolean showNegativeButton, boolean windowed) {
        super(dlgName, activity, title, showNegativeButton, windowed);
        mInflater = LayoutInflater.from(getContext());
        mProperties = properties;
        mGrid = grid;
        tvKeyScancode = mGrid.findViewById(R.id.tv_key_scancode);
        edtKeydown = mGrid.findViewById(R.id.edt_keydown);
        mFlKeys = mGrid.findViewById(R.id.keysFlowList);
        edtKeydown.setOnKeyListener((v, keyCode, event) -> {
            tvKeyScancode.setText("scan = " + event.getScanCode() +
                    ", code = " + getKeyDef(event));
            lastEvent = event;
            BackgroundThread.instance().postGUI(() -> {
                edtKeydown.setText("");
            }, 2000);
            return false;
        });
        btnAddKey = mGrid.findViewById(R.id.btn_add_key);
        btnAddKey.setBackgroundColor(colorGrayC);
        if (isEInk) Utils.setSolidButtonEink(btnAddKey);
        btnAddKey.setOnClickListener(v -> {
            if (lastEvent != null) {
                String keyMnemo = lastEvent.getScanCode() +
                        ", " + getKeyDef(lastEvent);
                if (!keys.contains(keyMnemo)) {
                    LinearLayout dicTag = addKeyView(keyMnemo);
                    mActivity.tintViewIcons(dicTag);
                } else {
                    mActivity.showToast(R.string.key_already_added);
                }
            }
        });
        fillExistingKeys();
        mCoolReader = (CoolReader) activity;
    }

    @NonNull
    private LinearLayout addKeyView(String keyMnemo) {
        View buttonView = mInflater.inflate(R.layout.tag_flow_item, null);
        LinearLayout dicTag = buttonView.findViewById(R.id.tag_flow_item_body);
        TextView tvText = buttonView.findViewById(R.id.tag_flow_item_text);
        tvText.setText(keyMnemo);
        dicTag.setPadding(5, 5, 5, 5);
        Utils.setDashedView(dicTag);
        mFlKeys.addView(dicTag);
        keys.add(keyMnemo);
        keyViews.add(buttonView);
        ImageView btnDel = buttonView.findViewById(R.id.tag_flow_value_del);
        btnDel.setOnClickListener(vv -> {
            removeKey(keyMnemo);
        });
        return dicTag;
    }

    private boolean closing = false;

    @Override
    protected void whenShow() {
        super.whenShow();
        edtKeydown.requestFocus();
    }

    @Override
    public void dismiss() {
        closing = true;
        super.dismiss();
    }

    @Override
    protected void onPositiveButtonClick() {
        String res = "";
        for (String s: keys) {
            if (res.equals("")) res = s;
            else res = res + "|" + s;
        }
        mProperties.setProperty(Settings.PROP_APP_HARDWARE_KEYS, res);
        super.onPositiveButtonClick();
    }

}
