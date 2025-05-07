package com.trianguloy.urlchecker.modules.list;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.Toast; // Import Toast

import com.trianguloy.urlchecker.R;
import com.trianguloy.urlchecker.activities.ModulesActivity;
import com.trianguloy.urlchecker.dialogs.MainDialog;
import com.trianguloy.urlchecker.modules.AModuleConfig;
import com.trianguloy.urlchecker.modules.AModuleData;
import com.trianguloy.urlchecker.modules.AModuleDialog;
import com.trianguloy.urlchecker.modules.AutomationRules;
import com.trianguloy.urlchecker.modules.companions.CTabs;
import com.trianguloy.urlchecker.modules.companions.Flags;
import com.trianguloy.urlchecker.modules.companions.Incognito;
import com.trianguloy.urlchecker.modules.companions.LastOpened;
import com.trianguloy.urlchecker.modules.companions.ShareUtility;
import com.trianguloy.urlchecker.modules.companions.Size;
import com.trianguloy.urlchecker.modules.list.ShortenUrlModule.ShortenUrlCallback; // Import ShortenUrlCallback
import com.trianguloy.urlchecker.url.UrlData;
import com.trianguloy.urlchecker.utilities.generics.GenericPref;
import com.trianguloy.urlchecker.utilities.methods.AndroidUtils;
import com.trianguloy.urlchecker.utilities.methods.JavaUtils;
import com.trianguloy.urlchecker.utilities.methods.PackageUtils;
import com.trianguloy.urlchecker.utilities.methods.UrlUtils;
import com.trianguloy.urlchecker.utilities.wrappers.IntentApp;
import com.trianguloy.urlchecker.utilities.wrappers.RejectionDetector;

import java.util.List;
import java.util.Objects;


/** This module contains an open and share buttons */
public class OpenModule extends AModuleData {

    public static GenericPref.Bool CLOSEOPEN_PREF(Context cntx) {
        return new GenericPref.Bool("open_closeopen", true, cntx);
    }

    public static GenericPref.Bool NOREFERRER_PREF(Context cntx) {
        return new GenericPref.Bool("open_noReferrer", false, cntx);
    }

    public static GenericPref.Bool REJECTED_PREF(Context cntx) {
        return new GenericPref.Bool("open_rejected", true, cntx);
    }

    public static GenericPref.Enumeration<Size> ICONSIZE_PREF(Context cntx) {
        return new GenericPref.Enumeration<>("open_iconsize", Size.NORMAL, Size.class, cntx);
    }

    @Override
    public String getId() {
        return "open";
    }

    @Override
    public int getName() {
        return R.string.mOpen_name;
    }

    @Override
    public AModuleDialog getDialog(MainDialog cntx) {
        return new OpenDialog(cntx);
    }

    @Override
    public AModuleConfig getConfig(ModulesActivity cntx) {
        return new OpenConfig(cntx);
    }

    @Override
    public List<AutomationRules.Automation<AModuleDialog>> getAutomations() {
        return (List<AutomationRules.Automation<AModuleDialog>>) (List<?>) OpenDialog.AUTOMATIONS;
    }
}

class OpenDialog extends AModuleDialog {

    static final List<AutomationRules.Automation<OpenDialog>> AUTOMATIONS = List.of(
            new AutomationRules.Automation<>("open", R.string.auto_open, dialog ->
                    dialog.openUrl(0)),
            new AutomationRules.Automation<>("share", R.string.auto_share, dialog ->
                    dialog.shareUtility.shareUrl(null)), // Modified to pass null
            new AutomationRules.Automation<>("copy", R.string.auto_copy, dialog ->
                    dialog.shareUtility.copyUrl()),
            new AutomationRules.Automation<>("ctabs", R.string.auto_ctabs, dialog ->
                    dialog.cTabs.setState(true)),
            new AutomationRules.Automation<>("incognito", R.string.auto_incognito, dialog ->
                    dialog.incognito.setState(true)),
            new AutomationRules.Automation<>("close", R.string.auto_close, dialog -> dialog.getActivity().finish())
    );

    private final GenericPref.Bool closeOpenPref;
    private final GenericPref.Bool noReferrerPref;
    private final GenericPref.Bool rejectedPref;
    private final GenericPref.Enumeration<Size> iconSizePref;

    private final LastOpened lastOpened;
    private final CTabs cTabs;
    private final Incognito incognito;
    private final RejectionDetector rejectionDetector;
    private final ShareUtility.Dialog shareUtility;

    private List<IntentApp> intentApps;
    private Button btn_open;
    private ImageButton btn_openWith;
    private View openParent;
    private Menu menu;
    private PopupMenu popup;
    private ImageButton btn_shorten_and_share; // Declare the new button

    public OpenDialog(MainDialog dialog) {
        super(dialog);
        lastOpened = new LastOpened(dialog);
        cTabs = new CTabs(dialog);
        incognito = new Incognito(dialog);
        rejectionDetector = new RejectionDetector(dialog);
        shareUtility = new ShareUtility.Dialog(dialog);
        closeOpenPref = OpenModule.CLOSEOPEN_PREF(dialog);
        noReferrerPref = OpenModule.NOREFERRER_PREF(dialog);
        rejectedPref = OpenModule.REJECTED_PREF(dialog);
        iconSizePref = OpenModule.ICONSIZE_PREF(dialog);
    }

    @Override
    public int getLayoutId() {
        return R.layout.dialog_open;
    }

    @Override
    public void onInitialize(View views) {
        var intent = getActivity().getIntent();

        // ctabs
        cTabs.initFrom(intent, views.findViewById(R.id.ctabs));

        // incognito
        incognito.initFrom(intent, views.findViewById(R.id.mode_incognito));

        // init open
        openParent = views.findViewById(R.id.open_parent);
        btn_open = views.findViewById(R.id.open);
        btn_open.setOnClickListener(v -> openUrl(0));

        // init openWith
        btn_openWith = views.findViewById(R.id.open_with);
        btn_openWith.setOnClickListener(v -> showList());

        // init openWith popup
        popup = new PopupMenu(getActivity(), btn_open);
        popup.setOnMenuItemClickListener(item -> {
            openUrl(item.getItemId());
            return false;
        });
        menu = popup.getMenu();

        // share
        shareUtility.onInitialize(views);

        // Initialize and set OnClickListener for shorten and share button
        btn_shorten_and_share = views.findViewById(R.id.btn_shorten_and_share);
        btn_shorten_and_share.setOnClickListener(v -> {
            ShortenUrlModule shortenUrlModule = mainDialog.getModule(ShortenUrlModule.class);
            if (shortenUrlModule != null) {
                String originalUrl = mainDialog.getUrlData().url;
                if (originalUrl != null && !originalUrl.isEmpty()) {
                    shortenUrlModule.shortenAndCallback(originalUrl, new ShortenUrlCallback() {
                        @Override
                        public void onUrlShortened(String shortenedUrl) {
                            if (shortenedUrl != null && !shortenedUrl.isEmpty()) {
                                shareUtility.shareUrl(shortenedUrl);
                            } else {
                                mainDialog.runOnUiThread(() -> Toast.makeText(mainDialog, "Failed to shorten URL", Toast.LENGTH_SHORT).show());
                            }
                        }
                    });
                } else {
                     mainDialog.runOnUiThread(() -> Toast.makeText(mainDialog, "No URL to shorten", Toast.LENGTH_SHORT).show());
                }
            } else {
                 mainDialog.runOnUiThread(() -> Toast.makeText(mainDialog, "Shorten URL module not available", Toast.LENGTH_SHORT).show());
            }
        });

    }

    @Override
    public void onDisplayUrl(UrlData urlData) {
        updateSpinner(urlData.url);
    }

    // ------------------- Spinner -------------------

    /** Populates the spinner with the apps that can open it, in preference order */
    private void updateSpinner(String url) {
        intentApps = IntentApp.getOtherPackages(UrlUtils.getViewIntent(url, null), getActivity());

        // remove referrer
        if (noReferrerPref.get()) {
            var referrer = AndroidUtils.getReferrer(getActivity());
            JavaUtils.removeIf(intentApps, ri -> Objects.equals(ri.getPackage(), referrer));
        }

        // remove rejected if desired (and is not a non-view action, like share)
        // note: this will be called each time, so a rejected package will not be rejected again if the user changes the url and goes back. This is expected
        if (rejectedPref.get() && Intent.ACTION_VIEW.equals(getActivity().getIntent().getAction())) {
            var rejected = rejectionDetector.getPrevious(url);
            JavaUtils.removeIf(intentApps, ri -> Objects.equals(ri.getComponent(), rejected));
        }

        // check no apps
        if (intentApps.isEmpty()) {
            btn_open.setText(R.string.mOpen_noapps);
            btn_open.setCompoundDrawables(null, null, null, null);
            AndroidUtils.setEnabled(openParent, false);
            btn_open.setEnabled(false);
            btn_openWith.setVisibility(View.GONE);
            return;
        }

        // sort
        lastOpened.sort(intentApps, getUrl());

        // set
        var label = intentApps.get(0).getLabel(getActivity());
//        label = getActivity().getString(R.string.mOpen_with, label);\n" +
//                "        btn_open.setText(label);\n" +
//                "        btn_open.setCompoundDrawables(intentApps.get(0).getIcon(getActivity(), iconSizePref.get()), null, null, null);\n" +
//                "        AndroidUtils.setEnabled(openParent, true);\n" +
//                "        btn_open.setEnabled(true);\n" +
//                "        menu.clear();\n" +
//                "        if (intentApps.size() == 1) {\n" +
//                "            btn_openWith.setVisibility(View.GONE);\n" +
//                "        } else {\n" +
//                "            btn_openWith.setVisibility(View.VISIBLE);\n" +
//                "            for (int i = 1; i < intentApps.size(); i++) {\n" +
//                "                label = intentApps.get(i).getLabel(getActivity());\n" +
//                "//                label = getActivity().getString(R.string.mOpen_with, label);\n" +
//                "                menu.add(Menu.NONE, i, i, label);//.setIcon(intentApps.get(i).getIcon(getActivity()));\n" +
//                "            }\n" +
//                "        }\n" +
//                "\n" +
//                "    }\n" +
//                "\n" +
//                "    // ------------------- Buttons -------------------\n" +
//                "\n" +
//                "    /**\n" +
//                "     * Open url in a specific app\n" +
//                "     *\n" +
//                "     * @param index index from the intentApps list of the app to use\n" +
//                "     */\n" +
//                "    private void openUrl(int index) {\n" +
//                "        // get\n" +
//                "        if (index < 0 || index >= intentApps.size()) return;\n" +
//                "        var chosen = intentApps.get(index);\n" +
//                "\n" +
//                "        // update as preferred over the rest\n" +
//                "        lastOpened.prefer(chosen, intentApps, getUrl());\n" +
//                "\n" +
//                "        // open\n" +
//                "        var intent = new Intent(getActivity().getIntent());\n" +
//                "        if (Intent.ACTION_VIEW.equals(intent.getAction())) {\n" +
//                "            // preserve original VIEW intent\n" +
//                "            intent.setData(Uri.parse(getUrl()));\n" +
//                "            intent.setComponent(chosen.getComponent());\n" +
//                "        } else {\n" +
//                "            // replace with new VIEW intent\n" +
//                "            intent = UrlUtils.getViewIntent(getUrl(), chosen);\n" +
//                "        }\n" +
//                "\n" +
//                "        // ctabs\n" +
//                "        cTabs.apply(intent);\n" +
//                "\n" +
//                "        // incognito\n" +
//                "        incognito.apply(intent);\n" +
//                "\n" +
//                "        // apply flags from global data (probably set by flags module, if active) or by default\n" +
//                "        Flags.applyGlobalFlags(intent, this);\n" +
//                "\n" +
//                "        // rejection detector: mark as open\n" +
//                "        rejectionDetector.markAsOpen(getUrl(), chosen);\n" +
//                "\n" +
//                "        // open\n" +
//                "        PackageUtils.startActivity(\n" +
//                "                    chooser,\n" +
//                "                    R.string.mOpen_noapps,\n" +
//                "                    mainDialog\n" +
//                "            );\n" +
//                "            if (closeSharePref.get()) {\n" +
//                "                mainDialog.finish();\n" +
//                "            }\n" +
//                "        }\n" +
//                "\n" +
//                "        /** Copy the url */\n" +
//                "        public void copyUrl() {\n" +
//                "            AndroidUtils.copyToClipboard(mainDialog, R.string.mOpen_clipboard, mainDialog.getUrlData().url);\n" +
//                "            if (closeCopyPref.get()) {\n" +
//                "                mainDialog.finish();\n" +
//                "            }\n" +
//                "        }\n" +
//                "    }\n" +
//                "\n" +
//                "}\n" +
//                "\n" +
//                "class OpenConfig extends AModuleConfig {\n" +
//                "\n" +
//                "    public OpenConfig(ModulesActivity activity) {\n" +
//                "        super(activity);\n" +
//                "    }\n" +
//                "\n" +
//                "    @Override\n" +
//                "    public int getLayoutId() {\n" +
//                "        return R.layout.config_open;\n" +
//                "    }\n" +
//                "\n" +
//                "    @Override\n" +
//                "    public void onInitialize(View views) {\n" +
//                "        CTabs.PREF(getActivity()).attachToSpinner(views.findViewById(R.id.ctabs_pref), null);\n" +
//                "        Incognito.PREF(getActivity()).attachToSpinner(views.findViewById(R.id.incognito_pref), null);\n" +
//                "        OpenModule.CLOSEOPEN_PREF(getActivity()).attachToSwitch(views.findViewById(R.id.closeopen_pref));\n" +
//                "        OpenModule.NOREFERRER_PREF(getActivity()).attachToSwitch(views.findViewById(R.id.noReferrer));\n" +
//                "        OpenModule.REJECTED_PREF(getActivity()).attachToSwitch(views.findViewById(R.id.rejected));\n" +
//                "        LastOpened.PERDOMAIN_PREF(getActivity()).attachToSwitch(views.findViewById(R.id.perDomain));\n" +
//                "        OpenModule.ICONSIZE_PREF(getActivity()).attachToSpinner(views.findViewById(R.id.iconsize_pref), null);\n" +
//                "\n" +
//                "        // share\n" +
//                "        ShareUtility.onInitializeConfig(views, getActivity());\n" +
//                "    }\n" +
//                "}\n"