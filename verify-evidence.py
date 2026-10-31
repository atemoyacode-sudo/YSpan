from pathlib import Path
import json,re,hashlib,math
root=Path(__file__).resolve().parent;e=root/'evidence'
client=(e/'final-packaged-client.log').read_text()
server=(e/'final-packaged-server.log').read_text()
records=[json.loads(s) for s in (e/'probe-records.jsonl').read_text().splitlines()]
start=max(i for i,r in enumerate(records) if r['stage']=='floor-ready')
run=records[start:];by={r['stage']:r for r in run}
assert 'Loading Minecraft 1.16.5 with Fabric Loader 0.16.14' in client
assert 'Loaded Fabric development mappings' not in client,'Must use packaged normal launch, not named dev classes'
assert not re.search(r'BOUNDARY_FAIL|REAL_PORTAL_FAIL|\[BelowZero\] REJECTED|Mixin apply for mod belowzero failed',client)
assert abs(by['floor-ready']['y']+2)<.01 and by['floor-ready']['onGround']
assert by['floor-ready']['world']=='minecraft:overworld'
a,b=by['floor-ready'],by['walked'];distance=math.hypot(a['x']-b['x'],a['z']-b['z']);assert distance>1 and b['onGround']
assert by['broken']['target']=='AIR' and by['placed']['target']=='COBBLESTONE'
assert 'client placed=Block{minecraft:cobblestone}' in client
for stage,y in [('bottom',-63),('seam',0),('upper',256),('top',318),('end',8),('return',-2)]:
 assert 'BOUNDARY_PASS '+stage+' ' in client
 r=by[stage+'-verified'];assert r['onGround'] and abs(r['y']-y)<.01
 assert r['world']==('minecraft:the_end' if stage=='end' else 'minecraft:overworld')
portal=re.search(r'REAL_PORTAL cause=END_PORTAL fromY=(-?[\d.]+) fromWorld=minecraft:overworld toWorld=minecraft:the_end',server)
assert portal and float(portal.group(1))<0
assert 'REAL_PORTAL_PASS' in client and by['portal-verified']['world']=='minecraft:the_end'
for name in ['below-zero-proof.png','boundary-bottom.png','boundary-top.png','portal-before-entry.png','portal-arrival.png']:
 assert (e/name).stat().st_size>10000,name
control=(e/'final-control-client.log').read_text();control_server=(e/'final-control-server.log').read_text()
assert 'CONTROL inactive floor=Block{minecraft:void_air}' in control
assert '[BelowZero] ACTIVE' not in control
control_record=[v for v in records if v['stage']=='control-floor'][-1]
assert control_record['world']=='minecraft:overworld' and abs(control_record['y']+2)<.01
assert 'BZProbe has the following entity data: [8.5d, -2.0d, 8.5d]' in control_server
manifest=json.loads((root/'manifest.json').read_text())
for name,data in manifest['artifacts'].items():
 assert hashlib.sha256((root/'release'/name).read_bytes()).hexdigest()==data['sha256']
execution=json.loads((e/'execution-manifest.json').read_text())
for key in ['client_jar','server_jar']:
 recorded=execution[key];assert recorded['sha256']==manifest['artifacts'][recorded['name']]['sha256']
for local in [root/'production-run/mods/below-zero-client-0.1.0-experimental.jar',root/'test-server/plugins/BelowZeroBridge-0.1.0-experimental.jar']:
 if local.exists():assert hashlib.sha256(local.read_bytes()).hexdigest()==manifest['artifacts'][local.name]['sha256']
results={'disabled_control_same_overworld_floor':'void_air on client, stone verified on server','result' :'pass','scope':'1.16.5 Fabric + Paper 26.3 build 230, local synthetic world','normal_packaged_jar_launch':True,'negative_y_render_and_collision':True,'walk_distance_blocks':distance,'native_break_and_place_server_confirmed':True,'verified_player_y':[-63,0,256,318],'end_and_return_dimension_switch':True,'real_end_portal_from_y':float(portal.group(1)),'production_pi_modified':False,'all_versions_supported':False,'unverified':['Lunar/Forge/OptiFine/Sodium compatibility','friend client and real stronghold','general block entity NBT and signage','fluids/redstone/pistons/vehicles/mobs end-to-end','long-run performance and multiple players','all Java versions and Bedrock']}
# Initial-preview results above remain historical, tied to their original JAR hash.
historical=results
compat=(e/'preview2-compat-client.log').read_text()
current=[json.loads(line) for line in (e/'preview2-compat-results.jsonl').read_text().splitlines()]
assert [r['scenario'] for r in current]==['vanilla-first','companion','vanilla-after-companion','companion-return','disconnect']
assert all(r['result']=='PASS' for r in current)
assert 'ALL_SCENARIOS_PASS' in compat
assert 'Loading Minecraft 1.16.5 with Fabric Loader 0.16.14' in compat
assert 'Loaded Fabric development mappings' not in compat
assert not re.search(r'YSpan COMPAT.*FAIL|Mixin apply.*failed|Companion data rejected',compat)
for r in current:
 if r['scenario'] in ['vanilla-first','vanilla-after-companion','disconnect']:
  assert not r['active'] and not r['advertised'] and r['handshakes']==0 and r['sections']==0
  assert r['min_y']==0 and r['max_y']==256
 else:
  assert r['active'] and r['advertised'] and r['handshakes']==1 and r['sections']>0
  assert r['y']==-2 and r['on_ground']
assert (e/'preview2-compat-server.log').read_text().count('target=COBBLESTONE onGround=true')==2
assert 'Negotiation checks passed: 20' in (e/'preview2-build.log').read_text()
assert 'BUILD SUCCESSFUL' in (e/'preview2-build.log').read_text()
for name,digest in manifest['sources'].items():
 assert hashlib.sha256((root/name).read_bytes()).hexdigest()==digest,name
assert manifest['artifacts']['BelowZeroBridge-0.1.0-experimental.jar']==json.loads((e/'preview1-manifest.json').read_text())['artifacts']['BelowZeroBridge-0.1.0-experimental.jar']
regression=(e/'preview2-regression-client.log').read_text()
for stage in ['bottom','seam','upper','top','end','return']:
 assert 'BOUNDARY_PASS '+stage+' ' in regression
assert 'REAL_PORTAL_PASS' in regression
assert not re.search(r'BOUNDARY_FAIL|REAL_PORTAL_FAIL|Companion data rejected|Mixin apply.*failed',regression)
assert 'target=AIR' in regression and 'target=COBBLESTONE' in regression
results={'release':'v0.1.0.preview.2','result':'pass','current':{'normal_packaged_client':True,'negotiation_checks':20,'scenarios':[r['scenario'] for r in current],'no_unsolicited_vanilla_handshakes':True,'vanilla_after_companion_clears_state':True,'negative_y_native_break_place':True,'height_boundaries_and_end_portal_regression':True,'server_companion_unchanged':True},'historical_preview1':historical,'unverified':['third-party production servers and anti-cheat','singleplayer gameplay','live unregister / malformed companion / silent companion','other mods and launchers','other Minecraft versions and Bedrock','long-running or multiplayer performance']}
(root/'verification.json').write_text(json.dumps(results,ensure_ascii=False,indent=2)+'\n')
print(json.dumps(results,ensure_ascii=False,indent=2))
