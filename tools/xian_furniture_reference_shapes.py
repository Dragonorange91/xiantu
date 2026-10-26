"""Geometry from the six user references, authored as native Java cubes via MCP.
Round forms use disjoint narrow bands, never overlapping coplanar disks.
"""
import math

def box(n,a,b,t='wood'):
    sizes=[b[i]-a[i]for i in range(3)]
    faces={f:{'uv':[0,0,min(16,w),min(16,h)],'texture':t+'.png'}for f,w,h in [('north',sizes[0],sizes[1]),('south',sizes[0],sizes[1]),('east',sizes[2],sizes[1]),('west',sizes[2],sizes[1]),('up',sizes[0],sizes[2]),('down',sizes[0],sizes[2])]}
    return {'name':n,'from':a,'to':b,'box_uv':False,'autouv':0,'faces':faces}

def cylinder(g,n,cx,cz,r,y,Y,t='stone',step=.5):
    count=math.ceil(2*r/step)
    for k in range(count):
        z=cz-r+2*r*k/count;Z=cz-r+2*r*(k+1)/count
        dx=math.sqrt(max(0,r*r-((z+Z)/2-cz)**2))
        g.append(box(n+str(k),[cx-dx,y,z],[cx+dx,Y,Z],t))

def ring(g,n,cx,cy,r,width,z,Z,t='edge',step=.5):
    for k in range(math.ceil(2*r/step)):
        y=cy-r+k*step;Y=min(cy+r,y+step);dy=(y+Y)/2-cy
        outer=math.sqrt(max(0,r*r-dy*dy));inner=math.sqrt(max(0,(r-width)**2-dy*dy))
        if inner<.01:g.append(box(n+str(k),[cx-outer,y,z],[cx+outer,Y,Z],t))
        else:
            for side in [-1,1]:g.append(box(n+str(k)+str(side),[cx-outer if side<0 else cx+inner,y,z],[cx-inner if side<0 else cx+outer,Y,Z],t))

def chair():
    g=[]
    for x in [2,12]:
        for z in [2,12]:
            g.extend([box('leg',[x,0,z],[x+1.5,9,z+1.5]),box('foot',[x-.25,0,z-.25],[x+1.75,.8,z+1.75],'edge')])
    g.extend([box('seat_frame',[1,8.6,1],[15,9.7,15],'edge'),box('seat',[2,9.7,2],[14,10.2,14]),box('front_apron',[2,7,1.6],[14,8.6,2.5],'edge'),box('stretcher',[3,2,2.2],[13,2.8,3.1],'edge')])
    for x in [2.2,12.7]:
        g.append(box('back_post',[x,10,12.6],[x+1.1,24.5,13.7],'edge'))
    # A broad carved central splat and curved crest, like the supplied Ming chair.
    splat=box('carved_splat',[6.3,10,12.8],[9.7,24.7,13.5],'wood')
    # The whole tall panel has its own complete artwork, never a cropped cabinet corner.
    for f in ['north','south']:splat['faces'][f]={'uv':[0,0,16,16],'texture':'chair_carving.png'}
    g.append(splat)
    for k in range(14):
        x=1+k;y=25.5-.025*(x-8)**2
        g.append(box('curved_crest'+str(k),[x,y,12.2],[x+1.08,y+.9,13.8],'edge'))
    for x in [1.3,13.5]:
        for k in range(8):
            z=3+k*1.2;y=15.3+.038*(z-7)**2
            g.append(box('curved_arm',[x,y,z],[x+1.2,y+.8,z+1.2],'edge'))
        for k in range(8):
            y=10.2+k*.64;z=3+1.1*math.sin(k*math.pi/7)
            g.append(box('s_arm_support',[x,y,z],[x+1.1,y+.64,z+1.1],'edge'))
    for x in [2,13]:g.append(box('side_stretcher',[x,3,3],[x+1,3.8,13],'edge'))
    return g

def stone_table():
    g=[]
    cylinder(g,'base',16,16,8,0,1.1)
    cylinder(g,'bevel',16,16,7.3,1.1,2)
    cylinder(g,'pedestal',16,16,4.5,2,10.4)
    cylinder(g,'neck',16,16,6.8,10.4,11.6)
    cylinder(g,'round_top',16,16,15.6,11.6,13.6)
    cylinder(g,'top_bevel',16,16,15.25,13.6,14)
    return g

def stool():
    g=[]
    # A low seat, approximately half the 14-unit tabletop height.
    for n,r,y,Y in [('base',5.2,0,.8),('body',5,.8,6.3),('groove',4.8,6.3,6.6),('seat',5.5,6.6,7.5)]:cylinder(g,n,8,8,r,y,Y)
    return g

def lantern():
    g=[]
    # Amber octagonal glazed-paper body, dark wooden eight-rib frame.
    for z,Z,w in [(3,4,3),(4,5,4),(5,11,5),(11,12,4),(12,13,3)]:g.append(box('luminous_paper',[8-w,3,z],[8+w,12.5,Z],'amber'))
    for x,z in [(3,5),(3,10.3),(4.4,3.7),(10.9,3.7),(12.3,5),(12.3,10.3),(4.4,12),(10.9,12)]:g.append(box('wood_rib',[x,2.5,z],[x+.7,13,z+.7],'edge'))
    for y in [2.4,3.4,11.7,12.7]:
        for z,Z,w in [(2.6,3.5,3),(3.5,4.5,4.3),(4.5,11.5,5.5),(11.5,12.5,4.3),(12.5,13.4,3)]:g.append(box('rim',[8-w,y,z],[8+w,y+.45,Z],'edge'))
    # Recognisable geometric fretwork; repeated bars remain a single static model.
    for z in [2.8,12.7]:
        for y in [4,10.3]:
            for x in [5,7.4,9.8]:
                g.extend([box('fret_u',[x,y,z],[x+.35,y+1.1,z+.45],'edge'),box('fret_top',[x,y+1,z],[x+1.7,y+1.35,z+.45],'edge'),box('fret_return',[x+1.35,y+.5,z],[x+1.7,y+1,z+.45],'edge')])
    for y,lo,hi in [(13,1.8,14.2),(13.5,3,13),(14,4.4,11.6),(14.5,6,10)]:g.append(box('pagoda_cap',[lo,y,lo],[hi,y+.5,hi],'edge'))
    for x in [1.2,13.5]:
        for z in [1.2,13.5]:g.append(box('upturned_corner',[x,13.4,z],[x+1.3,14.3,z+1.3],'edge'))
    g.extend([box('top_hook',[7.4,15,7.4],[8.6,16,8.6],'gold'),box('lower_foot',[5,1,5],[11,2.4,11],'edge'),box('pendant',[7,0,7],[9,1.2,9],'gold')])
    return g

def wardrobe():
    g=[]
    for x in [1,28]:
        for z in [2,11]:g.append(box('cabinet_leg',[x,0,z],[x+2.2,4,z+2.2]))
    g.extend([box('case_back',[2,3,12],[30,30,13.4]),box('left_stile',[1,3,1.8],[3,30,13]),box('right_stile',[29,3,1.8],[31,30,13]),box('bottom',[1,3,1],[31,4.8,13.7],'edge'),box('crown',[0,30,1],[32,31.5,14],'edge'),box('cornice',[.6,29.2,1.5],[31.4,30,13.5])])
    for x in [3.2,16.2]:
        g.extend([box('drawer',[x,4.8,1.5],[x+12.3,8.5,3],'edge'),box('drawer_inset',[x+.7,5.4,1.3],[x+11.6,7.9,1.6]),box('drawer_handle',[x+5,6,1],[x+7.2,6.7,1.5],'gold')])
    for x in [3.2,16.2]:
        g.append(box('door',[x,9,1.9],[x+12.3,29,3.3],'edge'))
        c=box('carved_door',[x+.8,9.8,1.7],[x+11.5,28.2,2.1],'carving');c['faces']['north']['uv']=[0,0,16,16];g.append(c)
        for xx in [x+.4,x+11.5]:g.append(box('door_bead',[xx,9.4,1.5],[xx+.35,28.6,1.9],'wood'))
        for yy in [9.4,28.2]:g.append(box('door_bead',[x+.4,yy,1.5],[x+11.8,yy+.35,1.9],'wood'))
    for x in [14.1,17]:g.extend([box('handle_plate',[x,17,1.1],[x+.8,21,1.6],'gold'),box('handle',[x,18.2,.6],[x+.8,19.3,1.2],'edge')])
    return g

def bookcase():
    g=[]
    for x in [0.8,29.8]:
        for z in [2,10]:g.append(box('leg',[x,0,z],[x+1.4,3,z+1.4],'edge'))
    g.extend([box('cabinet_base',[.5,2,1],[31.5,3.3,12.5]),box('top',[0,31,1],[32,32,12.5],'edge')])
    for x in [1,30]:g.append(box('upright',[x,3,2],[x+1,31,12],'edge'))
    # Slatted base cupboards, open upper shelf with a moon-gate frame.
    for x in [2,12,22]:
        g.append(box('base_cupboard',[x,3.3,3],[x+9.5,9,11]))
        for k in range(9):g.append(box('slat',[x+.2+k,3.6,2.5],[x+.6+k,8.6,3],'edge'))
    for y in [9,15,22,30]:g.append(box('shelf',[2,y,2],[30,y+.65,12],'edge'))
    for x,y,Y in [(7,9.7,15),(22,9.7,15),(12,15.7,22),(26,22.7,30),(6,22.7,30),(18,22.7,30)]:g.append(box('asymmetric_partition',[x,y,4],[x+.6,Y,12],'edge'))
    ring(g,'moon_gate',16,20,10.6,.8,1.1,2.2)
    for x,y,count in [(3,15.7,4),(21,15.7,5),(9,9.7,3)]:
        for k in range(count):g.append(box('book',[x+k*.7,y,6],[x+k*.7+.55,y+3.6+(k%2)*.4,10],'book'+str(k%3)))
    for x,y,z in [(4,23,7),(24,9.7,7),(20,23,7)]:
        cylinder(g,'vase_foot',x,z,1,y,y+.7,'jade');cylinder(g,'vase',x,z,1.4,y+.7,y+2.5,'jade');cylinder(g,'vase_neck',x,z,.65,y+2.5,y+3.6,'jade')
    g.extend([box('scrolls',[14,23,7],[17,24.4,10],'paper'),box('scrolls',[14.7,24.4,7],[16.4,25.4,10],'paper')])
    return g

def reference_models(existing):
    replacements={'xian_wood_chair':('中式高背圈椅',chair()),'xian_red_lantern':('中式宫灯',lantern()),'xian_round_stone_table':('圆石桌',stone_table()),'xian_stone_stool':('圆柱石凳',stool()),'xian_bookshelf':('中式月洞书架',bookcase())}
    result=[(id,*replacements[id])if id in replacements else(id,name,cubes)for id,name,cubes in existing]
    result.append(('xian_wood_wardrobe','中式双门衣柜',wardrobe()))
    return result
