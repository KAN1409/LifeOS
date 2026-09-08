#!/usr/bin/env python3
from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET

ROOT=Path(__file__).resolve().parents[1]
ANDROID='{http://schemas.android.com/apk/res/android}'
errors=[]
warn=[]

def req(cond,msg):
    if not cond: errors.append(msg)

def get(node,name,default=None):
    return node.attrib.get(ANDROID+name,default)

manifest=ET.parse(ROOT/'app/src/main/AndroidManifest.xml').getroot()
app=manifest.find('application')
req(manifest.attrib.get('package')=='com.kareem.lifeos','Manifest package must remain com.kareem.lifeos')
req(app is not None,'application element missing')
if app is not None:
    req(get(app,'allowBackup')=='false','android:allowBackup must be false')
    req(get(app,'fullBackupContent')=='false','android:fullBackupContent must be false')
    req(bool(get(app,'dataExtractionRules')),'android:dataExtractionRules must be set')
    req(get(app,'networkSecurityConfig')=='@xml/network_security_config','networkSecurityConfig must point to @xml/network_security_config')

    for a in app.findall('activity'):
        name=get(a,'name','')
        exported=get(a,'exported')
        if name=='.FeedActivity':
            req(exported=='true','FeedActivity must be exported for launcher')
        else:
            req(exported!='true',f'Non-launcher activity unexpectedly exported: {name}')

    for tag in ('service','receiver','provider'):
        for node in app.findall(tag):
            if get(node,'exported')=='true':
                name=get(node,'name','')
                permission=get(node,'permission','')
                req(bool(permission),f'Exported {tag} lacks an Android permission boundary: {name}')

net=ET.parse(ROOT/'app/src/main/res/xml/network_security_config.xml').getroot()
base=net.find('base-config')
req(base is not None and base.attrib.get('cleartextTrafficPermitted')=='false','Base network security must block cleartext')
allowed=[]
for dc in net.findall('domain-config'):
    if dc.attrib.get('cleartextTrafficPermitted')=='true':
        allowed += [(d.text or '').strip() for d in dc.findall('domain')]
req(set(allowed).issubset({'127.0.0.1','localhost'}),f'Cleartext allowlist contains non-local domains: {allowed}')

gradle=(ROOT/'app/build.gradle').read_text(encoding='utf-8')
req("applicationId 'com.kareem.lifeos'" in gradle,'applicationId changed from com.kareem.lifeos')
m=re.search(r'versionCode\s+(\d+)',gradle)
req(bool(m),'versionCode missing')
if m: req(int(m.group(1))>=46,'Full-QA build must be versionCode >= 46')

# Critical source contract: user-facing functional capability registry must be provider-backed.
registry=(ROOT/'app/src/main/java/com/kareem/lifeos/FunctionalCapabilityRegistry.java').read_text(encoding='utf-8')
for token in ('ConversationRepository.count','ObligationRepository.count','DecisionRepository.count','VoiceMemoryRepository.count','FileRepository.count','ProjectRepository.count','PlaceRepository.count','ContactPersonRepository.count','CalendarEventRepository'):
    req(token in registry,f'Functional capability registry lost provider backing: {token}')

print(f'Static QA: {len(errors)} fail, {len(warn)} warn')
for x in errors: print('FAIL:',x)
for x in warn: print('WARN:',x)
if errors: sys.exit(1)
