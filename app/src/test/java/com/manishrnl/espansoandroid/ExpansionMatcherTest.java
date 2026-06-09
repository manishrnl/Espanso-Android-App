package com.manishrnl.espansoandroid;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class ExpansionMatcherTest {
    private final List<Shortcut> shortcuts = Arrays.asList(
            new Shortcut(";email", "person@example.com", false, 1, "", "Email"),
            new Shortcut(";sig", "Regards", true, 1, "", "Email")
    );

    @Test
    public void findsImmediateAndAfterSpaceMatches() {
        ExpansionMatcher.Match immediate =
                ExpansionMatcher.findExpansion(shortcuts, "Hi ;email", 9);
        assertNotNull(immediate);
        assertEquals(3, immediate.start);
        assertEquals(9, immediate.end);

        ExpansionMatcher.Match afterSpace =
                ExpansionMatcher.findExpansion(shortcuts, ";sig ", 5);
        assertNotNull(afterSpace);
        assertEquals(0, afterSpace.start);
        assertEquals(5, afterSpace.end);
        assertEquals(true, afterSpace.appendSpace);
    }

    @Test
    public void doesNotExpandAfterSpaceShortcutBeforeSpace() {
        assertNull(ExpansionMatcher.findExpansion(shortcuts, ";sig", 4));
    }

    @Test
    public void findsCurrentSuggestionFragment() {
        assertEquals(
                ";em",
                ExpansionMatcher.findSuggestionFragment(shortcuts, "Hello ;em", 9, 6)
        );
        assertEquals(
                "",
                ExpansionMatcher.findSuggestionFragment(shortcuts, "Hello xyz", 9, 6)
        );
    }

    @Test
    public void removesWholeCodePointForUndoDetection() {
        assertEquals("ab", ExpansionMatcher.removeCodePointBefore("ab😀", 4));
    }
}
