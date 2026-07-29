\set ON_ERROR_STOP on
\getenv cuscrud_app_password CUSCRUD_APP_PASSWORD

SELECT format(
    'CREATE ROLE cuscrud_app LOGIN PASSWORD %L',
    :'cuscrud_app_password'
)
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'cuscrud_app') \gexec

SELECT format(
    'ALTER ROLE cuscrud_app WITH LOGIN PASSWORD %L',
    :'cuscrud_app_password'
)
WHERE EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'cuscrud_app') \gexec

SELECT 'CREATE DATABASE cuscrud OWNER cuscrud_app'
WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = 'cuscrud') \gexec

ALTER DATABASE cuscrud OWNER TO cuscrud_app;
ALTER DATABASE cuscrud SET timezone TO 'America/Recife';
