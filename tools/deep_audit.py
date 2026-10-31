#!/usr/bin/env python3
"""Static audit for the Yara Android/Kotlin source tree."""
from __future__ import annotations
import argparse, re, sys
from pathlib import Path
from collections import defaultdict
import xml.etree.ElementTree as ET

DECL_RE = re.compile(r'(?m)\b(?:(?:public|private|protected|internal|data|sealed|open|abstract|final)\s+)*(class|interface|object)\s+([A-Za-z_]\w*)')
FUNCTION_RE = re.compile(r'(?m)\bfun\s+(?:<[^>]+>\s*)?([A-Za-z_]\w*)\s*\(')
PACKAGE_RE = re.compile(r'(?m)^\s*package\s+([\w.]+)')
IMPORT_RE = re.compile(r'(?m)^\s*import\s+([\w.]+)(?:\s+as\s+\w+)?')
COMPANION_RE = re.compile(r'(?m)\bcompanion\s+object\b')
PROPERTY_RE = re.compile(r'\b(?:val|var)\s+([A-Za-z_]\w*)\s*(?::\s*([A-Za-z_]\w*))?')
COMPONENT_TYPES = ('activity', 'service', 'receiver', 'provider')


def mask_kotlin(s: str) -> str:
    """Replace comments and string contents with spaces, retaining newlines/positions."""
    out = list(s); i = 0; n = len(s)
    while i < n:
        if s.startswith('//', i):
            j = s.find('\n', i)
            if j < 0: j = n
            for k in range(i, j): out[k] = ' '
            i = j; continue
        if s.startswith('/*', i):
            j = s.find('*/', i + 2); j = n if j < 0 else j + 2
            for k in range(i, j):
                if out[k] != '\n': out[k] = ' '
            i = j; continue
        if s.startswith('"""', i):
            j = s.find('"""', i + 3); j = n if j < 0 else j + 3
            for k in range(i, j):
                if out[k] != '\n': out[k] = ' '
            i = j; continue
        if s[i] == '"':
            j = i + 1
            while j < n:
                if s[j] == '\\': j += 2; continue
                if s[j] == '"': j += 1; break
                j += 1
            for k in range(i, min(j, n)):
                if out[k] != '\n': out[k] = ' '
            i = j; continue
        i += 1
    return ''.join(out)


def brace_pairs(masked):
    stacks = {'{': [], '(': [], '[': []}; pairs = {}; errors = []
    close = {'}':'{', ')':'(', ']':'['}
    for i, c in enumerate(masked):
        if c in stacks: stacks[c].append(i)
        elif c in close:
            op = close[c]
            if not stacks[op]: errors.append((i, f'unmatched {c}'))
            else:
                j = stacks[op].pop(); pairs[j] = i; pairs[i] = j
    for op, st in stacks.items():
        for i in st: errors.append((i, f'unmatched {op}'))
    return pairs, errors


def line(s, pos): return s.count('\n', 0, pos) + 1

def decls(text, masked, package):
    result=[]
    for m in DECL_RE.finditer(masked):
        kind,name=m.group(1),m.group(2)
        brace=masked.find('{',m.end())
        result.append({'kind':kind,'name':name,'start':m.start(),'brace':brace,
                       'line':line(text,m.start()),'fqn':package+'.'+name})
    for m in COMPANION_RE.finditer(masked):
        brace=masked.find('{',m.end())
        if brace >= 0:
            result.append({'kind':'companion','name':'companion','start':m.start(),'brace':brace,
                           'line':line(text,m.start()),'fqn':package+'.companion'})
    return result


def enclosing_decl(ds, pos, pairs):
    candidates=[]
    for d in ds:
        end=pairs.get(d['brace'])
        if end is not None and d['brace'] < pos < end: candidates.append(d)
    return max(candidates, key=lambda d:d['brace'], default=None)


def audit(root: Path):
    issues=[]; stats=defaultdict(int)
    kt=sorted(root.rglob('*.kt'))
    files=[]
    for p in kt:
        text=p.read_text(encoding='utf-8', errors='replace'); masked=mask_kotlin(text)
        pkgm=PACKAGE_RE.search(masked); pkg=pkgm.group(1) if pkgm else None
        pairs, balance=brace_pairs(masked)
        files.append((p,text,masked,pkg,pairs,decls(text,masked,pkg or '')))
        if not pkg or pkg != 'com.yara.ai' and not pkg.startswith('com.yara.ai.'):
            issues.append((str(p),1,f'missing/invalid package com.yara.ai (found {pkg or "none"})'))
        for pos,msg in balance: issues.append((str(p),line(text,pos),f'brace/paren balance: {msg}'))
        for bad,label in [
            ('ExposedDropdownMenuBox','forbidden API ExposedDropdownMenuBox'),('menuAnchor(','forbidden API menuAnchor('),
            ('\bDivider\(','forbidden API standalone Divider('),
            ('androidx.compose.ui.platform.LocalLifecycleOwner','forbidden API LocalLifecycleOwner'),
            ('override fun onActivityResult','forbidden API onActivityResult'),('GlobalScope','forbidden API GlobalScope'),
            ('".associate { it','forbidden API ".associate { it'),('=program(','forbidden API =program('),
            ('program(VERTEX','forbidden API program(VERTEX'),('TextToSpeech(ctx){','forbidden TextToSpeech(ctx){ constructor misuse')]:
            pattern = bad if bad.startswith('\\b') else re.escape(bad)
            for m in re.finditer(pattern, masked): issues.append((str(p),line(text,m.start()),label))
        for m in re.finditer(r'(?:AmbientOverlayService\.OverlayState|\.OverlayState)', masked):
            prefix=masked[max(0,masked.rfind('\n',0,m.start())+1):m.start()]
            if not prefix.lstrip().startswith('import '):
                issues.append((str(p),line(text,m.start()),f'OverlayState reference must be top-level com.yara.ai.ambient.OverlayState: {text[m.start():m.end()]}'))
        # illegal nested named object / companion object
        for d in files[-1][5]:
            parent=enclosing_decl(files[-1][5], d['start'], pairs)
            if parent and parent['kind'] in ('object','companion') and d['kind'] in ('object','companion'):
                issues.append((str(p),d['line'],f'illegal nested {d["kind"]} {d["name"]} inside {parent["kind"]} {parent["name"]}'))
        # init references to later class properties
        for cd in files[-1][5]:
            if cd['kind'] not in ('class','object'): continue
            end=pairs.get(cd['brace']); body_start=cd['brace']+1
            if end is None: continue
            props=[]
            for pm in PROPERTY_RE.finditer(masked, body_start, end):
                # only declarations in this class body, not nested declaration bodies
                owner=enclosing_decl(files[-1][5],pm.start(),pairs)
                if owner and owner['brace'] != cd['brace']: continue
                props.append((pm.group(1),pm.start()))
            for im in re.finditer(r'\binit\s*\{', masked[body_start:end]):
                istart=body_start+im.start(); ib=masked.find('{',istart); ie=pairs.get(ib)
                if ie is None: continue
                later={n:pos for n,pos in props if pos>ie}
                for n,pos in later.items():
                    if re.search(r'\b'+re.escape(n)+r'\b',masked[ib+1:ie]):
                        issues.append((str(p),line(text,istart),f'init references property {n} declared after init (line {line(text,pos)})'))
    # declaration/import resolution
    declared={d['fqn'] for _,_,_,_,_,ds in files for d in ds if d['kind'] in ('class','interface','object')}
    # Imports may target top-level functions as well as types; generated Android R is valid.
    callable_declared=set()
    for p,text,masked,pkg,pairs,ds in files:
        callable_declared.update((pkg+'.'+m.group(1)) for m in FUNCTION_RE.finditer(masked) if pkg)
    declared |= callable_declared
    declared.add('com.yara.ai.R')
    for p,text,masked,pkg,pairs,ds in files:
        for im in IMPORT_RE.finditer(masked):
            name=im.group(1)
            if name.startswith('com.yara.ai.') and not name.endswith('.') and name.rsplit('.',1)[-1] != '*':
                if name not in declared:
                    issues.append((str(p),line(text,im.start()),f'import does not resolve to a declaration: {name}'))
    # data class property access, infer relevant receiver types
    data_props={}; type_props={}
    for p,text,masked,pkg,pairs,ds in files:
        for m in re.finditer(r'\bdata\s+class\s+([A-Za-z_]\w*)\s*\(([^)]*)\)',masked,re.S):
            name=m.group(1); props=set(x.group(1) for x in PROPERTY_RE.finditer(m.group(2)))
            data_props[name]=props; type_props[name]=props
        for d in ds:
            if d['kind']=='class':
                b=d['brace']; e=pairs.get(b,len(masked)); type_props.setdefault(d['name'],set()).update(x.group(1) for x in PROPERTY_RE.finditer(masked[b:e]))
    receivers={'provider','p','profile','chip','mem','token','particle','reminder','frame','setting'}
    for p,text,masked,pkg,pairs,ds in files:
        vars={}
        # typed declarations and constructor/function parameters
        for m in re.finditer(r'\b(?:val|var)\s+('+ '|'.join(receivers) + r')\s*:\s*([A-Za-z_]\w*)',masked): vars[m.group(1)]=m.group(2)
        for m in re.finditer(r'\b('+ '|'.join(receivers) + r')\s*:\s*([A-Za-z_]\w*)',masked): vars[m.group(1)]=m.group(2)
        for m in re.finditer(r'\b('+ '|'.join(receivers) + r')\s*=\s*([A-Za-z_]\w*)\s*\(',masked): vars.setdefault(m.group(1),m.group(2))
        for m in re.finditer(r'\b('+ '|'.join(receivers) + r')\.([A-Za-z_]\w*)',masked):
            r,prop=m.group(1),m.group(2); typ=vars.get(r)
            if typ and typ in type_props and prop not in type_props[typ] and prop not in ('javaClass',):
                issues.append((str(p),line(text,m.start()),f'unknown property access {r}.{prop} for {typ}'))
    # manifest/resources
    manifest=root/'app/src/main/AndroidManifest.xml'; manifest_text=manifest.read_text(encoding='utf-8') if manifest.exists() else ''
    manifest_pkg='com.yara.ai';
    declared_fqns=declared
    comp_manifest=[]
    if manifest.exists():
        try: mt=ET.fromstring(manifest_text)
        except ET.ParseError as e: issues.append((str(manifest),1,f'manifest XML parse error: {e}')); mt=None
        if mt is not None:
            app=next((x for x in mt if x.tag.endswith('application')),None)
            if app is not None:
                for el in app:
                    typ=el.tag.rsplit('}',1)[-1]
                    if typ in COMPONENT_TYPES:
                        raw=el.attrib.get('{http://schemas.android.com/apk/res/android}name','')
                        fqn=manifest_pkg+'.'+raw[1:].replace('/','.') if raw.startswith('.') else (raw if '.' in raw else manifest_pkg+'.'+raw)
                        comp_manifest.append((typ,fqn,el))
                        if fqn.startswith('com.yara.ai.') and fqn not in declared_fqns:
                            issues.append((str(manifest),line(manifest_text,manifest_text.find(raw)),f'manifest {typ} class missing from Kotlin declarations: {fqn}'))
            # all @resource refs in manifest
            resroot=root/'app/src/main/res'
            for m in re.finditer(r'@(?:string|xml|mipmap|layout)/([A-Za-z_]\w*)',manifest_text):
                kind,name=m.group(0).split('/')[0][1:],m.group(1)
                dirs={'string':'values','xml':'xml','mipmap':None,'layout':'layout'}
                if kind == 'string':
                    found=any(re.search(r'<string\b[^>]*\bname=[\"\']'+re.escape(name)+r'[\"\']', f.read_text(encoding='utf-8',errors='replace')) for f in (resroot/'values').glob('*.xml')) if (resroot/'values').exists() else False
                elif kind == 'mipmap':
                    found=any(f.stem==name for f in resroot.glob('mipmap-*/*') if f.is_file())
                else:
                    d=resroot/dirs[kind]
                    found=any(f.stem==name for f in d.rglob('*') if f.is_file()) if d.exists() else False
                if not found: issues.append((str(manifest),line(manifest_text,m.start()),f'manifest resource missing: @{kind}/{name}'))
    manifest_names={fqn for _,fqn,_ in comp_manifest}
    # Kotlin references to Android components in class literals
    for p,text,masked,pkg,pairs,ds in files:
        for m in re.finditer(r'\b([A-Za-z_]\w*(?:Activity|Service|Receiver))::class\.java',masked):
            n=m.group(1); candidates={d['fqn'] for d in ds if d['name']==n}
            # search all declarations
            candidates={d['fqn'] for *_,decl in files for d in decl if d['name']==n}
            fqn=next(iter(candidates),pkg+'.'+n)
            if fqn not in manifest_names:
                issues.append((str(p),line(text,m.start()),f'Kotlin component reference not declared in manifest: {fqn}'))
    # R resource references
    resroot=root/'app/src/main/res'
    for p,text,masked,pkg,pairs,ds in files:
        for m in re.finditer(r'(?<![\w.])R\.(layout|id|mipmap|drawable)\.([A-Za-z_]\w*)',masked):
            kind,name=m.group(1),m.group(2); dirs={'layout':'layout','id':None,'mipmap':None,'drawable':'drawable'}; ok=False
            if kind=='id':
                # IDs can be declared in any XML resource, including inline @+id.
                ok=any(re.search(r'@\+?id/'+re.escape(name)+r'\b',f.read_text(encoding='utf-8',errors='replace')) for f in resroot.rglob('*') if f.is_file())
            else:
                if kind == 'mipmap': ok=any(f.stem==name for f in resroot.glob('mipmap-*/*') if f.is_file())
                else:
                    d=resroot/dirs[kind]; ok=any(f.stem==name for f in d.rglob('*') if f.is_file()) if d.exists() else False
            if not ok: issues.append((str(p),line(text,m.start()),f'Kotlin resource missing: R.{kind}.{name}'))
    # sort and format
    issues.sort(key=lambda x:(x[0],x[1],x[2]))
    print('YARA AI LEGENDARY DEEP AUDIT')
    print(f'Root: {root}')
    print(f'Kotlin files scanned: {len(kt)}')
    print(f'Declarations indexed: {len(declared)}')
    print()
    if issues:
        print(f'FAIL ({len(issues)} issue(s))')
        for f,l,msg in issues: print(f'{f}:{l}: {msg}')
    else: print('PASS (0 issues)')
    return 1 if issues else 0

if __name__ == '__main__':
    ap=argparse.ArgumentParser(); ap.add_argument('root', nargs='?', default=Path(__file__).resolve().parents[1], type=Path)
    args=ap.parse_args(); raise SystemExit(audit(args.root.resolve()))
