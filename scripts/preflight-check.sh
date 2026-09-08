#!/bin/sh
set -eu

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

FAIL=0
warn() { echo "WARN: $1"; }
fail() { echo "FAIL: $1"; FAIL=1; }

echo "== 1. YAML parse =="
python3 - <<'EOF'
import glob, sys
bad = 0
for f in sorted(glob.glob("**/*.yml", recursive=True) + glob.glob("**/*.yaml", recursive=True)):
    if ".git/" in f:
        continue
    try:
        import yaml
        list(yaml.safe_load_all(open(f)))
    except Exception as e:  # noqa
        print(f"FAIL: {f}: {e}")
        bad = 1
sys.exit(bad)
EOF
[ $? -eq 0 ] || FAIL=1

echo "== 2. ansible syntax =="
if command -v ansible-playbook >/dev/null 2>&1; then
  for p in infra/ansible/playbooks/deploy-vm.yml infra/ansible/playbooks/deploy-k8s.yml infra/ansible/playbooks/install-docker.yml; do
    ansible-playbook --syntax-check "$p" >/dev/null 2>&1 || fail "syntax: $p"
  done
  echo "playbooks syntax OK"
else
  warn "ansible not installed, syntax check skipped"
fi

echo "== 3. bare string conditionals =="
if grep -rnE '^[[:space:]]+when: [a-z_]+$' infra/ansible/playbooks/ 2>/dev/null; then
  fail "bare string 'when:' found above (use '| bool')"
else
  echo "conditionals OK"
fi

echo "== 4. cross-play vars in deploy-k8s.yml =="
python3 - <<'EOF'
import re, sys, yaml
plays = yaml.safe_load(open("infra/ansible/playbooks/deploy-k8s.yml"))
magic = {"ansible_user", "ansible_host", "ansible_port", "ansible_connection",
         "playbook_dir", "inventory_dir", "inventory_hostname", "groups",
         "hostvars", "item", "ansible_loop_var", "role_path", "ansible_check_mode"}
bad = 0
for i, play in enumerate(plays):
    defined = set((play.get("vars") or {}).keys()) | magic
    tasks_blob = yaml.dump(play.get("tasks") or [])
    for r in sorted(set(re.findall(r"register:\s*([a-z_][a-z0-9_]*)", tasks_blob))):
        defined.add(r)
    blob = tasks_blob
    for v in sorted(set(re.findall(r"\{\{\s*([a-z_][a-z0-9_]*)\s*(?:\.[^}]*)?\}\}", blob))):
        base = v
        if base not in defined and not base.startswith("ansible_"):
            print(f"WARN: play {i} uses '{{{{ {v} }}}}' not defined in its vars")
print("vars check done")
EOF

echo "== 5. scripts referenced by CI =="
for s in $(grep -rhoE '\./scripts/[a-z0-9_.-]+\.sh' .github/workflows/ | sort -u); do
  if [ ! -f "$s" ]; then
    fail "referenced by CI but missing: $s"
    continue
  fi
  if ! git ls-files --error-unmatch "$s" >/dev/null 2>&1; then
    fail "not committed: $s"
  fi
  mode=$(git ls-files -s -- "$s" | awk '{print $1}')
  if [ "$mode" != "100755" ]; then
    fail "not executable in index ($mode): $s"
  fi
  sh -n "$s" || fail "sh syntax: $s"
done
echo "scripts check done"

echo "== 6. k8s paths referenced by playbooks =="
for p in k8s/namespace.yaml k8s/configmap.yaml k8s/hpa.yaml; do
  [ -f "$p" ] || fail "missing: $p"
done
for d in controller analytics alerts data-gateway data-simulator dashboard observability metrics-server; do
  [ -d "k8s/$d" ] || [ -f "k8s/$d" ] || fail "missing: k8s/$d"
done
echo "paths check done"

if [ "$FAIL" -ne 0 ]; then
  echo "PREFLIGHT FAILED"
  exit 1
fi
echo "PREFLIGHT OK"
