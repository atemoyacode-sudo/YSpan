"""Check the saved Forge packaged-client evidence; this does not launch Minecraft."""
from pathlib import Path
import hashlib,json,re,zipfile,math
root=Path(__file__).resolve().parent
e=root/'evidence'
manifest=json.loads((root/'manifest.json').read_text())
execution=json.loads((e/'forge-execution-manifest.json').read_text())
for item in ['client_jar','server_jar']:
 artifact=execution[item]
 assert hashlib.sha256((root/'release'/artifact['name']).read_bytes()).hexdigest()==artifact['sha256']
 assert manifest['artifacts'][artifact['name']]['sha256']==artifact['sha256']
for name,digest in manifest['forge_sources'].items():
 assert hashlib.sha256((root/name).read_bytes()).hexdigest()==digest,name
assert execution['launch_target']=='fmlclient' and not execution['development_classes']
assert execution['forge']=='36.2.42' and execution['class_major_version']==52
with zipfile.ZipFile(root/'release'/execution['client_jar']['name']) as jar:
 assert 'META-INF/mods.toml' in jar.namelist() and 'fabric.mod.json' not in jar.namelist()
 assert 'yspan.refmap.json' in jar.namelist()
 assert '0.1.0-preview.3' in jar.read('META-INF/mods.toml').decode()
 for name in jar.namelist():
  if name.endswith('.class'):assert int.from_bytes(jar.read(name)[6:8],'big')==52,name
compat=(e/'forge-compat-client.log').read_text()
regression=(e/'forge-regression-client.log').read_text()
server=(e/'forge-server-interactions.log').read_text()
for log in [compat,regression]:
 assert 'Forge mod loading, version 36.2.42, for MC 1.16.5' in log
 assert 'Service=ModLauncher Env=CLIENT' in log
 assert 'Forge preview loaded; inactive until companion negotiation' in log
 assert not re.search(r'BOUNDARY_FAIL|REAL_PORTAL_FAIL|YSpan COMPAT.*FAIL|Companion data rejected|Mixin apply.*failed',log)
records=[json.loads(line) for line in (e/'forge-compat-results.jsonl').read_text().splitlines()]
assert [r['scenario'] for r in records]==['vanilla-first','companion','vanilla-after-companion','companion-return','disconnect']
assert all(r['result']=='PASS' for r in records) and 'ALL_SCENARIOS_PASS' in compat
for r in records:
 if r['scenario'] in ['vanilla-first','vanilla-after-companion','disconnect']:
  assert not r['active'] and not r['advertised'] and r['handshakes']==0 and r['sections']==0
  assert r['min_y']==0 and r['max_y']==256
 else:
  assert r['active'] and r['advertised'] and r['handshakes']>=1 and r['sections']>0
  assert r['y']==-2 and r['on_ground']
assert server.count('stage=compat-placed')==2
for stage,y in [('bottom',-63),('seam',0),('upper',256),('top',318),('end',8),('return',-2)]:
 assert 'BOUNDARY_PASS '+stage+' ' in regression
 assert re.search(r'stage='+stage+r'-verified pos=[\d.-]+,'+re.escape(str(float(y)))+r',[\d.-]+ target=\w+ onGround=true',server)
assert 'stage=broken' in server and 'target=AIR' in server
assert re.search(r'stage=placed .*target=COBBLESTONE onGround=true',server)
assert 'client placed=Block{minecraft:cobblestone}' in regression
portal=re.search(r'REAL_PORTAL cause=END_PORTAL fromY=(-?[\d.]+)',server)
assert portal and float(portal.group(1))<0 and 'REAL_PORTAL_PASS' in regression
walk=re.search(r'stage=walked pos=([\d.-]+),-2.0,([\d.-]+)',server)
assert walk;distance=math.hypot(float(walk.group(1))-8.5,float(walk.group(2))-8.5);assert distance>1
build=(e/'forge-build.log').read_text()
assert 'Negotiation checks passed: 20' in build and 'BUILD SUCCESSFUL' in build
# Existing public artifacts remain identical to Preview 2.
old=json.loads((e/'preview2-manifest.json').read_text())
for name,data in old['artifacts'].items():assert manifest['artifacts'][name]==data
result={'release':'v0.1.0.preview.3','result':'pass','loader':'Forge 36.2.42 / Minecraft 1.16.5','java':'Azul Java 8u512 (ARM)','packaged_production_jar':True,'negotiation_checks':20,'scenarios':[r['scenario'] for r in records],'walk_distance_blocks':distance,'height_boundaries_player_y':[-63,0,256,318],'negative_y_native_break_place':True,'end_and_return':True,'real_end_portal_from_y':float(portal.group(1)),'existing_fabric_and_server_artifacts_unchanged':True,'test_runtime_native_override':'LWJGL 3.3.2 for macOS ARM, not bundled','unverified':['friend actual environment','Windows/Linux and normal x86 launcher runtime','other Forge versions','other mods, OptiFine and shaders','long-running multiplayer performance','all other Minecraft versions and Bedrock']}
(root/'forge-verification.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n')
print(json.dumps(result,ensure_ascii=False,indent=2))
