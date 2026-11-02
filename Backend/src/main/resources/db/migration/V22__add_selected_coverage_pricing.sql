-- Educational fixture rates, not insurer tariffs. Existing quotes remain unchanged.
ALTER TABLE ins_product_coverage ADD COLUMN monthly_premium NUMERIC(18,2)
    CHECK (monthly_premium > 0);
UPDATE ins_product_coverage pc SET monthly_premium = CASE c.coverage_code
    WHEN 'DEMO_GENERAL_CANCER_DIAGNOSIS' THEN 19900.00
    WHEN 'DEMO_MINOR_CANCER_DIAGNOSIS' THEN 7000.00
    WHEN 'DEMO_CANCER_SURGERY' THEN 3000.00 END
FROM ins_coverage c, ins_product_version p
WHERE c.coverage_id = pc.coverage_id AND p.product_version_id = pc.product_version_id
  AND p.product_code = 'CAPSURE-DEMO-CANCER' AND p.is_simulation = TRUE;
