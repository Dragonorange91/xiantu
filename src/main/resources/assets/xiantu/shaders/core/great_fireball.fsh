#version 150
uniform float Time;
uniform float Layer;
uniform float Opacity;
in vec3 localPosition;
in vec3 viewPosition;
in vec3 viewNormal;
out vec4 fragColor;
// Original, textureless solar convection evaluated in object space: no UV seam.
float hash(vec3 p){p=fract(p*.3183099+vec3(.11,.37,.73));p*=17.0;return fract(p.x*p.y*p.z*(p.x+p.y+p.z));}
float noise(vec3 p){vec3 i=floor(p),f=fract(p);f=f*f*(3.0-2.0*f);return mix(mix(mix(hash(i),hash(i+vec3(1,0,0)),f.x),mix(hash(i+vec3(0,1,0)),hash(i+vec3(1,1,0)),f.x),f.y),mix(mix(hash(i+vec3(0,0,1)),hash(i+vec3(1,0,1)),f.x),mix(hash(i+vec3(0,1,1)),hash(i+vec3(1,1,1)),f.x),f.y),f.z);}
float fbm(vec3 p){float sum=0.0,weight=.55;for(int i=0;i<4;i++){sum+=noise(p)*weight;p=p*2.07+vec3(7.3,2.8,5.1);weight*=.5;}return sum;}
void main(){
    vec3 p=normalize(localPosition);float t=Time*.12;
    float angle=t*.3;mat2 rot=mat2(cos(angle),-sin(angle),sin(angle),cos(angle));p.xz=rot*p.xz;
    vec3 warp=vec3(fbm(p*3.2+vec3(t,0,0)),fbm(p*3.2+vec3(0,t,7)),fbm(p*3.2+vec3(4,0,t)))-.5;
    float broad=fbm(p*5.5+warp*2.2-vec3(0,t*.7,0));
    float cells=fbm(p*27.0+warp*4.0+vec3(t*.8,-t,0));
    float fine=noise(p*81.0+warp*7.0-vec3(0,t*2.0,t));
    float heat=clamp(broad*.58+cells*.42+(fine-.5)*.11,0.0,1.0);
    float dark=smoothstep(.23,.40,broad);
    float vein=pow(clamp(1.0-abs(cells-.51)*7.0,0.0,1.0),2.0);
    vec3 color=mix(vec3(.32,.018,.001),vec3(1.0,.14,.002),smoothstep(.20,.46,heat));
    color=mix(color,vec3(1.0,.55,.008),smoothstep(.40,.59,heat));
    color=mix(color,vec3(1.0,.88,.22),smoothstep(.54,.71,heat)*.88);
    color+=vec3(.10,.095,.018)*vein*dark;
    float facing=abs(dot(normalize(viewNormal),normalize(-viewPosition)));
    color*=.70+.30*pow(facing,.22);
    if(Layer>.5){float rim=pow(1.0-facing,2.5);float turbulence=.55+.45*fbm(p*13.0+vec3(t*3.0));fragColor=vec4(1.0,.26+.14*cells,.008,rim*turbulence*.40*Opacity);}
    else fragColor=vec4(color,Opacity);
}
