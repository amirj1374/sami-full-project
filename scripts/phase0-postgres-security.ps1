param([string]$Project = "sami-phase0-security-$([Guid]::NewGuid().ToString('N').Substring(0,8))")
$container = "$Project-db"
$network = "$Project-net"
$ErrorActionPreference = 'Stop'
try {
  docker network create $network | Out-Null
  docker run -d --name $container --network $network -e POSTGRES_PASSWORD=test -e POSTGRES_DB=security postgres:16-alpine | Out-Null
  $ready = $false
  for ($i = 0; $i -lt 30; $i++) {
    if ((docker exec $container pg_isready -U postgres -d security 2>$null) -match 'accepting connections') { $ready = $true; break }
    Start-Sleep 1
  }
  if (-not $ready) { throw "Temporary PostgreSQL instance did not become ready" }
  $sql = @'
CREATE TABLE user_company_roles (id bigint primary key, tenant_id bigint not null, user_id bigint not null, company_id bigint not null, is_active boolean not null default true);
CREATE TABLE user_branch_grants (assignment_id bigint not null, tenant_id bigint not null, branch_id bigint not null, is_active boolean not null default true);
CREATE TABLE branches (id bigint primary key, tenant_id bigint not null, company_id bigint not null, is_active boolean not null default true);
INSERT INTO user_company_roles VALUES (10,1,7,100,true),(20,2,7,200,true);
INSERT INTO user_branch_grants VALUES (10,1,1000,true),(20,2,2000,true);
INSERT INTO branches VALUES (1000,1,100,true),(2000,2,200,true);
DO $$ BEGIN
  IF NOT EXISTS (SELECT 1 FROM user_company_roles a WHERE a.tenant_id=1 AND a.user_id=7 AND a.company_id=100 AND a.is_active) THEN RAISE EXCEPTION 'valid company grant denied'; END IF;
  IF EXISTS (SELECT 1 FROM user_company_roles a WHERE a.tenant_id=1 AND a.user_id=7 AND a.company_id=200 AND a.is_active) THEN RAISE EXCEPTION 'cross-company grant leaked'; END IF;
  IF EXISTS (SELECT 1 FROM user_company_roles a JOIN user_branch_grants g ON g.assignment_id=a.id AND g.tenant_id=a.tenant_id JOIN branches b ON b.id=g.branch_id AND b.tenant_id=g.tenant_id WHERE a.tenant_id=1 AND a.user_id=7 AND a.company_id=100 AND g.branch_id=2000 AND a.is_active AND g.is_active AND b.is_active AND b.company_id=a.company_id) THEN RAISE EXCEPTION 'cross-tenant branch leaked'; END IF;
  UPDATE user_branch_grants SET is_active=false WHERE assignment_id=10;
  IF EXISTS (SELECT 1 FROM user_company_roles a JOIN user_branch_grants g ON g.assignment_id=a.id WHERE a.id=10 AND a.is_active AND g.is_active) THEN RAISE EXCEPTION 'revoked grant remained active'; END IF;
  UPDATE user_company_roles SET company_id=999 WHERE id=10;
  IF EXISTS (SELECT 1 FROM user_company_roles a JOIN user_branch_grants g ON g.assignment_id=a.id WHERE a.id=10 AND a.company_id=100 AND a.is_active AND g.is_active) THEN RAISE EXCEPTION 'altered company context accepted'; END IF;
END $$;
'@
  $sql | docker exec -i $container psql -U postgres -d security -v ON_ERROR_STOP=1 | Out-Null
  if ($LASTEXITCODE -ne 0) { throw "PostgreSQL security assertions failed" }
  Write-Output "PASS: PostgreSQL grant isolation, revocation, stale context and altered-company checks"
} finally {
  docker rm -f $container 2>$null | Out-Null
  docker network rm $network 2>$null | Out-Null
}
