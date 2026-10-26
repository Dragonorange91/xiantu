"""Run through Blender MCP; export run at 96 Hz without retiming playback."""
import bpy
from pathlib import Path

OUT=Path(r'asset_sources\yanwei-fox-run-v2-20261015')
scene=bpy.context.scene
for obj in bpy.context.selected_objects:obj.select_set(False)
strips=[]
for name in ('FoxAnimationRoot','SmartRigArmature','Mesh_0'):
    obj=bpy.data.objects[name]
    obj.select_set(True)
    if obj.animation_data:
        obj.animation_data.action=None
        for track in obj.animation_data.nla_tracks:
            track.mute=False
            track.is_solo=False
            if track.name=='yanwei_fox.run':
                st=track.strips[0]
                # frame_start changes trim/duration; frame_start_ui shifts the strip.
                st.frame_start=1;st.scale=1;st.frame_end=19
                st.action_frame_start=1;st.action_frame_end=19
                st.frame_start_ui=4;st.scale=4
                assert abs(st.frame_start-4)<1e-6 and abs(st.frame_end-76)<1e-6
                strips.append(st)
bpy.context.view_layer.objects.active=bpy.data.objects['SmartRigArmature']
scene.render.fps=96
try:
    bpy.ops.export_scene.gltf(filepath=str(OUT/'yanwei_fox_blender_export.glb'),export_format='GLB',
        use_selection=True,export_animations=True,export_animation_mode='NLA_TRACKS',
        export_skins=True,export_force_sampling=False,export_optimize_animation_size=True)
finally:
    for st in strips:
        st.scale=1;st.frame_start_ui=1
        assert abs(st.frame_end-19)<1e-6
    scene.render.fps=24
    for name in ('FoxAnimationRoot','SmartRigArmature'):
        for track in bpy.data.objects[name].animation_data.nla_tracks:
            track.mute=track.name!='yanwei_fox.run'
    scene.frame_set(1)
    bpy.ops.wm.save_as_mainfile(filepath=str(OUT/'yanwei_fox_run_v2.blend'))
print('96Hz export complete; 24fps authoring and 0.75-second gait restored')
