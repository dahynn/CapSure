CREATE UNIQUE INDEX ux_pay_attempt_provider_payment_key
    ON public.pay_attempt (provider, provider_payment_key)
    WHERE provider_payment_key IS NOT NULL;
