package com.trianguloy.urlchecker.modules;

import android.content.Context;

import com.trianguloy.urlchecker.R;
import com.trianguloy.urlchecker.dialogs.MainDialog;
import com.trianguloy.urlchecker.modules.list.ShortenUrlModule;

public class ShortenUrlModuleData extends AModuleData {

    public static final String ID = "shorten_url_module";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public int getName() {
        return R.string.module_shorten_url;
    }

    @Override
    public AModuleDialog getDialog(MainDialog mainDialog) {
        return new ShortenUrlModule(mainDialog);
    }

    @Override
    public boolean isEnabledByDefault() {
        return false; // Set to true if you want it enabled by default
    }

    @Override
    public Automation[] getAutomations() {
        return new Automation[0]; // No automations for this module yet
    }
}