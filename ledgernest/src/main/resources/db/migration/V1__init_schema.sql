-- =====================================================
-- LEDGERNEST DATABASE SCHEMA
-- V1__init_schema.sql
-- All 8 tables for production-grade financial platform
-- =====================================================


-- =====================================================
-- 1. TENANTS (Companies using LedgerNest)
-- =====================================================
CREATE TABLE tenants (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(200)    NOT NULL,
    vat_number      VARCHAR(20),
    email           VARCHAR(255)    NOT NULL UNIQUE,
    phone           VARCHAR(20),
    address         TEXT,
    country_code    CHAR(2)         NOT NULL DEFAULT 'IE',
    plan_type       VARCHAR(20)     NOT NULL DEFAULT 'FREE',
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_plan_type CHECK (plan_type IN ('FREE', 'PRO', 'ENTERPRISE'))
);

COMMENT ON TABLE tenants IS 'Companies registered on LedgerNest platform';


-- =====================================================
-- 2. USERS (People inside each company)
-- =====================================================
CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID            NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    email           VARCHAR(255)    NOT NULL UNIQUE,
    password_hash   VARCHAR(255)    NOT NULL,
    first_name      VARCHAR(100),
    last_name       VARCHAR(100),
    role            VARCHAR(20)     NOT NULL DEFAULT 'VIEWER',
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    last_login      TIMESTAMPTZ,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_role CHECK (role IN ('OWNER', 'ADMIN', 'ACCOUNTANT', 'VIEWER'))
);

COMMENT ON TABLE users IS 'Users belonging to a tenant company';

-- Indexes for fast lookups
CREATE INDEX idx_users_email      ON users(email);
CREATE INDEX idx_users_tenant_id  ON users(tenant_id);


-- =====================================================
-- 3. CLIENTS (Customers of each SME)
-- =====================================================
CREATE TABLE clients (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID            NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    name            VARCHAR(200)    NOT NULL,
    email           VARCHAR(255),
    phone           VARCHAR(20),
    address         TEXT,
    vat_number      VARCHAR(20),
    country_code    CHAR(2)         DEFAULT 'IE',
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE clients IS 'Clients that tenants send invoices to';

CREATE INDEX idx_clients_tenant_id ON clients(tenant_id);


-- =====================================================
-- 4. INVOICES (Core of the platform)
-- =====================================================
CREATE TABLE invoices (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID            NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    client_id       UUID            NOT NULL REFERENCES clients(id),
    created_by      UUID            NOT NULL REFERENCES users(id),
    invoice_number  VARCHAR(50)     NOT NULL,
    status          VARCHAR(20)     NOT NULL DEFAULT 'DRAFT',
    issue_date      DATE            NOT NULL DEFAULT CURRENT_DATE,
    due_date        DATE            NOT NULL,
    subtotal        NUMERIC(15,2)   NOT NULL DEFAULT 0.00,
    vat_amount      NUMERIC(15,2)   NOT NULL DEFAULT 0.00,
    total_amount    NUMERIC(15,2)   NOT NULL DEFAULT 0.00,
    currency        CHAR(3)         NOT NULL DEFAULT 'EUR',
    notes           TEXT,
    pdf_url         TEXT,
    paid_at         TIMESTAMPTZ,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_invoice_status CHECK (status IN ('DRAFT', 'SENT', 'PAID', 'OVERDUE', 'CANCELLED')),
    CONSTRAINT uq_invoice_number   UNIQUE (tenant_id, invoice_number)
);

COMMENT ON TABLE invoices IS 'Invoices created by tenants for their clients';

CREATE INDEX idx_invoices_tenant_id  ON invoices(tenant_id);
CREATE INDEX idx_invoices_client_id  ON invoices(client_id);
CREATE INDEX idx_invoices_status     ON invoices(status);
CREATE INDEX idx_invoices_due_date   ON invoices(due_date);


-- =====================================================
-- 5. INVOICE LINE ITEMS (Each row inside invoice)
-- =====================================================
CREATE TABLE invoice_line_items (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_id      UUID            NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    description     TEXT            NOT NULL,
    quantity        NUMERIC(10,2)   NOT NULL DEFAULT 1.00,
    unit_price      NUMERIC(15,2)   NOT NULL,
    vat_rate        NUMERIC(5,2)    NOT NULL DEFAULT 23.00,
    vat_amount      NUMERIC(15,2)   NOT NULL DEFAULT 0.00,
    line_total      NUMERIC(15,2)   NOT NULL DEFAULT 0.00,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_vat_rate CHECK (vat_rate IN (0.00, 9.00, 13.50, 23.00)),
    CONSTRAINT chk_quantity CHECK (quantity > 0),
    CONSTRAINT chk_unit_price CHECK (unit_price >= 0)
);

COMMENT ON TABLE invoice_line_items IS 'Individual line items within an invoice';

CREATE INDEX idx_line_items_invoice_id ON invoice_line_items(invoice_id);


-- =====================================================
-- 6. EXPENSES (Business costs)
-- =====================================================
CREATE TABLE expenses (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID            NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    submitted_by        UUID            NOT NULL REFERENCES users(id),
    approved_by         UUID            REFERENCES users(id),
    category            VARCHAR(50)     NOT NULL,
    description         TEXT,
    amount              NUMERIC(15,2)   NOT NULL,
    vat_reclaimable     NUMERIC(15,2)   NOT NULL DEFAULT 0.00,
    receipt_url         TEXT,
    expense_date        DATE            NOT NULL,
    status              VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_expense_category CHECK (category IN (
        'TRAVEL', 'SOFTWARE', 'OFFICE', 'MEALS',
        'EQUIPMENT', 'MARKETING', 'UTILITIES', 'OTHER'
    )),
    CONSTRAINT chk_expense_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    CONSTRAINT chk_amount CHECK (amount > 0)
);

COMMENT ON TABLE expenses IS 'Business expenses logged by tenant users';

CREATE INDEX idx_expenses_tenant_id    ON expenses(tenant_id);
CREATE INDEX idx_expenses_submitted_by ON expenses(submitted_by);
CREATE INDEX idx_expenses_status       ON expenses(status);
CREATE INDEX idx_expenses_date         ON expenses(expense_date);


-- =====================================================
-- 7. VAT RETURNS (Quarterly tax summaries)
-- =====================================================
CREATE TABLE vat_returns (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID            NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    period_start        DATE            NOT NULL,
    period_end          DATE            NOT NULL,
    total_sales         NUMERIC(15,2)   NOT NULL DEFAULT 0.00,
    vat_collected       NUMERIC(15,2)   NOT NULL DEFAULT 0.00,
    vat_reclaimable     NUMERIC(15,2)   NOT NULL DEFAULT 0.00,
    vat_owed            NUMERIC(15,2)   NOT NULL DEFAULT 0.00,
    status              VARCHAR(20)     NOT NULL DEFAULT 'DRAFT',
    generated_at        TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    submitted_at        TIMESTAMPTZ,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_vat_return_status CHECK (status IN ('DRAFT', 'SUBMITTED', 'FILED')),
    CONSTRAINT uq_vat_period UNIQUE (tenant_id, period_start, period_end)
);

COMMENT ON TABLE vat_returns IS 'Quarterly VAT return summaries for Revenue.ie filing';

CREATE INDEX idx_vat_returns_tenant_id ON vat_returns(tenant_id);
CREATE INDEX idx_vat_returns_period    ON vat_returns(period_start, period_end);


-- =====================================================
-- 8. AUDIT LOGS (Every action ever taken)
-- =====================================================
CREATE TABLE audit_logs (
    id              BIGSERIAL       PRIMARY KEY,
    tenant_id       UUID            NOT NULL REFERENCES tenants(id),
    user_id         UUID            REFERENCES users(id),
    action          VARCHAR(50)     NOT NULL,
    resource_type   VARCHAR(50)     NOT NULL,
    resource_id     UUID,
    old_values      JSONB,
    new_values      JSONB,
    ip_address      INET,
    user_agent      TEXT,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_action CHECK (action IN (
        'CREATE', 'UPDATE', 'DELETE',
        'LOGIN', 'LOGOUT', 'EXPORT',
        'STATUS_CHANGE', 'PASSWORD_CHANGE'
    ))
);

COMMENT ON TABLE audit_logs IS 'Immutable audit trail for compliance - never update or delete';

CREATE INDEX idx_audit_tenant_id     ON audit_logs(tenant_id);
CREATE INDEX idx_audit_user_id       ON audit_logs(user_id);
CREATE INDEX idx_audit_resource      ON audit_logs(resource_type, resource_id);
CREATE INDEX idx_audit_created_at    ON audit_logs(created_at);