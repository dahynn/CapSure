-- 고객 채널과 운영 채널을 계정 역할로 분리한다.

ALTER TABLE public.usr_user
    ADD COLUMN access_role VARCHAR(30) NOT NULL DEFAULT 'ROLE_USER',
    ADD CONSTRAINT chk_usr_user_access_role
        CHECK (access_role IN ('ROLE_USER', 'ROLE_ADMIN'));

UPDATE public.usr_user
SET access_role = 'ROLE_ADMIN',
    updated_at = NOW()
WHERE email = 'demo@example.com';

CREATE INDEX ix_usr_user_access_role
    ON public.usr_user (access_role);
