package com.prisma.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class MathUtilTest {

    @Test
    public void testTruncateStandard() {
        // 16.65789... doit devenir 16.65
        assertEquals(16.65, MathUtil.truncate(16.65789), 0.0);
        
        // 15.999... doit devenir 15.99 et non 16.00 (pas d'arrondi)
        assertEquals(15.99, MathUtil.truncate(15.999), 0.0);

        // 10.0 doit rester 10.00
        assertEquals(10.0, MathUtil.truncate(10.0), 0.0);
        
        // Valeur limite 0
        assertEquals(0.0, MathUtil.truncate(0.0), 0.0);
    }

    @Test
    public void testTruncateSpecialValues() {
        // NaN et Infinis ne doivent pas planter la troncature
        assertTrue(Double.isNaN(MathUtil.truncate(Double.NaN)));
        assertTrue(Double.isInfinite(MathUtil.truncate(Double.POSITIVE_INFINITY)));
        assertTrue(Double.isInfinite(MathUtil.truncate(Double.NEGATIVE_INFINITY)));
    }

    @Test
    public void testFormatMoyenneAvecVirguleFrancaise() {
        // L'affichage sur les bulletins doit utiliser la virgule française
        assertEquals("16,65", MathUtil.formatMoyenne(16.65));
        assertEquals("0,00",  MathUtil.formatMoyenne(0.0));
        assertEquals("20,00", MathUtil.formatMoyenne(20.0));
        assertEquals("9,99",  MathUtil.formatMoyenne(9.99));
    }

    @Test
    public void testFormatRang() {
        // 1er de classe
        assertEquals("1er / 25 élèves", MathUtil.formatRang(1, 25));
        // 2ème
        assertEquals("2ème / 25 élèves", MathUtil.formatRang(2, 25));
        // Dernier élève (seul dans la classe)
        assertEquals("1er / 1 élève", MathUtil.formatRang(1, 1));
    }

    @Test
    public void testCalculMoyennePonderee() {
        // CDC §2.3.1 : Français 18 × 2 = 36
        assertEquals(36.0, MathUtil.calculerMoyennePonderee(18.0, 2.0), 0.001);
        // Mathématiques 17.5 × 3 = 52.5
        assertEquals(52.5, MathUtil.calculerMoyennePonderee(17.5, 3.0), 0.001);
    }
}
