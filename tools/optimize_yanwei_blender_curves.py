"""Run inside Blender through MCP after opening yanwei_fox_baseline.blend.

Only reduces linear scalar FCurve keys. Channels, bone IDs, and loop endpoints
remain present. The separate pack/audit steps preserve production mesh bytes.
"""
import json
import pathlib

import bpy

OUT = pathlib.Path('asset_sources/yanwei-fox-optimize-20261015')


def retain(points, epsilon):
    keep = {0, len(points) - 1}
    stack = [(0, len(points) - 1)]
    while stack:
        start, end = stack.pop()
        x0, y0 = points[start]
        x1, y1 = points[end]
        worst, index = 0.0, None
        for i in range(start + 1, end):
            x, y = points[i]
            expected = y0 + (y1 - y0) * (x - x0) / (x1 - x0)
            error = abs(y - expected)
            if error > worst:
                worst, index = error, i
        if index is not None and worst > epsilon:
            keep.add(index)
            stack.extend(((start, index), (index, end)))
    return keep


def main():
    assert len(bpy.data.objects['SmartRigArmature'].data.bones) == 43
    report = []
    armature = bpy.data.objects['SmartRigArmature']
    for action in bpy.data.actions:
        if not action.name.startswith('yanwei_fox.'):
            continue
        before = after = 0
        arm_slot = next(s.action_slot for track in armature.animation_data.nla_tracks
                        for s in track.strips if s.action == action)
        # Simplify only armature curves; root-motion keys are retained verbatim.
        for layer in action.layers:
            for strip in layer.strips:
                for slot in action.slots:
                    if slot != arm_slot:
                        continue
                    bag = strip.channelbag(slot)
                    if not bag:
                        continue
                    for curve in bag.fcurves:
                        points = [(float(k.co.x), float(k.co.y)) for k in curve.keyframe_points]
                        before += len(points)
                        if len(points) > 2 and all(k.interpolation == 'LINEAR' for k in curve.keyframe_points):
                            epsilon = 5e-5 if 'rotation' in curve.data_path else 1e-5
                            keep = retain(points, epsilon)
                            for i in range(len(points) - 1, -1, -1):
                                if i not in keep:
                                    curve.keyframe_points.remove(curve.keyframe_points[i], fast=True)
                            curve.update()
                            assert max(abs(curve.evaluate(x) - y) for x, y in points) <= epsilon + 1e-6
                        after += len(curve.keyframe_points)
        report.append({'action': action.name, 'keys_before': before, 'keys_after': after})
    for obj in bpy.context.selected_objects:
        obj.select_set(False)
    for name in ('FoxAnimationRoot', 'SmartRigArmature', 'Mesh_0'):
        bpy.data.objects[name].select_set(True)
    bpy.context.view_layer.objects.active = bpy.data.objects['SmartRigArmature']
    bpy.ops.wm.save_as_mainfile(filepath=str(OUT / 'yanwei_fox_optimized_candidate.blend'))
    bpy.ops.export_scene.gltf(filepath=str(OUT / 'yanwei_fox_blender_export.glb'), export_format='GLB',
                              use_selection=True, export_animations=True, export_animation_mode='NLA_TRACKS',
                              export_skins=True, export_force_sampling=False, export_optimize_animation_size=True)
    (OUT / 'curve-reduction.json').write_text(json.dumps(report, indent=2), encoding='utf-8')
    print(json.dumps(report))


if __name__ == '__main__':
    main()
