const { Client } = require('pg');

const c = new Client({
  host: '127.0.0.1',
  port: 5432,
  database: 'monastery',
  user: 'postgres',
  password: 'admin'
});

async function seed() {
  await c.connect();

  await c.query(`INSERT INTO negocios(id, nombre, slug, color_primario, activo, creado_en)
    VALUES('00000000-0000-0000-0000-000000000001', 'Monastery Club', 'monastery', '#D4AF37', true, NOW())
    ON CONFLICT DO NOTHING`);
  console.log('Negocio OK');

  await c.query(`INSERT INTO auth_users(email, username, password, name, role, negocio_id, is_active, created_at, updated_at)
    VALUES($1,$2,$3,$4,$5,$6,$7,NOW(),NOW()) ON CONFLICT DO NOTHING`,
    ['admin@monastery.co', 'monastery', '$2b$10$HFq1EvwgiCdW/qMxmTUbQOb4BN3kkSXrZ0z29u7QhizKKYKBqVHKe', 'Administrador', 'ADMIN', '00000000-0000-0000-0000-000000000001', true]);
  console.log('User monastery OK');

  await c.query(`INSERT INTO auth_users(email, username, password, name, role, negocio_id, is_active, created_at, updated_at)
    VALUES($1,$2,$3,$4,$5,$6,$7,NOW(),NOW()) ON CONFLICT DO NOTHING`,
    ['admin@local.co', 'adminlocal', '$2b$10$qW2UDJMhQ/8VD1qZVyIKhuKNAZlxq6TNkdtrt2LpLmfBgWoFVgSMe', 'Admin Local', 'ADMIN', '00000000-0000-0000-0000-000000000001', true]);
  console.log('User adminlocal OK');

  const r = await c.query('SELECT username, email, role FROM auth_users');
  console.log('Usuarios:', r.rows);

  await c.end();
}

seed().catch(e => { console.error('Error:', e.message); process.exit(1); });
