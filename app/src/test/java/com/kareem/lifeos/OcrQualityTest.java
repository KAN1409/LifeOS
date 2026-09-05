package com.kareem.lifeos;

import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

public class OcrQualityTest {
    @Test public void arabicSearchNormalizationPreservesMeaningButNormalizesVariants(){assertEquals("اجمالي 1250 جنيه",OcrQuality.normalizeForSearch("إجمالى ١٢٥٠ جنيه"));}
    @Test public void criticalTokensKeepMoneyDateAndEmail(){List<String> x=OcrQuality.criticalTokens("Total EGP 4,250 on 05/09/2026. mail a@b.com");assertTrue(x.toString().contains("EGP 4,250"));assertTrue(x.toString().contains("05/09/2026"));assertTrue(x.toString().contains("a@b.com"));}
    @Test public void benchmarkErrorIsZeroForEquivalentArabicDigitForms(){assertEquals(0.0,OcrBenchmark.cer("المبلغ ١٢٥٠","المبلغ 1250"),0.0001);assertEquals(0.0,OcrBenchmark.wer("المبلغ ١٢٥٠","المبلغ 1250"),0.0001);}
    @Test public void mergeDistinctDoesNotDuplicateEquivalentLines(){assertEquals("الإجمالي 100",OcrQuality.mergeDistinct("الإجمالي 100","الاجمالي 100"));}
}
