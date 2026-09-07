#!/usr/bin/env python3
from pathlib import Path
import json,re,sys,xml.etree.ElementTree as ET

ROOT=Path(__file__).resolve().parents[1]
SRC=ROOT/'app/src/main/java'
TESTS=[ROOT/'app/src/test/java',ROOT/'app/src/androidTest/java']
ANDROID='{http://schemas.android.com/apk/res/android}'
fail=[]; warn=[]; info=[]; recommendations=[]

def add_fail(x): fail.append(x)
def add_warn(x): warn.append(x)
def text_files(root):
    return [p for p in root.rglob('*') if p.is_file() and p.suffix in {'.java','.kt','.xml','.gradle','.py','.md'}]

manifest=ET.parse(ROOT/'app/src/main/AndroidManifest.xml').getroot(); app=manifest.find('application')
activities=[]; components=[]; permissions=[]
for p in manifest.findall('uses-permission'):
    permissions.append(p.attrib.get(ANDROID+'name',''))
if app is not None:
    for tag in ('activity','service','receiver','provider'):
        for n in app.findall(tag):
            name=n.attrib.get(ANDROID+'name',''); exp=n.attrib.get(ANDROID+'exported','false'); perm=n.attrib.get(ANDROID+'permission','')
            components.append({'type':tag,'name':name,'exported':exp,'permission':perm})
            if tag=='activity': activities.append(name)
            if exp=='true' and tag!='activity' and not perm:add_fail(f'Exported {tag} lacks permission boundary: {name}')

# Every declared Activity must be explicitly named by instrumentation coverage.
android_test='\n'.join(p.read_text('utf-8',errors='ignore') for d in TESTS for p in d.rglob('*') if p.suffix in {'.java','.kt'})
missing=[]
for a in activities:
    simple=a.split('.')[-1]
    if simple not in android_test: missing.append(a)
if missing:add_fail('Manifest activities without instrumentation reference: '+', '.join(missing))

# Secret / accidental credential scan in production source only.
secret_patterns=[
    (re.compile(r'AIza[0-9A-Za-z_-]{20,}'),'Google API key literal'),
    (re.compile(r'\bsk-[A-Za-z0-9_-]{20,}'),'API secret literal'),
    (re.compile(r'(?i)bearer\s+[A-Za-z0-9._-]{24,}'),'Bearer token literal'),
    (re.compile(r'(?i)(api[_-]?key|client[_-]?secret|access[_-]?token)\s*[=:]\s*["\'][^"\']{12,}["\']'),'credential-like literal'),
]
prod_files=text_files(SRC)
for p in prod_files:
    s=p.read_text('utf-8',errors='ignore')
    for rx,label in secret_patterns:
        if rx.search(s):add_fail(f'{label} found in {p.relative_to(ROOT)}')
    for m in re.finditer(r'https?://[^\s"\')>]+',s):
        u=m.group(0)
        if u.startswith('http://') and not ('127.0.0.1' in u or 'localhost' in u):add_fail(f'Non-local cleartext URL in {p.relative_to(ROOT)}: {u}')

# SQLite migration hygiene and architecture debt inventory.
db_helpers=[]
for p in prod_files:
    if p.suffix!='.java':continue
    s=p.read_text('utf-8',errors='ignore')
    if 'extends SQLiteOpenHelper' in s:
        ver=None
        m=re.search(r'VERSION\s*=\s*(\d+)',s)
        if m:ver=int(m.group(1))
        elif 'super(' in s:
            mm=re.search(r'super\([^;]*?,\s*null\s*,\s*(\d+)\s*\)',s)
            if mm:ver=int(mm.group(1))
        empty=bool(re.search(r'onUpgrade\([^)]*\)\s*\{\s*\}',s,re.S))
        db_helpers.append({'file':str(p.relative_to(ROOT)),'version':ver,'empty_on_upgrade':empty})
        if ver and ver>1 and empty:add_fail(f'Database v{ver} has empty onUpgrade: {p.relative_to(ROOT)}')
        elif empty:add_warn(f'Database currently v1 has no future migration body: {p.relative_to(ROOT)}')

# Detect silent result caps that can turn into correctness bugs as the user's history grows.
cap_patterns=[
    (r'list\([^\n]{0,80},\s*600\)', 'Search/provider scan appears capped at 600 rows'),
    (r'list\([^\n]{0,80},\s*3000\)', 'Object reload appears capped at 3000 rows'),
    (r'list\([^\n]{0,80},\s*1000\)\.size\(\)', 'Count appears capped at 1000 rows'),
    (r'recentAndUpcoming\([^\n]{0,80},\s*1000\)', 'Calendar operation appears capped at 1000 rows'),
]
for p in prod_files:
    s=p.read_text('utf-8',errors='ignore')
    for pat,label in cap_patterns:
        if re.search(pat,s):recommendations.append(f'{label}: {p.relative_to(ROOT)}')

# Broad quality signals: swallowed exceptions, catch-all Throwable, direct thread creation.
metrics={'catch_ignored':0,'catch_throwable':0,'new_thread':0,'todo':0,'production_java_files':0}
for p in prod_files:
    if p.suffix!='.java':continue
    metrics['production_java_files']+=1;s=p.read_text('utf-8',errors='ignore')
    metrics['catch_ignored']+=len(re.findall(r'catch\s*\([^)]*\s+ignored\s*\)',s))
    metrics['catch_throwable']+=len(re.findall(r'catch\s*\(\s*Throwable\b',s))
    metrics['new_thread']+=s.count('new Thread(')
    metrics['todo']+=len(re.findall(r'(?i)\bTODO\b',s))
if metrics['catch_ignored']>20:recommendations.append(f"High swallowed-exception count ({metrics['catch_ignored']}); replace silent catches with typed failure/result telemetry where user-visible truth depends on them")
if metrics['catch_throwable']>10:recommendations.append(f"High catch(Throwable) count ({metrics['catch_throwable']}); narrow exception boundaries to avoid hiding programmer errors")

# Permission surface audit: powerful permissions are always surfaced in the report for human review.
powerful={
 'android.permission.SYSTEM_ALERT_WINDOW','android.permission.CALL_PHONE','android.permission.ANSWER_PHONE_CALLS','android.permission.MANAGE_OWN_CALLS',
 'android.permission.WRITE_CONTACTS','android.permission.GET_ACCOUNTS','android.permission.WRITE_CALENDAR','android.permission.USE_EXACT_ALARM','android.permission.SCHEDULE_EXACT_ALARM',
 'android.permission.RECORD_AUDIO','android.permission.ACCESS_FINE_LOCATION','android.permission.READ_CONTACTS','android.permission.READ_CALENDAR'}
powerful_declared=sorted(set(permissions)&powerful)
if powerful_declared:recommendations.append('Review necessity/least-privilege for powerful manifest permissions: '+', '.join(powerful_declared))

report={
 'status':'FAIL' if fail else 'PASS','failures':fail,'warnings':warn,'recommendations':sorted(set(recommendations)),
 'metrics':metrics,'manifest':{'activities':activities,'components':components,'permissions':permissions},'sqlite_helpers':db_helpers
}
out=ROOT/'app/build/qa';out.mkdir(parents=True,exist_ok=True);(out/'exhaustive_static.json').write_text(json.dumps(report,indent=2),encoding='utf-8')
print(f"Exhaustive static QA: {len(fail)} fail, {len(warn)} warn, {len(report['recommendations'])} recommendations")
for x in fail:print('FAIL:',x)
for x in warn:print('WARN:',x)
for x in report['recommendations']:print('RECOMMEND:',x)
if fail:sys.exit(1)
