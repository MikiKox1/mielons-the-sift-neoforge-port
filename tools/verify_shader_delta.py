"""Exact final-JAR delta against its protected pre-shader counterpart."""
from pathlib import Path
from zipfile import ZipFile
import hashlib
import json

ROOT=Path(__file__).resolve().parents[1]


def digest(p):
    with p.open('rb') as f:return hashlib.file_digest(f,'sha256').hexdigest()


def verify(jar):
    plan=json.loads((ROOT/'SHADER-INTEGRATION-RESOURCES.json').read_text(encoding='utf8'))
    base=ROOT.parents[1]/'artifacts/builds'/plan['base_jar']
    assert digest(base)==plan['base_jar_sha256'],'Protected baseline JAR changed'
    with ZipFile(base) as old, ZipFile(jar) as new:
        before={n for n in old.namelist() if not n.endswith('/')}
        after={n for n in new.namelist() if not n.endswith('/')}
        assert before <= after,'Unexpected removed artifact entries'
        changed={n for n in before if old.read(n)!=new.read(n)}
        added=after-before
        allowed_classes=set(plan['allowed_client_classes'])
        for name in (changed|added):
            if name.endswith('.class'):
                assert name in allowed_classes or name.split('$')[0]+'.class' in allowed_classes, 'Unexpected production change: '+name
        allowed_other={'META-INF/neoforge.mods.toml','the_sift.client.mixins.json','SOURCE_PROVENANCE.md'}
        assert {n for n in changed if not n.endswith('.class')} <= allowed_other
        assert {n for n in added if not n.endswith('.class')} == set(plan['added_resources'])|{'SHADER-INTEGRATION-RESOURCES.json'}
        for name in before:
            if name.startswith(('assets/','data/')):
                assert old.read(name)==new.read(name),'Pre-shader resource changed: '+name
        for name,sha in plan['added_resources'].items():
            assert hashlib.sha256(new.read(name)).hexdigest()==sha,'Wrong target shader: '+name
        assert new.testzip() is None
        assert old.read('mielon/thesift/client/entity/SingerRenderer.class')==new.read('mielon/thesift/client/entity/SingerRenderer.class'),'Singer altered'
        assert old.read('mielon/thesift/world/SafePortalArrival.class')==new.read('mielon/thesift/world/SafePortalArrival.class'),'Portal fix altered'
        for forbidden in ('net/irisshaders/','net/caffeinemc/','ShaderQaAgent','world_state.dat','.mca','playerdata/'):
            assert not any(forbidden in n for n in after),'QA/dependency/world bundled: '+forbidden
        return {'status':'PASS','base_sha256':plan['base_jar_sha256'],'changed':sorted(changed),'added':sorted(added),
                'existing_resources_byte_identical':sum(n.startswith(('assets/','data/')) for n in before),
                'singer_unchanged':True,'main_portal_fix_unchanged':True}
