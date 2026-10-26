"""Archived initial generator. Do not overwrite the reviewed authoring script."""
raise SystemExit('Retired: use author_mountain_bear.py through Blender MCP. This template predates the reviewed anatomical weights.')
from pathlib import Path
r=Path(__file__).resolve().parent
s=(r/'author_moss_bear.py').read_text(encoding='utf8')
s=s.replace("# Remove scene objects only.","assert bpy.context.scene.name == 'MountainBearAuthoring'\n# Remove scene objects only.")
s=s.replace('苔背熊','镇岳熊').replace('tai_bei_xiong','zhen_yue_xiong').replace('tai-bei-xiong','zhen-yue-xiong')
s=s.replace('math.radians(43)','math.radians(20)').replace('.351807','.294189')
start=s.index("bone('root'");end=s.index("for name,points in legs.items():",start)
s=s[:start]+'''bone('root',(0,0,-.294189),(0,0,-.20)).use_deform=False
bone('hips',(0,.25,-.005),(0,.12,.045),'root')
bone('spine',(0,.12,.045),(0,-.015,.085),'hips')
bone('chest',(0,-.015,.085),(0,-.16,.105),'spine')
bone('neck',(0,-.16,.105),(.025,-.30,.105),'chest')
bone('head',(.025,-.30,.105),(.075,-.445,.08),'neck')
legs={
 'front_upper.L':((-.19,-.09,.07),(-.22,-.17,-.09),(-.205,-.25,-.255),(-.205,-.33,-.278)),
 'front_upper.R':((.19,-.09,.07),(.22,-.18,-.09),(.19,-.27,-.255),(.19,-.35,-.278)),
 'hind_upper.L':((-.145,.25,-.005),(-.18,.30,-.15),(-.17,.30,-.26),(-.17,.21,-.278)),
 'hind_upper.R':((.145,.24,-.005),(.18,.29,-.15),(.17,.28,-.26),(.17,.19,-.278)),
}
'''+s[end:]
s=s.replace("tail=[(0,.33,-.02),(0,.41,-.03),(0,.49,-.045),(0,.57,-.05),(0,.64,-.055)]","tail=[(0,.35,-.03),(0,.385,-.04),(0,.42,-.055),(0,.455,-.07),(0,.49,-.08)]")
s=s.replace("if p.y < -.30 and p.z > -.025:","if p.y < -.31 and p.z > -.045:")
s=s.replace("elif p.z > .15 and p.y > -.22:","elif p.z > .15 and p.y > -.21:")
s=s.replace("('walk','walk',.32),('run','run',.38)","('walk','walk',.28),('run','run',.34)")
s=s.replace('unit=.71','unit=.585')
s=s.replace("'torso_yaw_degrees':43,'head_yaw_degrees':-43,'target_height_blocks':2.1","'torso_yaw_degrees':20,'head_yaw_degrees':-20,'target_height_blocks':2.5")
(r/'author_mountain_bear.py').write_text(s,encoding='utf8')
p=(r/'render_rock_boar.py').read_text(encoding='utf8').replace('rock-boar-previews','mountain-bear-previews').replace('yan_bei_ye_zhu','zhen_yue_xiong').replace("('windup','windup',18","('slam','slam',32")
p=p.replace('cd.ortho_scale=1.6','cd.ortho_scale=1.45')
(r/'render_mountain_bear.py').write_text(p,encoding='utf8')
