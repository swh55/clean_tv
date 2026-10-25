package pro.datascoop.tv;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** The design tokens: the constants every screen shares keep their values and their package. */
public class TokensTest {

    @Test
    public void the_package_is_the_new_one() {
        // The rename (pro.datascoop.afnon → pro.datascoop.tv) touched every source; this pins it here.
        assertEquals("pro.datascoop.tv", Tokens.class.getPackage().getName());
    }

    @Test
    public void lapis_gets_lighter_toward_700() {
        assertTrue("950 is the darkest", Tokens.LAPIS_950 < Tokens.LAPIS_900);
        assertTrue(Tokens.LAPIS_900 < Tokens.LAPIS_800);
        assertTrue(Tokens.LAPIS_800 < Tokens.LAPIS_700);
    }

    @Test
    public void motion_values_are_sane() {
        assertTrue(Tokens.APPROACH_MIN > 0);
        assertTrue(Tokens.MOTION_CALL_AUTO_DISMISS_MS >= Tokens.MOTION_CALL_SILENT_DISMISS_MS);
        assertTrue(Tokens.MOTION_OVERLAY_FADE_MS > 0);
        assertTrue(Tokens.MOTION_SAVED_LIFETIME_MS > Tokens.MOTION_OVERLAY_FADE_MS);
    }

    @Test
    public void layout_rects_hold_together() {
        assertTrue(Tokens.COURT_W > 0);
        assertTrue(Tokens.ARCH_W == 600);
        assertTrue(Tokens.RADIUS_TILE > 0);
        assertTrue(Tokens.FONT_SIZE_DISPLAY_XL > Tokens.FONT_SIZE_DISPLAY_XS);
    }
}
