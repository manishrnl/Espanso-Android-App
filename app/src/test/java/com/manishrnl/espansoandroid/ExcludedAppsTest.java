package com.manishrnl.espansoandroid;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ExcludedAppsTest {
    @Test
    public void defaultExcludedPackagesContainsFinancialApps() {
        String[] defaults = AppPreferences.getDefaultExcludedPackages();
        assertNotNull(defaults);
        assertTrue(defaults.length > 30);

        Set<String> set = new HashSet<>(Arrays.asList(defaults));
        assertTrue(set.contains("net.one97.paytm"));
        assertTrue(set.contains("com.phonepe.app"));
        assertTrue(set.contains("com.google.android.apps.nbu.paisa.user"));
        assertTrue(set.contains("com.navi.passport"));
        assertTrue(set.contains("com.sbi.yono"));
        assertTrue(set.contains("com.dreamplug.android.cred"));
    }

    @Test
    public void defaultExcludedPackagesIsCloned() {
        String[] first = AppPreferences.getDefaultExcludedPackages();
        String[] second = AppPreferences.getDefaultExcludedPackages();
        assertArrayEquals(first, second);

        // Mutating returned array does not mutate internal array
        first[0] = "mutated.package";
        assertEquals("com.navi.passport", second[0]);
    }
}
