-- Non-secret, deterministic seed data. Adjust permission matrix before admin implementation.
INSERT INTO roles (code, name, description, is_system) VALUES
('CUSTOMER', 'Customer', 'Customer shopping permissions', TRUE),
('ADMIN', 'Administrator', 'Platform administration permissions', TRUE),
('OPERATIONS', 'Operations', 'Fulfilment and inventory operations', TRUE),
('SUPPORT', 'Support', 'Customer support operations', TRUE);

INSERT INTO permissions (code, description) VALUES
('PRODUCT_VIEW', 'View catalogue products'),
('PRODUCT_CREATE', 'Create catalogue products'),
('PRODUCT_UPDATE', 'Update catalogue products'),
('INVENTORY_VIEW', 'View inventory'),
('INVENTORY_ADJUST', 'Adjust inventory with audit'),
('ORDER_VIEW_OWN', 'View own orders'),
('ORDER_VIEW_ALL', 'View orders in authorised operations'),
('ORDER_MANAGE', 'Manage authorised order operations'),
('SHIPMENT_MANAGE', 'Manage shipments'),
('REFUND_MANAGE', 'Manage authorised refunds'),
('REVIEW_MODERATE', 'Moderate product reviews'),
('PROMOTION_MANAGE', 'Manage promotions and coupons'),
('USER_MANAGE', 'Manage users with least privilege'),
('REPORT_VIEW', 'View administrative reports');

-- Explicit initial grant examples; expand after reviewing the full permission matrix.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE (r.code = 'CUSTOMER' AND p.code IN ('PRODUCT_VIEW','ORDER_VIEW_OWN'))
   OR (r.code = 'OPERATIONS' AND p.code IN ('PRODUCT_VIEW','INVENTORY_VIEW','INVENTORY_ADJUST','ORDER_VIEW_ALL','ORDER_MANAGE','SHIPMENT_MANAGE'))
   OR (r.code = 'SUPPORT' AND p.code IN ('PRODUCT_VIEW','ORDER_VIEW_ALL'))
   OR (r.code = 'ADMIN');
-- This is a seed baseline, not the final authorization policy. No default privileged USER rows created.
