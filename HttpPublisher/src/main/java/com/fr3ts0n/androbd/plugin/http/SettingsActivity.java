package com.fr3ts0n.androbd.plugin.http;

import android.Manifest;
import android.os.Build;
import android.os.Bundle;
import android.preference.MultiSelectListPreference;
import android.preference.PreferenceActivity;

import java.util.Arrays;
import java.util.Comparator;

public class SettingsActivity extends PreferenceActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(com.fr3ts0n.androbd.plugin.R.xml.settings);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            requestPermissions(new String[]{ Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION },
                    0);
        }
    }

    /**
     * Initialize selection of data items to be published
     * from list of data items currently known
     */
    void initItemSelection()
    {
        // get collected data items from set of known data items
        CharSequence[] items = new CharSequence[HttpPlugin.mKnownItems.size()];
        items = HttpPlugin.mKnownItems.toArray(items);
        // sort array to make it readable
        Arrays.sort(items, new Comparator<CharSequence>()
        {
            @Override
            public int compare(CharSequence o1, CharSequence o2)
            {
                return o1.toString().compareTo(o2.toString());
            }
        });

        // assign list to preference selection
        MultiSelectListPreference pref = (MultiSelectListPreference)findPreference(HttpPlugin.ITEMS_SELECTED);
        pref.setEntries(items);
        pref.setEntryValues(items);
    }
}
