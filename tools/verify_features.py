#!/usr/bin/env python3
from pathlib import Path
import re,sys,xml.etree.ElementTree as ET
root=Path(__file__).resolve().parents[1]; src=root/'app/src/main/java'; cat=src/'com/yara/ai/features/FeatureCatalog.kt'; keys=re.findall(r'YaraFeature\("([a-z0-9_]+)"',cat.read_text()); catalog=cat.read_text(); files=[p for p in src.rglob('*.kt') if p!=cat]; failed=False
print('FEATURE COVERAGE')
for key in keys:
 n=sum(len(re.findall(r'"'+re.escape(key)+r'"',p.read_text())) for p in files); print(f'{key}: {n}')
 if n==0: failed=True
print('FEATURE_COVERAGE_FAIL='+str(failed)); print('CATALOG_ENTRY_COUNT='+str(len(keys)))
# data class constructor properties are explicit and non-empty
for p in src.rglob('*.kt'):
 text=p.read_text()
 for m in re.finditer(r'data\s+class\s+(\w+)\s*\((.*?)\)',text,re.S):
  missing=[]
  for prop in re.findall(r'\b(?:val|var)\s+(\w+)\s*:',m.group(2)):
   if not re.search(r'\b(?:val|var)\s+'+re.escape(prop)+r'\s*:',m.group(2)): missing.append(prop)
  print(f'DATA_CLASS {m.group(1)}: missing={missing}')
  failed |= bool(missing)
try: ET.parse(root/'app/src/main/AndroidManifest.xml'); print('XML_OK=True')
except Exception as e: print('XML_OK=False '+str(e)); failed=True
manifest=(root/'app/src/main/AndroidManifest.xml').read_text(); kt='\n'.join(p.read_text() for p in src.rglob('*.kt'))
for token in ['ExposedDropdownMenuBox','menuAnchor',r'\bDivider\s*\(','LocalLifecycleOwner','onActivityResult','GlobalScope']:
 hit=bool(re.search(token,kt)); print(f'FORBIDDEN {token}={hit}'); failed |= hit
for required in ['.assist.LiveAssistService','.ambient.AmbientOverlayService','.ui.VideoCallActivity','.ui.ScreenShareActivity']:
 print('MANIFEST_'+required.replace('.','_')+'='+str(required in manifest)); failed |= required not in manifest
print('SCENE_FILES='+str(all((src/'com/yara/ai/ambient'/x).exists() for x in ['SceneEngine.kt','SceneView.kt'])))
print('EMOTE_FILES='+str(all((src/'com/yara/ai/ambient'/x).exists() for x in ['EmoteEngine.kt','AmbientOverlayService.kt'])))
print('GAME_FILES='+str(all((src/'com/yara/ai/games'/x).exists() for x in ['GameCenterScreen.kt','TicTacToeGame.kt','ConnectFourGame.kt','LudoGame.kt','MemoryMatchGame.kt','SnakesLaddersGame.kt'])))
print('MLKIT_TRY_CATCH='+str('FaceDetection' in kt and 'catch' in kt)); print('VIDEO_SCREEN_REFERENCED='+str('VideoCallActivity' in (src/'com/yara/ai/ui/screens/ChatScreen.kt').read_text() and 'ScreenShareActivity' in (src/'com/yara/ai/ui/screens/ChatScreen.kt').read_text()))
print('RESULT='+('FAIL' if failed else 'PASS')); sys.exit(1 if failed else 0)
