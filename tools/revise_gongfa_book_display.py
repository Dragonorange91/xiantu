"""Update existing native book display slots without rebuilding approved geometry."""
import json
from blockbench_bridge import Bridge
from build_building_openings_blockbench import save_export
from build_gongfa_book_models import ASSETS, DISPLAY, write
from generate_gongfa_books import OUT, catalog


def main():
    bridge = Bridge()
    schemas = {t['name']: t for t in bridge.request('tools/list', {})['tools']}
    assert 'risky_eval' in schemas and 'export_model' in schemas
    projects = bridge.value(bridge.call('risky_eval', code='ModelProject.all.map(p=>({name:p.name,uuid:p.uuid}))'))
    current = bridge.value(bridge.call('get_project_info'))['project']['uuid']
    for entry in catalog():
        candidates = [p for p in projects if p['name'] == '线装功法_' + entry['name']]
        assert len(candidates) == 1, (entry['id'], candidates)
        uuid = candidates[0]['uuid']
        code = '(()=>{const p=ModelProject.all.find(p=>p.uuid===' + json.dumps(uuid) + ');p.select();Undo.initEdit({display_slots:Object.keys(' + json.dumps(DISPLAY) + ')});Object.entries(' + json.dumps(DISPLAY) + ').forEach(([k,v])=>{Project.display_settings[k]=new DisplaySlot(k,v);});Undo.finishEdit("Face book cover toward player");return {uuid:Project.uuid,name:Project.name};})()'
        result = bridge.value(bridge.call('risky_eval', code=code))
        assert result['uuid'] == uuid, result
        save_export(bridge, 'project', OUT / 'models' / (entry['id'] + '.bbmodel'))
        for directory in (OUT / 'models', ASSETS / 'models/item'):
            path = directory / (entry['id'] + '.json')
            model = json.loads(path.read_text(encoding='utf-8'))
            model['display'] = DISPLAY
            write(path, model)
        print('UPDATED', entry['id'], flush=True)
    bridge.call('risky_eval', code='ModelProject.all.find(p=>p.uuid===' + json.dumps(current) + ').select()')


if __name__ == '__main__':
    main()
